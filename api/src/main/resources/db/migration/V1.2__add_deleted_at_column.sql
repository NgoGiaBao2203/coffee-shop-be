-- 1. Add deleted_at to profiles table
ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL;

-- 2. Add deleted_at to profile_shops table (if soft deleting relationship mappings too)
ALTER TABLE profile_shops
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL;