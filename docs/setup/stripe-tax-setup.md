# Stripe Tax (TVA) — Guide de mise en place

> **Document compagnon** : la configuration générale de Stripe (clés API, webhooks, Customer Portal, cartes de test) est décrite dans [**Configuration Stripe — Guide d'intégration**](./stripe-onboarding.md). Le présent document couvre **uniquement Stripe Tax** : à quoi ça sert dans CYNA, comment l'activer dans le dashboard Stripe, et comment le passer en production.

Ce guide s'adresse à la personne qui **exploite** la plateforme CYNA (le « client »). En le suivant, vous activez le calcul automatique de la TVA sur les abonnements, y compris l'**autoliquidation B2B intra-UE** (reverse charge).

---

## 0. En une phrase

Stripe Tax calcule, applique et ventile **automatiquement** la bonne TVA sur chaque facture d'abonnement, selon **l'adresse de facturation du client** et **son numéro de TVA** — sans qu'on maintienne nous-mêmes les taux par pays.

Côté code, **rien n'est à modifier** : tout est déjà implémenté et piloté par un simple interrupteur de configuration (`STRIPE_TAX_ENABLED`). Ce guide explique ce qu'il faut faire **dans le dashboard Stripe** pour que cet interrupteur puisse être mis sur `true`.

---

## 1. Comment ça marche dans CYNA (pour comprendre la configuration)

Lorsque Stripe Tax est activé, à chaque checkout le backend (voir [`StripePaymentAdapter`](../../cyna-backend/src/main/java/com/cyna/modules/payment/infrastructure/stripe/StripePaymentAdapter.java)) :

1. **Marque les prix comme HT** (`tax_behavior = exclusive`) : le catalogue est hors taxe, Stripe ajoute la TVA par-dessus.
2. **Affecte un code de taxe produit** (par défaut « SaaS » `txcd_10103000`) à chaque produit créé chez Stripe.
3. **Renseigne l'adresse de facturation** du client sur le `Customer` Stripe (lue depuis la carte) **avant** la première facture → c'est cette adresse qui détermine la juridiction de TVA.
4. **Attache le numéro de TVA B2B** (si saisi au checkout) comme `tax_id` sur le `Customer` → c'est ce qui déclenche l'**autoliquidation** (0 % de TVA) pour un numéro intra-UE transfrontalier valide.
5. **Active `automatic_tax`** sur chaque abonnement → Stripe calcule et ventile la TVA sur la facture initiale **et tous les renouvellements**.

> ⚠️ **Point critique** : si `STRIPE_TAX_ENABLED=true` mais qu'**aucune immatriculation fiscale** n'est déclarée dans le dashboard (étape 4 ci-dessous), Stripe ne peut pas déterminer de taux et **la création d'abonnement échoue**. Activez l'interrupteur seulement une fois le dashboard configuré.

---

## 2. Pré-requis

- Un compte Stripe (voir [stripe-onboarding.md](./stripe-onboarding.md)).
- En **production** : l'entité juridique et son **adresse professionnelle** renseignées dans Stripe (`Settings → Business`), car elles servent d'**adresse d'origine** pour la TVA.
- Savoir dans **quels pays** vous êtes redevable de la TVA (au minimum la France au lancement).

> En **mode test**, aucun KYB n'est requis : vous pouvez tout configurer et tester librement. Les écrans sont identiques au live, préfixés par `/test/`.

---

## 3. Activer Stripe Tax

1. Aller sur **`Dashboard → Settings → Tax`**
   - Test : [https://dashboard.stripe.com/test/settings/tax](https://dashboard.stripe.com/test/settings/tax)
   - Live : [https://dashboard.stripe.com/settings/tax](https://dashboard.stripe.com/settings/tax)
2. Renseigner l'**adresse d'origine** (origin address) — l'adresse depuis laquelle vous « vendez ». En général le siège de l'entreprise.
3. Définir la **catégorie de taxe par défaut** (default tax category) : choisir **« Software as a service (SaaS) »** pour rester cohérent avec le code de taxe par défaut de l'application (`txcd_10103000`).
   > Catalogue complet des codes : [https://docs.stripe.com/tax/tax-codes](https://docs.stripe.com/tax/tax-codes). Si vos produits relèvent d'une autre catégorie, voir l'étape 6 pour aligner `STRIPE_TAX_CODE`.

---

## 4. Déclarer les immatriculations fiscales (le plus important)

Une **immatriculation** (tax registration) dit à Stripe : « je suis redevable de la TVA dans ce pays, voici mon régime ». **Sans au moins une immatriculation, Stripe Tax ne calcule rien.**

1. Aller sur **`Dashboard → Tax → Registrations`**
   - Test : [https://dashboard.stripe.com/test/tax/registrations](https://dashboard.stripe.com/test/tax/registrations)
2. **Add registration** → choisir le pays (ex. **France**), la date d'effet et le régime.
3. Répéter pour chaque pays où vous êtes redevable.

### Cas France-only (lancement)
Une seule immatriculation **France** suffit : Stripe facture 20 % de TVA aux clients français et gère les exonérations pour les ventes hors champ.

### Cas international UE (croissance)
Au fur et à mesure de l'expansion, ajoutez les immatriculations des autres pays UE, **ou** déclarez le **guichet unique OSS** (One-Stop Shop) qui couvre les ventes B2C dans toute l'UE via une seule immatriculation. Stripe surveille même les **seuils** (`Tax → Thresholds`) et vous alerte quand vous devenez redevable dans un nouveau pays.

> **Reverse charge B2B** : pour une vente à une entreprise d'un **autre** pays UE disposant d'un numéro de TVA valide, Stripe applique automatiquement l'autoliquidation (0 % de TVA, mention « reverse charge » sur la facture) — **à condition** que le numéro de TVA soit transmis (ce que fait CYNA au checkout) et que vous ayez une immatriculation dans le pays du client ou via l'OSS.

---

## 5. (Optionnel) Collecte du numéro de TVA dans le Customer Portal

CYNA collecte déjà le numéro de TVA **au checkout**. Si vous voulez aussi permettre aux clients de l'ajouter/modifier depuis leur espace Stripe :

`Dashboard → Settings → Billing → Customer portal` → activer **« Tax ID »** sous *Customer information*.

---

## 6. Configuration backend

Deux variables d'environnement pilotent Stripe Tax (voir [`application.yml`](../../cyna-backend/src/main/resources/application.yml) et [`.env.example`](../../cyna-backend/.env.example)) :

```env
# Mettre à true UNIQUEMENT après avoir activé Tax + déclaré au moins une
# immatriculation dans le dashboard (étapes 3 et 4). Sinon la création
# d'abonnement échoue.
STRIPE_TAX_ENABLED=true

# Code de taxe produit appliqué aux produits créés par l'app.
# Défaut : Software as a service (SaaS). À changer seulement si votre
# catalogue relève d'une autre catégorie (cf. étape 3).
STRIPE_TAX_CODE=txcd_10103000
```

| Variable | Défaut | Rôle |
|----------|--------|------|
| `STRIPE_TAX_ENABLED` | `false` | Interrupteur maître. `false` = comportement historique (aucune TVA calculée par Stripe). `true` = calcul + autoliquidation activés. |
| `STRIPE_TAX_CODE` | `txcd_10103000` | Code de taxe des produits (SaaS par défaut). |

> Laisser `false` en développement / CI / sur tout compte Stripe où Tax n'est pas configuré : l'application démarre et fonctionne normalement, simplement sans TVA Stripe.

Après modification du `.env`, **redémarrer le backend**.

---

## 7. Tester en mode test (avant la prod)

Stripe Tax fonctionne intégralement en mode test, sans coût. Scénarios à valider :

| Scénario | Adresse de facturation | N° de TVA | Résultat attendu |
|----------|------------------------|-----------|------------------|
| **B2C / B2B France** | France | aucun ou FR… | TVA française **20 %** ajoutée |
| **B2B intra-UE** | autre pays UE (ex. Allemagne) | n° valide de ce pays (ex. `DE123456789`) | **Autoliquidation** : 0 % de TVA, mention reverse charge |
| **Hors UE** | ex. États-Unis | — | Pas de TVA UE |

Procédure :
1. Configurer le dashboard **test** (étapes 3-4) avec une immatriculation France (+ une UE pour tester le reverse charge).
2. `STRIPE_TAX_ENABLED=true` dans `.env`, redémarrer.
3. Passer une commande avec la carte de test `4242 4242 4242 4242` et l'adresse / n° de TVA du scénario.
4. Vérifier la **facture** générée dans `Dashboard → test → Billing → Invoices` : le détail TVA (taux, montant, juridiction, mention reverse charge) doit correspondre.

> **Numéros de TVA de test** : en mode test, Stripe ne fait pas d'appel VIES réel ; un numéro **au bon format** pour le pays suffit à déclencher la logique d'autoliquidation. En **production**, Stripe valide le numéro via VIES — un numéro invalide est ignoré et la TVA est facturée normalement.

> **Comportement en cas de numéro invalide/non reconnu** : l'attachement du numéro de TVA est *best-effort*. Si le format est invalide ou le pays non géré, l'application **n'échoue pas le paiement** : elle facture la TVA standard (B2C). C'est volontaire — il vaut mieux trop facturer (récupérable) que pas assez. Le préfixe pays détermine le type Stripe : `eu_vat` (UE), `gb_vat` (Royaume-Uni), `ch_vat` (Suisse), `no_vat` (Norvège). Pour d'autres régions, étendre `EU_VAT_PREFIXES` / `taxIdTypeFor` dans [`StripePaymentAdapter`](../../cyna-backend/src/main/java/com/cyna/modules/payment/infrastructure/stripe/StripePaymentAdapter.java).

---

## 8. Passage en production — checklist

- [ ] Mode live activé dans Stripe, clés `sk_live_…` / `pk_live_…` en place (voir [stripe-onboarding.md](./stripe-onboarding.md) §7).
- [ ] **Adresse d'origine** + catégorie de taxe par défaut renseignées en **live** (`/settings/tax`).
- [ ] **Au moins une immatriculation** déclarée en **live** (`/tax/registrations`) — France au minimum.
- [ ] (International) Immatriculations supplémentaires ou **OSS** déclarés ; alertes de **seuils** surveillées.
- [ ] `STRIPE_TAX_ENABLED=true` et `STRIPE_TAX_CODE` corrects dans l'environnement de prod, backend redémarré.
- [ ] Un achat de test réel (ou en test mode miroir) confirme le bon taux sur la facture.
- [ ] L'affichage front indique bien « TVA estimée » (la TVA définitive figure sur la facture Stripe).

---

## 9. Périmètre & limites (à connaître)

- **Calcul, facturation et ventilation de la TVA** : couverts (initial + renouvellements).
- **Autoliquidation B2B intra-UE** : couverte (numéro de TVA transmis au checkout → `tax_id` Stripe → VIES en prod).
- **Déclarations / télédéclarations (filing)** : **non couvertes par cette intégration**. Stripe fournit les **rapports** (`Tax → Registrations → Reports` ou Stripe Tax exports) à remettre à votre comptable, ou via un partenaire de filing Stripe. La plateforme ne produit pas les déclarations fiscales elle-même.
- **Tarification Stripe Tax** : facturé par Stripe au prorata des transactions taxées (gratuit en mode test). À arbitrer côté exploitant.

---

## 10. Dépannage

| Symptôme | Cause probable | Correctif |
|----------|----------------|-----------|
| `Subscription.create` échoue après activation | `STRIPE_TAX_ENABLED=true` sans immatriculation | Déclarer une immatriculation (étape 4) ou repasser à `false` |
| TVA non calculée (0 €) alors qu'attendue | Adresse de facturation absente/incomplète sur la carte | Vérifier que le checkout collecte bien l'adresse ; l'app *fail-closed* si l'adresse est inexploitable |
| Reverse charge non appliqué en prod | N° de TVA invalide (VIES) ou même pays que l'origine | Vérifier le numéro ; l'autoliquidation ne s'applique qu'en **transfrontalier** UE |
| Démarrage backend KO | Variable mal formée | `STRIPE_TAX_ENABLED` doit valoir `true`/`false`, `STRIPE_TAX_CODE` un code `txcd_…` valide |
