ALTER TABLE invoices ADD COLUMN invoice_code VARCHAR(30);

UPDATE invoices
SET invoice_code = 'INV-' || to_char(created_at, 'YYYYMMDD') || '-'
    || upper(substr(replace(invoice_id::text, '-', ''), 1, 8))
WHERE invoice_code IS NULL;

ALTER TABLE invoices ALTER COLUMN invoice_code SET NOT NULL;

CREATE UNIQUE INDEX uq_invoices_invoice_code ON invoices (invoice_code);
