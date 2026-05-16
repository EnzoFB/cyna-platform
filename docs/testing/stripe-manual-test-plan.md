# Plan de tests manuels — Paiement & abonnements (Stripe)

> Ce document liste **uniquement les tests qui ne peuvent pas être
> automatisés de façon déterministe** pour ce projet : le comportement réel
> de l'API Stripe (3DS, déclin carte), les allers-retours webhook réels, et
> les parcours UX de bout en bout.
>
> Tout le reste est couvert par des tests automatisés (voir
> [§ Périmètre](#1-périmètre--ce-qui-est-déjà-automatisé)). Valider le
> comportement d'un prestataire de paiement en **mode test** est la pratique
> standard de l'industrie, pas un contournement.

---

## Sommaire

1. [Périmètre — ce qui est déjà automatisé](#1-périmètre--ce-qui-est-déjà-automatisé)
2. [Prérequis](#2-prérequis)
3. [Comment observer les résultats](#3-comment-observer-les-résultats)
4. [Cartes de test Stripe](#4-cartes-de-test-stripe)
5. [Plan de tests](#5-plan-de-tests)
6. [Aide-mémoire Stripe CLI](#6-aide-mémoire-stripe-cli)
7. [Checklist smoke avant soutenance](#7-checklist-smoke-avant-soutenance)
8. [Feuille de passage (sign-off)](#8-feuille-de-passage-sign-off)

---

## 1. Périmètre — ce qui est déjà automatisé

**Ne PAS re-tester manuellement** (couvert par la CI / `./gradlew test`) :

| Couvert automatiquement | Test |
|---|---|
| Parsing du contrat Stripe + vérif signature HMAC (schémas 2024 **et** 2025) | `StripePaymentAdapterWebhookTest` |
| Routage / déduplication / idempotence des webhooks | `ProcessWebhookCommandHandlerTest` |
| Logique métier abonnement/paiement | tests unitaires `subscription.*` / `payment.application.*` |
| Persistance, migrations Flyway, FK, réconciliation anti-drift, annulation V14, ledger dédup | tests Testcontainers (`StripeWebhookSyncIntegrationTest`, `CancelSubscriptionIntegrationTest`, `ProcessedStripeEventRepositoryIntegrationTest`) |

**À tester manuellement** (ce document) : comportement réel de l'API Stripe
sortante (3DS, déclins), parcours UX complets, allers-retours webhook réels,
emails effectivement reçus.

---

## 2. Prérequis

### 2.1. Stripe en mode test

- Dashboard Stripe en **Test mode** (toggle en haut à gauche).
- Récupérer les clés test : `sk_test_…`, `pk_test_…`.

### 2.2. Stripe CLI (indispensable)

Le Stripe CLI permet de **recevoir les webhooks en local** et de
**déclencher des événements** sans attendre (renouvellements, échecs).

```bash
# Installation : https://stripe.com/docs/stripe-cli
stripe login                       # lie le CLI au compte test
stripe listen --forward-to localhost:8080/api/v1/payments/webhook
```

> `stripe listen` affiche un secret `whsec_…`. **Mettre CE secret** dans
> `STRIPE_WEBHOOK_SECRET` du backend local (sinon les signatures sont
> rejetées — comportement attendu et testé). Laisser `stripe listen` tourner
> dans un terminal dédié pendant toute la session de test.

### 2.3. Environnement applicatif

- Backend lancé : `./gradlew bootRun` (port 8080), `.env` avec les clés test.
- Front PWA lancé : `npm run start:pwa` (port 4200).
- PostgreSQL up (conteneur `cyna-postgres`, db `cyna` / user `cyna`).
- Un compte utilisateur de test créé et connecté.
- Brevo configuré (clé API test) pour vérifier les emails — sinon vérifier
  les logs backend `Brevo response: 2xx`.

### 2.4. Accès vérification base

```bash
docker exec -i cyna-postgres psql -U cyna -d cyna
```

---

## 3. Comment observer les résultats

| Canal | Usage |
|---|---|
| Terminal `stripe listen` | Voir les événements émis + le code HTTP renvoyé par le webhook (doit être `200`) |
| Stripe Dashboard → Payments / Subscriptions / Events | État Stripe autoritaire, payloads, bouton **Resend** |
| SQL (`psql` ci-dessus) | Vérifier l'état local après webhook |
| Boîte mail du compte test / logs backend | Vérifier les emails transactionnels |
| Navigateur (PWA) | Parcours UX, badges, redirections portail |

Requêtes SQL utiles :

```sql
-- état d'une commande
SELECT id, status, total_amount FROM order_schema.orders ORDER BY created_at DESC LIMIT 5;
-- état des abonnements d'un user
SELECT id, status, auto_renew, end_at, stripe_subscription_id
  FROM subscription_schema.subscriptions WHERE user_id = '<uuid>' ORDER BY created_at DESC;
-- état paiement
SELECT id, status, stripe_subscription_id FROM payment_schema.payments ORDER BY created_at DESC LIMIT 5;
-- ledger de déduplication des webhooks
SELECT event_id, event_type, processed_at FROM payment_schema.processed_stripe_events
  ORDER BY processed_at DESC LIMIT 10;
-- preuve de consentement RGPD
SELECT action, label_version, given_at, ip_address FROM payment_schema.payment_consent_log
  WHERE user_id = '<uuid>' ORDER BY given_at DESC;
```

---

## 4. Cartes de test Stripe

| Numéro | Comportement | Exp / CVC |
|---|---|---|
| `4242 4242 4242 4242` | Paiement réussi (Visa) | toute date future / tout CVC |
| `4000 0025 0000 3155` | **Nécessite 3DS2** (authentification forte) | idem |
| `4000 0000 0000 0002` | Carte refusée (generic decline) | idem |
| `4000 0000 0000 9995` | Refusée (fonds insuffisants) | idem |
| `4000 0000 0000 0069` | Carte expirée | idem |
| `4000 0000 0000 0341` | Échoue après attachement (test sauvegarde) | idem |

---

## 5. Plan de tests

> Convention : **Préconditions → Étapes → Résultat attendu → Vérification**.
> Cocher la case dans la [feuille de passage](#8-feuille-de-passage-sign-off).

### TC1 — Checkout nominal (paiement réussi)

- **Préconditions** : panier avec ≥ 1 produit mensuel, user connecté, `stripe listen` actif.
- **Étapes** : `/checkout` → adresse → carte `4242 4242 4242 4242` → valider.
- **Résultat attendu** : redirection `/checkout/success`, email de confirmation de commande reçu.
- **Vérification** :
  - `stripe listen` : `invoice.paid`, `customer.subscription.created`, `payment_intent.succeeded` → tous `200`.
  - SQL : `orders.status = 'PAID'`, `payments.status = 'SUCCEEDED'`, `subscriptions.status = 'ACTIVE'`.
  - `processed_stripe_events` : une ligne par event traité.

### TC2 — Checkout avec authentification forte (3DS2)

- **Étapes** : checkout avec `4000 0025 0000 3155` → compléter la modale 3DS Stripe.
- **Résultat attendu** : après validation 3DS, même issue que TC1.
- **Point de vigilance** : si l'utilisateur **abandonne** la 3DS → le paiement
  reste `incomplete` ; la commande ne doit pas être marquée PAID, l'UX doit
  l'indiquer (ligne `incomplete` remontée). Tester aussi ce cas.

### TC3 — Carte refusée

- **Étapes** : checkout avec `4000 0000 0000 0002`.
- **Résultat attendu** : message d'erreur clair (carte refusée), **aucun**
  abonnement créé, panier conservé, pas de redirection succès.
- **Vérification** : aucune nouvelle ligne `orders` en `PAID`, aucun
  `subscriptions`.

### TC4 — Fonds insuffisants

- **Étapes** : `4000 0000 0000 9995`.
- **Résultat attendu** : message d'erreur dédié (fonds insuffisants), état inchangé.

### TC5 — Panier multi-cycle (mensuel + annuel)

- **Préconditions** : panier avec 1 produit MENSUEL + 1 produit ANNUEL.
- **Résultat attendu** : 2 abonnements Stripe créés (1 par ligne, cycles
  distincts), 2 abonnements locaux, le bandeau de récurrence affiche bien les
  **deux** engagements.
- **Vérification** : Stripe Dashboard → 2 subscriptions ; SQL → 2 lignes
  `subscriptions` avec `billing_cycle` MONTHLY et ANNUAL.

### TC6 — Activation d'abonnement via webhook réel

- **Objectif** : prouver l'aller-retour webhook complet (pas mocké).
- **Étapes** : faire TC1, observer `stripe listen`.
- **Résultat attendu** : `subscriptions.status` passe à `ACTIVE` **suite au
  webhook** `invoice.paid` (billing_reason=subscription_create), pas avant.

### TC7 — Renouvellement réussi

- **Objectif** : valider l'extension de période sans attendre 1 mois.
- **Méthode A (Test Clock — recommandé)** : créer l'abonnement via un
  [Stripe Test Clock](https://stripe.com/docs/billing/testing/test-clocks),
  avancer l'horloge d'un cycle.
- **Méthode B (rapide)** : `stripe trigger invoice.paid` (vérifier le
  `billing_reason` ; pour un vrai cycle utiliser le Test Clock).
- **Résultat attendu** : `subscriptions.end_at` étendu d'une période,
  `status` reste `ACTIVE`.
- **Vérification** : SQL avant/après sur `end_at`.

### TC8 — Échec de renouvellement → PAST_DUE

- **Étapes** : `stripe trigger invoice.payment_failed` (ou Test Clock avec
  carte qui échoue au cycle).
- **Résultat attendu** :
  - `subscriptions.status = 'PAST_DUE'`.
  - **Email « échec de paiement »** reçu (template `subscription-payment-failed`).
  - PWA → onglet Abonnements : **badge rouge « Paiement en retard »** +
    bouton « Mettre à jour le paiement » qui ouvre le portail Stripe.
- **Vérification** : SQL `status`, boîte mail, écran.

### TC9 — Annulation d'abonnement (cancel at period end)

- **Étapes** : PWA → Abonnements → désactiver le renouvellement / annuler.
- **Résultat attendu** :
  - Réponse immédiate : état projeté `status=ACTIVE`, `autoRenew=false`.
  - **La DB locale n'est PAS mutée tant que le webhook
    `customer.subscription.updated` n'est pas arrivé** (invariant V14).
  - Après webhook : `cancelled_at` renseigné, accès conservé jusqu'à `end_at`.
  - **Email de confirmation d'annulation** reçu.
- **Vérification** : SQL juste après le clic (inchangé) puis après le webhook
  (mis à jour) ; boîte mail.

### TC10 — Portail Stripe : ajouter une carte

- **Étapes** : PWA → Moyens de paiement → « Ouvrir le portail Stripe » →
  ajouter une carte → revenir.
- **Résultat attendu** : webhook `payment_method.attached` → après rechargement,
  la carte apparaît dans la liste read-only de la PWA.
- **Vérification** : `stripe listen` (event `attached` `200`), liste PWA.

### TC11 — Portail Stripe : supprimer une carte

- **Étapes** : portail Stripe → supprimer une carte non liée à un abo actif.
- **Résultat attendu** : webhook `payment_method.detached` → la carte
  disparaît de la liste PWA après rechargement.

### TC12 — Mise à jour automatique de carte

- **Étapes** : `stripe trigger payment_method.automatically_updated` (ou via
  Test Clock / reissue simulée).
- **Résultat attendu** : `last4`/`expiry` rafraîchis en local sans action user.

### TC13 — Factures PDF

- **Préconditions** : au moins un paiement réussi (TC1).
- **Étapes** : PWA → onglet Historique → section « Mes factures » →
  « Télécharger le PDF ».
- **Résultat attendu** : la liste affiche la facture (numéro, montant,
  statut « Payée »), le bouton ouvre le **PDF Stripe** dans un nouvel onglet.

### TC14 — Preuve de consentement RGPD

- **Étapes A** : checkout, nouvelle carte, **case « Réutiliser cette carte »
  cochée** → valider.
- **Résultat attendu A** : une ligne dans `payment_consent_log`
  (`action=SAVE_CARD_AT_CHECKOUT`, `label_version`, `ip_address`,
  `user_agent`, `given_at`).
- **Étapes B** : checkout, case **décochée**.
- **Résultat attendu B** : **aucune** ligne ajoutée.
- **Vérification** : requête SQL `payment_consent_log`.

### TC15 — Idempotence webhook (anti-doublon)

- **Étapes** : Stripe Dashboard → Events → choisir un événement déjà traité
  (ex. `invoice.paid` de TC1) → **Resend**.
- **Résultat attendu** : le backend répond `200`, **aucun effet de bord
  dupliqué** (pas de double email, pas de double activation), log
  `already processed — skipping (duplicate delivery)`.
- **Vérification** : `processed_stripe_events` toujours **1 seule ligne**
  pour cet `event_id` ; état métier inchangé.

### TC16 — Rejet de signature webhook

- **Étapes** :
  ```bash
  curl -i -X POST localhost:8080/api/v1/payments/webhook \
    -H "Stripe-Signature: t=1,v1=deadbeef" -d '{}'
  ```
- **Résultat attendu** : réponse non-2xx (signature invalide), **aucune**
  modification d'état. (Le parsing + rejet est aussi couvert en unitaire.)

### TC17 — Carte expirée filtrée au checkout

- **Préconditions** : forcer une carte sauvegardée à une date passée en SQL :
  ```sql
  UPDATE payment_schema.saved_payment_methods
     SET exp_month='01', exp_year='2020' WHERE id='<uuid-carte>';
  ```
- **Résultat attendu** : PWA → la carte porte le badge « Expirée » et
  **n'apparaît pas** dans le sélecteur de carte au checkout.

---

## 6. Aide-mémoire Stripe CLI

```bash
# Recevoir les webhooks en local (laisser tourner)
stripe listen --forward-to localhost:8080/api/v1/payments/webhook

# Déclencher des événements
stripe trigger payment_intent.succeeded
stripe trigger invoice.paid
stripe trigger invoice.payment_failed
stripe trigger customer.subscription.updated
stripe trigger customer.subscription.deleted
stripe trigger payment_method.attached
stripe trigger payment_method.detached

# Rejouer un événement précis (test idempotence)
stripe events resend evt_xxx
```

> Pour les renouvellements **réels** (extension de période, dunning), utiliser
> les **Test Clocks** Stripe plutôt que `stripe trigger` : ils simulent le
> passage du temps sur une vraie subscription.

---

## 7. Checklist smoke avant soutenance

À dérouler **la veille**, dans cet ordre, sur un environnement propre :

- [ ] TC1 — checkout nominal de bout en bout (le parcours qui sera démontré)
- [ ] TC2 — 3DS2 (montre la conformité SCA)
- [ ] TC8 — échec renouvellement → PAST_DUE + email + badge (montre la gestion des cas dégradés)
- [ ] TC9 — annulation (montre l'invariant single-write V14)
- [ ] TC13 — factures PDF (exigence du cadrage)
- [ ] TC15 — idempotence webhook (réponse au Risque 2 du cadrage)
- [ ] Préparer 2-3 captures/écrans de secours au cas où le réseau Stripe lague le jour J

---

## 8. Feuille de passage (sign-off)

| TC | Intitulé | Testé par | Date | Résultat | Notes |
|----|----------|-----------|------|----------|-------|
| TC1 | Checkout nominal | | | ☐ OK ☐ KO | |
| TC2 | 3DS2 (+ abandon) | | | ☐ OK ☐ KO | |
| TC3 | Carte refusée | | | ☐ OK ☐ KO | |
| TC4 | Fonds insuffisants | | | ☐ OK ☐ KO | |
| TC5 | Multi-cycle | | | ☐ OK ☐ KO | |
| TC6 | Activation via webhook | | | ☐ OK ☐ KO | |
| TC7 | Renouvellement réussi | | | ☐ OK ☐ KO | |
| TC8 | Échec renouvellement | | | ☐ OK ☐ KO | |
| TC9 | Annulation | | | ☐ OK ☐ KO | |
| TC10 | Portail : ajout carte | | | ☐ OK ☐ KO | |
| TC11 | Portail : suppression carte | | | ☐ OK ☐ KO | |
| TC12 | Auto-update carte | | | ☐ OK ☐ KO | |
| TC13 | Factures PDF | | | ☐ OK ☐ KO | |
| TC14 | Consentement RGPD | | | ☐ OK ☐ KO | |
| TC15 | Idempotence webhook | | | ☐ OK ☐ KO | |
| TC16 | Rejet signature | | | ☐ OK ☐ KO | |
| TC17 | Carte expirée filtrée | | | ☐ OK ☐ KO | |

---

## Annexe — liens

- [Stripe CLI](https://stripe.com/docs/stripe-cli)
- [Stripe test cards](https://stripe.com/docs/testing)
- [Stripe Test Clocks](https://stripe.com/docs/billing/testing/test-clocks)
- [Doc opérations / config Stripe](../setup/payment-methods-operations.md)
- [Stratégie de tests du projet](./testing-strategy.md)

---

*Dernière mise à jour : 2026-05-15. Mettre à jour à chaque ajout de parcours
de paiement ou de webhook.*
