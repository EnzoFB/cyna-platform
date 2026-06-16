# Branchement API Carrousel Promotions (Page Offres)

## Objectif

Documenter la specificite du branchement API du carrousel promotionnel affiche sur la page `Offres du moment` (PWA).

## Perimetre Front

- Application: `cyna-frontend/projects/pwa`
- Feature: `offers`
- Service de recuperation: `OfferPromotionService`
- Composant consommateur: `OffersComponent`

## Endpoint utilise

- Methode: `GET`
- URL: `${environment.apiUrl}/offers/promotions`
- Query param:
  - `lang`: langue active (`fr`, `en`, etc.)

Exemple:

```http
GET /offers/promotions?lang=fr
```

## Contrat de reponse attendu

Le front attend une enveloppe:

```ts
ApiResponse<OfferPromotionDto[]>
```

avec un tableau dans `data`.

### DTO attendu

```ts
interface OfferPromotionDto {
  id: string;                    // requis
  title: string;                 // requis
  description: string;           // requis (texte affiche sous le carrousel)
  ctaLabel?: string | null;      // optionnel
  ctaRoute?: string | null;      // optionnel
  ctaUrl?: string | null;        // optionnel
  background?: string | null;    // optionnel (fallback visuel)
  imageUrl?: string | null;      // optionnel (visuel principal)
  active?: boolean;              // optionnel (si false => exclu)
  order?: number;                // optionnel (tri ascendant)
}
```

## Regles de mapping cote Front

Dans `OfferPromotionService`:

1. Filtrage:
  - conserve uniquement les elements avec `active !== false`
  - conserve uniquement les elements avec `id`, `title`, `description` non vides
2. Tri:
  - tri par `order` ascendant
  - sans `order`, valeur virtuelle `Number.MAX_SAFE_INTEGER` (elements en fin)
3. Normalisation:
  - trim des champs texte
  - `background` fallback sur un degrade par defaut si absent
4. Robustesse:
  - si `data` vide, mapping vide, erreur HTTP, ou payload incomplet: fallback local i18n

## Regles d affichage UI

Dans `offers.component.html`:

1. Le carrousel affiche un slide actif.
2. Le visuel de slide:
  - `imageUrl` si present
  - sinon fond via `background`
3. Le texte visible est sous le carrousel:
  - `description` de la promotion active
4. La pagination (dots) pilote `activePromotionIndex`.
5. Rotation automatique:
  - toutes les `7000 ms`

## Fallback local (sans API exploitable)

Sources i18n:

- `cyna-frontend/projects/pwa/src/assets/i18n/fr.json`
- `cyna-frontend/projects/pwa/src/assets/i18n/en.json`
- namespace: `offers.promotions.fallback.*`

Cas couverts:

- API indisponible
- erreur reseau / 4xx / 5xx
- `data` vide
- items invalides apres filtrage

## Contraintes backend recommandees

1. Endpoint public ou accessible sans imposer une redirection login sur page publique.
2. Reponse stable sur le schema ci-dessus.
3. `order` exploitable pour la priorisation metier.
4. `imageUrl` absolue ou resoluble par le front.
5. Texte `description` concis (affiche directement sous le visuel).

## Checklist de validation

1. `GET /offers/promotions?lang=fr` retourne au moins 1 item actif valide.
2. Le tri observe en UI suit `order`.
3. Changer de langue recharge les promotions.
4. Si API coupee, fallback traduit apparait.
5. Aucun blocage d acces a `/offers` pour un visiteur non connecte.

## Fichiers front lies

- `cyna-frontend/projects/pwa/src/app/features/offers/services/offer-promotion.service.ts`
- `cyna-frontend/projects/pwa/src/app/features/offers/models/offer-promotion.model.ts`
- `cyna-frontend/projects/pwa/src/app/features/offers/offers.component.ts`
- `cyna-frontend/projects/pwa/src/app/features/offers/offers.component.html`
