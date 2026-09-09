DELIMITER //

CREATE PROCEDURE drop_index_if_exists(
    IN table_name_param VARCHAR(64),
    IN index_name_param VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_param
          AND INDEX_NAME = index_name_param
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', table_name_param, ' DROP INDEX ', index_name_param);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

CALL drop_index_if_exists('product_reviews', 'uk_product_review_user_product')//

DROP PROCEDURE drop_index_if_exists//

DELIMITER ;
