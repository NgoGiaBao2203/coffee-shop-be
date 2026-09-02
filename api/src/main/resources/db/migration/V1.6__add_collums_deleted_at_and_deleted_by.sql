-- =========================================
-- Add deleted_at & deleted_by to all tables
-- =========================================

ALTER TABLE roles
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by UUID;

ALTER TABLE shops
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by UUID;

ALTER TABLE profile_shops
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by UUID;

ALTER TABLE invoices
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by UUID;

ALTER TABLE invoice_details
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by UUID;

ALTER TABLE drinks
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by UUID;

ALTER TABLE drink_categories
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by UUID;

ALTER TABLE drink_details
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by UUID;

-- =========================================
-- FK constraints for deleted_by
-- =========================================

ALTER TABLE roles
ADD CONSTRAINT fk_roles_deleted_by FOREIGN KEY (deleted_by) REFERENCES profiles (profile_id);

ALTER TABLE shops
ADD CONSTRAINT fk_shops_deleted_by FOREIGN KEY (deleted_by) REFERENCES profiles (profile_id);

ALTER TABLE profile_shops
ADD CONSTRAINT fk_profile_shops_deleted_by FOREIGN KEY (deleted_by) REFERENCES profiles (profile_id);

ALTER TABLE invoices
ADD CONSTRAINT fk_invoices_deleted_by FOREIGN KEY (deleted_by) REFERENCES profiles (profile_id);

ALTER TABLE invoice_details
ADD CONSTRAINT fk_invoice_details_deleted_by FOREIGN KEY (deleted_by) REFERENCES profiles (profile_id);

ALTER TABLE drinks
ADD CONSTRAINT fk_drinks_deleted_by FOREIGN KEY (deleted_by) REFERENCES profiles (profile_id);

ALTER TABLE drink_categories
ADD CONSTRAINT fk_drink_categories_deleted_by FOREIGN KEY (deleted_by) REFERENCES profiles (profile_id);

ALTER TABLE drink_details
ADD CONSTRAINT fk_drink_details_deleted_by FOREIGN KEY (deleted_by) REFERENCES profiles (profile_id);