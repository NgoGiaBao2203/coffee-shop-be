-- 1. Add the role_id column to profiles matching the UUID type of the roles table
ALTER TABLE profiles ADD COLUMN role_id UUID;

-- 2. Add the foreign key constraint linking it to the roles table
ALTER TABLE profiles
ADD CONSTRAINT fk_profiles_role FOREIGN KEY (role_id) REFERENCES roles (role_id);