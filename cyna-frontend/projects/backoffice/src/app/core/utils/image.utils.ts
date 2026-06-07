/**
 * Detects the MIME type of a base64-encoded image from its leading bytes
 * and returns a valid data-URI string ready for use in <img [src]>.
 *
 * Supported formats:
 *  - PNG  : base64 starts with 'iVBOR' (magic bytes 0x89 0x50 0x4E 0x47)
 *  - SVG  : base64 starts with 'PHN2' (<sv) or 'PD94' (<?x)
 *  - JPEG : fallback
 */
export function toImageSrc(base64: string): string {
  if (base64.startsWith('iVBOR')) return `data:image/png;base64,${base64}`;
  if (base64.startsWith('PHN2') || base64.startsWith('PD94')) return `data:image/svg+xml;base64,${base64}`;
  return `data:image/jpeg;base64,${base64}`;
}
