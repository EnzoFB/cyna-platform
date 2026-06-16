-- =====================================================
-- CYNA PLATFORM — DONNÉES INITIALES (SEED)
-- =====================================================


-- =====================================================
-- 1. CATÉGORIES
-- =====================================================

INSERT INTO product_schema.categories (id, name, image, is_active)
VALUES
(
    '00000000-0000-0000-0000-000000000001',
    'SOC',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="720" height="280" viewBox="0 0 720 280"><defs><linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="#0A1628"/><stop offset="100%" stop-color="#1E3A5F"/></linearGradient></defs><rect width="720" height="280" fill="url(#g)"/><line x1="220" y1="48" x2="220" y2="232" stroke="#00D9FF" stroke-width="1" opacity="0.18"/><circle cx="110" cy="140" r="58" fill="none" stroke="#00D9FF" stroke-width="2" opacity="0.18"/><path d="M110 72 L154 93 L149 137 C145 161 135 177 110 193 C85 177 75 161 71 137 L66 93 Z" fill="none" stroke="#00D9FF" stroke-width="7"/><path d="M93 137 L107 152 L132 120" fill="none" stroke="#00D9FF" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/><text x="244" y="106" fill="#E9F4FF" font-size="62" font-family="Arial, sans-serif" font-weight="700" letter-spacing="3">SOC</text><text x="244" y="144" fill="#7BBFDF" font-size="15" font-family="Arial, sans-serif">Security Operations Center</text><line x1="244" y1="158" x2="692" y2="158" stroke="#1A3A5F" stroke-width="1"/><rect x="244" y="168" width="104" height="22" rx="11" fill="#0C2240" stroke="#00D9FF" stroke-width="1"/><text x="296" y="183" fill="#00D9FF" font-size="11" font-family="Arial, sans-serif" text-anchor="middle">Supervision 24/7</text><rect x="356" y="168" width="80" height="22" rx="11" fill="#0C2240" stroke="#00D9FF" stroke-width="1"/><text x="396" y="183" fill="#00D9FF" font-size="11" font-family="Arial, sans-serif" text-anchor="middle">SIEM · SOAR</text><rect x="444" y="168" width="96" height="22" rx="11" fill="#0C2240" stroke="#00D9FF" stroke-width="1"/><text x="492" y="183" fill="#00D9FF" font-size="11" font-family="Arial, sans-serif" text-anchor="middle">Threat Hunting</text><rect x="568" y="78" width="9" height="124" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="568" y="118" width="9" height="44" rx="4" fill="#00D9FF" opacity="0.55"/><rect x="585" y="68" width="9" height="144" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="585" y="92" width="9" height="72" rx="4" fill="#00D9FF" opacity="0.45"/><rect x="602" y="78" width="9" height="124" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="602" y="128" width="9" height="30" rx="4" fill="#00D9FF" opacity="0.65"/><rect x="619" y="72" width="9" height="136" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="619" y="98" width="9" height="60" rx="4" fill="#00D9FF" opacity="0.4"/><rect x="636" y="78" width="9" height="124" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="636" y="108" width="9" height="48" rx="4" fill="#00D9FF" opacity="0.58"/><rect x="653" y="82" width="9" height="116" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="653" y="114" width="9" height="28" rx="4" fill="#00D9FF" opacity="0.5"/><rect x="670" y="74" width="9" height="132" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="670" y="88" width="9" height="82" rx="4" fill="#00D9FF" opacity="0.42"/><rect x="687" y="78" width="9" height="124" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="687" y="122" width="9" height="36" rx="4" fill="#00D9FF" opacity="0.6"/></svg>$svg$, 'UTF8'),
    TRUE
),
(
    '00000000-0000-0000-0000-000000000002',
    'EDR',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="720" height="280" viewBox="0 0 720 280"><defs><linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="#0F233D"/><stop offset="100%" stop-color="#2B507A"/></linearGradient></defs><rect width="720" height="280" fill="url(#g)"/><line x1="220" y1="48" x2="220" y2="232" stroke="#00D9FF" stroke-width="1" opacity="0.18"/><rect x="54" y="80" width="112" height="88" rx="12" fill="none" stroke="#00D9FF" stroke-width="7"/><rect x="68" y="94" width="84" height="12" rx="4" fill="#00D9FF" opacity="0.25"/><rect x="68" y="114" width="60" height="10" rx="4" fill="#00D9FF" opacity="0.18"/><rect x="68" y="130" width="72" height="10" rx="4" fill="#00D9FF" opacity="0.18"/><path d="M78 154 L100 178 L148 124" fill="none" stroke="#00D9FF" stroke-width="8" stroke-linecap="round" stroke-linejoin="round"/><circle cx="110" cy="220" r="6" fill="#00D9FF" opacity="0.5"/><line x1="110" y1="168" x2="110" y2="214" stroke="#00D9FF" stroke-width="3" opacity="0.4"/><text x="244" y="106" fill="#E9F4FF" font-size="62" font-family="Arial, sans-serif" font-weight="700" letter-spacing="3">EDR</text><text x="244" y="144" fill="#7BBFDF" font-size="15" font-family="Arial, sans-serif">Endpoint Detection &amp; Response</text><line x1="244" y1="158" x2="692" y2="158" stroke="#1A3A5F" stroke-width="1"/><rect x="244" y="168" width="94" height="22" rx="11" fill="#0C2240" stroke="#00D9FF" stroke-width="1"/><text x="291" y="183" fill="#00D9FF" font-size="11" font-family="Arial, sans-serif" text-anchor="middle">Agent léger</text><rect x="346" y="168" width="104" height="22" rx="11" fill="#0C2240" stroke="#00D9FF" stroke-width="1"/><text x="398" y="183" fill="#00D9FF" font-size="11" font-family="Arial, sans-serif" text-anchor="middle">Console cloud</text><rect x="458" y="168" width="88" height="22" rx="11" fill="#0C2240" stroke="#00D9FF" stroke-width="1"/><text x="502" y="183" fill="#00D9FF" font-size="11" font-family="Arial, sans-serif" text-anchor="middle">Isolation auto</text><rect x="568" y="78" width="9" height="124" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="568" y="106" width="9" height="62" rx="4" fill="#00D9FF" opacity="0.5"/><rect x="585" y="68" width="9" height="144" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="585" y="96" width="9" height="52" rx="4" fill="#00D9FF" opacity="0.42"/><rect x="602" y="78" width="9" height="124" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="602" y="118" width="9" height="64" rx="4" fill="#00D9FF" opacity="0.62"/><rect x="619" y="72" width="9" height="136" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="619" y="102" width="9" height="38" rx="4" fill="#00D9FF" opacity="0.38"/><rect x="636" y="78" width="9" height="124" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="636" y="112" width="9" height="56" rx="4" fill="#00D9FF" opacity="0.55"/><rect x="653" y="82" width="9" height="116" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="653" y="110" width="9" height="24" rx="4" fill="#00D9FF" opacity="0.48"/><rect x="670" y="74" width="9" height="132" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="670" y="94" width="9" height="70" rx="4" fill="#00D9FF" opacity="0.44"/><rect x="687" y="78" width="9" height="124" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="687" y="116" width="9" height="44" rx="4" fill="#00D9FF" opacity="0.58"/></svg>$svg$, 'UTF8'),
    TRUE
),
(
    '00000000-0000-0000-0000-000000000003',
    'XDR',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="720" height="280" viewBox="0 0 720 280"><defs><linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="#13263F"/><stop offset="100%" stop-color="#345D8A"/></linearGradient></defs><rect width="720" height="280" fill="url(#g)"/><line x1="220" y1="48" x2="220" y2="232" stroke="#00D9FF" stroke-width="1" opacity="0.18"/><circle cx="90" cy="96" r="20" fill="#00D9FF" opacity="0.15" stroke="#00D9FF" stroke-width="3"/><circle cx="90" cy="96" r="7" fill="#00D9FF"/><circle cx="164" cy="96" r="20" fill="#00D9FF" opacity="0.15" stroke="#00D9FF" stroke-width="3"/><circle cx="164" cy="96" r="7" fill="#00D9FF"/><circle cx="127" cy="170" r="20" fill="#00D9FF" opacity="0.15" stroke="#00D9FF" stroke-width="3"/><circle cx="127" cy="170" r="7" fill="#00D9FF"/><line x1="90" y1="96" x2="164" y2="96" stroke="#00D9FF" stroke-width="3" opacity="0.6"/><line x1="90" y1="96" x2="127" y2="170" stroke="#00D9FF" stroke-width="3" opacity="0.6"/><line x1="164" y1="96" x2="127" y2="170" stroke="#00D9FF" stroke-width="3" opacity="0.6"/><circle cx="55" cy="160" r="12" fill="#00D9FF" opacity="0.1" stroke="#00D9FF" stroke-width="2"/><circle cx="55" cy="160" r="4" fill="#00D9FF" opacity="0.6"/><line x1="90" y1="96" x2="55" y2="160" stroke="#00D9FF" stroke-width="2" opacity="0.35" stroke-dasharray="4,3"/><circle cx="170" cy="190" r="12" fill="#00D9FF" opacity="0.1" stroke="#00D9FF" stroke-width="2"/><circle cx="170" cy="190" r="4" fill="#00D9FF" opacity="0.6"/><line x1="127" y1="170" x2="170" y2="190" stroke="#00D9FF" stroke-width="2" opacity="0.35" stroke-dasharray="4,3"/><text x="244" y="106" fill="#E9F4FF" font-size="62" font-family="Arial, sans-serif" font-weight="700" letter-spacing="3">XDR</text><text x="244" y="144" fill="#7BBFDF" font-size="15" font-family="Arial, sans-serif">Extended Detection &amp; Response</text><line x1="244" y1="158" x2="692" y2="158" stroke="#1A3A5F" stroke-width="1"/><rect x="244" y="168" width="116" height="22" rx="11" fill="#0C2240" stroke="#00D9FF" stroke-width="1"/><text x="302" y="183" fill="#00D9FF" font-size="11" font-family="Arial, sans-serif" text-anchor="middle">Multi-sources</text><rect x="368" y="168" width="98" height="22" rx="11" fill="#0C2240" stroke="#00D9FF" stroke-width="1"/><text x="417" y="183" fill="#00D9FF" font-size="11" font-family="Arial, sans-serif" text-anchor="middle">Corrélation IA</text><rect x="474" y="168" width="88" height="22" rx="11" fill="#0C2240" stroke="#00D9FF" stroke-width="1"/><text x="518" y="183" fill="#00D9FF" font-size="11" font-family="Arial, sans-serif" text-anchor="middle">Playbooks</text><rect x="568" y="78" width="9" height="124" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="568" y="100" width="9" height="56" rx="4" fill="#00D9FF" opacity="0.52"/><rect x="585" y="68" width="9" height="144" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="585" y="88" width="9" height="88" rx="4" fill="#00D9FF" opacity="0.43"/><rect x="602" y="78" width="9" height="124" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="602" y="124" width="9" height="36" rx="4" fill="#00D9FF" opacity="0.63"/><rect x="619" y="72" width="9" height="136" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="619" y="96" width="9" height="64" rx="4" fill="#00D9FF" opacity="0.41"/><rect x="636" y="78" width="9" height="124" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="636" y="104" width="9" height="52" rx="4" fill="#00D9FF" opacity="0.56"/><rect x="653" y="82" width="9" height="116" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="653" y="108" width="9" height="32" rx="4" fill="#00D9FF" opacity="0.49"/><rect x="670" y="74" width="9" height="132" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="670" y="92" width="9" height="76" rx="4" fill="#00D9FF" opacity="0.45"/><rect x="687" y="78" width="9" height="124" rx="4" fill="#00D9FF" opacity="0.08"/><rect x="687" y="118" width="9" height="40" rx="4" fill="#00D9FF" opacity="0.59"/></svg>$svg$, 'UTF8'),
    TRUE
)
ON CONFLICT (id) DO NOTHING;


-- =====================================================
-- 2. TRADUCTIONS DES CATÉGORIES (fr)
-- =====================================================

INSERT INTO product_schema.category_translations (category_id, locale, full_name, description)
VALUES
(
    '00000000-0000-0000-0000-000000000001', 'fr',
    'Security Operations Center',
    'Centre opérationnel dédié à la surveillance, la détection et la réponse aux incidents de sécurité en temps réel.'
),
(
    '00000000-0000-0000-0000-000000000002', 'fr',
    'Endpoint Detection & Response',
    'Solution de sécurité permettant de surveiller, détecter et répondre aux menaces sur les postes de travail et serveurs.'
),
(
    '00000000-0000-0000-0000-000000000003', 'fr',
    'Extended Detection & Response',
    'Plateforme unifiée de détection et de réponse couvrant plusieurs couches de sécurité (endpoint, réseau, cloud).'
)
ON CONFLICT (category_id, locale) DO NOTHING;


-- =====================================================
-- 2b. TRADUCTIONS DES CATÉGORIES (en)
-- =====================================================

INSERT INTO product_schema.category_translations (category_id, locale, full_name, description)
VALUES
(
    '00000000-0000-0000-0000-000000000001', 'en',
    'Security Operations Center',
    'Dedicated operational center for real-time security monitoring, threat detection, and incident response.'
),
(
    '00000000-0000-0000-0000-000000000002', 'en',
    'Endpoint Detection & Response',
    'Security solution that monitors, detects, and responds to threats on workstations and servers.'
),
(
    '00000000-0000-0000-0000-000000000003', 'en',
    'Extended Detection & Response',
    'Unified detection and response platform covering multiple security layers (endpoint, network, cloud).'
)
ON CONFLICT (category_id, locale) DO NOTHING;


-- =====================================================
-- 3. PRODUITS
-- =====================================================

INSERT INTO product_schema.products (
    id, category_id,
    monthly_price, annual_price, currency,
    is_published, is_available, priority_level, free_trial_days
)
VALUES
-- SOC
('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',  99.99,   999.99, 'EUR', TRUE, TRUE,  1, 14),
('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 299.99,  2999.99, 'EUR', TRUE, TRUE,  5, 14),
('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 799.99,  7999.99, 'EUR', TRUE, FALSE, 10, 14),
-- EDR
('20000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002',   4.99,    49.99, 'EUR', TRUE, TRUE,  1, 30),
('20000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',   9.99,    99.99, 'EUR', TRUE, TRUE,  5, 30),
('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002',  14.99,   149.99, 'EUR', TRUE, FALSE, 10, 30),
-- XDR
('30000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003', 199.99,  1999.99, 'EUR', TRUE, TRUE,  5, 30),
('30000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003', 399.99,  3999.99, 'EUR', TRUE, TRUE, 10, 30),
('30000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003', 699.99,  6999.99, 'EUR', TRUE, FALSE, 10, 30)
ON CONFLICT (id) DO NOTHING;


-- =====================================================
-- 4. TRADUCTIONS DES PRODUITS (fr)
-- =====================================================

INSERT INTO product_schema.product_translations
    (product_id, locale, name, service_description, technical_description, highlight_points)
VALUES
(
    '10000000-0000-0000-0000-000000000001', 'fr',
    'SOC Starter',
    'Supervision de sécurité 24/7 pour petites entreprises avec alerting de base',
    'SIEM mutualisé, collecte logs, alertes temps réel, dashboard basique',
    '["Protection en temps réel","Mises à jour automatiques","Support 24/7"]'
),
(
    '10000000-0000-0000-0000-000000000002', 'fr',
    'SOC Advanced',
    'SOC avancé avec analystes dédiés et réponse aux incidents',
    'SIEM dédié, playbooks automatisés, SOAR, intégration API, SLA 24/7',
    '["Protection en temps réel","Mises à jour automatiques","Support 24/7"]'
),
(
    '10000000-0000-0000-0000-000000000003', 'fr',
    'SOC Enterprise',
    'SOC complet avec équipe dédiée et threat intelligence',
    'SIEM + SOAR + Threat Intel + hunting proactif + intégration SI complexe',
    '["Protection en temps réel","Mises à jour automatiques","Support 24/7"]'
),
(
    '20000000-0000-0000-0000-000000000001', 'fr',
    'EDR Essential',
    'Protection des endpoints avec détection comportementale',
    'Agent léger, détection malware, isolation machine, console cloud',
    '["Détection avancée des menaces","Réponse automatisée","Analyse comportementale"]'
),
(
    '20000000-0000-0000-0000-000000000002', 'fr',
    'EDR Professional',
    'EDR avancé avec réponse automatisée',
    'Détection comportementale avancée, remédiation auto, forensic tools',
    '["Détection avancée des menaces","Réponse automatisée","Analyse comportementale"]'
),
(
    '20000000-0000-0000-0000-000000000003', 'fr',
    'EDR Elite',
    'Protection endpoint premium avec threat hunting',
    'EDR + threat hunting + sandboxing + intégration SIEM',
    '["Détection avancée des menaces","Réponse automatisée","Analyse comportementale"]'
),
(
    '30000000-0000-0000-0000-000000000001', 'fr',
    'XDR Core',
    'Corrélation des événements sécurité multi-sources',
    'Collecte logs, corrélation basique, dashboard centralisé',
    '["Protection réseau étendue","Corrélation multi-sources","Tableau de bord unifié"]'
),
(
    '30000000-0000-0000-0000-000000000002', 'fr',
    'XDR Advanced',
    'XDR avec intelligence avancée et détection automatisée',
    'Corrélation IA, détection anomalies, intégration EDR + SIEM',
    '["Protection réseau étendue","Corrélation multi-sources","Tableau de bord unifié"]'
),
(
    '30000000-0000-0000-0000-000000000003', 'fr',
    'XDR Ultimate',
    'Plateforme XDR complète avec automatisation et orchestration',
    'XDR + SOAR + IA + playbooks avancés + réponse automatisée complète',
    '["Protection réseau étendue","Corrélation multi-sources","Tableau de bord unifié"]'
)
ON CONFLICT (product_id, locale) DO NOTHING;


-- =====================================================
-- 4b. TRADUCTIONS DES PRODUITS (en)
-- =====================================================

INSERT INTO product_schema.product_translations
    (product_id, locale, name, service_description, technical_description, highlight_points)
VALUES
(
    '10000000-0000-0000-0000-000000000001', 'en',
    'SOC Starter',
    '24/7 security monitoring for small businesses with basic alerting',
    'Shared SIEM, log collection, real-time alerts, basic dashboard',
    '["Real-time protection","Automatic updates","24/7 support"]'
),
(
    '10000000-0000-0000-0000-000000000002', 'en',
    'SOC Advanced',
    'Advanced SOC with dedicated analysts and incident response',
    'Dedicated SIEM, automated playbooks, SOAR, API integration, 24/7 SLA',
    '["Real-time protection","Automatic updates","24/7 support"]'
),
(
    '10000000-0000-0000-0000-000000000003', 'en',
    'SOC Enterprise',
    'Complete SOC with dedicated team and threat intelligence',
    'SIEM + SOAR + Threat Intel + proactive hunting + complex IT integration',
    '["Real-time protection","Automatic updates","24/7 support"]'
),
(
    '20000000-0000-0000-0000-000000000001', 'en',
    'EDR Essential',
    'Endpoint protection with behavioral detection',
    'Lightweight agent, malware detection, machine isolation, cloud console',
    '["Advanced threat detection","Automated response","Behavioral analysis"]'
),
(
    '20000000-0000-0000-0000-000000000002', 'en',
    'EDR Professional',
    'Advanced EDR with automated response',
    'Advanced behavioral detection, auto-remediation, forensic tools',
    '["Advanced threat detection","Automated response","Behavioral analysis"]'
),
(
    '20000000-0000-0000-0000-000000000003', 'en',
    'EDR Elite',
    'Premium endpoint protection with threat hunting',
    'EDR + threat hunting + sandboxing + SIEM integration',
    '["Advanced threat detection","Automated response","Behavioral analysis"]'
),
(
    '30000000-0000-0000-0000-000000000001', 'en',
    'XDR Core',
    'Multi-source security event correlation',
    'Log collection, basic correlation, centralized dashboard',
    '["Extended network protection","Multi-source correlation","Unified dashboard"]'
),
(
    '30000000-0000-0000-0000-000000000002', 'en',
    'XDR Advanced',
    'XDR with advanced intelligence and automated detection',
    'AI correlation, anomaly detection, EDR + SIEM integration',
    '["Extended network protection","Multi-source correlation","Unified dashboard"]'
),
(
    '30000000-0000-0000-0000-000000000003', 'en',
    'XDR Ultimate',
    'Complete XDR platform with automation and orchestration',
    'XDR + SOAR + AI + advanced playbooks + full automated response',
    '["Extended network protection","Multi-source correlation","Unified dashboard"]'
)
ON CONFLICT (product_id, locale) DO NOTHING;


-- =====================================================
-- 5. IMAGES DES PRODUITS (SVG binaire)
-- =====================================================

INSERT INTO product_schema.product_images
    (id, product_id, image_data, mime_type, display_order, created_at, updated_at)
VALUES

-- ── SOC Starter (3 images) ───────────────────────────────────────────────────
(
    'b0000000-0000-0000-0001-000000000001',
    '10000000-0000-0000-0000-000000000001',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#060E1F"/>
      <stop offset="100%" stop-color="#0D2137"/>
    </linearGradient>
    <filter id="glow"><feGaussianBlur stdDeviation="4" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <path d="M200 80 L320 130 L310 240 C300 310 270 360 200 400 C130 360 100 310 90 240 L80 130 Z"
        fill="none" stroke="#00D9FF" stroke-width="6" filter="url(#glow)"/>
  <circle cx="200" cy="235" r="50" fill="none" stroke="#00D9FF" stroke-width="4" opacity="0.6"/>
  <path d="M170 235 L190 255 L235 210" fill="none" stroke="#00FF99" stroke-width="6" stroke-linecap="round" filter="url(#glow)"/>
  <text x="400" y="200" fill="#E0F4FF" font-size="52" font-family="Arial,sans-serif" font-weight="700">SOC</text>
  <text x="400" y="260" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif" font-weight="400">Starter</text>
  <text x="400" y="310" fill="#5B9EC9" font-size="18" font-family="Arial,sans-serif">Supervision 24/7 · Alerting · SIEM</text>
  <line x1="400" y1="325" x2="680" y2="325" stroke="#1A4A6A" stroke-width="1"/>
  <text x="400" y="350" fill="#2A7FAF" font-size="14" font-family="Arial,sans-serif">Protection continue pour petites entreprises</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 0, NOW(), NOW()
),
(
    'b0000000-0000-0000-0001-000000000002',
    '10000000-0000-0000-0000-000000000001',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#060E1F"/>
      <stop offset="100%" stop-color="#0D2137"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <rect x="60" y="80" width="280" height="340" rx="16" fill="none" stroke="#1A4A6A" stroke-width="2"/>
  <rect x="80" y="110" width="240" height="30" rx="6" fill="#0D2A44"/>
  <rect x="80" y="155" width="180" height="20" rx="4" fill="#0A1E33"/>
  <rect x="80" y="185" width="200" height="20" rx="4" fill="#0A1E33"/>
  <rect x="80" y="215" width="160" height="20" rx="4" fill="#0A1E33"/>
  <rect x="80" y="255" width="240" height="60" rx="8" fill="#051428" stroke="#00D9FF" stroke-width="1"/>
  <circle cx="110" cy="285" r="14" fill="#00D9FF" opacity="0.15" stroke="#00D9FF" stroke-width="2"/>
  <text x="135" y="290" fill="#00D9FF" font-size="14" font-family="monospace">0 alertes actives</text>
  <text x="420" y="190" fill="#E0F4FF" font-size="40" font-family="Arial,sans-serif" font-weight="700">Tableau de bord</text>
  <text x="420" y="240" fill="#5B9EC9" font-size="22" font-family="Arial,sans-serif">Monitoring centralisé</text>
  <text x="420" y="290" fill="#2A5F7F" font-size="16" font-family="Arial,sans-serif">Visualisation des événements</text>
  <text x="420" y="320" fill="#2A5F7F" font-size="16" font-family="Arial,sans-serif">de sécurité en temps réel</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 1, NOW(), NOW()
),
(
    'b0000000-0000-0000-0001-000000000003',
    '10000000-0000-0000-0000-000000000001',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#060E1F"/>
      <stop offset="100%" stop-color="#0D2137"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="100" y="160" fill="#E0F4FF" font-size="48" font-family="Arial,sans-serif" font-weight="700">Alerting</text>
  <text x="100" y="215" fill="#5B9EC9" font-size="22" font-family="Arial,sans-serif">Notifications intelligentes</text>
  <rect x="100" y="250" width="580" height="50" rx="10" fill="#051428" stroke="#00D9FF" stroke-width="1.5"/>
  <circle cx="135" cy="275" r="12" fill="#00D9FF" opacity="0.2" stroke="#00D9FF" stroke-width="2"/>
  <text x="160" y="281" fill="#00D9FF" font-size="15" font-family="monospace">ALERTE · Connexion suspecte détectée</text>
  <rect x="100" y="315" width="580" height="50" rx="10" fill="#051428" stroke="#00FF99" stroke-width="1.5"/>
  <circle cx="135" cy="340" r="12" fill="#00FF99" opacity="0.2" stroke="#00FF99" stroke-width="2"/>
  <text x="160" y="346" fill="#00FF99" font-size="15" font-family="monospace">INFO · Analyse terminée · 0 menace</text>
  <rect x="100" y="380" width="580" height="50" rx="10" fill="#051428" stroke="#FFB800" stroke-width="1.5"/>
  <circle cx="135" cy="405" r="12" fill="#FFB800" opacity="0.2" stroke="#FFB800" stroke-width="2"/>
  <text x="160" y="411" fill="#FFB800" font-size="15" font-family="monospace">WARN · Trafic inhabituel · port 4444</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 2, NOW(), NOW()
),

-- ── SOC Advanced (4 images) ──────────────────────────────────────────────────
(
    'b0000000-0000-0000-0002-000000000001',
    '10000000-0000-0000-0000-000000000002',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#06101F"/>
      <stop offset="100%" stop-color="#102840"/>
    </linearGradient>
    <filter id="glow"><feGaussianBlur stdDeviation="5" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <path d="M200 70 L340 125 L328 255 C316 335 282 390 200 430 C118 390 84 335 72 255 L60 125 Z"
        fill="none" stroke="#00D9FF" stroke-width="7" filter="url(#glow)"/>
  <path d="M200 70 L340 125 L328 255 C316 335 282 390 200 430 C118 390 84 335 72 255 L60 125 Z"
        fill="#00D9FF" opacity="0.04"/>
  <circle cx="200" cy="255" r="70" fill="none" stroke="#00D9FF" stroke-width="3" opacity="0.4"/>
  <circle cx="200" cy="255" r="40" fill="none" stroke="#00D9FF" stroke-width="2" opacity="0.6"/>
  <circle cx="200" cy="255" r="10" fill="#00D9FF" opacity="0.8" filter="url(#glow)"/>
  <text x="420" y="190" fill="#E0F4FF" font-size="52" font-family="Arial,sans-serif" font-weight="700">SOC</text>
  <text x="420" y="250" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif" font-weight="400">Advanced</text>
  <text x="420" y="300" fill="#5B9EC9" font-size="18" font-family="Arial,sans-serif">SIEM · SOAR · Playbooks</text>
  <text x="420" y="340" fill="#2A7FAF" font-size="15" font-family="Arial,sans-serif">Analystes dédiés · Réponse aux incidents</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 0, NOW(), NOW()
),
(
    'b0000000-0000-0000-0002-000000000002',
    '10000000-0000-0000-0000-000000000002',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#06101F"/>
      <stop offset="100%" stop-color="#102840"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="120" fill="#E0F4FF" font-size="40" font-family="Arial,sans-serif" font-weight="700">SOAR Automatisé</text>
  <rect x="80" y="155" width="620" height="3" fill="#1A4A6A"/>
  <rect x="80" y="180" width="140" height="80" rx="10" fill="#051428" stroke="#00D9FF" stroke-width="2"/>
  <text x="150" y="228" fill="#00D9FF" font-size="13" font-family="monospace" text-anchor="middle">Détection</text>
  <line x1="220" y1="220" x2="280" y2="220" stroke="#00D9FF" stroke-width="2" marker-end="url(#arrow)"/>
  <rect x="280" y="180" width="140" height="80" rx="10" fill="#051428" stroke="#00FF99" stroke-width="2"/>
  <text x="350" y="228" fill="#00FF99" font-size="13" font-family="monospace" text-anchor="middle">Analyse</text>
  <line x1="420" y1="220" x2="480" y2="220" stroke="#00FF99" stroke-width="2"/>
  <rect x="480" y="180" width="140" height="80" rx="10" fill="#051428" stroke="#FFB800" stroke-width="2"/>
  <text x="550" y="228" fill="#FFB800" font-size="13" font-family="monospace" text-anchor="middle">Réponse</text>
  <line x1="550" y1="260" x2="550" y2="320" stroke="#FFB800" stroke-width="2"/>
  <rect x="480" y="320" width="140" height="80" rx="10" fill="#051428" stroke="#FF4B4B" stroke-width="2"/>
  <text x="550" y="368" fill="#FF4B4B" font-size="13" font-family="monospace" text-anchor="middle">Remédiation</text>
  <text x="80" y="450" fill="#2A5F7F" font-size="16" font-family="Arial,sans-serif">Orchestration automatique des playbooks de sécurité</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 1, NOW(), NOW()
),
(
    'b0000000-0000-0000-0002-000000000003',
    '10000000-0000-0000-0000-000000000002',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#06101F"/>
      <stop offset="100%" stop-color="#102840"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#E0F4FF" font-size="40" font-family="Arial,sans-serif" font-weight="700">SLA 24/7 · Garantie de service</text>
  <circle cx="200" cy="280" r="110" fill="none" stroke="#1A4A6A" stroke-width="16"/>
  <path d="M200 170 A110 110 0 1 1 93 335" fill="none" stroke="#00D9FF" stroke-width="16" stroke-linecap="round"/>
  <text x="200" y="270" fill="#00D9FF" font-size="44" font-family="Arial,sans-serif" font-weight="700" text-anchor="middle">99.9%</text>
  <text x="200" y="310" fill="#5B9EC9" font-size="16" font-family="Arial,sans-serif" text-anchor="middle">disponibilité</text>
  <text x="430" y="210" fill="#E0F4FF" font-size="22" font-family="Arial,sans-serif">Temps de réponse</text>
  <text x="430" y="250" fill="#00D9FF" font-size="36" font-family="Arial,sans-serif" font-weight="700">&lt; 15 min</text>
  <text x="430" y="310" fill="#E0F4FF" font-size="22" font-family="Arial,sans-serif">Incidents traités/mois</text>
  <text x="430" y="350" fill="#00FF99" font-size="36" font-family="Arial,sans-serif" font-weight="700">∞</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 2, NOW(), NOW()
),
(
    'b0000000-0000-0000-0002-000000000004',
    '10000000-0000-0000-0000-000000000002',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#06101F"/>
      <stop offset="100%" stop-color="#102840"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#E0F4FF" font-size="40" font-family="Arial,sans-serif" font-weight="700">Intégration API</text>
  <rect x="80" y="150" width="180" height="60" rx="10" fill="#051428" stroke="#00D9FF" stroke-width="2"/>
  <text x="170" y="187" fill="#00D9FF" font-size="14" font-family="monospace" text-anchor="middle">Firewall</text>
  <rect x="80" y="240" width="180" height="60" rx="10" fill="#051428" stroke="#00D9FF" stroke-width="2"/>
  <text x="170" y="277" fill="#00D9FF" font-size="14" font-family="monospace" text-anchor="middle">IDS / IPS</text>
  <rect x="80" y="330" width="180" height="60" rx="10" fill="#051428" stroke="#00D9FF" stroke-width="2"/>
  <text x="170" y="367" fill="#00D9FF" font-size="14" font-family="monospace" text-anchor="middle">SIEM Interne</text>
  <rect x="320" y="210" width="160" height="80" rx="14" fill="#0A1E33" stroke="#00FF99" stroke-width="3"/>
  <text x="400" y="258" fill="#00FF99" font-size="16" font-family="monospace" text-anchor="middle">SOC Advanced</text>
  <line x1="260" y1="180" x2="320" y2="240" stroke="#1A4A6A" stroke-width="2" stroke-dasharray="6,4"/>
  <line x1="260" y1="270" x2="320" y2="260" stroke="#1A4A6A" stroke-width="2" stroke-dasharray="6,4"/>
  <line x1="260" y1="360" x2="320" y2="280" stroke="#1A4A6A" stroke-width="2" stroke-dasharray="6,4"/>
  <rect x="540" y="210" width="180" height="80" rx="10" fill="#051428" stroke="#FFB800" stroke-width="2"/>
  <text x="630" y="258" fill="#FFB800" font-size="14" font-family="monospace" text-anchor="middle">Dashboard &amp; Reports</text>
  <line x1="480" y1="250" x2="540" y2="250" stroke="#1A4A6A" stroke-width="2" stroke-dasharray="6,4"/>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 3, NOW(), NOW()
),

-- ── SOC Enterprise (3 images) ────────────────────────────────────────────────
(
    'b0000000-0000-0000-0003-000000000001',
    '10000000-0000-0000-0000-000000000003',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#080C18"/>
      <stop offset="100%" stop-color="#0E2030"/>
    </linearGradient>
    <filter id="glow"><feGaussianBlur stdDeviation="6" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <path d="M200 60 L360 120 L346 270 C330 360 294 415 200 455 C106 415 70 360 54 270 L40 120 Z"
        fill="none" stroke="#FFD700" stroke-width="8" filter="url(#glow)"/>
  <text x="200" y="240" fill="#FFD700" font-size="60" font-family="Arial,sans-serif" font-weight="900" text-anchor="middle" filter="url(#glow)">★</text>
  <text x="200" y="295" fill="#FFD700" font-size="16" font-family="Arial,sans-serif" text-anchor="middle">ENTERPRISE</text>
  <text x="420" y="185" fill="#E0F4FF" font-size="52" font-family="Arial,sans-serif" font-weight="700">SOC</text>
  <text x="420" y="245" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif">Enterprise</text>
  <text x="420" y="300" fill="#FFD700" font-size="18" font-family="Arial,sans-serif">Threat Intelligence · Hunting proactif</text>
  <text x="420" y="340" fill="#5B9EC9" font-size="15" font-family="Arial,sans-serif">Équipe dédiée · SIEM + SOAR + Threat Intel</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 0, NOW(), NOW()
),
(
    'b0000000-0000-0000-0003-000000000002',
    '10000000-0000-0000-0000-000000000003',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#080C18"/>
      <stop offset="100%" stop-color="#0E2030"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#FFD700" font-size="38" font-family="Arial,sans-serif" font-weight="700">Threat Intelligence</text>
  <circle cx="400" cy="280" r="80" fill="#051020" stroke="#FFD700" stroke-width="3"/>
  <text x="400" y="270" fill="#FFD700" font-size="14" font-family="monospace" text-anchor="middle">THREAT</text>
  <text x="400" y="295" fill="#FFD700" font-size="14" font-family="monospace" text-anchor="middle">INTEL HUB</text>
  <circle cx="180" cy="160" r="40" fill="#051020" stroke="#00D9FF" stroke-width="2"/>
  <text x="180" y="167" fill="#00D9FF" font-size="12" font-family="monospace" text-anchor="middle">IOC</text>
  <line x1="218" y1="182" x2="325" y2="230" stroke="#00D9FF" stroke-width="1.5" stroke-dasharray="5,3"/>
  <circle cx="620" cy="160" r="40" fill="#051020" stroke="#00D9FF" stroke-width="2"/>
  <text x="620" y="167" fill="#00D9FF" font-size="12" font-family="monospace" text-anchor="middle">CVE</text>
  <line x1="582" y1="182" x2="475" y2="230" stroke="#00D9FF" stroke-width="1.5" stroke-dasharray="5,3"/>
  <circle cx="180" cy="380" r="40" fill="#051020" stroke="#FF4B4B" stroke-width="2"/>
  <text x="180" y="387" fill="#FF4B4B" font-size="12" font-family="monospace" text-anchor="middle">APT</text>
  <line x1="218" y1="362" x2="325" y2="322" stroke="#FF4B4B" stroke-width="1.5" stroke-dasharray="5,3"/>
  <circle cx="620" cy="380" r="40" fill="#051020" stroke="#00FF99" stroke-width="2"/>
  <text x="620" y="387" fill="#00FF99" font-size="12" font-family="monospace" text-anchor="middle">FEEDS</text>
  <line x1="582" y1="362" x2="475" y2="322" stroke="#00FF99" stroke-width="1.5" stroke-dasharray="5,3"/>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 1, NOW(), NOW()
),
(
    'b0000000-0000-0000-0003-000000000003',
    '10000000-0000-0000-0000-000000000003',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#080C18"/>
      <stop offset="100%" stop-color="#0E2030"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif" font-weight="700">Threat Hunting Proactif</text>
  <rect x="80" y="145" width="620" height="2" fill="#1A3A5A"/>
  <text x="80" y="195" fill="#FFD700" font-size="18" font-family="monospace">→ Recherche des menaces latentes</text>
  <text x="80" y="235" fill="#00D9FF" font-size="18" font-family="monospace">→ Analyse comportementale avancée</text>
  <text x="80" y="275" fill="#00FF99" font-size="18" font-family="monospace">→ Corrélation multi-sources (SIEM+EDR+NDR)</text>
  <text x="80" y="315" fill="#FF4B4B" font-size="18" font-family="monospace">→ Détection des APT et zero-days</text>
  <text x="80" y="355" fill="#FFD700" font-size="18" font-family="monospace">→ Rapports d'investigation détaillés</text>
  <text x="80" y="440" fill="#2A5F7F" font-size="15" font-family="Arial,sans-serif">Inclus avec SOC Enterprise · Équipe d'experts dédiée</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 2, NOW(), NOW()
),

-- ── EDR Essential (2 images) ─────────────────────────────────────────────────
(
    'b0000000-0000-0000-0004-000000000001',
    '20000000-0000-0000-0000-000000000001',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#060F20"/>
      <stop offset="100%" stop-color="#0C2238"/>
    </linearGradient>
    <filter id="glow"><feGaussianBlur stdDeviation="4" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <rect x="80" y="100" width="260" height="180" rx="16" fill="none" stroke="#00D9FF" stroke-width="5" filter="url(#glow)"/>
  <rect x="100" y="120" width="220" height="140" rx="8" fill="#051020"/>
  <rect x="115" y="135" width="190" height="15" rx="3" fill="#0D2A44"/>
  <rect x="115" y="160" width="140" height="12" rx="3" fill="#0A1E33"/>
  <rect x="115" y="182" width="160" height="12" rx="3" fill="#0A1E33"/>
  <rect x="115" y="204" width="120" height="12" rx="3" fill="#0A1E33"/>
  <rect x="180" y="280" width="80" height="12" rx="6" fill="#1A4A6A"/>
  <rect x="130" y="292" width="180" height="30" rx="4" fill="#051020" stroke="#1A4A6A" stroke-width="1"/>
  <path d="M165 230 L190 258 L238 212" fill="none" stroke="#00FF99" stroke-width="6" stroke-linecap="round" filter="url(#glow)"/>
  <text x="420" y="185" fill="#E0F4FF" font-size="52" font-family="Arial,sans-serif" font-weight="700">EDR</text>
  <text x="420" y="245" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif">Essential</text>
  <text x="420" y="300" fill="#5B9EC9" font-size="18" font-family="Arial,sans-serif">Agent léger · Console cloud</text>
  <text x="420" y="340" fill="#2A7FAF" font-size="15" font-family="Arial,sans-serif">Protection endpoint · Isolation machine</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 0, NOW(), NOW()
),
(
    'b0000000-0000-0000-0004-000000000002',
    '20000000-0000-0000-0000-000000000001',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#060F20"/>
      <stop offset="100%" stop-color="#0C2238"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#E0F4FF" font-size="40" font-family="Arial,sans-serif" font-weight="700">Détection Comportementale</text>
  <rect x="80" y="150" width="260" height="120" rx="12" fill="#051020" stroke="#00D9FF" stroke-width="2"/>
  <text x="210" y="195" fill="#5B9EC9" font-size="14" font-family="monospace" text-anchor="middle">Comportement normal</text>
  <polyline points="100,250 130,230 160,245 190,220 220,238 250,215 280,232 310,218 330,230" fill="none" stroke="#00FF99" stroke-width="2.5"/>
  <rect x="400" y="150" width="320" height="120" rx="12" fill="#051020" stroke="#FF4B4B" stroke-width="2"/>
  <text x="560" y="195" fill="#FF4B4B" font-size="14" font-family="monospace" text-anchor="middle">Anomalie détectée</text>
  <polyline points="420,240 450,235 480,238 510,230 520,190 530,245 560,235 580,238 610,233 700,236" fill="none" stroke="#FF4B4B" stroke-width="2.5"/>
  <circle cx="520" cy="190" r="10" fill="#FF4B4B" opacity="0.8"/>
  <text x="80" y="380" fill="#00D9FF" font-size="18" font-family="monospace">ALERTE : Processus inhabituel détecté sur endpoint-07</text>
  <text x="80" y="440" fill="#2A5F7F" font-size="15" font-family="Arial,sans-serif">Isolation automatique · Analyse forensique déclenchée</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 1, NOW(), NOW()
),

-- ── EDR Professional (3 images) ──────────────────────────────────────────────
(
    'b0000000-0000-0000-0005-000000000001',
    '20000000-0000-0000-0000-000000000002',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#060F20"/>
      <stop offset="100%" stop-color="#0C2238"/>
    </linearGradient>
    <filter id="glow"><feGaussianBlur stdDeviation="4" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <rect x="60" y="100" width="260" height="180" rx="16" fill="none" stroke="#00D9FF" stroke-width="5" filter="url(#glow)"/>
  <rect x="80" y="120" width="220" height="140" rx="8" fill="#051020"/>
  <path d="M120 200 L148 228 L210 170" fill="none" stroke="#00FF99" stroke-width="8" stroke-linecap="round" filter="url(#glow)"/>
  <text x="420" y="185" fill="#E0F4FF" font-size="52" font-family="Arial,sans-serif" font-weight="700">EDR</text>
  <text x="420" y="245" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif">Professional</text>
  <text x="420" y="300" fill="#5B9EC9" font-size="18" font-family="Arial,sans-serif">Remédiation auto · Forensic tools</text>
  <text x="420" y="340" fill="#2A7FAF" font-size="15" font-family="Arial,sans-serif">Détection comportementale avancée</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 0, NOW(), NOW()
),
(
    'b0000000-0000-0000-0005-000000000002',
    '20000000-0000-0000-0000-000000000002',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#060F20"/>
      <stop offset="100%" stop-color="#0C2238"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif" font-weight="700">Remédiation Automatique</text>
  <rect x="80" y="150" width="620" height="2" fill="#1A3A5A"/>
  <text x="80" y="200" fill="#FF4B4B" font-size="16" font-family="monospace">① Ransomware détecté sur WORKSTATION-12</text>
  <text x="80" y="240" fill="#FFB800" font-size="16" font-family="monospace">② Isolation réseau automatique déclenchée</text>
  <text x="80" y="280" fill="#00D9FF" font-size="16" font-family="monospace">③ Snapshot mémoire capturé pour analyse</text>
  <text x="80" y="320" fill="#00FF99" font-size="16" font-family="monospace">④ Processus malveillant terminé · 0 propagation</text>
  <text x="80" y="360" fill="#00FF99" font-size="16" font-family="monospace">⑤ Restauration depuis backup validé</text>
  <text x="80" y="440" fill="#2A5F7F" font-size="15" font-family="Arial,sans-serif">Temps de réponse total : 47 secondes · Aucune intervention manuelle</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 1, NOW(), NOW()
),
(
    'b0000000-0000-0000-0005-000000000003',
    '20000000-0000-0000-0000-000000000002',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#060F20"/>
      <stop offset="100%" stop-color="#0C2238"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif" font-weight="700">Forensic &amp; Investigation</text>
  <rect x="80" y="155" width="300" height="220" rx="12" fill="#051020" stroke="#00D9FF" stroke-width="2"/>
  <text x="230" y="190" fill="#5B9EC9" font-size="15" font-family="monospace" text-anchor="middle">Timeline d'attaque</text>
  <line x1="130" y1="210" x2="130" y2="355" stroke="#1A4A6A" stroke-width="2"/>
  <circle cx="130" cy="225" r="6" fill="#FF4B4B"/>
  <text x="148" y="230" fill="#FF4B4B" font-size="12" font-family="monospace">08:14 · Phishing email</text>
  <circle cx="130" cy="260" r="6" fill="#FFB800"/>
  <text x="148" y="265" fill="#FFB800" font-size="12" font-family="monospace">08:17 · Macro exécutée</text>
  <circle cx="130" cy="295" r="6" fill="#FF4B4B"/>
  <text x="148" y="300" fill="#FF4B4B" font-size="12" font-family="monospace">08:19 · Lateral movement</text>
  <circle cx="130" cy="330" r="6" fill="#00D9FF"/>
  <text x="148" y="335" fill="#00D9FF" font-size="12" font-family="monospace">08:21 · Bloqué par EDR</text>
  <text x="430" y="195" fill="#E0F4FF" font-size="22" font-family="Arial,sans-serif">Analyse complète</text>
  <text x="430" y="235" fill="#5B9EC9" font-size="16" font-family="Arial,sans-serif">· Arbre de processus complet</text>
  <text x="430" y="270" fill="#5B9EC9" font-size="16" font-family="Arial,sans-serif">· Hash des fichiers suspects</text>
  <text x="430" y="305" fill="#5B9EC9" font-size="16" font-family="Arial,sans-serif">· Rapport d'investigation PDF</text>
  <text x="430" y="340" fill="#5B9EC9" font-size="16" font-family="Arial,sans-serif">· Recommandations correctives</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 2, NOW(), NOW()
),

-- ── EDR Elite (3 images) ─────────────────────────────────────────────────────
(
    'b0000000-0000-0000-0006-000000000001',
    '20000000-0000-0000-0000-000000000003',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#060D1A"/>
      <stop offset="100%" stop-color="#0A1C2E"/>
    </linearGradient>
    <filter id="glow"><feGaussianBlur stdDeviation="5" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="120" fill="#FFD700" font-size="52" font-family="Arial,sans-serif" font-weight="700">EDR Elite</text>
  <text x="80" y="175" fill="#5B9EC9" font-size="22" font-family="Arial,sans-serif">Protection endpoint premium</text>
  <rect x="80" y="210" width="620" height="2" fill="#1A3A5A"/>
  <text x="80" y="260" fill="#E0F4FF" font-size="18" font-family="Arial,sans-serif">★  EDR complet avec threat hunting</text>
  <text x="80" y="300" fill="#E0F4FF" font-size="18" font-family="Arial,sans-serif">★  Sandboxing des fichiers suspects</text>
  <text x="80" y="340" fill="#E0F4FF" font-size="18" font-family="Arial,sans-serif">★  Intégration SIEM native</text>
  <text x="80" y="380" fill="#FFD700" font-size="18" font-family="Arial,sans-serif">★  Réponse automatisée niveau enterprise</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 0, NOW(), NOW()
),
(
    'b0000000-0000-0000-0006-000000000002',
    '20000000-0000-0000-0000-000000000003',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#060D1A"/>
      <stop offset="100%" stop-color="#0A1C2E"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif" font-weight="700">Sandboxing Avancé</text>
  <rect x="100" y="155" width="260" height="200" rx="16" fill="#051020" stroke="#FFD700" stroke-width="3"/>
  <text x="230" y="195" fill="#FFD700" font-size="14" font-family="monospace" text-anchor="middle">SANDBOX ISOLÉE</text>
  <rect x="130" y="215" width="200" height="120" rx="8" fill="#030A14" stroke="#1A3A5A" stroke-width="1"/>
  <text x="230" y="250" fill="#FF4B4B" font-size="12" font-family="monospace" text-anchor="middle">malware.exe</text>
  <text x="230" y="275" fill="#FFB800" font-size="11" font-family="monospace" text-anchor="middle">→ Analyse en cours...</text>
  <text x="230" y="300" fill="#00FF99" font-size="11" font-family="monospace" text-anchor="middle">✓ Isolé · Sans risque</text>
  <text x="430" y="195" fill="#E0F4FF" font-size="22" font-family="Arial,sans-serif">Détonation sécurisée</text>
  <text x="430" y="235" fill="#5B9EC9" font-size="16" font-family="Arial,sans-serif">Exécution du fichier dans</text>
  <text x="430" y="265" fill="#5B9EC9" font-size="16" font-family="Arial,sans-serif">un environnement isolé</text>
  <text x="430" y="310" fill="#E0F4FF" font-size="16" font-family="Arial,sans-serif">→ Comportement analysé</text>
  <text x="430" y="345" fill="#E0F4FF" font-size="16" font-family="Arial,sans-serif">→ IOC extraits</text>
  <text x="430" y="380" fill="#E0F4FF" font-size="16" font-family="Arial,sans-serif">→ Rapport de détonation</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 1, NOW(), NOW()
),
(
    'b0000000-0000-0000-0006-000000000003',
    '20000000-0000-0000-0000-000000000003',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#060D1A"/>
      <stop offset="100%" stop-color="#0A1C2E"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif" font-weight="700">Couverture Endpoints</text>
  <rect x="80" y="155" width="580" height="2" fill="#1A3A5A"/>
  <rect x="80" y="185" width="110" height="80" rx="10" fill="#051020" stroke="#00D9FF" stroke-width="2"/>
  <text x="135" y="230" fill="#00D9FF" font-size="12" font-family="monospace" text-anchor="middle">Windows</text>
  <rect x="210" y="185" width="110" height="80" rx="10" fill="#051020" stroke="#00D9FF" stroke-width="2"/>
  <text x="265" y="230" fill="#00D9FF" font-size="12" font-family="monospace" text-anchor="middle">macOS</text>
  <rect x="340" y="185" width="110" height="80" rx="10" fill="#051020" stroke="#00D9FF" stroke-width="2"/>
  <text x="395" y="230" fill="#00D9FF" font-size="12" font-family="monospace" text-anchor="middle">Linux</text>
  <rect x="470" y="185" width="110" height="80" rx="10" fill="#051020" stroke="#00FF99" stroke-width="2"/>
  <text x="525" y="230" fill="#00FF99" font-size="12" font-family="monospace" text-anchor="middle">Mobile</text>
  <text x="80" y="340" fill="#FFD700" font-size="22" font-family="Arial,sans-serif">Agents ultra-légers · 0.1% CPU</text>
  <text x="80" y="385" fill="#5B9EC9" font-size="16" font-family="Arial,sans-serif">Déploiement silencieux · Gestion centralisée via console cloud</text>
  <text x="80" y="450" fill="#2A5F7F" font-size="14" font-family="Arial,sans-serif">Support illimité · Mises à jour automatiques · Sans redémarrage</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 2, NOW(), NOW()
),

-- ── XDR Core (3 images) ──────────────────────────────────────────────────────
(
    'b0000000-0000-0000-0007-000000000001',
    '30000000-0000-0000-0000-000000000001',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#070D1C"/>
      <stop offset="100%" stop-color="#0F2035"/>
    </linearGradient>
    <filter id="glow"><feGaussianBlur stdDeviation="4" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <circle cx="200" cy="140" r="28" fill="#051020" stroke="#00D9FF" stroke-width="3" filter="url(#glow)"/>
  <text x="200" y="147" fill="#00D9FF" font-size="11" font-family="monospace" text-anchor="middle">Réseau</text>
  <circle cx="200" cy="320" r="28" fill="#051020" stroke="#00D9FF" stroke-width="3"/>
  <text x="200" y="327" fill="#00D9FF" font-size="11" font-family="monospace" text-anchor="middle">Email</text>
  <circle cx="100" cy="230" r="28" fill="#051020" stroke="#00D9FF" stroke-width="3"/>
  <text x="100" y="237" fill="#00D9FF" font-size="11" font-family="monospace" text-anchor="middle">Cloud</text>
  <circle cx="200" cy="230" r="40" fill="#051020" stroke="#00FF99" stroke-width="4" filter="url(#glow)"/>
  <text x="200" y="237" fill="#00FF99" font-size="12" font-family="monospace" text-anchor="middle">XDR</text>
  <line x1="200" y1="168" x2="200" y2="190" stroke="#00D9FF" stroke-width="2"/>
  <line x1="200" y1="270" x2="200" y2="292" stroke="#00D9FF" stroke-width="2"/>
  <line x1="128" y1="230" x2="160" y2="230" stroke="#00D9FF" stroke-width="2"/>
  <text x="400" y="185" fill="#E0F4FF" font-size="52" font-family="Arial,sans-serif" font-weight="700">XDR</text>
  <text x="400" y="245" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif">Core</text>
  <text x="400" y="300" fill="#5B9EC9" font-size="18" font-family="Arial,sans-serif">Corrélation multi-sources</text>
  <text x="400" y="340" fill="#2A7FAF" font-size="15" font-family="Arial,sans-serif">Dashboard centralisé · Logs unifiés</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 0, NOW(), NOW()
),
(
    'b0000000-0000-0000-0007-000000000002',
    '30000000-0000-0000-0000-000000000001',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#070D1C"/>
      <stop offset="100%" stop-color="#0F2035"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif" font-weight="700">Collecte de logs unifiée</text>
  <rect x="80" y="150" width="580" height="2" fill="#1A3A5A"/>
  <rect x="80" y="180" width="560" height="40" rx="8" fill="#051020" stroke="#1A4A6A" stroke-width="1"/>
  <text x="100" y="206" fill="#00D9FF" font-size="13" font-family="monospace">[INFO]  10.0.1.5  → AUTH  SUCCESS  user=admin  2026-05-12T09:14:32Z</text>
  <rect x="80" y="230" width="560" height="40" rx="8" fill="#051020" stroke="#FF4B4B" stroke-width="1"/>
  <text x="100" y="256" fill="#FF4B4B" font-size="13" font-family="monospace">[WARN]  10.0.1.9  → SCAN  DETECTED  ports=22,80,443  2026-05-12T09:15:01Z</text>
  <rect x="80" y="280" width="560" height="40" rx="8" fill="#051020" stroke="#1A4A6A" stroke-width="1"/>
  <text x="100" y="306" fill="#5B9EC9" font-size="13" font-family="monospace">[INFO]  firewall   → DROP  src=185.234.x.x  dst=10.0.1.5  2026-05-12T09:15:02Z</text>
  <rect x="80" y="330" width="560" height="40" rx="8" fill="#051020" stroke="#FFB800" stroke-width="1"/>
  <text x="100" y="356" fill="#FFB800" font-size="13" font-family="monospace">[WARN]  EDR-Agent  → PROC  suspicious  cmd=powershell -enc ...  09:15:10Z</text>
  <text x="80" y="440" fill="#2A5F7F" font-size="15" font-family="Arial,sans-serif">Corrélation automatique · Détection de schémas d'attaque multi-étapes</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 1, NOW(), NOW()
),
(
    'b0000000-0000-0000-0007-000000000003',
    '30000000-0000-0000-0000-000000000001',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#070D1C"/>
      <stop offset="100%" stop-color="#0F2035"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif" font-weight="700">Dashboard Centralisé</text>
  <rect x="80" y="150" width="620" height="260" rx="14" fill="#051020" stroke="#1A4A6A" stroke-width="2"/>
  <rect x="100" y="170" width="160" height="90" rx="8" fill="#030A14" stroke="#00D9FF" stroke-width="1.5"/>
  <text x="180" y="200" fill="#5B9EC9" font-size="12" font-family="monospace" text-anchor="middle">Alertes / 24h</text>
  <text x="180" y="235" fill="#00D9FF" font-size="28" font-family="Arial,sans-serif" font-weight="700" text-anchor="middle">247</text>
  <rect x="280" y="170" width="160" height="90" rx="8" fill="#030A14" stroke="#00FF99" stroke-width="1.5"/>
  <text x="360" y="200" fill="#5B9EC9" font-size="12" font-family="monospace" text-anchor="middle">Bloquées</text>
  <text x="360" y="235" fill="#00FF99" font-size="28" font-family="Arial,sans-serif" font-weight="700" text-anchor="middle">245</text>
  <rect x="460" y="170" width="160" height="90" rx="8" fill="#030A14" stroke="#FF4B4B" stroke-width="1.5"/>
  <text x="540" y="200" fill="#5B9EC9" font-size="12" font-family="monospace" text-anchor="middle">En cours</text>
  <text x="540" y="235" fill="#FF4B4B" font-size="28" font-family="Arial,sans-serif" font-weight="700" text-anchor="middle">2</text>
  <polyline points="100,360 150,340 200,350 250,320 300,335 350,310 400,325 450,305 500,318 550,300 600,312 660,295" fill="none" stroke="#00D9FF" stroke-width="2"/>
  <text x="100" y="435" fill="#2A5F7F" font-size="13" font-family="monospace">Tendance des incidents · 30 derniers jours</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 2, NOW(), NOW()
),

-- ── XDR Advanced (5 images) ──────────────────────────────────────────────────
(
    'b0000000-0000-0000-0008-000000000001',
    '30000000-0000-0000-0000-000000000002',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#070D1C"/>
      <stop offset="100%" stop-color="#0F2035"/>
    </linearGradient>
    <filter id="glow"><feGaussianBlur stdDeviation="5" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <circle cx="200" cy="100" r="28" fill="#051020" stroke="#00D9FF" stroke-width="3"/>
  <text x="200" y="107" fill="#00D9FF" font-size="10" font-family="monospace" text-anchor="middle">Endpoints</text>
  <circle cx="100" cy="250" r="28" fill="#051020" stroke="#00D9FF" stroke-width="3"/>
  <text x="100" y="257" fill="#00D9FF" font-size="10" font-family="monospace" text-anchor="middle">Cloud</text>
  <circle cx="200" cy="400" r="28" fill="#051020" stroke="#00D9FF" stroke-width="3"/>
  <text x="200" y="407" fill="#00D9FF" font-size="10" font-family="monospace" text-anchor="middle">Email</text>
  <circle cx="330" cy="180" r="28" fill="#051020" stroke="#00D9FF" stroke-width="3"/>
  <text x="330" y="187" fill="#00D9FF" font-size="10" font-family="monospace" text-anchor="middle">Réseau</text>
  <circle cx="330" cy="320" r="28" fill="#051020" stroke="#00D9FF" stroke-width="3"/>
  <text x="330" y="327" fill="#00D9FF" font-size="10" font-family="monospace" text-anchor="middle">Identity</text>
  <circle cx="215" cy="250" r="52" fill="#051020" stroke="#00FF99" stroke-width="4" filter="url(#glow)"/>
  <text x="215" y="245" fill="#00FF99" font-size="13" font-family="monospace" text-anchor="middle">XDR</text>
  <text x="215" y="265" fill="#00FF99" font-size="11" font-family="monospace" text-anchor="middle">Advanced</text>
  <line x1="200" y1="128" x2="205" y2="198" stroke="#1A4A6A" stroke-width="1.5" stroke-dasharray="4,3"/>
  <line x1="128" y1="250" x2="163" y2="250" stroke="#1A4A6A" stroke-width="1.5" stroke-dasharray="4,3"/>
  <line x1="200" y1="372" x2="205" y2="302" stroke="#1A4A6A" stroke-width="1.5" stroke-dasharray="4,3"/>
  <line x1="302" y1="196" x2="262" y2="222" stroke="#1A4A6A" stroke-width="1.5" stroke-dasharray="4,3"/>
  <line x1="302" y1="306" x2="262" y2="276" stroke="#1A4A6A" stroke-width="1.5" stroke-dasharray="4,3"/>
  <text x="430" y="185" fill="#E0F4FF" font-size="52" font-family="Arial,sans-serif" font-weight="700">XDR</text>
  <text x="430" y="245" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif">Advanced</text>
  <text x="430" y="300" fill="#5B9EC9" font-size="18" font-family="Arial,sans-serif">Corrélation IA · Détection anomalies</text>
  <text x="430" y="340" fill="#2A7FAF" font-size="15" font-family="Arial,sans-serif">EDR + SIEM + Réseau + Cloud</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 0, NOW(), NOW()
),
(
    'b0000000-0000-0000-0008-000000000002',
    '30000000-0000-0000-0000-000000000002',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#070D1C"/>
      <stop offset="100%" stop-color="#0F2035"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif" font-weight="700">Corrélation par IA</text>
  <rect x="80" y="155" width="580" height="2" fill="#1A3A5A"/>
  <text x="80" y="205" fill="#5B9EC9" font-size="16" font-family="Arial,sans-serif">Modèle d'apprentissage automatique</text>
  <rect x="80" y="225" width="580" height="50" rx="10" fill="#051020" stroke="#00D9FF" stroke-width="1.5"/>
  <rect x="95" y="240" width="260" height="20" rx="6" fill="#0D2A44"/>
  <rect x="95" y="240" width="204" height="20" rx="6" fill="#00D9FF" opacity="0.5"/>
  <text x="630" y="258" fill="#00D9FF" font-size="14" font-family="monospace">78%</text>
  <rect x="80" y="285" width="580" height="50" rx="10" fill="#051020" stroke="#00FF99" stroke-width="1.5"/>
  <rect x="95" y="300" width="260" height="20" rx="6" fill="#0D2A44"/>
  <rect x="95" y="300" width="240" height="20" rx="6" fill="#00FF99" opacity="0.5"/>
  <text x="630" y="318" fill="#00FF99" font-size="14" font-family="monospace">92%</text>
  <rect x="80" y="345" width="580" height="50" rx="10" fill="#051020" stroke="#FFB800" stroke-width="1.5"/>
  <rect x="95" y="360" width="260" height="20" rx="6" fill="#0D2A44"/>
  <rect x="95" y="360" width="130" height="20" rx="6" fill="#FFB800" opacity="0.5"/>
  <text x="630" y="378" fill="#FFB800" font-size="14" font-family="monospace">50%</text>
  <text x="80" y="450" fill="#2A5F7F" font-size="14" font-family="Arial,sans-serif">Précision de détection · Faux positifs · Couverture MITRE ATT&amp;CK</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 1, NOW(), NOW()
),
(
    'b0000000-0000-0000-0008-000000000003',
    '30000000-0000-0000-0000-000000000002',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#070D1C"/>
      <stop offset="100%" stop-color="#0F2035"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif" font-weight="700">Détection des Anomalies</text>
  <text x="80" y="165" fill="#5B9EC9" font-size="18" font-family="Arial,sans-serif">Baseline comportementale · Déviation automatique</text>
  <polyline points="80,320 130,315 180,318 230,310 280,315 330,305 380,270 410,290 440,275 480,310 530,308 580,312 630,305 680,308" fill="none" stroke="#1A4A6A" stroke-width="2"/>
  <polyline points="80,320 130,315 180,318 230,310 280,315 330,305" fill="none" stroke="#00FF99" stroke-width="3"/>
  <polyline points="330,305 380,270 410,290 440,275" fill="none" stroke="#FF4B4B" stroke-width="3"/>
  <polyline points="440,275 480,310 530,308 580,312 630,305 680,308" fill="none" stroke="#00FF99" stroke-width="3"/>
  <circle cx="380" cy="270" r="10" fill="#FF4B4B" opacity="0.9"/>
  <circle cx="440" cy="275" r="10" fill="#FF4B4B" opacity="0.9"/>
  <rect x="350" y="220" width="160" height="36" rx="6" fill="#FF4B4B" opacity="0.15" stroke="#FF4B4B" stroke-width="1"/>
  <text x="430" y="243" fill="#FF4B4B" font-size="13" font-family="monospace" text-anchor="middle">Anomalie détectée</text>
  <text x="80" y="440" fill="#2A5F7F" font-size="14" font-family="Arial,sans-serif">Alerte automatique · Corrélation avec logs réseau et EDR</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 2, NOW(), NOW()
),
(
    'b0000000-0000-0000-0008-000000000004',
    '30000000-0000-0000-0000-000000000002',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#070D1C"/>
      <stop offset="100%" stop-color="#0F2035"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif" font-weight="700">MITRE ATT&amp;CK Coverage</text>
  <rect x="80" y="150" width="580" height="2" fill="#1A3A5A"/>
  <rect x="80" y="175" width="78" height="45" rx="6" fill="#00D9FF" opacity="0.25" stroke="#00D9FF" stroke-width="1"/>
  <text x="119" y="202" fill="#00D9FF" font-size="11" font-family="monospace" text-anchor="middle">Recon</text>
  <rect x="168" y="175" width="78" height="45" rx="6" fill="#00D9FF" opacity="0.25" stroke="#00D9FF" stroke-width="1"/>
  <text x="207" y="202" fill="#00D9FF" font-size="11" font-family="monospace" text-anchor="middle">Resource</text>
  <rect x="256" y="175" width="78" height="45" rx="6" fill="#FF4B4B" opacity="0.35" stroke="#FF4B4B" stroke-width="1.5"/>
  <text x="295" y="202" fill="#FF4B4B" font-size="11" font-family="monospace" text-anchor="middle">Initial</text>
  <rect x="344" y="175" width="78" height="45" rx="6" fill="#FF4B4B" opacity="0.35" stroke="#FF4B4B" stroke-width="1.5"/>
  <text x="383" y="202" fill="#FF4B4B" font-size="11" font-family="monospace" text-anchor="middle">Execution</text>
  <rect x="432" y="175" width="78" height="45" rx="6" fill="#00FF99" opacity="0.3" stroke="#00FF99" stroke-width="1.5"/>
  <text x="471" y="202" fill="#00FF99" font-size="11" font-family="monospace" text-anchor="middle">Persist.</text>
  <rect x="520" y="175" width="78" height="45" rx="6" fill="#00FF99" opacity="0.3" stroke="#00FF99" stroke-width="1.5"/>
  <text x="559" y="202" fill="#00FF99" font-size="11" font-family="monospace" text-anchor="middle">Priv. Esc</text>
  <rect x="80" y="230" width="78" height="45" rx="6" fill="#00FF99" opacity="0.3" stroke="#00FF99" stroke-width="1.5"/>
  <text x="119" y="257" fill="#00FF99" font-size="11" font-family="monospace" text-anchor="middle">Defense</text>
  <rect x="168" y="230" width="78" height="45" rx="6" fill="#00FF99" opacity="0.3" stroke="#00FF99" stroke-width="1.5"/>
  <text x="207" y="257" fill="#00FF99" font-size="11" font-family="monospace" text-anchor="middle">Cred. Acc</text>
  <rect x="256" y="230" width="78" height="45" rx="6" fill="#FFB800" opacity="0.3" stroke="#FFB800" stroke-width="1.5"/>
  <text x="295" y="257" fill="#FFB800" font-size="11" font-family="monospace" text-anchor="middle">Discovery</text>
  <rect x="344" y="230" width="78" height="45" rx="6" fill="#FFB800" opacity="0.3" stroke="#FFB800" stroke-width="1.5"/>
  <text x="383" y="257" fill="#FFB800" font-size="11" font-family="monospace" text-anchor="middle">Lateral</text>
  <rect x="432" y="230" width="78" height="45" rx="6" fill="#00FF99" opacity="0.3" stroke="#00FF99" stroke-width="1.5"/>
  <text x="471" y="257" fill="#00FF99" font-size="11" font-family="monospace" text-anchor="middle">Collect.</text>
  <rect x="520" y="230" width="78" height="45" rx="6" fill="#00FF99" opacity="0.3" stroke="#00FF99" stroke-width="1.5"/>
  <text x="559" y="257" fill="#00FF99" font-size="11" font-family="monospace" text-anchor="middle">C2</text>
  <text x="80" y="340" fill="#00FF99" font-size="22" font-family="Arial,sans-serif" font-weight="700">87% de couverture MITRE ATT&amp;CK</text>
  <text x="80" y="390" fill="#5B9EC9" font-size="16" font-family="Arial,sans-serif">Techniques détectées : 174 / 200</text>
  <text x="80" y="450" fill="#2A5F7F" font-size="14" font-family="Arial,sans-serif">Mis à jour en continu · Corrélation automatique avec les TTPs connus</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 3, NOW(), NOW()
),
(
    'b0000000-0000-0000-0008-000000000005',
    '30000000-0000-0000-0000-000000000002',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#070D1C"/>
      <stop offset="100%" stop-color="#0F2035"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif" font-weight="700">Intégration EDR + SIEM</text>
  <rect x="80" y="175" width="240" height="120" rx="14" fill="#051020" stroke="#00D9FF" stroke-width="2"/>
  <text x="200" y="225" fill="#00D9FF" font-size="16" font-family="Arial,sans-serif" font-weight="700" text-anchor="middle">EDR Agent</text>
  <text x="200" y="255" fill="#5B9EC9" font-size="13" font-family="monospace" text-anchor="middle">Endpoint telemetry</text>
  <rect x="480" y="175" width="240" height="120" rx="14" fill="#051020" stroke="#00FF99" stroke-width="2"/>
  <text x="600" y="225" fill="#00FF99" font-size="16" font-family="Arial,sans-serif" font-weight="700" text-anchor="middle">SIEM</text>
  <text x="600" y="255" fill="#5B9EC9" font-size="13" font-family="monospace" text-anchor="middle">Log aggregation</text>
  <rect x="300" y="195" width="200" height="80" rx="12" fill="#040D1C" stroke="#FFB800" stroke-width="2"/>
  <text x="400" y="240" fill="#FFB800" font-size="15" font-family="Arial,sans-serif" font-weight="700" text-anchor="middle">XDR Advanced</text>
  <line x1="320" y1="235" x2="300" y2="235" stroke="#1A4A6A" stroke-width="2" stroke-dasharray="5,3"/>
  <line x1="500" y1="235" x2="480" y2="235" stroke="#1A4A6A" stroke-width="2" stroke-dasharray="5,3"/>
  <text x="80" y="370" fill="#E0F4FF" font-size="18" font-family="Arial,sans-serif">→ Corrélation cross-sources en temps réel</text>
  <text x="80" y="410" fill="#E0F4FF" font-size="18" font-family="Arial,sans-serif">→ Réduction des faux positifs de 60%</text>
  <text x="80" y="450" fill="#2A5F7F" font-size="14" font-family="Arial,sans-serif">Connecteurs natifs disponibles pour 50+ produits de sécurité</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 4, NOW(), NOW()
),

-- ── XDR Ultimate (3 images) ──────────────────────────────────────────────────
(
    'b0000000-0000-0000-0009-000000000001',
    '30000000-0000-0000-0000-000000000003',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#060B16"/>
      <stop offset="100%" stop-color="#0C1C2E"/>
    </linearGradient>
    <filter id="glow"><feGaussianBlur stdDeviation="6" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <circle cx="200" cy="250" r="130" fill="none" stroke="#FFD700" stroke-width="2" opacity="0.3"/>
  <circle cx="200" cy="250" r="90" fill="none" stroke="#FFD700" stroke-width="2" opacity="0.5"/>
  <circle cx="200" cy="250" r="50" fill="#051020" stroke="#FFD700" stroke-width="3" filter="url(#glow)"/>
  <text x="200" y="245" fill="#FFD700" font-size="13" font-family="Arial,sans-serif" text-anchor="middle">XDR</text>
  <text x="200" y="265" fill="#FFD700" font-size="11" font-family="Arial,sans-serif" text-anchor="middle">ULTIMATE</text>
  <circle cx="200" cy="120" r="20" fill="#051020" stroke="#00D9FF" stroke-width="2"/>
  <text x="200" y="127" fill="#00D9FF" font-size="9" font-family="monospace" text-anchor="middle">EDR</text>
  <circle cx="310" cy="180" r="20" fill="#051020" stroke="#00D9FF" stroke-width="2"/>
  <text x="310" y="187" fill="#00D9FF" font-size="9" font-family="monospace" text-anchor="middle">SIEM</text>
  <circle cx="330" cy="320" r="20" fill="#051020" stroke="#00D9FF" stroke-width="2"/>
  <text x="330" y="327" fill="#00D9FF" font-size="9" font-family="monospace" text-anchor="middle">SOAR</text>
  <circle cx="200" cy="380" r="20" fill="#051020" stroke="#00D9FF" stroke-width="2"/>
  <text x="200" y="387" fill="#00D9FF" font-size="9" font-family="monospace" text-anchor="middle">NDR</text>
  <circle cx="90" cy="320" r="20" fill="#051020" stroke="#00D9FF" stroke-width="2"/>
  <text x="90" y="327" fill="#00D9FF" font-size="9" font-family="monospace" text-anchor="middle">Cloud</text>
  <circle cx="70" cy="180" r="20" fill="#051020" stroke="#00D9FF" stroke-width="2"/>
  <text x="70" y="187" fill="#00D9FF" font-size="9" font-family="monospace" text-anchor="middle">IAM</text>
  <text x="430" y="185" fill="#FFD700" font-size="52" font-family="Arial,sans-serif" font-weight="700">XDR</text>
  <text x="430" y="245" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif">Ultimate</text>
  <text x="430" y="300" fill="#5B9EC9" font-size="18" font-family="Arial,sans-serif">XDR + SOAR + IA + Playbooks</text>
  <text x="430" y="340" fill="#2A7FAF" font-size="15" font-family="Arial,sans-serif">Réponse automatisée complète</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 0, NOW(), NOW()
),
(
    'b0000000-0000-0000-0009-000000000002',
    '30000000-0000-0000-0000-000000000003',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#060B16"/>
      <stop offset="100%" stop-color="#0C1C2E"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#FFD700" font-size="38" font-family="Arial,sans-serif" font-weight="700">Orchestration Complète</text>
  <rect x="80" y="155" width="580" height="2" fill="#2A3A5A"/>
  <rect x="80" y="185" width="580" height="55" rx="10" fill="#051020" stroke="#FFD700" stroke-width="2"/>
  <text x="100" y="218" fill="#FFD700" font-size="14" font-family="monospace">PLAYBOOK  · Ransomware Response  · Auto-triggered</text>
  <rect x="80" y="250" width="580" height="55" rx="10" fill="#051020" stroke="#00D9FF" stroke-width="1.5"/>
  <text x="100" y="283" fill="#00D9FF" font-size="14" font-family="monospace">PLAYBOOK  · Phishing Triage       · SLA: 5min</text>
  <rect x="80" y="315" width="580" height="55" rx="10" fill="#051020" stroke="#00FF99" stroke-width="1.5"/>
  <text x="100" y="348" fill="#00FF99" font-size="14" font-family="monospace">PLAYBOOK  · Insider Threat         · Continuous</text>
  <rect x="80" y="380" width="580" height="55" rx="10" fill="#051020" stroke="#00D9FF" stroke-width="1.5"/>
  <text x="100" y="413" fill="#5B9EC9" font-size="14" font-family="monospace">PLAYBOOK  · Zero-Day Mitigation   · Priority: HIGH</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 1, NOW(), NOW()
),
(
    'b0000000-0000-0000-0009-000000000003',
    '30000000-0000-0000-0000-000000000003',
    convert_to($svg$<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#060B16"/>
      <stop offset="100%" stop-color="#0C1C2E"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <text x="80" y="110" fill="#E0F4FF" font-size="38" font-family="Arial,sans-serif" font-weight="700">Réponse Automatisée</text>
  <text x="80" y="165" fill="#FFD700" font-size="20" font-family="Arial,sans-serif">Temps moyen de confinement : &lt; 2 minutes</text>
  <rect x="80" y="200" width="580" height="2" fill="#1A3A5A"/>
  <text x="80" y="250" fill="#E0F4FF" font-size="17" font-family="Arial,sans-serif">✦  Isolation endpoint en 1 clic (ou automatique)</text>
  <text x="80" y="290" fill="#E0F4FF" font-size="17" font-family="Arial,sans-serif">✦  Blocage IP / domaine malveillant instantané</text>
  <text x="80" y="330" fill="#E0F4FF" font-size="17" font-family="Arial,sans-serif">✦  Désactivation compte compromis (AD / Entra ID)</text>
  <text x="80" y="370" fill="#E0F4FF" font-size="17" font-family="Arial,sans-serif">✦  Notification RSSI + ticket ITSM automatique</text>
  <text x="80" y="410" fill="#FFD700" font-size="17" font-family="Arial,sans-serif">✦  Rapport post-incident généré en temps réel</text>
  <text x="80" y="460" fill="#2A5F7F" font-size="14" font-family="Arial,sans-serif">XDR Ultimate · Inclus sans surcoût · Support 24/7 garanti</text>
</svg>$svg$, 'UTF8'),
    'image/svg+xml', 2, NOW(), NOW()
)

ON CONFLICT (id) DO NOTHING;


-- =====================================================
-- 6. PARAMÈTRES DU CARROUSEL D'OFFRES (singleton)
-- =====================================================

INSERT INTO product_schema.offer_carousel_settings (id, max_slides)
VALUES (1, 5)
ON CONFLICT (id) DO NOTHING;


-- =====================================================
-- 7. TRADUCTIONS DES PARAMÈTRES DU CARROUSEL
-- =====================================================

INSERT INTO product_schema.offer_carousel_settings_translations (settings_id, locale, fixed_text)
VALUES
    (1, 'fr', 'Offres exclusives · Durée limitée · Profitez de réductions sur nos solutions de cybersécurité'),
    (1, 'en', 'Exclusive offers · Limited time · Save on our cybersecurity solutions')
ON CONFLICT (settings_id, locale) DO NOTHING;


-- =====================================================
-- 8. PROMOTIONS
-- =====================================================

INSERT INTO product_schema.promotions (id, product_id, discount_percent, start_at, end_at, is_enabled)
VALUES
-- SOC Starter -20% (offre estivale : juin → août 2026)
(
    'c0000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    20,
    TIMESTAMPTZ '2026-06-01 00:00:00+00',
    TIMESTAMPTZ '2026-08-31 23:59:59+00',
    TRUE
),
-- EDR Essential -30% (offre découverte : juin → juillet 2026)
(
    'c0000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000001',
    30,
    TIMESTAMPTZ '2026-06-10 00:00:00+00',
    TIMESTAMPTZ '2026-07-31 23:59:59+00',
    TRUE
),
-- XDR Core -15% (offre printemps → été 2026)
(
    'c0000000-0000-0000-0000-000000000003',
    '30000000-0000-0000-0000-000000000001',
    15,
    TIMESTAMPTZ '2026-05-01 00:00:00+00',
    TIMESTAMPTZ '2026-07-31 23:59:59+00',
    TRUE
),
-- SOC Advanced -10% (rentrée : juillet → septembre 2026)
(
    'c0000000-0000-0000-0000-000000000004',
    '10000000-0000-0000-0000-000000000002',
    10,
    TIMESTAMPTZ '2026-07-01 00:00:00+00',
    TIMESTAMPTZ '2026-09-30 23:59:59+00',
    TRUE
)
ON CONFLICT (id) DO NOTHING;


-- =====================================================
-- 9. TRADUCTIONS DES PROMOTIONS
-- =====================================================

INSERT INTO product_schema.promotion_translations (promotion_id, locale, marketing_text)
VALUES
-- SOC Starter -20%
('c0000000-0000-0000-0000-000000000001', 'fr', 'Offre Été : -20% sur SOC Starter. Supervision 24/7 à prix réduit pour toute souscription avant le 31 août.'),
('c0000000-0000-0000-0000-000000000001', 'en', 'Summer Deal: -20% on SOC Starter. 24/7 monitoring at a reduced price for any subscription before August 31.'),
-- EDR Essential -30%
('c0000000-0000-0000-0000-000000000002', 'fr', 'Offre Découverte : -30% sur EDR Essential. Protégez vos endpoints dès maintenant avec notre tarif de lancement.'),
('c0000000-0000-0000-0000-000000000002', 'en', 'Discovery Offer: -30% on EDR Essential. Protect your endpoints now with our launch pricing.'),
-- XDR Core -15%
('c0000000-0000-0000-0000-000000000003', 'fr', 'Printemps Sécurité : -15% sur XDR Core. Corrélation multi-sources à tarif préférentiel jusqu''au 31 juillet.'),
('c0000000-0000-0000-0000-000000000003', 'en', 'Security Spring: -15% on XDR Core. Multi-source correlation at a preferential rate until July 31.'),
-- SOC Advanced -10%
('c0000000-0000-0000-0000-000000000004', 'fr', 'Offre Rentrée : -10% sur SOC Advanced. Préparez votre sécurité pour la rentrée avec analystes dédiés.'),
('c0000000-0000-0000-0000-000000000004', 'en', 'Back-to-Business Offer: -10% on SOC Advanced. Get your security ready for the new season with dedicated analysts.')
ON CONFLICT (promotion_id, locale) DO NOTHING;
