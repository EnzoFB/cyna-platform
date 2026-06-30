# Système de Logging Centralisé — Fiche Technique

## 1. Architecture

```
Angular (navigateur)
  ├─ erreurs JS ───────────────┐
  ├─ requêtes HTTP ────────────┼──▶ Spring Boot ───▶ fichiers JSON
  └─ Correlation ID ◀──────────┘         │              │
                                           └─ API logs ──┘
                                                    │
                                               Promtail
                                                    │
                                                  Loki
                                                    │
                                                Grafana
```

---

## 2. Backend — Filtres & Endpoints

### CorrelationIdFilter
- Attribue un UUID unique à chaque requête HTTP (récupéré de l'en-tête `X-Correlation-Id` ou généré automatiquement)
- Propage cet ID dans tous les logs (MDC) et le renvoie dans la réponse HTTP
- **À quoi ça sert :** Traçabilité bout-en-bout. Un seul ID permet de relier une action dans le navigateur à toutes les API et logs serveur associés.

### RequestLoggingFilter
- Loggue chaque requête HTTP en JSON structuré
- **Champs loggués :** `method`, `uri`, `status`, `duration_ms`, `traceId`, `clientIp`, `userAgent`
- **À quoi ça sert :** Alimenter les métriques de performance et le taux d'erreur du dashboard **Backend Overview**.

### ClientLogController
- Endpoint `POST /api/v1/logs/client`
- Accepte un batch de logs du navigateur (max 50 entrées)
- **Champs reçus :** `level`, `message`, `url`, `correlationId`, `appVersion`, `stackTrace`
- **Sécurité :** Les messages sont nettoyés (caractères de contrôle supprimés, troncature à 2000 caractères)
- **À quoi ça sert :** Centraliser les erreurs JavaScript et les problèmes réseau vécus par les utilisateurs finaux. Sans ça, ces erreurs restent dans la console du navigateur du client et sont invisibles pour l'équipe.

---

## 3. Frontend — Collecte & Envoi

### LoggingService
- Buffer mémoire + IndexedDB (`cyna-logs`) pour le mode offline
- **Flush immédiat** pour ERROR et WARN
- **Flush périodique** pour INFO (toutes les 30 secondes)
- **Cap de stockage offline :** 100 logs maximum
- **PII stripping :** Mots de passe, emails, numéros de carte et CVV sont remplacés par `[REDACTED]` avant envoi
- **À quoi ça sert :** Garantir qu'aucune erreur client n'est perdue, même en connexion intermittente. Réduire le bruit réseau en ne synchronisant périodiquement que les logs non critiques.

### GlobalErrorHandler
- Intercepte toutes les erreurs Angular non gérées par un `try/catch`
- Forward immédiat au LoggingService avec l'URL courante
- Protection anti-récursion intégrée
- **À quoi ça sert :** Transformer les erreurs "silencieuses" (composant qui plante sans message visible) en événements traçables dans Loki.

### Intercepteurs HTTP
- **correlationIdInterceptor :** Ajoute l'en-tête `X-Correlation-Id` à chaque requête HTTP. L'ID est stocké en `sessionStorage` pour être réutilisé sur toute la session.
- **httpErrorLoggingInterceptor :** Capture les erreurs HTTP (4xx, 5xx) et les loggue côté frontend. Exclut l'endpoint de logging pour éviter une boucle infinie.
- **À quoi ça sert :** Le Correlation ID garantit la traçabilité. L'intercepteur d'erreur permet de distinguer "l'API a répondu en erreur" vs "le réseau a coupé".

---

## 4. Dashboards Grafana

### Backend Overview

| Panneau | Requête LogQL | Ce qu'il représente |
|---------|--------------|---------------------|
| **Log Volume** | `sum(rate({app="cyna-backend"} [5m]))` | Volume total de logs backend. Une chute brutale = panne silencieuse potentielle. |
| **Error Count (5m)** | `sum(count_over_time({app="cyna-backend",level="ERROR"} [5m]))` | Nombre d'erreurs sur les 5 dernières minutes. > 0 = investigation immédiate. |
| **Request Rate** | `sum(rate({app="cyna-backend",logger="RequestLoggingFilter"} [5m]))` | Débit de requêtes HTTP par minute. Permet de corréler charge et incidents. |
| **Error Rate %** | `(sum(rate({app="cyna-backend",level="ERROR"} [5m])) / sum(rate({app="cyna-backend"} [5m]))) * 100` | Pourcentage de requêtes en erreur. Seuils : **vert** < 5%, **jaune** 5–10%, **rouge** > 10%. |
| **Avg Response Time** | `avg_over_time({app="cyna-backend",logger="RequestLoggingFilter"} \| json \| unwrap duration_ms [5m])` | Temps de réponse moyen des requêtes HTTP. Détecte les ralentissements. |
| **Raw Logs** | `{app="cyna-backend"}` | Logs bruts backend. Investigation manuelle au cas par cas. |

---

### Frontend Health

| Panneau | Requête LogQL | Ce qu'il représente |
|---------|--------------|---------------------|
| **Client Errors** | `sum(rate({app="cyna-frontend",level="ERROR"} [5m]))` | Nombre d'erreurs côté navigateur. Chaque point est une erreur vécue par un utilisateur réel (JS, réseau, etc.). |
| **Total Logs / min** | `sum(rate({app="cyna-frontend"} [5m])) * 60` | Activité totale du frontend en logs par minute. Permet de corréler trafic et volume d'erreurs. |
| **API Errors vs JS Errors** | Série A : `sum(rate({app="cyna-frontend",level="ERROR"} \|= "HTTP" [5m]))`  <br> Série B : `sum(rate({app="cyna-frontend",level="ERROR"} != "HTTP" [5m]))` | Deux courbes sur un même graphique pour isoler la source de l'erreur. Voir le détail ci-dessous. |
| **Raw Logs** | `{app="cyna-frontend"}` | Logs bruts frontend. Lecture du message d'erreur exact et de la stack trace. |

#### Détail — API Errors vs JS Errors

Ce panneau affiche **deux courbes superposées** sur une timeline. Elles partagent le même axe temps, mais représentent deux causes d'erreur différentes.

```
Erreurs/min
    │
 10 ┤     ╱╲  ←── JS Errors (bleu)
    │    ╱  ╲      "Cannot read property of undefined"
  5 ┤──╱────╲───  ←── API Errors (rouge)
    │ ╱        ╲     "HTTP 500 on GET /api/v1/orders"
  0 ┼────┬────┬────┬──→ Temps
      14h  15h  16h
```

| Courbe | Couleur | Ce qui la fait monter | Exemple de log |
|--------|---------|----------------------|----------------|
| **API Errors** | Rouge | Le backend répond mal ou pas du tout. | `HTTP 500 Internal Server Error on POST /api/v1/payments` |
| **JS Errors** | Bleu | Le navigateur exécute du code qui plante. | `TypeError: Cannot read properties of null` |

**Comment lire le graphique :**
- **Seule la rouge monte** → Le backend est coupable. Investiger l'API concernée.
- **Seule la bleue monte** → Bug frontend (release cassée, navigateur incompatible). Pas besoin de regarder le backend.
- **Les deux montent ensemble** → Effet de bord (ex: l'API est down → le frontend n'a pas géré le cas et plante en cascade).

---

### Incident Search

**Filtres disponibles :**

| Variable | Type | Description |
|----------|------|-------------|
| **App** | Custom (`cyna-backend`, `cyna-frontend`) | Choix de la source de logs |
| **Level** | Custom (`INFO`, `WARN`, `ERROR`, `DEBUG`) | Filtrage par sévérité |
| **TraceId** | Texte libre | Entrer un Correlation ID pour retrouver toute la chaîne d'une action utilisateur |

**Panel Logs Explorer :**
- Requête : `{app="$app",level="$level"} |= "$traceId"`
- **Usage typique :** Copier le `traceId` d'une erreur → coller dans le filtre → visualiser tous les logs frontend et backend liés à cette session.

---

## 5. Infrastructure

| Composant | Rôle | Config clé |
|-----------|------|------------|
| **Loki** | Stockage TSDB des logs. Retention 7 jours (168h). | `loki-config.yml` : mode single-node, filesystem object store, TSDB index |
| **Promtail** | Parse les fichiers JSON (`cyna-*.json.log`) et pousse vers Loki. Extrait les labels (`level`, `traceId`, `method`, `status`, `url`). | `promtail-config.yml` : 3 jobs (backend-file-logs, frontend-file-logs, docker-logs) |
| **Grafana** | Visualisation. Dashboards versionnés en JSON (reproductibles sur n'importe quel env). Rafraîchissement toutes les 10s. | Provisioning automatique des datasources et dashboards au démarrage |

---

*Document généré le 2026-06-10 pour la branche `feat/logging-system`.*
