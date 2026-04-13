INSERT INTO product_schema.categories (id, name, description, is_active)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'SOC', 'Security Operations Center', TRUE),
    ('00000000-0000-0000-0000-000000000002', 'EDR', 'Endpoint Detection & Response', TRUE),
    ('00000000-0000-0000-0000-000000000003', 'XDR', 'Extended Detection & Response', TRUE)
    ON CONFLICT (name) DO NOTHING;


INSERT INTO product_schema.products (
    id,
    name,
    category,
    service_description,
    technical_description,
    monthly_price,
    annual_price,
    currency,
    status,
    priority
)
VALUES
(
    '10000000-0000-0000-0000-000000000001',
    'SOC Starter',
    'SOC',
    'Supervision de sécurité 24/7 pour petites entreprises avec alerting de base',
    'SIEM mutualisé, collecte logs, alertes temps réel, dashboard basique',
    99.99,
    999.99,
    'EUR',
    'PUBLISHED',
    'NORMALE'
),
(
    '10000000-0000-0000-0000-000000000002',
    'SOC Advanced',
    'SOC',
    'SOC avancé avec analystes dédiés et réponse aux incidents',
    'SIEM dédié, playbooks automatisés, SOAR, intégration API, SLA 24/7',
    299.99,
    2999.99,
    'EUR',
    'PUBLISHED',
    'MOYENNE'
),
(
    '10000000-0000-0000-0000-000000000003',
    'SOC Enterprise',
    'SOC',
    'SOC complet avec équipe dédiée et threat intelligence',
    'SIEM + SOAR + Threat Intel + hunting proactif + intégration SI complexe',
    799.99,
    7999.99,
    'EUR',
    'PUBLISHED',
    'HAUTE'
),

(
    '20000000-0000-0000-0000-000000000001',
    'EDR Essential',
    'EDR',
    'Protection des endpoints avec détection comportementale',
    'Agent léger, détection malware, isolation machine, console cloud',
    4.99,
    49.99,
    'EUR',
    'PUBLISHED',
    'NORMALE'
),
(
    '20000000-0000-0000-0000-000000000002',
    'EDR Professional',
    'EDR',
    'EDR avancé avec réponse automatisée',
    'Détection comportementale avancée, remédiation auto, forensic tools',
    9.99,
    99.99,
    'EUR',
    'PUBLISHED',
    'MOYENNE'
),
(
    '20000000-0000-0000-0000-000000000003',
    'EDR Elite',
    'EDR',
    'Protection endpoint premium avec threat hunting',
    'EDR + threat hunting + sandboxing + intégration SIEM',
    14.99,
    149.99,
    'EUR',
    'PUBLISHED',
    'HAUTE'
),

(
    '30000000-0000-0000-0000-000000000001',
    'XDR Core',
    'XDR',
    'Corrélation des événements sécurité multi-sources',
    'Collecte logs, corrélation basique, dashboard centralisé',
    199.99,
    1999.99,
    'EUR',
    'PUBLISHED',
    'MOYENNE'
),
(
    '30000000-0000-0000-0000-000000000002',
    'XDR Advanced',
    'XDR',
    'XDR avec intelligence avancée et détection automatisée',
    'Corrélation IA, détection anomalies, intégration EDR + SIEM',
    399.99,
    3999.99,
    'EUR',
    'PUBLISHED',
    'HAUTE'
),
(
    '30000000-0000-0000-0000-000000000003',
    'XDR Ultimate',
    'XDR',
    'Plateforme XDR complète avec automatisation et orchestration',
    'XDR + SOAR + IA + playbooks avancés + réponse automatisée complète',
    699.99,
    6999.99,
    'EUR',
    'PUBLISHED',
    'HAUTE'
)

    ON CONFLICT (id) DO NOTHING;
