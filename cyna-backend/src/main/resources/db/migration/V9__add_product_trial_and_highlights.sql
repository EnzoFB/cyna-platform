-- V9: Add free trial days and highlight points to products
ALTER TABLE product_schema.products
    ADD COLUMN free_trial_days INTEGER NOT NULL DEFAULT 0;

ALTER TABLE product_schema.products
    ADD COLUMN highlight_points JSONB NOT NULL DEFAULT '[]';

-- Update seed data with sample highlight points and trial days
UPDATE product_schema.products
SET free_trial_days = 14,
    highlight_points = '["Protection en temps réel","Mises à jour automatiques","Support 24/7"]'
WHERE name LIKE '%SOC%';

UPDATE product_schema.products
SET free_trial_days = 30,
    highlight_points = '["Détection avancée des menaces","Réponse automatisée","Analyse comportementale"]'
WHERE name LIKE '%EDR%';

UPDATE product_schema.products
SET free_trial_days = 30,
    highlight_points = '["Protection réseau étendue","Corrélation multi-sources","Tableau de bord unifié"]'
WHERE name LIKE '%XDR%';
