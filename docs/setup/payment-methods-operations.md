# Moyens de paiement — Configuration & opérations hors code

> Document opérationnel listant **tout ce qu'il faut configurer en dehors du
> code** pour que la gestion des moyens de paiement fonctionne en production :
> Stripe Dashboard, conformité légale, variables d'environnement, tests
> post-déploiement, monitoring, support utilisateur.
>
> À lire **avant** la première mise en production et **à chaque** changement
> de mode (test ↔ live).

---

## Sommaire

1. [Vue d'ensemble du dispositif](#1-vue-densemble-du-dispositif)
2. [Configuration Stripe Dashboard](#2-configuration-stripe-dashboard)
3. [Conformité légale et RGPD](#3-conformité-légale-et-rgpd)
4. [Variables d'environnement](#4-variables-denvironnement)
5. [Tests manuels post-déploiement](#5-tests-manuels-post-déploiement)
6. [Monitoring et alerting](#6-monitoring-et-alerting)
7. [Procédures support](#7-procédures-support)
8. [Communication utilisateur](#8-communication-utilisateur)
9. [Maintenance récurrente](#9-maintenance-récurrente)

---

## 1. Vue d'ensemble du dispositif

> **Note** — Refonte du 2026-05-14 : passage en mode « delegated » radical.
> L'onglet PWA est désormais en lecture seule + un bouton « Ouvrir le
> portail Stripe ». Toutes les opérations actives passent par Stripe.

### Ce que fait notre code (UI custom)

- **Au checkout uniquement** :
  - Saisie d'une nouvelle carte via Stripe Elements + SetupIntent (la
    donnée carte ne touche jamais notre backend)
  - Sélection d'une adresse / carte enregistrée
  - Consentement RGPD explicite pour la sauvegarde des nouvelles
  - **Audit log du consentement** (table `payment_consent_log`, traçabilité
    Art. 7.1 RGPD : IP + User-Agent + horodatage + version du libellé)
  - Filtrage des cartes expirées du sélecteur
- **Onglet « Mes moyens de paiement »** :
  - Affichage **read-only** de la liste avec badges `Par défaut` / `Expirée`
  - **Un seul bouton** : « Ouvrir le portail Stripe »

### Ce qui est délégué à Stripe Customer Portal

- **Toutes les opérations actives** sur les cartes :
  - Ajout (en dehors du checkout) — mais la prochaine fois qu'on passe au
    checkout, l'ajout custom reste disponible
  - Suppression (avec gestion native des abos actifs)
  - Définition de la carte par défaut
  - Mise à jour d'une carte expirée
- Téléchargement des factures
- Annulation / pause d'abonnements

### Ce qui se synchronise automatiquement via webhooks

- `payment_method.attached` → cartes ajoutées via Stripe Portal
  apparaissent dans notre liste read-only
- `payment_method.detached` → cartes supprimées via Stripe Portal
  disparaissent de notre liste
- `payment_method.automatically_updated` → cartes mises à jour par
  l'émetteur (Card Account Updater Stripe) ont leur `last4` / `expiry`
  refresh en local sans action utilisateur

---

## 2. Configuration Stripe Dashboard

> **Faire la config en mode `test` ET en mode `live` séparément.** Stripe
> traite ces deux modes comme deux comptes distincts. Toute config faite en
> test n'est PAS automatiquement répliquée en live.

### 2.1. Signer le Data Processing Agreement (DPA)

Obligatoire pour la conformité RGPD : Stripe est sous-traitant et doit avoir
un DPA signé par votre entité.

- URL : [https://dashboard.stripe.com/legal/dpa](https://dashboard.stripe.com/legal/dpa)
- Sélectionner « EU-based controller » si Cyna est en France.
- Télécharger la copie signée et l'archiver dans le drive légal de
  l'entreprise.
- ☐ DPA signé en mode **test**
- ☐ DPA signé en mode **live** (compte production)

### 2.2. Configurer le Customer Portal

Settings → Billing → **Customer portal**

URL :
- Test : https://dashboard.stripe.com/test/settings/billing/portal
- Live : https://dashboard.stripe.com/settings/billing/portal

#### Fonctionnalités à activer

| Section | Réglage | Pourquoi |
|---|---|---|
| **Invoice history** | ☑ Activer | Obligation légale française : factures téléchargeables sur 10 ans |
| **Customer information** | ☐ Désactiver | Nous gérons nom/email côté Cyna |
| **Payment methods** | ☑ Activer tout (Add, Remove, Set default) | Coeur de la délégation |
| **Subscriptions — Cancel** | ☑ Activer | Permettre la résiliation depuis le portail (obligation Article L121-21-7 du Code de la conso pour les services à reconduction tacite) |
| **Subscriptions — Pause** | ☐ ou ☑ selon politique commerciale | À arbitrer avec le métier |
| **Subscriptions — Update plan** | ☐ Désactiver | Geré côté Cyna pour rester maître du catalogue |
| **Subscriptions — Update quantity** | ☐ Désactiver | Idem |

#### Branding

- ☐ Uploader le logo Cyna (PNG transparent, 256×256 min)
- ☐ Définir la couleur primaire = `#11C5DF` (cyan Cyna)
- ☐ Définir la couleur d'accent = `#4F73F5` (bleu primaire Cyna)
- ☐ Définir le **Business name** = `Cyna`
- ☐ Définir le **Support email** = `support@cyna.app` (à confirmer)
- ☐ Définir l'**URL des CGU** = `https://app.cyna.fr/terms`
- ☐ Définir l'**URL de la politique de confidentialité** = `https://app.cyna.fr/legal-notice`

#### Return URLs

- ☐ Whitelister les domaines de retour autorisés :
  - `http://localhost:4200` (dev local)
  - `https://app.cyna.fr` (live)
  - `https://staging.cyna.fr` (staging, si applicable)

> Le frontend appelle `paymentService.openBillingPortal(window.location.href)`,
> donc Stripe doit accepter le `returnUrl` envoyé. Sans whitelisting, Stripe
> renvoie une erreur.

### 2.3. Configurer les webhooks

URL :
- Test : https://dashboard.stripe.com/test/webhooks
- Live : https://dashboard.stripe.com/webhooks

#### Endpoints à créer

| Mode | URL | Description |
|---|---|---|
| Test | `https://staging.cyna.fr/api/v1/payments/webhook` (ou tunnel local pour dev) | Tester le flow complet sans toucher au live |
| Live | `https://api.cyna.fr/api/v1/payments/webhook` | Endpoint de production |

#### Liste des évènements à abonner (12 events au total)

**Payments / Subscriptions** (existants, à vérifier qu'ils sont toujours actifs) :

- ☐ `payment_intent.succeeded`
- ☐ `payment_intent.payment_failed`
- ☐ `invoice.paid`
- ☐ `invoice.payment_failed`
- ☐ `customer.subscription.created`
- ☐ `customer.subscription.updated`
- ☐ `customer.subscription.deleted`

**Payment methods** (les 3 nouveaux à ajouter — implémentation `SyncSavedPaymentMethodFromStripeCommand`) :

- ☐ `payment_method.attached`
- ☐ `payment_method.detached`
- ☐ `payment_method.automatically_updated`

#### Récupérer le secret de signature

- Après création de l'endpoint, Stripe affiche un `whsec_xxxxxxxx`.
- Le stocker dans la variable d'env `STRIPE_WEBHOOK_SECRET` du backend (cf
  [section 4](#4-variables-denvironnement)).
- ☐ Secret webhook stocké en mode **test**
- ☐ Secret webhook stocké en mode **live**

> ⚠️ Le secret est **différent** entre les modes test et live. Ne pas
> mélanger sinon les signatures sont rejetées.

### 2.4. Configurer la Card Account Updater (optionnel mais recommandé)

Settings → Subscriptions and emails → **Card account updater**

- ☑ Activer « Automatically update cards »
  - Quand un émetteur (Visa, Mastercard) réémet une carte, Stripe met
    automatiquement à jour le numéro côté Customer sans intervention de
    l'utilisateur.
  - Combiné au webhook `payment_method.automatically_updated`, notre cache
    local se met à jour automatiquement.
  - **Coût** : facturation à la transaction réussie qui n'aurait pas eu lieu
    sans l'update. Voir la grille tarifaire Stripe.

### 2.5. Configurer les emails Stripe (optionnel mais recommandé)

Settings → Subscriptions and emails → **Customer emails**

- ☑ « Send finalized invoices and credit notes » → facture automatique par
  email (obligation légale française)
- ☑ « Send emails about expiring cards » → email à J-30 avant expiration
- ☑ « Send emails when card payments fail » → email si renouvellement échoue
- ☐ « Send emails for refunds » → selon politique

Ces emails sont envoyés depuis Stripe avec le branding configuré en 2.2.

### 2.6. API keys & restricted keys

Settings → Developers → **API keys**

#### Vérifier l'usage actuel

- La clé `sk_live_...` doit être stockée **uniquement** dans
  `STRIPE_SECRET_KEY` du backend prod.
- La clé `pk_live_...` peut être publique (`STRIPE_PUBLISHABLE_KEY`,
  injectée dans les bundles PWA).

#### Bonne pratique : restricted keys pour les jobs

Si on a des jobs cron ou des integrations side-car (reporting, BI) :

- ☐ Créer une **restricted key** par integration avec uniquement les scopes
  nécessaires (jamais full access).
- ☐ Documenter dans un fichier interne le rôle de chaque restricted key.

---

## 3. Conformité légale et RGPD

### 3.1. Mettre à jour les CGV / CGU

Fichier : [`cyna-frontend/projects/pwa/src/assets/i18n/{fr,en}.json`](../../cyna-frontend/projects/pwa/src/assets/i18n/) section `terms.*`

Pages affichées :
- `https://app.cyna.fr/terms`
- `https://app.cyna.fr/legal-notice`

#### Articles à ajouter / mettre à jour

- ☐ **Article sous-traitant de paiement** :

  > « Cyna fait appel à la société Stripe, Inc. (510 Townsend Street, San
  > Francisco, CA 94103, États-Unis) et à sa filiale européenne Stripe
  > Technology Europe Limited (1 Grand Canal Street Lower, Dublin 2,
  > Irlande) en tant que sous-traitant de paiement pour le traitement des
  > transactions, la conservation sécurisée des moyens de paiement et la
  > gestion des renouvellements d'abonnement. Les données de carte bancaire
  > ne transitent jamais par les serveurs de Cyna : elles sont collectées
  > directement par l'iframe sécurisée Stripe Elements et stockées
  > exclusivement chez Stripe, prestataire certifié PCI-DSS niveau 1.
  > Stripe agit en qualité de sous-traitant au sens de l'article 28 du
  > RGPD ; les modalités de cette sous-traitance sont définies par le
  > Stripe Data Processing Agreement disponible sur stripe.com/legal/dpa. »

- ☐ **Article conservation des moyens de paiement** :

  > « Lorsque vous souscrivez un abonnement, votre moyen de paiement est
  > conservé par notre prestataire Stripe afin de permettre les
  > renouvellements automatiques de votre abonnement. À tout moment, vous
  > pouvez consulter, modifier ou supprimer vos moyens de paiement
  > enregistrés depuis votre espace personnel (« Mes moyens de paiement »).
  > La gestion détaillée de ces moyens de paiement est assurée via le
  > portail sécurisé Stripe, accessible depuis votre compte. »

- ☐ **Article résiliation** : confirmer que la procédure de résiliation
  passe par le portail Stripe est mentionnée (Article L121-21-7 du Code de
  la consommation).

### 3.2. Politique de confidentialité

#### Sections à compléter

- ☐ **Sous-traitants** : ajouter Stripe à la liste (siège, finalité, durée
  de conservation, transferts hors UE — Stripe est SCC + adequacy decision
  US covered).
- ☐ **Catégories de données traitées** : ajouter « moyens de paiement
  enregistrés (token Stripe + 4 derniers chiffres + date d'expiration —
  jamais le numéro complet) ».
- ☐ **Base légale** :
  - Pour la conservation pendant l'abonnement actif : **exécution du
    contrat** (Art. 6.1.b RGPD).
  - Pour la conservation au-delà (réutilisation prochains achats) :
    **consentement** (Art. 6.1.a RGPD) → matérialisé par la checkbox du
    checkout.
- ☐ **Durée de conservation** : tant que l'abonnement est actif + durée
  légale comptable (10 ans pour les factures, Art. L123-22 Code de
  commerce).
- ☐ **Droits des personnes** : décrire comment l'utilisateur peut accéder,
  rectifier, supprimer ses moyens de paiement (via l'onglet et/ou le
  portail Stripe).

### 3.3. Audit log du consentement (Art. 7.1 RGPD) — **désormais implémenté**

Depuis la refonte du 2026-05-14, **chaque fois** qu'un utilisateur coche la
checkbox « Réutiliser cette carte » au checkout, une ligne est insérée dans
la table `payment_schema.payment_consent_log` :

| Colonne | Contenu |
|---|---|
| `id` | UUID auto-généré |
| `user_id` | FK vers l'utilisateur |
| `action` | `SAVE_CARD_AT_CHECKOUT` (enum) |
| `label_version` | Version du libellé du consentement (`2026-05-14` actuellement) |
| `stripe_payment_method_id` | Le `pm_xxx` Stripe associé |
| `ip_address` | IP du client (extraite serveur-side de `X-Forwarded-For` ou socket) |
| `user_agent` | User-Agent HTTP (tronqué à 512 caractères) |
| `given_at` | Horodatage `NOW()` |

#### À faire côté Ops

- ☐ **Bump `CARD_CONSENT_LABEL_VERSION`** dans
  [`consent-log.service.ts`](../../cyna-frontend/projects/pwa/src/app/core/services/consent-log.service.ts)
  **chaque fois** que tu modifies le wording de la checkbox de consentement
  dans `fr.json` ou `en.json` (clés `checkout.payment.saveConsent` /
  `saveConsentHelp`). Sinon les anciennes preuves de consentement
  pointeront vers un libellé qui ne reflète plus ce que l'utilisateur a vu.
- ☐ **Conserver l'historique des libellés**. Garder dans un fichier
  versionné (par ex. `docs/legal/consent-labels-history.md`) le mapping
  `label_version` → texte exact affiché. Sans ça, la version stockée en
  BDD ne sert à rien quand on doit prouver ce que le user a vu.
- ☐ **Audit régulier** : extraire les logs en cas de plainte CNIL ou de
  demande utilisateur. Requête type :

  ```sql
  SELECT id, action, label_version, given_at, ip_address, user_agent
  FROM payment_schema.payment_consent_log
  WHERE user_id = '<uuid>'
  ORDER BY given_at DESC;
  ```

- ☐ **Politique de rétention** : à arbitrer avec le DPO. Recommandation
  CNIL pour les preuves de consentement = 5 ans après le retrait du
  consentement ou la fin de la relation contractuelle. **Ne JAMAIS
  supprimer une row** sans validation DPO.

### 3.4. Registre des activités de traitement (Art. 30 RGPD)

Ajouter au registre (Excel / outil dédié) une ligne dédiée :

| Champ | Valeur |
|---|---|
| **Nom du traitement** | Gestion des moyens de paiement enregistrés |
| **Finalité** | Permettre les renouvellements automatiques d'abonnement et la réutilisation simplifiée pour les achats futurs |
| **Base légale** | Exécution du contrat (renouvellement) + consentement (réutilisation) |
| **Catégories de personnes** | Clients ayant souscrit un abonnement |
| **Catégories de données** | Token de paiement (Stripe `pm_xxx`), brand de la carte, 4 derniers chiffres, mois/année d'expiration, nom du titulaire |
| **Destinataires** | Stripe Payments Europe Ltd (sous-traitant) |
| **Transferts hors UE** | Oui — Stripe Inc. (US) sous Standard Contractual Clauses |
| **Durée de conservation** | Pendant la durée de l'abonnement + 10 ans pour les factures associées |
| **Mesures de sécurité** | Tokenisation Stripe (PCI-DSS niveau 1), pas de stockage du PAN, accès chiffré, authentification forte (SCA/3DS2) |

### 3.4. Information sous-traitants à l'utilisateur

Sur la page de paiement et la page « Mes moyens de paiement », un texte
court doit indiquer le rôle de Stripe (déjà présent dans les checkboxes
de consentement et le bandeau du portail — vérifier que le texte est bien
visible et non masqué par les styles).

- ☐ Texte sur la checkbox d'enregistrement de la carte (checkout) mentionne
  Stripe explicitement
- ☐ Bandeau Stripe Portal sur l'onglet « Mes moyens de paiement » mentionne
  le caractère sécurisé du portail

---

## 4. Variables d'environnement

Fichier : `cyna-backend/.env` (et secrets manager en prod)

| Variable | Valeur en test | Valeur en prod | Rôle |
|---|---|---|---|
| `STRIPE_SECRET_KEY` | `sk_test_...` | `sk_live_...` | Clé secrète backend pour appels Stripe API |
| `STRIPE_PUBLISHABLE_KEY` | `pk_test_...` | `pk_live_...` | Clé publique, injectée dans le bundle PWA |
| `STRIPE_WEBHOOK_SECRET` | `whsec_test_...` (mode test) | `whsec_live_...` (mode live) | Validation de signature webhook — **différent par mode et par endpoint** |
| `JWT_SECRET` | (déjà configuré) | (déjà configuré) | Inchangé |
| `BREVO_API_KEY` | (déjà configuré) | (déjà configuré) | Inchangé |

### Checklist par environnement

- ☐ Dev local (`cyna-backend/.env`) → mode test
- ☐ Staging → mode test (idéalement compte Stripe de staging dédié)
- ☐ Production → mode live

> ⚠️ Ne **jamais** committer `.env` dans Git. Vérifier `.gitignore`.
> Utiliser un secrets manager (AWS Secrets Manager, Vault, GitHub Secrets…)
> pour la prod.

### Variables côté frontend (PWA)

Fichier : `cyna-frontend/projects/pwa/src/environments/environment.{development,production}.ts`

- ☐ `stripePublishableKey` aligné avec le mode du backend (test ↔ test,
  live ↔ live).
- ☐ `apiUrl` pointe vers le bon backend.

---

## 5. Tests manuels post-déploiement

> À effectuer après chaque déploiement majeur, en mode test d'abord, puis
> avec une vraie carte en live avant ouverture publique.
>
> **Refonte 2026-05-14** : depuis le passage en mode délégué, l'onglet
> « Mes moyens de paiement » ne permet plus d'ajouter / supprimer / set
> default depuis l'UI custom. Toutes ces actions passent par le portail
> Stripe. Les sections 5.4 et 5.6 ont été adaptées en conséquence ; la
> section 5.10 a été ajoutée pour tester le nouvel audit log RGPD.

### 5.1. Cartes de test Stripe

Pour le mode test, utiliser les numéros officiels :

| Numéro | Comportement |
|---|---|
| `4242 4242 4242 4242` | Succès (Visa) |
| `4000 0025 0000 3155` | Nécessite 3DS2 (auth forte) |
| `4000 0000 0000 0002` | Refusée (generic decline) |
| `4000 0000 0000 9995` | Refusée (insufficient funds) |
| `4000 0000 0000 0069` | Carte expirée |
| `4000 0000 0000 0341` | Échec au moment de la sauvegarde (utile pour tester error UX) |

Date d'expiration : n'importe quelle date future. CVC : n'importe quel
nombre à 3 chiffres.

### 5.2. Checkout — ajout d'une nouvelle carte

- ☐ Aller sur `/checkout` avec des produits dans le panier
- ☐ Choisir « Nouvelle adresse » et la remplir
- ☐ Vérifier que la checkbox « Enregistrer cette adresse » est **cochée**
  par défaut
- ☐ Choisir « Nouvelle carte » et saisir `4242 4242 4242 4242`
- ☐ Vérifier que la checkbox « Réutiliser cette carte » est **décochée**
  par défaut
- ☐ Cocher la checkbox carte
- ☐ Valider le paiement
- ☐ Vérifier la redirection vers `/checkout/success`
- ☐ Aller sur `/account` → onglet adresses → l'adresse y figure
- ☐ Aller sur `/account` → onglet « Mes moyens de paiement » → la carte y
  figure avec badge `Par défaut`

### 5.3. Checkout — réutilisation

- ☐ Vider le panier et re-remplir
- ☐ Aller sur `/checkout` → vérifier que :
  - Le mode adresse est « Adresse enregistrée » par défaut
  - L'adresse pré-sélectionnée est celle marquée par défaut
  - Le mode paiement est « Carte enregistrée » par défaut
  - La carte pré-sélectionnée est celle marquée par défaut
- ☐ Valider sans rien modifier → paiement OK

### 5.4. Onglet moyens de paiement — set default (via Stripe Portal)

- ☐ Avoir au moins 2 cartes enregistrées
- ☐ Aller sur `/account` → onglet « Mes moyens de paiement »
- ☐ Vérifier que la liste est en lecture seule (pas de bouton « Par défaut »
  sur les cartes — c'est normal depuis la refonte 2026-05-14)
- ☐ Cliquer sur « Ouvrir le portail Stripe »
- ☐ Dans le portail, définir une autre carte comme défaut
- ☐ Revenir sur Cyna, recharger → le badge `Par défaut` reflète bien le
  choix (via le webhook `payment_method.attached` ou un refresh manuel)
- ☐ Aller sur Stripe Dashboard → Customer → vérifier que
  `invoice_settings.default_payment_method` pointe sur la nouvelle carte

### 5.5. Onglet moyens de paiement — cartes expirées

> Pour ce test, il faut une carte avec une date d'expiration passée.
> Solution la plus simple : insérer une ligne directement en BDD avec un
> `exp_year` ancien.

```sql
UPDATE payment_schema.saved_payment_methods
SET exp_month = '01', exp_year = '2020'
WHERE id = '<uuid-carte-test>';
```

- ☐ Recharger la page → badge `Expirée` rouge visible
- ☐ Bouton « Par défaut » désactivé avec tooltip
- ☐ Aller sur `/checkout` → la carte expirée n'apparaît PAS dans le
  sélecteur

### 5.6. Stripe Customer Portal

- ☐ Cliquer sur « Ouvrir le portail Stripe » dans l'onglet moyens de
  paiement
- ☐ Vérifier la redirection vers `billing.stripe.com`
- ☐ Vérifier que le logo Cyna et les couleurs sont appliqués
- ☐ Ajouter une nouvelle carte depuis le portail
- ☐ Cliquer « Retour » → revenir sur l'onglet moyens de paiement
- ☐ **Attendre quelques secondes** puis recharger → la nouvelle carte
  apparaît dans la liste (sync via webhook `payment_method.attached`)

### 5.7. Stripe Customer Portal — suppression

- ☐ Depuis le portail Stripe, supprimer une carte qui n'est pas associée à
  un abonnement actif
- ☐ Revenir sur Cyna, recharger → la carte a disparu (webhook
  `payment_method.detached`)
- ☐ Tenter de supprimer depuis le portail Stripe une carte associée à un
  abonnement actif → Stripe doit prévenir l'utilisateur

### 5.8. Webhooks — vérifier les évènements reçus

Stripe Dashboard → Developers → Webhooks → cliquer sur l'endpoint
configuré → onglet « Events » :

- ☐ Pour chaque event tiré pendant les tests, vérifier le statut `200 OK`
  et la durée < 2s
- ☐ Vérifier qu'**aucun** event n'est en `failed` ou `pending retry`
- ☐ En cas de `failed`, cliquer sur l'event pour voir le payload + la
  réponse du backend → diagnostiquer

### 5.10. Audit log de consentement RGPD (nouveau — 2026-05-14)

Après un test de checkout avec la checkbox « Réutiliser cette carte »
cochée :

- ☐ Connecté à la BDD, exécuter :

  ```sql
  SELECT id, action, label_version, given_at, ip_address,
         LEFT(user_agent, 60) AS user_agent_short,
         stripe_payment_method_id
  FROM payment_schema.payment_consent_log
  WHERE user_id = '<uuid-du-user-de-test>'
  ORDER BY given_at DESC;
  ```

- ☐ Vérifier qu'une row a été insérée :
  - `action` = `SAVE_CARD_AT_CHECKOUT`
  - `label_version` = `2026-05-14` (ou la version courante)
  - `ip_address` non null
  - `user_agent` non null
  - `stripe_payment_method_id` correspond au `pm_xxx` qu'on vient de sauver
  - `given_at` à quelques secondes près de l'horodatage du clic
- ☐ Re-tester avec la case décochée → **aucune** row ne doit être insérée

### 5.9. Pré-mise en production live

À ne faire **qu'après** validation complète en test :

- ☐ Basculer toutes les variables d'env vers les valeurs `live`
- ☐ Déployer le backend et le frontend en prod
- ☐ Effectuer une **vraie transaction** avec une carte personnelle de
  l'équipe (montant minimal, par ex. un abonnement à 1€/mois si possible)
- ☐ Annuler immédiatement l'abonnement via le portail Stripe
- ☐ Vérifier le remboursement / la non-facturation
- ☐ Vérifier la facture reçue par email

---

## 6. Monitoring et alerting

### 6.1. Métriques à suivre dans Stripe Dashboard

URL : https://dashboard.stripe.com/dashboard

- **Daily transactions** — détecter une chute brutale (panne potentielle)
- **Payment success rate** — devrait être > 95% en EU avec SCA correctement
  configuré
- **Failed payments breakdown** — identifier les causes principales
  (declined, expired, 3DS failed…)
- **Disputes** — escalader immédiatement si une dispute apparaît

### 6.2. Métriques à suivre côté backend

Logs à surveiller (Loki, Grafana, ou équivalent) :

| Pattern | Signification | Action |
|---|---|---|
| `[set-default] Stripe customer update failed` | Sync default Stripe en échec | Vérifier la clé API et la connectivité |
| `[delete-pm] Stripe list-subs failed` | Liste subs Stripe inaccessible | Idem |
| `[delete-pm] Stripe default sync failed after promotion` | Default désynchronisé après suppression | Réparer en re-cliquant « Par défaut » côté UI |
| `[sync-pm] attached event for unknown stripe customer` | Webhook orphelin | Probablement test/live cross-contamination, vérifier les secrets |
| `Stripe webhook received` (absence de logs) | Webhooks non reçus | Vérifier endpoint configuré, signature, firewall |

### 6.3. Alertes à configurer

Si vous utilisez Sentry / Datadog / Grafana Alerting :

- ☐ Alerte sur **chute du taux de succès paiement** (< 90% sur 1h)
- ☐ Alerte sur **erreurs webhook** (status code non-2xx sur l'endpoint
  `/payments/webhook`)
- ☐ Alerte sur **erreurs `STRIPE_ERROR`** dans les logs (>= 5 en 10 min)
- ☐ Alerte sur **renouvellement échoué** : webhook `invoice.payment_failed`
  avec `billing_reason=subscription_cycle` (action support requise)

---

## 7. Procédures support

### 7.1. « Le portail Stripe montre N cartes mais "Mes moyens de paiement" en montre moins »

⚠️ **Ce n'est pas forcément un bug — c'est souvent le comportement voulu.**
« Mes moyens de paiement » lit le cache local `saved_payment_methods` ; le
portail Stripe montre **toutes** les cartes attachées au Customer. Une ligne
locale n'est créée que (1) au checkout **si l'utilisateur a coché « Réutiliser
cette carte »**, ou (2) via le webhook `payment_method.attached` (ajout via
portail). Deux causes possibles, souvent combinées :

**Cause A — par design (modèle de consentement RGPD).** Toute carte utilisée
à un checkout est attachée au Customer Stripe (`usage=off_session`, requis
pour les renouvellements) et apparaît donc dans le portail. Mais si
l'utilisateur n'a **pas coché** « réutiliser cette carte », elle est
**volontairement non affichée** dans « Mes moyens de paiement ». C'est le
cœur du modèle de consentement — ne pas le traiter comme un défaut.

**Cause B — webhook non configuré.** Si les 3 events
`payment_method.{attached,detached,automatically_updated}` ne sont pas
abonnés (cf [§2.3](#23-configurer-les-webhooks)) ou non reçus, les cartes
ajoutées via le portail ne se synchronisent jamais en local.

**Diagnostic** (psql sur la base) :

```sql
-- cartes locales du user
SELECT stripe_payment_method_id, brand, last4, is_default
  FROM payment_schema.saved_payment_methods WHERE user_id='<uuid>';
-- consentements donnés au checkout
SELECT stripe_payment_method_id, given_at
  FROM payment_schema.payment_consent_log
  WHERE user_id='<uuid>' AND action='SAVE_CARD_AT_CHECKOUT';
-- webhooks payment_method.* effectivement traités
SELECT event_id, event_type, processed_at
  FROM payment_schema.processed_stripe_events
  WHERE event_type LIKE 'payment_method.%' ORDER BY processed_at DESC;
```

| Observation | Conclusion |
|---|---|
| Peu de consentements, aucune carte ajoutée via portail | **Cause A** — comportement RGPD voulu, rien à corriger |
| Cartes ajoutées via portail mais 3ᵉ requête vide | **Cause B** — events `payment_method.*` non abonnés/non reçus → corriger en [§2.3](#23-configurer-les-webhooks) (test ET live, secrets distincts) |
| Webhooks abonnés mais events `failed` dans le Dashboard | Endpoint injoignable au moment de l'ajout (firewall / pas de `stripe listen` en local) → **Resend** depuis le Dashboard |

**Workaround utilisateur** : cliquer sur « Ouvrir le portail Stripe » puis
« Retour » ; si les webhooks sont correctement abonnés, l'ajout/suppression
fait au portail déclenche `SyncSavedPaymentMethodFromStripeCommand` et la
liste se resynchronise au rechargement.

### 7.2. « Mon abonnement n'a pas pu être renouvelé »

Cause typique : carte expirée ou bloquée.

**Procédure** :
1. Stripe Dashboard → chercher le Customer → Subscriptions → trouver
   l'abonnement en `past_due`
2. Identifier la PaymentMethod attachée
3. Envoyer un email à l'utilisateur l'invitant à mettre à jour son moyen
   de paiement depuis le portail Stripe
4. Stripe relance automatiquement le paiement selon la dunning policy
   (Settings → Subscriptions and emails → Manage failed payments)

### 7.3. « Je veux supprimer ma carte mais le bouton ne fonctionne pas »

C'est probablement parce que la carte est associée à un abonnement actif.
Notre UI ne fait pas la suppression — c'est délégué au portail Stripe.

**Procédure** :
1. Diriger l'utilisateur vers « Ouvrir le portail Stripe »
2. Dans le portail, l'utilisateur peut annuler ses abonnements puis
   supprimer la carte

### 7.4. « Je n'ai pas reçu ma facture »

Vérifier dans Stripe Dashboard → Customer → Invoices que la facture a bien
été émise et envoyée. Si oui, la renvoyer manuellement (bouton « Send
invoice »).

Si non, vérifier que l'option « Send finalized invoices » est activée dans
Settings → Subscriptions and emails.

### 7.5. « Je demande la suppression complète de mes données (RGPD) »

1. ☐ Aller dans Stripe Dashboard → Customer → bouton **Delete customer**
   - Cela détache toutes les cartes (`pm_xxx` retirés) et anonymise le
     Customer Stripe
   - Les factures restent (obligation légale 10 ans)
2. ☐ Côté Cyna BDD : exécuter le script de suppression utilisateur
   (à scripter si pas déjà disponible) :
   - Supprimer les rows `saved_payment_methods` du user
   - Anonymiser le user dans `user_schema.users` (email pseudo, name
     vide, etc.)
   - Garder les `orders` / `subscriptions` historiques pour la compta
3. ☐ Confirmer la suppression à l'utilisateur par email

---

## 8. Communication utilisateur

### 8.1. Emails transactionnels gérés par Stripe

Configurés en [2.5](#25-configurer-les-emails-stripe-optionnel-mais-recommandé) :

- Facture émise / payée
- Carte expirant bientôt (J-30)
- Paiement de renouvellement échoué

### 8.2. Emails métier (à gérer côté Cyna via Brevo)

- ☐ Email de bienvenue (déjà géré ailleurs probablement)
- ☐ Confirmation d'abonnement
- ☐ **Email « moyen de paiement enregistré »** — facultatif mais
  professionnel : à l'enregistrement d'une nouvelle carte, envoyer un email
  avec le brand + last4 + instruction pour supprimer si non reconnu (cf
  pratique des banques). Aide à détecter les fraudes / usurpations.

### 8.3. FAQ utilisateur à publier

À ajouter dans la section aide / FAQ du site :

- « Pourquoi suis-je redirigé vers Stripe pour supprimer une carte ? »
- « Mon abonnement va expirer, que dois-je faire ? »
- « Comment télécharger mes factures ? »
- « Mes données bancaires sont-elles stockées chez Cyna ? » (non — Stripe
  uniquement)
- « Comment annuler un abonnement ? »

---

## 9. Maintenance récurrente

### 9.1. Mensuel

- ☐ Vérifier les logs d'erreur Stripe (cf [6.2](#62-métriques-à-suivre-côté-backend))
- ☐ Vérifier le taux de succès des paiements
- ☐ Vérifier le nombre de webhooks `failed` (devrait être 0)

### 9.2. Trimestriel

- ☐ Réviser les permissions des **restricted keys** Stripe
- ☐ Réviser les **return URLs** whitelistées du Customer Portal
- ☐ Vérifier que les CGV / politique de confidentialité reflètent toujours
  l'état du système (notamment liste des sous-traitants)

### 9.3. Annuel

- ☐ **Renouveler le DPA** si nécessaire (Stripe envoie une notification)
- ☐ Re-vérifier l'attestation **PCI-DSS SAQ A** (formulaire à signer
  annuellement, Stripe en fournit un template)
- ☐ Auditer le registre RGPD Art. 30 et confirmer qu'il est à jour
- ☐ Vérifier que les certificats TLS de l'endpoint webhook sont valides et
  renouvelés
- ☐ Vérifier la conformité réglementaire continue (PSD3 arrive en 2026/2027,
  surveiller les annonces Stripe)

### 9.4. Au changement de version de l'API Stripe

Stripe met à jour son API ~3 fois par an. Notre code lit le JSON brut des
webhooks pour rester version-agnostic (cf commentaire dans
`StripePaymentAdapter.parseWebhookEvent`), mais certaines évolutions
peuvent impacter :

- ☐ Lire les **release notes** Stripe à chaque version majeure
- ☐ Tester en mode test avec la nouvelle version pinned avant de basculer
  en live
- ☐ Mettre à jour la dépendance `stripe-java` du backend si nécessaire

---

## Annexe — Glossaire

| Terme | Définition |
|---|---|
| **PCI-DSS** | Payment Card Industry Data Security Standard — norme de sécurité pour le traitement des cartes |
| **SAQ A** | Self-Assessment Questionnaire A — niveau de conformité PCI-DSS le plus léger, applicable quand le numéro de carte ne touche jamais nos serveurs |
| **SCA** | Strong Customer Authentication — obligation européenne d'authentification forte (PSD2) |
| **3DS2** | 3-D Secure version 2 — mécanisme technique mettant en œuvre la SCA |
| **DPA** | Data Processing Agreement — contrat de sous-traitance imposé par le RGPD |
| **PaymentMethod (`pm_xxx`)** | Identifiant Stripe d'un moyen de paiement tokenisé |
| **Customer (`cus_xxx`)** | Identifiant Stripe d'un utilisateur |
| **SetupIntent** | Flow Stripe pour collecter une carte sans débit immédiat |
| **Card Account Updater** | Service Stripe qui met à jour automatiquement les cartes réémises par l'émetteur |

---

## Annexe — Liens utiles

- [Stripe Customer Portal docs](https://stripe.com/docs/billing/subscriptions/customer-portal)
- [Stripe Webhooks docs](https://stripe.com/docs/webhooks)
- [Stripe Card testing](https://stripe.com/docs/testing)
- [Stripe DPA](https://dashboard.stripe.com/legal/dpa)
- [PCI-DSS SAQ A](https://www.pcisecuritystandards.org/document_library/?category=saqs)
- [CNIL — Registre Art. 30](https://www.cnil.fr/fr/RGPD-le-registre-des-activites-de-traitement)
- [Code Cyna — `payment` module](../../cyna-backend/src/main/java/com/cyna/modules/payment/)
- [Code Cyna — onglet moyens de paiement PWA](../../cyna-frontend/projects/pwa/src/app/features/account/tabs/payment-methods/)

---

*Dernière mise à jour : 2026-05-14 (refonte Option A radicale + audit log
RGPD). À mettre à jour à chaque modification significative de la
configuration ou de la conformité.*
