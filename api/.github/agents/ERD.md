# Database ERD Design

## Tables

### `roles`
| Column | Type | Constraints |
|--------|------|-------------|
| role_id | UUID | Primary Key |
| role_name | VARCHAR(50) | Unique, Not Null |
| description | VARCHAR(255) | |
| is_deleted | BOOLEAN | Default: false |
| deleted_at | TIMESTAMP | |
| deleted_by | UUID | FK → profiles.profile_id |

---

### `profiles`
| Column | Type | Constraints |
|--------|------|-------------|
| profile_id | UUID | Primary Key |
| email | VARCHAR(100) | Unique, Not Null |
| username | VARCHAR(100) | Unique, Not Null |
| full_name | VARCHAR(100) | |
| password | VARCHAR(256) | |
| phone_number | VARCHAR(11) | |
| role_id | UUID | FK → roles.role_id |
| is_deleted | BOOLEAN | Default: false |
| deleted_at | TIMESTAMP | Soft delete timestamp |
| deleted_by | UUID | FK → profiles.profile_id |
| created_at | TIMESTAMP | Default: now() |
| updated_at | TIMESTAMP | |

---

### `shops`
| Column | Type | Constraints |
|--------|------|-------------|
| shop_id | UUID | Primary Key |
| shop_name | VARCHAR(100) | Not Null |
| address | VARCHAR(255) | |
| phone_number | VARCHAR(11) | |
| is_deleted | BOOLEAN | Default: false |
| deleted_at | TIMESTAMP | |
| deleted_by | UUID | FK → profiles.profile_id |
| created_at | TIMESTAMP | Default: now() |
| updated_at | TIMESTAMP | |

---

### `profile_shops`
| Column | Type | Constraints |
|--------|------|-------------|
| profile_shop_id | UUID | Primary Key |
| profile_id | UUID | Not Null, FK → profiles.profile_id |
| shop_id | UUID | Not Null, FK → shops.shop_id |
| role_id | UUID | Not Null, FK → roles.role_id |
| deleted_at | TIMESTAMP | |
| deleted_by | UUID | FK → profiles.profile_id |
| created_at | TIMESTAMP | Default: now() |
| update_at | TIMESTAMP | |

---

### `shop_tables`
| Column | Type | Constraints |
|--------|------|-------------|
| table_id | UUID | Primary Key |
| table_number | INTEGER | Not Null |
| description | VARCHAR(255) | |
| status | INTEGER | Default: 1 |
| shop_id | UUID | Not Null, FK → shops.shop_id |
| is_deleted | BOOLEAN | Default: false |
| deleted_at | TIMESTAMP | |
| deleted_by | UUID | FK → profiles.profile_id |
| created_at | TIMESTAMP | Default: now() |
| updated_at | TIMESTAMP | |

---

### `invoices`
| Column | Type | Constraints |
|--------|------|-------------|
| invoice_id | UUID | Primary Key |
| profile_id | UUID | Not Null, FK → profiles.profile_id |
| shop_id | UUID | Not Null, FK → shops.shop_id |
| table_id | UUID | Not Null, FK → shop_tables.table_id |
| total_amount | DECIMAL(18,2) | |
| status | INTEGER | |
| is_deleted | BOOLEAN | Default: false |
| deleted_at | TIMESTAMP | |
| deleted_by | UUID | FK → profiles.profile_id |
| created_at | TIMESTAMP | Default: now() |
| update_at | TIMESTAMP | |

---

### `invoice_details`
| Column | Type | Constraints |
|--------|------|-------------|
| invoice_detail_id | UUID | Primary Key |
| invoice_id | UUID | Not Null, FK → invoices.invoice_id |
| drink_detail_id | UUID | Not Null, FK → drink_details.drink_detail_id |
| quantity | INTEGER | |
| unit_price | DECIMAL(18,2) | |
| note | VARCHAR(1000) | |
| deleted_at | TIMESTAMP | |
| deleted_by | UUID | FK → profiles.profile_id |

---

### `drink_categories`
| Column | Type | Constraints |
|--------|------|-------------|
| drink_category_id | UUID | Primary Key |
| shop_id | UUID | Not Null, FK → shops.shop_id |
| category_name | VARCHAR(100) | |
| is_deleted | BOOLEAN | Default: false |
| deleted_at | TIMESTAMP | |
| deleted_by | UUID | FK → profiles.profile_id |

---

### `drinks`
| Column | Type | Constraints |
|--------|------|-------------|
| drink_id | UUID | Primary Key |
| drink_category_id | UUID | Not Null, FK → drink_categories.drink_category_id |
| shop_id | UUID | Not Null, FK → shops.shop_id |
| drink_name | VARCHAR(100) | |
| image_url | VARCHAR(200) | |
| status | INTEGER | |
| is_deleted | BOOLEAN | Default: false |
| deleted_at | TIMESTAMP | |
| deleted_by | UUID | FK → profiles.profile_id |

---

### `drink_details`
| Column | Type | Constraints |
|--------|------|-------------|
| drink_detail_id | UUID | Primary Key |
| drink_id | UUID | Not Null, FK → drinks.drink_id |
| size | VARCHAR(5) | |
| price | DECIMAL(18,2) | |
| deleted_at | TIMESTAMP | |
| deleted_by | UUID | FK → profiles.profile_id |

---

## Relationships

| From | Column | To | Column | Description |
|------|--------|----|--------|-------------|
| profiles | role_id | roles | role_id | Each profile has one role |
| profiles | deleted_by | profiles | profile_id | Self-reference: who deleted the profile |
| roles | deleted_by | profiles | profile_id | Who deleted the role |
| shops | deleted_by | profiles | profile_id | Who deleted the shop |
| profile_shops | profile_id | profiles | profile_id | Profile assigned to shop |
| profile_shops | shop_id | shops | shop_id | Shop in the assignment |
| profile_shops | role_id | roles | role_id | Role of profile within the shop |
| profile_shops | deleted_by | profiles | profile_id | Who deleted the assignment |
| shop_tables | shop_id | shops | shop_id | Table belongs to a shop |
| shop_tables | deleted_by | profiles | profile_id | Who deleted the table |
| invoices | profile_id | profiles | profile_id | Invoice created by profile |
| invoices | shop_id | shops | shop_id | Invoice belongs to a shop |
| invoices | table_id | shop_tables | table_id | Invoice linked to a table |
| invoices | deleted_by | profiles | profile_id | Who deleted the invoice |
| invoice_details | invoice_id | invoices | invoice_id | Detail belongs to an invoice |
| invoice_details | drink_detail_id | drink_details | drink_detail_id | Detail references a drink variant |
| invoice_details | deleted_by | profiles | profile_id | Who deleted the detail |
| drink_categories | shop_id | shops | shop_id | Category belongs to a shop |
| drink_categories | deleted_by | profiles | profile_id | Who deleted the category |
| drinks | drink_category_id | drink_categories | drink_category_id | Drink belongs to a category |
| drinks | shop_id | shops | shop_id | Drink belongs to a shop |
| drinks | deleted_by | profiles | profile_id | Who deleted the drink |
| drink_details | drink_id | drinks | drink_id | Detail (size/price) of a drink |
| drink_details | deleted_by | profiles | profile_id | Who deleted the drink detail |
