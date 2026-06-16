# Intégration Stripe — Guide technique

Ce document décrit comment intégrer Stripe (mode test) dans la CYNA Platform de manière propre et sécurisée, en respectant l'architecture existante (Clean Architecture, DDD, modular monolith).

---

## Flux global

```
PWA Angular                    Backend Spring Boot             Stripe
────────────                   ────────────────────            ──────

1. POST /api/v1/payments ─────────────────────────►
                               Crée PaymentIntent ──────────► Stripe API
                              ◄── { clientSecret } ◄─────────────────

2. stripe.confirmCardPayment(clientSecret)
   (données carte → Stripe directement, jamais vers le backend)
                                                   ──► Stripe traite

3.                             POST /webhook ◄───── payment_intent.succeeded
                               Vérifie signature
                               Met à jour Payment
                               Publie PaymentSucceeded
```

Le backend ne voit jamais les données de carte. Il crée un `PaymentIntent`, retourne un `clientSecret` au frontend, puis attend la confirmation de Stripe via webhook. C'est le seul flux acceptable pour rester conforme PCI DSS.

---

## Partie 1 — Backend

### 1.1 Dépendance

Ajouter le SDK Java Stripe dans `cyna-backend/build.gradle.kts` : `com.stripe:stripe-java:26.13.0`.

### 1.2 Configuration

Trois variables d'environnement à ajouter dans `.env.example` et `application.yml` :

- `STRIPE_SECRET_KEY` (`sk_test_...`) — uniquement côté backend, jamais exposé
- `STRIPE_WEBHOOK_SECRET` (`whsec_...`) — secret de signature des webhooks, généré depuis le dashboard Stripe
- `STRIPE_PUBLISHABLE_KEY` (`pk_test_...`) — clé publique, peut être transmise au frontend

Créer un `@ConfigurationProperties` `StripeProperties` pour injecter ces valeurs proprement.

### 1.3 Adaptation du `PaymentGatewayPort`

Le port existant doit être adapté : `initiatePayment` ne retourne plus un résultat de succès/échec immédiat, mais un `clientSecret` + l'ID du `PaymentIntent` Stripe (`pi_...`). Ce `clientSecret` sera renvoyé au frontend pour qu'il finalise le paiement.

Enrichir `PaymentGatewayResult` avec un champ `clientSecret` optionnel.

### 1.4 `InitiatePaymentCommandHandler`

Le handler crée le `Payment` en statut `PENDING`, appelle le port pour créer le `PaymentIntent`, stocke l'ID Stripe (`pi_...`) comme `externalTransactionId` dans l'agrégat, puis retourne un read model `InitiatePaymentResult` contenant le `clientSecret`. **Il ne marque pas le paiement comme réussi** — cette responsabilité appartient au webhook.

### 1.5 `StripePaymentAdapter` (infrastructure / ACL)

Implémente `PaymentGatewayPort` dans la couche infrastructure. Responsabilités :

- `initiatePayment` : appelle l'API Stripe pour créer un `PaymentIntent`. Le montant doit être converti en centimes (ex. `10.00€` → `1000`). Stocker le `paymentId` interne dans les métadonnées Stripe pour retrouver le paiement lors du webhook. Retourner le `clientSecret` et l'ID du `PaymentIntent`.
- `initiateRefund` : appelle l'API Stripe pour créer un `Refund` à partir de l'`externalTransactionId`.
- Mapper les `PaymentMethod` du domaine vers les types Stripe (`CREDIT_CARD` → `"card"`, `BANK_TRANSFER` → `"sepa_debit"`).
- Encapsuler toutes les `StripeException` et les retourner comme `PaymentGatewayResult.failure(...)`.

### 1.6 Endpoint `POST /api/v1/payments`

Enrichir `PaymentResponse` avec un champ `clientSecret`. Ce champ est retourné uniquement lors de la création, il sera utilisé par le frontend pour appeler `stripe.confirmCardPayment`.

### 1.7 Endpoint webhook `POST /api/v1/payments/webhook`

C'est le point le plus critique de l'intégration.

**Ce qu'il doit faire :**
- Lire le body en bytes bruts (`byte[]`) — ne jamais le désérialiser avant la vérification de signature
- Vérifier la signature via `Webhook.constructEvent(rawBody, stripeSignatureHeader, webhookSecret)` — rejeter avec `400` si invalide
- Gérer les événements suivants : `payment_intent.succeeded`, `payment_intent.payment_failed`, `charge.refunded`
- Dispatcher vers `ProcessPaymentResultCommand` via le Mediator
- Retourner `200 OK` immédiatement, même si le traitement échoue (Stripe retentera sinon)

**Sécurité :** exclure cet endpoint du filtre d'authentification JWT dans Spring Security. La vérification de signature Stripe est le seul mécanisme d'authentification de cet endpoint.

### 1.8 `ProcessPaymentResultCommandHandler`

Doit être **idempotent** : si le webhook arrive deux fois pour le même `externalTransactionId` avec le même statut, le second appel doit retourner `Result.success()` sans modifier l'état. Stripe peut délivrer le même événement plusieurs fois.

### 1.9 Migration Flyway

Vérifier que la table `payment_schema.payments` possède bien une colonne `external_transaction_id` indexée et unique, pour permettre la recherche par `PaymentIntent ID` Stripe lors du traitement webhook.

---

## Partie 2 — Frontend (PWA Angular)

### 2.1 Installation

Installer le package `@stripe/stripe-js` (uniquement la lib navigateur — pas de `stripe` Node).

### 2.2 Configuration

Ajouter `stripePublishableKey: 'pk_test_...'` dans `projects/pwa/src/environments/environment.ts`. La clé publiable est publique par nature, elle peut figurer dans le code frontend sans risque.

### 2.3 `StripeService`

Créer un service Angular injectable (`core/services/stripe.service.ts`) qui encapsule :

- L'initialisation de Stripe via `loadStripe(publishableKey)` (appel unique au démarrage)
- La création et le montage d'un `CardElement` Stripe dans un conteneur HTML donné
- La confirmation du paiement via `stripe.confirmCardPayment(clientSecret, { payment_method: { card } })`

### 2.4 Composant de paiement

Flux attendu dans le composant :

1. À l'affichage, appeler `POST /api/v1/payments` avec l'`orderId` → récupérer le `clientSecret`
2. Monter le `CardElement` Stripe dans le DOM (formulaire de saisie de carte hébergé par Stripe)
3. Au clic sur "Payer", appeler `stripe.confirmCardPayment(clientSecret)`
4. Afficher un état intermédiaire "Paiement en cours de vérification..." — le résultat immédiat de `confirmCardPayment` confirme que la carte a été soumise à Stripe, pas que le paiement est définitivement accepté
5. Le statut final est déterminé par le webhook backend ; prévoir un polling ou un SSE pour notifier l'utilisateur du résultat réel

---

## Partie 3 — Tests

### Cartes de test Stripe

| Numéro | Résultat |
|---|---|
| `4242 4242 4242 4242` | Paiement réussi |
| `4000 0000 0000 9995` | Refusé — fonds insuffisants |
| `4000 0025 0000 3155` | Authentification 3DS requise |

Date d'expiration : n'importe quelle date future. CVV : n'importe quels 3 chiffres.

### Webhooks en local

Stripe ne peut pas joindre `localhost`. Utiliser la **Stripe CLI** :

```bash
stripe listen --forward-to localhost:8080/api/v1/payments/webhook
```

La CLI affiche un `whsec_...` à utiliser comme `STRIPE_WEBHOOK_SECRET` en local. Pour déclencher un événement manuellement : `stripe trigger payment_intent.succeeded`.

---

## Partie 4 — Checklist sécurité

| Point | Détail |
|---|---|
| `STRIPE_SECRET_KEY` uniquement en variable d'env backend | Jamais dans le code ni dans git |
| Vérification de signature sur chaque webhook | Sans ça, n'importe qui peut envoyer de faux événements |
| Body webhook lu en bytes bruts avant toute opération | Toute conversion préalable invalide la signature |
| Traitement webhook idempotent | Stripe peut livrer le même événement plusieurs fois |
| `clientSecret` jamais loggué | Il permet de confirmer le paiement — traiter comme un secret |
| Données de carte jamais transmises au backend | PCI DSS — Stripe Elements s'en charge intégralement |
| Endpoint `/webhook` exclu du filtre JWT | Stripe n'envoie pas de JWT |
| HTTPS obligatoire en production | Stripe refuse les webhooks HTTP hors mode test |

---

## Partie 5 — Ce qui ne change pas dans l'architecture

L'intégration Stripe s'insère dans le flux inter-modules existant sans le modifier :

- `order` publie `OrderConfirmed` → `payment` réagit et lance `InitiatePaymentCommand`
- `payment` publie `PaymentSucceeded` / `PaymentFailed` → `order` met à jour son état
- `notification` réagit à `PaymentSucceeded` pour envoyer le reçu

La seule nouveauté est que `POST /api/v1/payments` retourne désormais un `clientSecret` que le frontend utilise pour finaliser le paiement côté Stripe.

---

## Documents liés

- [Payment Module](modules/payment-module.md)
- [Inter-Module Communication](architecture/inter-module-communication.md)
- [Domain Events](domain/domain-events.md)
- [Security Baseline](security/security-baseline.md)
- [Transaction Management](development/transaction-management.md)
