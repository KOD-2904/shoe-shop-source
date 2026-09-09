-- Fix legacy cart_items schema where variant_size_id was unique globally.
-- Correct rule: the same variant_size_id can appear in many carts, but only once per cart.
-- Run this against the MySQL database used by the app.

SET @schema_name = DATABASE();

-- A non-unique index is required for the FK before dropping a legacy unique index.
SET @has_variant_index = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'cart_items'
      AND index_name = 'idx_cart_items_variant_size_id'
);

SET @sql = IF(
    @has_variant_index = 0,
    'CREATE INDEX idx_cart_items_variant_size_id ON cart_items (variant_size_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop unique indexes that only cover variant_size_id.
SELECT GROUP_CONCAT(CONCAT('DROP INDEX `', index_name, '`') SEPARATOR ', ')
INTO @drop_unique_variant_indexes
FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'cart_items'
      AND non_unique = 0
    GROUP BY index_name
    HAVING COUNT(*) = 1
       AND MAX(column_name = 'variant_size_id') = 1
) legacy_unique_indexes;

SET @sql = IF(
    @drop_unique_variant_indexes IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE cart_items ', @drop_unique_variant_indexes)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure the intended uniqueness still exists.
SET @has_cart_variant_unique = (
    SELECT COUNT(*)
    FROM (
        SELECT index_name
        FROM information_schema.statistics
        WHERE table_schema = @schema_name
          AND table_name = 'cart_items'
          AND non_unique = 0
        GROUP BY index_name
        HAVING COUNT(*) = 2
           AND SUM(column_name = 'cart_id') = 1
           AND SUM(column_name = 'variant_size_id') = 1
    ) intended_unique_indexes
);

SET @sql = IF(
    @has_cart_variant_unique = 0,
    'ALTER TABLE cart_items ADD CONSTRAINT uk_cart_variant_size UNIQUE (cart_id, variant_size_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
