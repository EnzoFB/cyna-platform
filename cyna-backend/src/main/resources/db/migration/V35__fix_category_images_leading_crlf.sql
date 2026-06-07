-- =====================================================
-- V35: Fix category images seeded with a leading CRLF.
-- The V2 migration used dollar-quoting with a newline
-- before <svg>, producing \r\n<svg…> in the stored bytes.
-- The frontend prefix-detection expects the SVG to start
-- directly with '<svg' (base64 prefix PHN2).
-- This migration re-seeds the three category images
-- without any leading whitespace.
-- =====================================================

UPDATE product_schema.categories
SET image = convert_to('<svg xmlns="http://www.w3.org/2000/svg" width="720" height="280" viewBox="0 0 720 280"><defs><linearGradient id="soc" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="#0A1628"/><stop offset="100%" stop-color="#1E3A5F"/></linearGradient></defs><rect width="720" height="280" fill="url(#soc)"/><circle cx="150" cy="140" r="64" fill="none" stroke="#00D9FF" stroke-width="10"/><path d="M150 94 L188 112 L182 154 C178 176 168 191 150 206 C132 191 122 176 118 154 L112 112 Z" fill="none" stroke="#00D9FF" stroke-width="8"/><text x="250" y="160" fill="#E9F4FF" font-size="64" font-family="Arial, sans-serif" font-weight="700">SOC</text></svg>', 'UTF8')
WHERE name = 'SOC';

UPDATE product_schema.categories
SET image = convert_to('<svg xmlns="http://www.w3.org/2000/svg" width="720" height="280" viewBox="0 0 720 280"><defs><linearGradient id="edr" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="#0F233D"/><stop offset="100%" stop-color="#2B507A"/></linearGradient></defs><rect width="720" height="280" fill="url(#edr)"/><rect x="82" y="86" width="136" height="108" rx="14" fill="none" stroke="#00D9FF" stroke-width="10"/><path d="M110 140 L138 168 L190 116" fill="none" stroke="#00D9FF" stroke-width="10" stroke-linecap="round"/><text x="250" y="160" fill="#E9F4FF" font-size="64" font-family="Arial, sans-serif" font-weight="700">EDR</text></svg>', 'UTF8')
WHERE name = 'EDR';

UPDATE product_schema.categories
SET image = convert_to('<svg xmlns="http://www.w3.org/2000/svg" width="720" height="280" viewBox="0 0 720 280"><defs><linearGradient id="xdr" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="#13263F"/><stop offset="100%" stop-color="#345D8A"/></linearGradient></defs><rect width="720" height="280" fill="url(#xdr)"/><circle cx="132" cy="96" r="22" fill="#00D9FF"/><circle cx="196" cy="96" r="22" fill="#00D9FF"/><circle cx="164" cy="170" r="22" fill="#00D9FF"/><line x1="132" y1="96" x2="196" y2="96" stroke="#00D9FF" stroke-width="8"/><line x1="132" y1="96" x2="164" y2="170" stroke="#00D9FF" stroke-width="8"/><line x1="196" y1="96" x2="164" y2="170" stroke="#00D9FF" stroke-width="8"/><text x="250" y="160" fill="#E9F4FF" font-size="64" font-family="Arial, sans-serif" font-weight="700">XDR</text></svg>', 'UTF8')
WHERE name = 'XDR';
