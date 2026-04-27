CREATE TABLE IF NOT EXISTS product_schema.product_images (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    product_id      UUID            NOT NULL,
    image_url       TEXT            NOT NULL,
    display_order   INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_product_images PRIMARY KEY (id),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id)
        REFERENCES product_schema.products(id) ON DELETE CASCADE,
    CONSTRAINT ck_product_images_display_order CHECK (display_order >= 0),
    CONSTRAINT uq_product_images_product_order UNIQUE (product_id, display_order)
);

-- Convention:
-- display_order = 0 => image de couverture (catalogue + première image en page détail)
-- display_order > 0 => images secondaires ordonnées
CREATE INDEX IF NOT EXISTS idx_product_images_product_id
    ON product_schema.product_images (product_id);

CREATE INDEX IF NOT EXISTS idx_product_images_display_order
    ON product_schema.product_images (display_order);

CREATE TRIGGER trg_product_images_updated_at
    BEFORE UPDATE ON product_schema.product_images
    FOR EACH ROW
    EXECUTE FUNCTION product_schema.update_updated_at();

INSERT INTO product_schema.product_images (product_id, image_url, display_order)
VALUES
    ('10000000-0000-0000-0000-000000000001', '/assets/images/catalog/products/soc-1.svg', 0),
    ('10000000-0000-0000-0000-000000000001', '/assets/images/catalog/products/soc-2.svg', 1),
    ('10000000-0000-0000-0000-000000000001', '/assets/images/catalog/products/soc-3.svg', 2),
    ('10000000-0000-0000-0000-000000000002', '/assets/images/catalog/products/soc-1.svg', 0),
    ('10000000-0000-0000-0000-000000000002', '/assets/images/catalog/products/soc-2.svg', 1),
    ('10000000-0000-0000-0000-000000000002', '/assets/images/catalog/products/soc-3.svg', 2),
    ('10000000-0000-0000-0000-000000000003', '/assets/images/catalog/products/soc-1.svg', 0),
    ('10000000-0000-0000-0000-000000000003', '/assets/images/catalog/products/soc-2.svg', 1),
    ('10000000-0000-0000-0000-000000000003', '/assets/images/catalog/products/soc-3.svg', 2),
    ('20000000-0000-0000-0000-000000000001', '/assets/images/catalog/products/edr-1.svg', 0),
    ('20000000-0000-0000-0000-000000000001', '/assets/images/catalog/products/edr-2.svg', 1),
    ('20000000-0000-0000-0000-000000000001', '/assets/images/catalog/products/edr-3.svg', 2),
    ('20000000-0000-0000-0000-000000000002', '/assets/images/catalog/products/edr-1.svg', 0),
    ('20000000-0000-0000-0000-000000000002', '/assets/images/catalog/products/edr-2.svg', 1),
    ('20000000-0000-0000-0000-000000000002', '/assets/images/catalog/products/edr-3.svg', 2),
    ('20000000-0000-0000-0000-000000000003', '/assets/images/catalog/products/edr-1.svg', 0),
    ('20000000-0000-0000-0000-000000000003', '/assets/images/catalog/products/edr-2.svg', 1),
    ('20000000-0000-0000-0000-000000000003', '/assets/images/catalog/products/edr-3.svg', 2),
    ('30000000-0000-0000-0000-000000000001', '/assets/images/catalog/products/xdr-1.svg', 0),
    ('30000000-0000-0000-0000-000000000001', '/assets/images/catalog/products/xdr-2.svg', 1),
    ('30000000-0000-0000-0000-000000000001', '/assets/images/catalog/products/xdr-3.svg', 2),
    ('30000000-0000-0000-0000-000000000002', '/assets/images/catalog/products/xdr-1.svg', 0),
    ('30000000-0000-0000-0000-000000000002', '/assets/images/catalog/products/xdr-2.svg', 1),
    ('30000000-0000-0000-0000-000000000002', '/assets/images/catalog/products/xdr-3.svg', 2),
    ('30000000-0000-0000-0000-000000000003', '/assets/images/catalog/products/xdr-1.svg', 0),
    ('30000000-0000-0000-0000-000000000003', '/assets/images/catalog/products/xdr-2.svg', 1),
    ('30000000-0000-0000-0000-000000000003', '/assets/images/catalog/products/xdr-3.svg', 2)
ON CONFLICT (product_id, display_order)
DO UPDATE
SET
    image_url = EXCLUDED.image_url,
    updated_at = NOW();

UPDATE product_schema.categories
SET image = convert_to($svg$
<svg xmlns="http://www.w3.org/2000/svg" width="720" height="280" viewBox="0 0 720 280">
  <defs>
    <linearGradient id="soc" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#0A1628"/>
      <stop offset="100%" stop-color="#1E3A5F"/>
    </linearGradient>
  </defs>
  <rect width="720" height="280" fill="url(#soc)"/>
  <circle cx="150" cy="140" r="64" fill="none" stroke="#00D9FF" stroke-width="10"/>
  <path d="M150 94 L188 112 L182 154 C178 176 168 191 150 206 C132 191 122 176 118 154 L112 112 Z" fill="none" stroke="#00D9FF" stroke-width="8"/>
  <text x="250" y="160" fill="#E9F4FF" font-size="64" font-family="Arial, sans-serif" font-weight="700">SOC</text>
</svg>
$svg$, 'UTF8')
WHERE name = 'SOC' AND image IS NULL;

UPDATE product_schema.categories
SET image = convert_to($svg$
<svg xmlns="http://www.w3.org/2000/svg" width="720" height="280" viewBox="0 0 720 280">
  <defs>
    <linearGradient id="edr" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#0F233D"/>
      <stop offset="100%" stop-color="#2B507A"/>
    </linearGradient>
  </defs>
  <rect width="720" height="280" fill="url(#edr)"/>
  <rect x="82" y="86" width="136" height="108" rx="14" fill="none" stroke="#00D9FF" stroke-width="10"/>
  <path d="M110 140 L138 168 L190 116" fill="none" stroke="#00D9FF" stroke-width="10" stroke-linecap="round"/>
  <text x="250" y="160" fill="#E9F4FF" font-size="64" font-family="Arial, sans-serif" font-weight="700">EDR</text>
</svg>
$svg$, 'UTF8')
WHERE name = 'EDR' AND image IS NULL;

UPDATE product_schema.categories
SET image = convert_to($svg$
<svg xmlns="http://www.w3.org/2000/svg" width="720" height="280" viewBox="0 0 720 280">
  <defs>
    <linearGradient id="xdr" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#13263F"/>
      <stop offset="100%" stop-color="#345D8A"/>
    </linearGradient>
  </defs>
  <rect width="720" height="280" fill="url(#xdr)"/>
  <circle cx="132" cy="96" r="22" fill="#00D9FF"/>
  <circle cx="196" cy="96" r="22" fill="#00D9FF"/>
  <circle cx="164" cy="170" r="22" fill="#00D9FF"/>
  <line x1="132" y1="96" x2="196" y2="96" stroke="#00D9FF" stroke-width="8"/>
  <line x1="132" y1="96" x2="164" y2="170" stroke="#00D9FF" stroke-width="8"/>
  <line x1="196" y1="96" x2="164" y2="170" stroke="#00D9FF" stroke-width="8"/>
  <text x="250" y="160" fill="#E9F4FF" font-size="64" font-family="Arial, sans-serif" font-weight="700">XDR</text>
</svg>
$svg$, 'UTF8')
WHERE name = 'XDR' AND image IS NULL;
