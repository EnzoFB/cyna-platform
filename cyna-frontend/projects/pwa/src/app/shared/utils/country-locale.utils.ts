import frLocale from 'i18n-iso-countries/langs/fr.json';
import enLocale from 'i18n-iso-countries/langs/en.json';

export interface CountryLocalePayload {
  readonly locale: string;
  readonly countries: Record<string, string | string[]>;
}

export interface Country {
  readonly code: string;
  readonly name: string;
}

/**
 * Resolves the i18n-iso-countries JSON locale payload for the given language tag.
 * Falls back to French for any non-English locale.
 */
export function resolveCountryLocale(lang: string): CountryLocalePayload {
  return lang.startsWith('en')
    ? (enLocale as CountryLocalePayload)
    : (frLocale as CountryLocalePayload);
}

/**
 * Returns a sorted list of countries for the given language tag.
 */
export function getCountryList(lang: string): Country[] {
  const payload = resolveCountryLocale(lang);
  return Object.entries(payload.countries)
    .map(([code, name]) => ({ code, name: Array.isArray(name) ? name[0] : name }))
    .filter((c): c is Country => Boolean(c.name))
    .sort((a, b) => a.name.localeCompare(b.name, payload.locale));
}
