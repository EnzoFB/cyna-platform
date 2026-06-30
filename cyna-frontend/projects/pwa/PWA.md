# Documentation PWA — FRONT-01

## 1. Vue d'ensemble

Configuration PWA du frontend Angular CYNA. Stack : `@angular/service-worker`, `ngsw-config.json`, `manifest.webmanifest`.

## 2. Fichiers de configuration

### `manifest.webmanifest`
Déclare l'app au navigateur.
- `id: "/"` — identifiant stable (recommandé Chrome)
- `icons` — 8 icônes carrées (72..512) `purpose: "any"`
- `icons` maskables — 2 icônes (192, 512) `purpose: "maskable"` (ne jamais combiner avec `any`)
- `screenshots` — 2 captures obligatoires pour l'Enhanced Install UI :
  - `wide` : 1280x720
  - `narrow` : 750x1334

### `ngsw-config.json`
Cache du Service Worker.
- `assetGroups` : prefetch app (JS/CSS/HTML) + lazy assets (images/i18n)
- `dataGroups` : API en `freshness` (1h max, 100 req)

### `app.config.ts`
Enregistre le SW :
```typescript
provideServiceWorker('ngsw-worker.js', {
  enabled: !isDevMode(),
  registrationStrategy: 'registerWhenStable:30000'
})
```

### `angular.json`
Active le SW en production via `serviceWorker: "projects/pwa/ngsw-config.json"`.

> **Note :** le builder copie `src/assets` à la racine du `dist`. Les chemins du manifest doivent pointer vers `icons/pwa/...` (pas `assets/icons/pwa/...`).

### `index.html`
```html
<link rel="manifest" href="manifest.webmanifest">
<meta name="theme-color" content="#1976d2">
```

## 3. Prérequis installation Chrome

| Prérequis | Statut |
|-----------|--------|
| HTTPS (ou localhost) | OK |
| SW actif | OK |
| Icône >= 192x192 (`any`) | OK |
| Icône `maskable` | OK |
| Screenshots wide + narrow | OK |

## 4. Build et test

```bash
cd cyna-frontend
npm run build:pwa
npx http-server dist/pwa/browser -p 8081
```

Vérifications : DevTools > Application > Manifest (pas de warning) + Service Workers (`ngsw-worker.js` actif).

**Routage SPA** : `http-server` ne gère pas les 404. Alternative :
```bash
npx serve dist/pwa/browser -l 8081 --single
```

## 5. Assets PWA

```
projects/pwa/src/assets/icons/pwa/
├── icon-{72..512}.png      # 8 icônes carrées
├── maskable-{192,512}.png  # 2 icônes adaptatives
├── wide.png                # screenshot desktop 1280x720
└── narrow.png              # screenshot mobile 750x1334
```

## 6. Checklist commit

- [ ] `npm run build:pwa` OK
- [ ] Manifest sans warning (DevTools)
- [ ] SW actif
- [ ] Bouton install visible
