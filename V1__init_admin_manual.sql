-- Manual initial admin seed for a fresh database.
-- Run this after the app starts once and Hibernate creates/updates the schema.
-- Keep this file outside db/migration so Flyway will not run it automatically.
--
-- Replace @admin_email and @admin_password_hash before running.
-- @admin_password_hash must be a BCrypt hash, not the plain password.

SET @admin_email = 'ttthinh2904@gmail.com';
SET @admin_password_hash = '$2a$10$/NVnYyOrbcpIw4qRGz91uuTbIBASBzmiazjSs4CB5VD7U3n1EG9XC';
SET @now = NOW(6);

-- 1) Seed roles before creating the admin user.
SET @role_user_id = COALESCE((SELECT id FROM `role` WHERE code = 'ROLE_USER' LIMIT 1), UUID());
SET @role_admin_id = COALESCE((SELECT id FROM `role` WHERE code = 'ROLE_ADMIN' LIMIT 1), UUID());
SET @role_staff_id = COALESCE((SELECT id FROM `role` WHERE code = 'ROLE_STAFF' LIMIT 1), UUID());

INSERT INTO `role` (id, created_at, updated_at, code, name, description)
SELECT @role_user_id, @now, @now, 'ROLE_USER', 'User', 'Default user role'
WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE code = 'ROLE_USER');

INSERT INTO `role` (id, created_at, updated_at, code, name, description)
SELECT @role_admin_id, @now, @now, 'ROLE_ADMIN', 'Admin', 'Administrator'
WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE code = 'ROLE_ADMIN');

INSERT INTO `role` (id, created_at, updated_at, code, name, description)
SELECT @role_staff_id, @now, @now, 'ROLE_STAFF', 'Staff', 'Staff'
WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE code = 'ROLE_STAFF');

SET @role_user_id = (SELECT id FROM `role` WHERE code = 'ROLE_USER' LIMIT 1);
SET @role_admin_id = (SELECT id FROM `role` WHERE code = 'ROLE_ADMIN' LIMIT 1);
SET @role_staff_id = (SELECT id FROM `role` WHERE code = 'ROLE_STAFF' LIMIT 1);

-- 2) Seed permissions.
SET @perm_catalog_id = COALESCE((SELECT id FROM permission WHERE code = 'CATALOG_MANAGE' LIMIT 1), UUID());
SET @perm_orders_id = COALESCE((SELECT id FROM permission WHERE code = 'ORDER_MANAGE' LIMIT 1), UUID());
SET @perm_inventory_id = COALESCE((SELECT id FROM permission WHERE code = 'INVENTORY_MANAGE' LIMIT 1), UUID());
SET @perm_users_id = COALESCE((SELECT id FROM permission WHERE code = 'USER_MANAGE' LIMIT 1), UUID());
SET @perm_shop_id = COALESCE((SELECT id FROM permission WHERE code = 'SHOP' LIMIT 1), UUID());

INSERT INTO permission (id, created_at, updated_at, code, name, description)
SELECT @perm_catalog_id, @now, @now, 'CATALOG_MANAGE', 'Manage catalog', 'Create and update products, variants, brands, and categories'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'CATALOG_MANAGE');

INSERT INTO permission (id, created_at, updated_at, code, name, description)
SELECT @perm_orders_id, @now, @now, 'ORDER_MANAGE', 'Manage orders', 'Update order status and shipping handoff'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'ORDER_MANAGE');

INSERT INTO permission (id, created_at, updated_at, code, name, description)
SELECT @perm_inventory_id, @now, @now, 'INVENTORY_MANAGE', 'Manage inventory', 'Adjust stock and inventory settings'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'INVENTORY_MANAGE');

INSERT INTO permission (id, created_at, updated_at, code, name, description)
SELECT @perm_users_id, @now, @now, 'USER_MANAGE', 'Manage users', 'View and administer users'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'USER_MANAGE');

INSERT INTO permission (id, created_at, updated_at, code, name, description)
SELECT @perm_shop_id, @now, @now, 'SHOP', 'Shop', 'Browse, cart, checkout, orders, wishlist, and reviews'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'SHOP');

SET @perm_catalog_id = (SELECT id FROM permission WHERE code = 'CATALOG_MANAGE' LIMIT 1);
SET @perm_orders_id = (SELECT id FROM permission WHERE code = 'ORDER_MANAGE' LIMIT 1);
SET @perm_inventory_id = (SELECT id FROM permission WHERE code = 'INVENTORY_MANAGE' LIMIT 1);
SET @perm_users_id = (SELECT id FROM permission WHERE code = 'USER_MANAGE' LIMIT 1);
SET @perm_shop_id = (SELECT id FROM permission WHERE code = 'SHOP' LIMIT 1);

-- 3) Link permissions to roles.
INSERT INTO role_permission (role_id, permission_id)
SELECT @role_admin_id, @perm_catalog_id
WHERE NOT EXISTS (SELECT 1 FROM role_permission WHERE role_id = @role_admin_id AND permission_id = @perm_catalog_id);

INSERT INTO role_permission (role_id, permission_id)
SELECT @role_admin_id, @perm_orders_id
WHERE NOT EXISTS (SELECT 1 FROM role_permission WHERE role_id = @role_admin_id AND permission_id = @perm_orders_id);

INSERT INTO role_permission (role_id, permission_id)
SELECT @role_admin_id, @perm_inventory_id
WHERE NOT EXISTS (SELECT 1 FROM role_permission WHERE role_id = @role_admin_id AND permission_id = @perm_inventory_id);

INSERT INTO role_permission (role_id, permission_id)
SELECT @role_admin_id, @perm_users_id
WHERE NOT EXISTS (SELECT 1 FROM role_permission WHERE role_id = @role_admin_id AND permission_id = @perm_users_id);

INSERT INTO role_permission (role_id, permission_id)
SELECT @role_admin_id, @perm_shop_id
WHERE NOT EXISTS (SELECT 1 FROM role_permission WHERE role_id = @role_admin_id AND permission_id = @perm_shop_id);

INSERT INTO role_permission (role_id, permission_id)
SELECT @role_staff_id, @perm_catalog_id
WHERE NOT EXISTS (SELECT 1 FROM role_permission WHERE role_id = @role_staff_id AND permission_id = @perm_catalog_id);

INSERT INTO role_permission (role_id, permission_id)
SELECT @role_staff_id, @perm_orders_id
WHERE NOT EXISTS (SELECT 1 FROM role_permission WHERE role_id = @role_staff_id AND permission_id = @perm_orders_id);

INSERT INTO role_permission (role_id, permission_id)
SELECT @role_staff_id, @perm_inventory_id
WHERE NOT EXISTS (SELECT 1 FROM role_permission WHERE role_id = @role_staff_id AND permission_id = @perm_inventory_id);

INSERT INTO role_permission (role_id, permission_id)
SELECT @role_user_id, @perm_shop_id
WHERE NOT EXISTS (SELECT 1 FROM role_permission WHERE role_id = @role_user_id AND permission_id = @perm_shop_id);

-- 4) Create or update the admin user.
SET @admin_user_id = COALESCE((SELECT id FROM user_account WHERE email = @admin_email LIMIT 1), UUID());

INSERT INTO user_account (
    id,
    created_at,
    updated_at,
    email,
    phone,
    password,
    google_id,
    status,
    is_email_verified
)
SELECT
    @admin_user_id,
    @now,
    @now,
    @admin_email,
    NULL,
    @admin_password_hash,
    NULL,
    'ACTIVE',
    b'1'
WHERE NOT EXISTS (SELECT 1 FROM user_account WHERE email = @admin_email);

UPDATE user_account
SET
    password = @admin_password_hash,
    status = 'ACTIVE',
    is_email_verified = b'1',
    updated_at = @now
WHERE email = @admin_email;

SET @admin_user_id = (SELECT id FROM user_account WHERE email = @admin_email LIMIT 1);

-- 5) Give the admin LOCAL login and admin/user roles.
INSERT INTO user_provider (user_id, provider)
SELECT @admin_user_id, 'LOCAL'
WHERE NOT EXISTS (
    SELECT 1 FROM user_provider
    WHERE user_id = @admin_user_id AND provider = 'LOCAL'
);

INSERT INTO user_role (user_id, role_id)
SELECT @admin_user_id, @role_admin_id
WHERE NOT EXISTS (
    SELECT 1 FROM user_role
    WHERE user_id = @admin_user_id AND role_id = @role_admin_id
);

INSERT INTO user_role (user_id, role_id)
SELECT @admin_user_id, @role_user_id
WHERE NOT EXISTS (
    SELECT 1 FROM user_role
    WHERE user_id = @admin_user_id AND role_id = @role_user_id
);
