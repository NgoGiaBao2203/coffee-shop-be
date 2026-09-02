-- =========================================
-- Table: shop_tables (for ordering drinks)
-- =========================================
CREATE TABLE shop_tables (
    table_id UUID PRIMARY KEY NOT NULL,
    table_number INTEGER NOT NULL,
    description VARCHAR(255),
    status INTEGER DEFAULT 1, -- 1 = available, 2 = occupied, 3 = reserved
    shop_id UUID NOT NULL,
    is_deleted BOOLEAN DEFAULT false,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP
);

ALTER TABLE shop_tables
ADD CONSTRAINT fk_shop_tables_shop FOREIGN KEY (shop_id) REFERENCES shops (shop_id);

ALTER TABLE shop_tables
ADD CONSTRAINT fk_shop_tables_deleted_by FOREIGN KEY (deleted_by) REFERENCES profiles (profile_id);