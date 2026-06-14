# Configuration Stripe — Guide d'intégration

> **Document compagnon** : pour comprendre **comment** les commandes et paiements fonctionnent (séquences, états, routage des webhooks, modèle de données), voir [**Orders & Payments — flux complet**](../flows/order-payment-flow.md). Le présent document couvre uniquement la **mise en place** technique.
>
> **TVA / Stripe Tax** : pour activer le calcul automatique de la TVA et l'autoliquidation B2B intra-UE, voir le guide dédié [**Stripe Tax (TVA) — Guide de mise en place**](./stripe-tax-setup.md).

Ce document décrit les étapes nécessaires pour configurer Stripe sur le projet CYNA avant de pouvoir accepter des paiements en environnement de développement ou de production.

---

## 1. Créer un compte Stripe

1. Rendez-vous sur [https://dashboard.stripe.com/register](https://dashboard.stripe.com/register)
2. Créez un compte avec l'adresse e-mail professionnelle du projet
3. Complétez la vérification du compte (e-mail)
4. En développement, **aucune vérification KYB n'est requise** : le mode test est actif par défaut

---

## 2. Récupérer les clés API

### Emplacement dans le Dashboard

`Dashboard Stripe → Développeurs → Clés API`

### Clés à récupérer

| Clé | Description | Où l'utiliser |
|-----|-------------|---------------|
| `pk_test_...` | Clé publique (côté client) | Frontend Angular (`environment.ts`) |
| `sk_test_...` | Clé secrète (côté serveur) | Backend Spring Boot (`.env`) |

> **Règle de sécurité** : la clé secrète (`sk_test_...` / `sk_live_...`) ne doit **jamais** être exposée côté client ni commitée dans Git. Elle doit uniquement être présente dans les variables d'environnement du serveur.

---

## 3. Configurer les variables d'environnement backend

Ajoutez ces variables dans votre fichier `.env` (non versionné) :

```env
STRIPE_SECRET_KEY=sk_test_XXXXXXXXXXXXXXXXXXXXXXXX
STRIPE_PUBLISHABLE_KEY=pk_test_XXXXXXXXXXXXXXXXXXXXXXXX
STRIPE_WEBHOOK_SECRET=whsec_XXXXXXXXXXXXXXXXXXXXXXXX
```

Le `STRIPE_WEBHOOK_SECRET` sera obtenu à l'étape 4.

---

## 4. Configurer le Webhook Stripe

Les webhooks permettent à Stripe de notifier votre backend lorsqu'un paiement est confirmé. C'est le mécanisme principal de fiabilité : même si l'utilisateur ferme son navigateur après avoir payé, le webhook garantit que la commande est traitée.

### 4.1 En développement (Stripe CLI)

La Stripe CLI permet de recevoir les webhooks en local sans serveur exposé publiquement.

**Installation :**
```bash
# macOS
brew install stripe/stripe-cli/stripe

# Windows (Scoop)
scoop install stripe

# Linux / Docker
curl -s https://packages.stripe.dev/api/security/keypair/stripe-cli-gpg/public | gpg --dearmor | sudo tee /usr/share/keyrings/stripe.gpg
echo "deb [signed-by=/usr/share/keyrings/stripe.gpg] https://packages.stripe.dev/stripe-cli-debian-local stable main" | sudo tee -a /etc/apt/sources.list.d/stripe.list
sudo apt update && sudo apt install stripe
```

**Authentification :**
```bash
stripe login
```

**Écoute des webhooks en local :**
```bash
stripe listen \
  --events invoice.paid,invoice.payment_failed,customer.subscription.deleted,payment_intent.succeeded,payment_intent.payment_failed \
  --forward-to localhost:8080/api/v1/payments/webhook
```

Cette commande affiche un `whsec_...` à utiliser comme `STRIPE_WEBHOOK_SECRET` dans `cyna-backend/.env`. **Cette commande doit rester active** pendant tout le développement local — sans elle, le backend ne reçoit pas les webhooks et la commande reste en `PENDING` après paiement.

**Déclenchement manuel (tests) :**
```bash
# Simuler un paiement réussi
stripe trigger payment_intent.succeeded

# Simuler un paiement échoué
stripe trigger payment_intent.payment_failed
```

**Vérifier qu'un webhook arrive bien au backend :**
Le terminal `stripe listen` affiche chaque événement avec son code de réponse. Un `200 POST` confirme que le backend a accepté l'événement.

**Test E2E complet du parcours :**
La suite Playwright dans `e2e/` inclut un test (`20-webhook-end-to-end.spec.ts`) qui valide le pipeline complet : paiement par carte → webhook reçu → `Order` passe à `PAID` → `Subscription` locale créée. Pour qu'il passe, `stripe listen` doit tourner en parallèle.

### 4.2 En production

1. `Dashboard Stripe → Développeurs → Webhooks → Ajouter un endpoint`
2. URL : `https://votre-domaine.com/api/v1/payments/webhook`
3. Événements à écouter (modèle abonnement récurrent) :
   - `invoice.paid` — premier paiement (`subscription_create`) + renouvellements (`subscription_cycle`)
   - `invoice.payment_failed` — paiement initial ou renouvellement échoué
   - `customer.subscription.deleted` — abonnement annulé chez Stripe
   - `payment_intent.succeeded` — fallback de compatibilité ; permet aussi de tracer le PaymentIntent du premier invoice
   - `payment_intent.payment_failed` — fallback
4. Après création, cliquez sur **Révéler la clé de signature** (`whsec_...`)
5. Copiez-la dans `STRIPE_WEBHOOK_SECRET`

> **Pourquoi le webhook est critique** : c'est lui qui déclenche la mise à jour de la commande (statut `PAID`) et la création des abonnements. Sans webhook, les paiements ne seront jamais confirmés côté backend. La vérification de la signature webhook empêche toute injection frauduleuse.

---

## 4bis. Activer le Customer Portal (une seule fois par compte Stripe)

Le Customer Portal est l'écran Stripe-hosted accessible depuis le bouton **"Gérer mes paiements et factures"** sur la page **/account/subscriptions**. Il permet au client de voir/changer/supprimer ses cartes, télécharger ses factures et annuler ses abonnements sans qu'on ait à coder cette UI.

**Activation (1 minute, à faire une fois en test ET une fois en live) :**

1. Aller sur `Dashboard Stripe → Settings → Billing → Customer portal`
   ou directement [https://dashboard.stripe.com/test/settings/billing/portal](https://dashboard.stripe.com/test/settings/billing/portal)
2. Régler les options souhaitées (par défaut tout est activé : invoice history, update payment method, cancel subscription)
3. Cliquer **Save** en bas de page

Sans cette étape de validation, l'API renvoie `400 No configuration provided` au premier appel à `BillingPortal.Session.create`.

L'endpoint backend correspondant est `POST /api/v1/payments/billing-portal` ; il retourne `{url: "https://billing.stripe.com/p/session/..."}` que le frontend ouvre via `window.location.href`.

---

## 5. Configurer la clé publique côté frontend

Dans le fichier `cyna-frontend/projects/pwa/src/environments/environment.ts` :

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  stripePublishableKey: 'pk_test_XXXXXXXXXXXXXXXXXXXXXXXX',
};
```

Pour la production, configurez le même champ dans `environment.prod.ts` avec la clé live `pk_live_...`.

---

## 6. Cartes de test

Stripe fournit des numéros de carte fictifs pour simuler différents scénarios en mode test :

| Scénario | Numéro de carte | Expiration | CVC |
|----------|-----------------|------------|-----|
| Paiement réussi | `4242 4242 4242 4242` | Toute date future | N'importe quel CVC |
| Authentification 3D Secure requise | `4000 0025 0000 3155` | Toute date future | N'importe quel CVC |
| Paiement refusé (fonds insuffisants) | `4000 0000 0000 9995` | Toute date future | N'importe quel CVC |
| Paiement refusé (carte invalide) | `4000 0000 0000 0002` | Toute date future | N'importe quel CVC |
| Paiement refusé (CVC incorrect) | `4000 0000 0000 0101` | Toute date future | N'importe quel CVC |

---

## 7. Passer en production

Avant d'activer le mode live :

1. **Désactiver le mode test** dans le Dashboard Stripe (`Activer les paiements réels`)
2. **Remplacer les clés** : `pk_test_...` → `pk_live_...` et `sk_test_...` → `sk_live_...`
3. **Recréer le webhook** avec l'URL de production et récupérer le nouveau `whsec_live_...`
4. **Vérifier la conformité PCI DSS** : le projet utilise Stripe Elements (hosted fields), ce qui réduit la surface de conformité au niveau SAQ A
5. **Activer les paiements récurrents (off-session)** : vérifier que `setup_future_usage: off_session` est bien configuré pour permettre les renouvellements d'abonnements sans interaction utilisateur

---

## 8. Récapitulatif des variables d'environnement

### Backend (`.env`)

```env
# Base de données
DB_HOST=localhost
DB_PORT=5432
DB_NAME=cyna
DB_USERNAME=cyna
DB_PASSWORD=cyna_dev_password

# JWT
JWT_SECRET=cyna-dev-secret-key-must-be-at-least-256-bits-long-for-hs256
JWT_ACCESS_EXPIRATION_HOURS=1
JWT_REFRESH_EXPIRATION_HOURS=24

# Stripe
STRIPE_SECRET_KEY=sk_test_XXXXXXXXXXXXXXXXXXXXXXXX
STRIPE_PUBLISHABLE_KEY=pk_test_XXXXXXXXXXXXXXXXXXXXXXXX
STRIPE_WEBHOOK_SECRET=whsec_XXXXXXXXXXXXXXXXXXXXXXXX
# Stripe Tax (TVA) — voir stripe-tax-setup.md. Laisser false tant que Tax
# n'est pas activé + immatriculé dans le dashboard, sinon les abonnements échouent.
STRIPE_TAX_ENABLED=false
STRIPE_TAX_CODE=txcd_10103000

# Application
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8080
```

### Frontend (`environment.ts`)

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  stripePublishableKey: 'pk_test_XXXXXXXXXXXXXXXXXXXXXXXX',
};
```

---

## 9. Architecture du flux de paiement

```
Frontend (Angular)
    │
    ├─ 1. POST /api/v1/orders          → Crée la commande (statut PENDING)
    │
    ├─ 2. POST /api/v1/payments/initiate  → Crée la Subscription Stripe
    │         (default_incomplete + recurring.interval = MONTH/YEAR)
    │         └─ Retourne { clientSecret } extrait du 1er invoice payment_intent
    │
    ├─ 3. stripe.confirmCardPayment(clientSecret)
    │         └─ Confirmation côté client (3DS si nécessaire)
    │         └─ La carte est enregistrée comme méthode par défaut sur la sub
    │
    └─ 4. En cas de succès → Toast "Paiement validé" + redirection /catalog

Backend (Spring Boot) — webhooks
    │
    ├─ payment_intent.succeeded
    │     └─ Marque le Payment SUCCEEDED → publie PaymentSucceeded
    │           └─ PaymentSucceededReconciliationHandler :
    │                 ├─ marque l'Order PAID
    │                 └─ crée les Subscription locales (1 par produit)
    │
    ├─ invoice.paid (subscription_create) — fallback du PI sur premier invoice
    │
    ├─ invoice.paid (subscription_cycle) — RENOUVELLEMENT
    │     └─ subscriptionCommandApi.renewByStripeId(subId, periodEnd)
    │           └─ étend end_at + next_billing_at sur les Subscription locales
    │
    ├─ invoice.payment_failed (subscription_cycle)
    │     └─ subscriptionCommandApi.markPastDueByStripeId(subId)
    │
    └─ customer.subscription.deleted
          └─ subscriptionCommandApi.cancelByStripeId(subId)
```

Le webhook est la source de vérité côté backend. La confirmation côté client (étape 3) met seulement à jour l'UI ; c'est le webhook qui déclenche la logique métier (création de la Subscription locale, renouvellements, annulations).

## 10. Pourquoi `parseWebhookEvent` parse le JSON brut

Le SDK `stripe-java` 26.x est figé sur l'API version `2024-06-20` alors que les comptes Stripe créés récemment utilisent `2025-03-31.basil` (ou plus récent) par défaut. Quand le schéma diverge (par ex. `invoice.subscription` est devenu `invoice.parent.subscription` en 2025-03-31), la désérialisation typée du SDK retourne silencieusement `Optional.empty()` et tous les champs extraits valent `null`. Pour rester robuste à cette dérive, `StripePaymentAdapter.parseWebhookEvent` lit `event.getDataObjectDeserializer().getRawJson()` et extrait les champs nécessaires (paymentIntentId, subscriptionId, billingReason, periodEnd) directement via Gson.
