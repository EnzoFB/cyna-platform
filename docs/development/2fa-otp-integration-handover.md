# Ticket 2FA OTP - Reste a faire (integration et branchement)

## Ce qui est deja fait dans le backend

- Nouveau flow de connexion en 2 etapes:
  - `POST /api/v1/auth/login`: verifie `email + mot de passe`, cree un challenge OTP, renvoie `challengeId`.
  - `POST /api/v1/auth/login/verify-otp`: verifie `challengeId + otpCode`, puis renvoie les tokens JWT.
- Generation OTP numerique (6 chiffres).
- Expiration OTP (5 minutes).
- Validation OTP + gestion des erreurs metier (`invalid`, `expired`, `already used`).
- Persistance OTP en base:
  - migration `V3__create_login_otp_challenges_table.sql`
  - repository + entity JPA + mapper.
- Tests unitaires du coeur OTP ajoutes et passants.

## Ce qui reste a faire pour l'integration email reelle

1. Remplacer l'adapter de livraison OTP.
- Classe actuelle: `LoggingOtpDeliveryAdapter` (log seulement).
- A faire: implementer un adapter reel `OtpDeliveryPort` (SMTP/SendGrid/Mailgun/etc.).

2. Ajouter la configuration provider.
- Variables a ajouter selon provider:
  - hote SMTP / API URL
  - identifiants (API key, username/password)
  - expediteur (`from`)
  - timeout/retry
- Stocker les secrets hors repo (vault/secret manager/variables securees CI-CD).

3. Brancher un template email OTP.
- Template HTML + texte brut.
- Contenu minimal:
  - code OTP
  - date/heure d'expiration
  - message de securite ("si vous n'etes pas a l'origine de cette demande...").
- Ajouter i18n si besoin produit.

4. Ajouter la robustesse d'envoi.
- Retry controle (ex: 1-2 tentatives max).
- Journalisation technique sans exposer le code OTP en production.
- Metriques: taux d'envoi, erreurs provider, latence.

## Ce qui reste a faire pour le branchement frontend

1. Adapter le login UI en 2 etapes.
- Etape 1: submit email/password -> recuperer `challengeId`.
- Etape 2: afficher saisie OTP -> appeler `/auth/login/verify-otp`.

2. Gestions d'erreurs UX.
- Mauvais OTP.
- OTP expire.
- Challenge invalide/deja utilise.
- Eventuellement bouton "renvoyer un code" (endpoint a prevoir si demande metier).

3. Gestion etat/session.
- Ne stocker tokens qu'apres succes de `verify-otp`.
- Stocker `challengeId` de maniere temporaire et nettoyee apres usage.

## Points securite recommandes (prochaine iteration)

1. Limitation de tentative OTP.
- Ex: nombre max d'essais par challenge et/ou par utilisateur.

2. Limitation de frequence des demandes OTP.
- Ex: anti-spam sur `/auth/login`.

3. Purge des OTP expires/consommes.
- Job planifie pour nettoyer la table.

4. Tracabilite securite.
- Audit des evenements login OTP (sans journaliser le code brut).

## Checklist de validation integration

1. Flux complet manuel:
- `login` -> email recu -> `verify-otp` -> tokens valides.

2. Cas erreurs:
- code faux
- code expire
- challenge reutilise
- provider email indisponible.

3. Verification post-deploiement:
- variables d'environnement en place
- alerting sur taux d'echec envoi
- absence de logs OTP en clair en environnement production.
