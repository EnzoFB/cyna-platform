# Glossaire

Ce document définit les principaux concepts, termes et patterns utilisés dans le projet **CYNA Platform**.

Son objectif est de garantir une compréhension commune de l’architecture et des choix techniques.

## Concepts d’architecture

### Modular Monolith

Un *Modular Monolith* est une application monolithique structurée en modules indépendants.

Chaque module possède :

- son domaine métier ;
- ses cas d’usage ;
- ses repositories ;
- ses endpoints.

Les modules communiquent via des contrats internes, et non via la base de données.

**Avantages :**

- simplicité de déploiement ;
- forte maintenabilité ;
- évolutivité vers des microservices.

### Clean Architecture

La *Clean Architecture* organise le code en couches afin de séparer :

- la logique métier ;
- les cas d’usage ;
- les interfaces externes ;
- les frameworks.

Structure utilisée dans le projet :

- `domain`
- `application`
- `interfaces`
- `infrastructure`

**Règle principale :**

- Les dépendances pointent toujours vers l’intérieur.
- Le domaine ne dépend d’aucune technologie.

### Hexagonal Architecture

L’architecture hexagonale est un modèle proche de la *Clean Architecture* qui introduit les concepts de :

- ports ;
- adapters.

Le cœur métier expose des ports (interfaces), implémentés par des adapters (JPA, API externes, etc.).

## Patterns applicatifs

### CQRS (Command Query Responsibility Segregation)

CQRS sépare les opérations :

- **Command** → modifie l’état du système ;
- **Query** → lit les données.

Exemples :

- `CreateOrderCommand`
- `GetOrderQuery`

**Avantages :**

- code plus clair ;
- séparation lecture/écriture ;
- meilleure testabilité.

### Repository Pattern

Le *Repository Pattern* abstrait l’accès aux données.

Le domaine dépend d’une interface de repository, tandis que l’infrastructure fournit l’implémentation.

Exemples :

- `ProductRepository` (interface)
- `JpaProductRepositoryAdapter` (implémentation)

**Avantages :**

- découplage vis-à-vis de la base de données ;
- testabilité.

### Unit of Work (UoW)

Le *Unit of Work* garantit que plusieurs opérations sur les données sont exécutées dans une seule transaction.

Dans ce projet, cela est généralement assuré par les transactions Spring.

**Objectifs :**

- cohérence des données ;
- rollback en cas d’erreur.

### Result Pattern

Le *Result Pattern* encapsule le résultat d’une opération.

Au lieu de lancer une exception pour une erreur métier, une méthode retourne :

- `Result<T>`

Contenant :

- `success`
- `value`
- `error`

**Avantages :**

- gestion explicite des erreurs ;
- code plus prévisible.

### Specification Pattern

Le *Specification Pattern* permet de construire dynamiquement des filtres pour les requêtes.

Il est particulièrement utile pour les recherches complexes.

Exemples :

- filtre par prix ;
- filtre par catégorie ;
- filtre par disponibilité.

Les specifications peuvent être combinées.

## Patterns d’intégration

### Domain Event

Un *Domain Event* représente un événement important qui vient de se produire dans le domaine.

Exemples :

- `UserRegistered`
- `OrderPaid`
- `SubscriptionActivated`

Les événements permettent aux modules de réagir sans dépendre directement les uns des autres.

### Outbox Pattern

L’*Outbox Pattern* garantit la fiabilité de la publication d’événements.

Principe :

- un événement est enregistré dans une table *outbox* ;
- un worker publie cet événement ;
- l’événement est marqué comme traité.

**Avantages :**

- évite la perte d’événements ;
- cohérence transactionnelle.

### Idempotency

Une opération est idempotente si elle peut être exécutée plusieurs fois sans changer le résultat.

Exemple :

- si un webhook de paiement est reçu deux fois, la commande ne doit être créée qu’une seule fois.

Ce mécanisme est essentiel pour :

- les paiements ;
- les webhooks ;
- les confirmations de commande.

### Anti-Corruption Layer (ACL)

L’*Anti-Corruption Layer* protège le domaine des dépendances externes.

Elle traduit les modèles externes (Stripe, API tierces, etc.) en modèles internes.

**Avantages :**

- isolation du domaine ;
- facilité de remplacement d’un provider externe.

## Concepts de sécurité

### JWT (JSON Web Token)

Un JWT est un token signé contenant des informations d’authentification.

Dans ce projet :

- **Access Token** → durée courte ;
- **Refresh Token** → permet de renouveler l’*access token*.

Les tokens contiennent :

- l’identifiant utilisateur ;
- les rôles.

### RBAC (Role-Based Access Control)

Le RBAC est un système de gestion des permissions basé sur les rôles.

Exemples :

- `CUSTOMER`
- `ADMIN`
- `SUPPORT`

Les accès aux endpoints sont contrôlés selon ces rôles.

## Concepts frontend

### PWA (Progressive Web App)

Une PWA est une application web qui peut fonctionner comme une application native.

**Caractéristiques :**

- installable ;
- prise en charge hors ligne ;
- service worker ;
- responsive.

### Design System

Un *Design System* est une bibliothèque de composants réutilisables.

Exemples de composants :

- `Button`
- `Card`
- `Modal`
- `Table`
- `Input`

**Objectifs :**

- cohérence visuelle ;
- accélération du développement.

## Concepts DevOps

### CI (Continuous Integration)

La *Continuous Integration* consiste à exécuter automatiquement :

- les tests ;
- le lint ;
- la compilation,

à chaque modification du code.

### Monorepo

Un *Monorepo* est un repository contenant plusieurs applications.

Dans ce projet :

- PWA
- Backoffice
- API
- Shared libraries

**Avantages :**

- partage de code ;
- cohérence ;
- gestion simplifiée des dépendances.
