-- ============================================================
-- V10 : Foreign keys + updated_at triggers for orders + subscriptions
-- Hardens referential integrity (no orphan subscriptions) and ensures
-- updated_at is bumped on every row update at the DB level, regardless
-- of who issued the UPDATE (JPA mapper, raw SQL, admin tooling, …).
-- ============================================================

-- ---------- subscription_schema.subscriptions: FK constraints ----------

ALTER TABLE subscription_schema.subscriptions
    ADD CONSTRAINT fk_subscriptions_user
        FOREIGN KEY (user_id) REFERENCES user_schema.users (id);

ALTER TABLE subscription_schema.subscriptions
    ADD CONSTRAINT fk_subscriptions_order
        FOREIGN KEY (order_id) REFERENCES order_schema.orders (id);

ALTER TABLE subscription_schema.subscriptions
    ADD CONSTRAINT fk_subscriptions_product
        FOREIGN KEY (product_id) REFERENCES product_schema.products (id);

-- ---------- updated_at triggers ----------
-- Reuses the update_updated_at_column() function defined in V6 (payment schema).

CREATE TRIGGER set_updated_at_subscriptions
    BEFORE UPDATE ON subscription_schema.subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER set_updated_at_orders
    BEFORE UPDATE ON order_schema.orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
