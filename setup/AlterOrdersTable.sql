-- Run this file once against an existing onlinebookstore database.
-- It is idempotent and works on MySQL versions that do not support
-- ALTER TABLE ... ADD COLUMN IF NOT EXISTS.
USE onlinebookstore;

DROP PROCEDURE IF EXISTS migrate_orders_lifecycle;
DELIMITER //

CREATE PROCEDURE migrate_orders_lifecycle()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'cancel_reason'
    ) THEN
        ALTER TABLE orders ADD COLUMN cancel_reason TEXT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'cancelled_by'
    ) THEN
        ALTER TABLE orders ADD COLUMN cancelled_by ENUM('CUSTOMER', 'SELLER', 'SYSTEM') NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'cancelled_at'
    ) THEN
        ALTER TABLE orders ADD COLUMN cancelled_at TIMESTAMP NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'shipped_at'
    ) THEN
        ALTER TABLE orders ADD COLUMN shipped_at TIMESTAMP NULL;
    END IF;
END //

DELIMITER ;
CALL migrate_orders_lifecycle();
DROP PROCEDURE migrate_orders_lifecycle;

-- Normalize legacy rows before enforcing the current order lifecycle.
UPDATE orders
SET status = 'CONFIRMED'
WHERE status = 'PAID' OR status IS NULL;

ALTER TABLE orders
    MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'PENDING';

-- Existing shipping orders predate shipped_at. Use their order time as a
-- conservative fallback so the auto-complete task can handle them.
UPDATE orders
SET shipped_at = order_date
WHERE status = 'SHIPPING' AND shipped_at IS NULL;

