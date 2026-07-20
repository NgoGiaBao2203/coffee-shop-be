-- Create Tables --
CREATE TABLE roles (
                       role_id UUID PRIMARY KEY NOT NULL,
                       role_name VARCHAR(50) NOT NULL UNIQUE,
                       description VARCHAR(255),
                       is_deleted BOOLEAN DEFAULT false
);

CREATE TABLE shops (
                       shop_id UUID PRIMARY KEY NOT NULL,
                       shop_name VARCHAR(100) NOT NULL,
                       address VARCHAR(255),
                       phone_number VARCHAR(11),
                       is_deleted BOOLEAN DEFAULT false,
                       created_at TIMESTAMP DEFAULT now(),
                       updated_at TIMESTAMP
);

CREATE TABLE profiles (
                          profile_id UUID PRIMARY KEY NOT NULL,
                          email VARCHAR(100) UNIQUE NOT NULL,
                          username VARCHAR(100) UNIQUE NOT NULL,
                          full_name VARCHAR(100),
                          password VARCHAR(256),
                          created_at TIMESTAMP DEFAULT now(),
                          updated_at TIMESTAMP,
                          phone_number VARCHAR(11),
                          is_deleted BOOLEAN DEFAULT false
);

CREATE TABLE profile_shops (
                               profile_shop_id UUID PRIMARY KEY NOT NULL,
                               profile_id UUID NOT NULL,
                               shop_id UUID NOT NULL,
                               role_id UUID NOT NULL,
                               created_at TIMESTAMP DEFAULT now(),
                               update_at TIMESTAMP
);

CREATE TABLE invoices (
                          invoice_id UUID PRIMARY KEY NOT NULL,
                          created_at TIMESTAMP DEFAULT now(),
                          update_at TIMESTAMP,
                          total_amount DECIMAL(18,2),
                          status INTEGER,
                          table_number INTEGER,
                          is_deleted BOOLEAN DEFAULT false,
                          profile_id UUID NOT NULL,
                          shop_id UUID NOT NULL
);

CREATE TABLE invoice_details (
                                 invoice_detail_id UUID PRIMARY KEY NOT NULL,
                                 quantity INTEGER,
                                 unit_price DECIMAL(18,2),
                                 note VARCHAR(1000),
                                 invoice_id UUID NOT NULL,
                                 drink_detail_id UUID NOT NULL
);

CREATE TABLE drinks (
                        drink_id UUID PRIMARY KEY NOT NULL,
                        drink_name VARCHAR(100),
                        image_url VARCHAR(200),
                        status INTEGER,
                        is_deleted BOOLEAN DEFAULT false,
                        drink_category_id UUID NOT NULL,
                        shop_id UUID NOT NULL
);

CREATE TABLE drink_categories (
                                  drink_category_id UUID PRIMARY KEY NOT NULL,
                                  category_name VARCHAR(100),
                                  is_deleted BOOLEAN DEFAULT false,
                                  shop_id UUID NOT NULL
);

CREATE TABLE drink_details (
                               drink_detail_id UUID PRIMARY KEY NOT NULL,
                               size VARCHAR(5),
                               price DECIMAL(18,2),
                               drink_id UUID NOT NULL
);

-- Add Foreign Key Constraints (Relationships) --
ALTER TABLE profile_shops ADD CONSTRAINT fk_profile_shops_profile FOREIGN KEY (profile_id) REFERENCES profiles(profile_id);
ALTER TABLE profile_shops ADD CONSTRAINT fk_profile_shops_shop FOREIGN KEY (shop_id) REFERENCES shops(shop_id);
ALTER TABLE profile_shops ADD CONSTRAINT fk_profile_shops_role FOREIGN KEY (role_id) REFERENCES roles(role_id);

ALTER TABLE invoices ADD CONSTRAINT fk_invoices_profile FOREIGN KEY (profile_id) REFERENCES profiles(profile_id);
ALTER TABLE invoices ADD CONSTRAINT fk_invoices_shop FOREIGN KEY (shop_id) REFERENCES shops(shop_id);

ALTER TABLE drink_categories ADD CONSTRAINT fk_drink_categories_shop FOREIGN KEY (shop_id) REFERENCES shops(shop_id);

ALTER TABLE drinks ADD CONSTRAINT fk_drinks_category FOREIGN KEY (drink_category_id) REFERENCES drink_categories(drink_category_id);
ALTER TABLE drinks ADD CONSTRAINT fk_drinks_shop FOREIGN KEY (shop_id) REFERENCES shops(shop_id);

ALTER TABLE drink_details ADD CONSTRAINT fk_drink_details_drink FOREIGN KEY (drink_id) REFERENCES drinks(drink_id);

ALTER TABLE invoice_details ADD CONSTRAINT fk_invoice_details_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id);
ALTER TABLE invoice_details ADD CONSTRAINT fk_invoice_details_drink_detail FOREIGN KEY (drink_detail_id) REFERENCES drink_details(drink_detail_id);