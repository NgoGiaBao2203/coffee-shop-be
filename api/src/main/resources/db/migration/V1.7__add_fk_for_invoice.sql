---------------------------------------------------------
-- Update invoices: add table_id and remove table_number
---------------------------------------------------------
ALTER TABLE invoices ADD COLUMN table_id UUID;

ALTER TABLE invoices
ADD CONSTRAINT fk_invoices_table FOREIGN KEY (table_id) REFERENCES shop_tables (table_id);

-- Remove old column if no longer needed
ALTER TABLE invoices DROP COLUMN table_number;