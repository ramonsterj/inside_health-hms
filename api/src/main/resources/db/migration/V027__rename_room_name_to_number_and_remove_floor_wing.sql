-- Rename 'name' column to 'number' and remove 'floor'/'wing' columns from rooms table

-- Step 1: Rename the 'name' column to 'number'
ALTER TABLE rooms RENAME COLUMN name TO number;

-- Step 2: Drop the floor index and columns
DROP INDEX IF EXISTS idx_rooms_floor;
ALTER TABLE rooms DROP COLUMN IF EXISTS floor;
ALTER TABLE rooms DROP COLUMN IF EXISTS wing;
