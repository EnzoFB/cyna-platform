1. Organisation du projet

La gestion du projet est réalisée directement dans GitHub afin de centraliser :

le code

les tâches

le suivi d’avancement

les revues de code

Outils utilisés :

GitHub Issues → gestion des User Stories

GitHub Project (Kanban) → suivi de l’avancement

Milestones → phases du projet

Labels → classification des tâches

Pull Requests → revue de code

2. Phases du projet

Phase 1 — Initialisation & socle technique

Objectif : mettre en place les fondations de la plateforme.
Exemples :
infrastructure backend / frontend
authentification
catalogue produits
panier
commandes
paiement
sécurité de base

Phase 2 — Fonctionnalités cœur

Objectif : rendre la plateforme exploitable.
Exemples :
espace client
gestion abonnements
back-office
recherche produits
gestion contenu
optimisation performance

Phase 3 — Back-office & conformité

Objectif : préparer la plateforme à une exploitation réelle.
Exemples :
conformité RGPD
facturation
CI/CD
documentation
dashboard admin

Phase 4 — Tests & sécurisation finale

Objectif : stabiliser la plateforme avant livraison.
Exemples :
audit sécurité
tests E2E
corrections
optimisation finale

3. Gestion des tâches

Chaque User Story ou tâche technique est créée sous forme d’Issue GitHub.

Exemple :

AUTH-10 Authentification 2FA

Chaque issue doit contenir :

description
critères d’acceptation
responsable
labels
milestone

4. Labels utilisés
   Type
   type:feature
   type:tech
   type:bug
   type:doc
   type:test
   type:security
   Zone technique
   area:frontend
   area:backend
   area:admin
   area:infra
   area:payment
   Priorité
   priority:P1
   priority:P2
   priority:P3
   Statut
   status:ready
   status:blocked
   status:review
5. Kanban

Colonnes du projet :

Backlog
À faire
En cours
Review
Bloqué
Terminé

Cycle d’une tâche :

Backlog
↓
À faire
↓
En cours
↓
Review
↓
Terminé 6. Workflow Git

Le projet utilise 3 types de branches :

main
dev
feature/\*

- Branche main

Branche stable du projet.
Règles :
aucun commit direct
uniquement des Pull Requests depuis dev

- Branche dev

Branche d’intégration des fonctionnalités.
Elle sert à :
regrouper les fonctionnalités
tester l’intégration
préparer la version finale

- Branches feature

Chaque fonctionnalité possède une branche dédiée.
Exemples :
feature/AUTH-10-2fa
feature/PROD-03-api-crud-produits
feature/CART-04-page-panier 7. Cycle de développement

1. Créer une issue

Exemple :

AUTH-10 Authentification 2FA 2. Créer une branche
git checkout dev
git pull origin dev
git checkout -b feature/AUTH-10-2fa 3. Développer
git add .
git commit -m "AUTH-10 implémentation 2FA"
git push origin feature/AUTH-10-2fa 4. Pull Request

Créer une PR :

feature/AUTH-10-2fa → dev 5. Review

Un membre de l’équipe vérifie :
qualité du code
architecture
absence de bugs

6. Merge dans dev
   feature → dev

7. Tests

Tests d’intégration sur la branche dev.

8. Release

Quand les fonctionnalités sont validées :
dev → main
Après validation collective de l’équipe.

8. Bonnes pratiques

Ne jamais commit directement sur main
Une branche = une fonctionnalité
Toujours utiliser Pull Requests
Lier les PR aux issues

Exemple :
Closes #45
GitHub fermera automatiquement l’issue.

9. Résumé du workflow
   Issue
   ↓
   branche feature
   ↓
   Pull Request vers dev
   ↓
   review
   ↓
   merge dans dev
   ↓
   tests
   ↓
   Pull Request dev → main
   ↓
   merge final

Schéma du workflow Git du projet CYNA
+-------------------+
| main |
| version stable |
+---------▲---------+
|
| Pull Request
|
+---------+---------+
| dev |
| branche intégration|
+----▲--------▲-----+
| |
Pull Request | | Pull Request
| |
+--------------+ +--------------+
| |
+-------+--------+ +-------+--------+
| feature/AUTH-10| | feature/PROD-03|
| 2FA login | | API produits |
+----------------+ +----------------+

Cycle de développement visuel
Issue créée
│
▼
Création branche feature
│
▼
Développement
│
▼
Pull Request → dev
│
▼
Review de code
│
▼
Merge dans dev
│
▼
Tests d’intégration
│
▼
Pull Request dev → main
│
▼
Version stable
Exemple concret
Issue :
AUTH-10 Authentification 2FA

Branche :
feature/AUTH-10-2fa

Pull Request :
feature/AUTH-10-2fa → dev

Merge final :
dev → main
