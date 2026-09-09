-- Ensure the configured admin account has ROLE_ADMIN and ROLE_USER.
-- Replace the email below if your admin email differs from .env APP_INIT_ADMIN_EMAIL.

SET @admin_email = 'admin@shoe-shop.local';

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM user_account u
JOIN role r ON r.code IN ('ROLE_ADMIN', 'ROLE_USER')
LEFT JOIN user_role ur ON ur.user_id = u.id AND ur.role_id = r.id
WHERE u.email = @admin_email
  AND ur.user_id IS NULL;

UPDATE user_account
SET status = 'ACTIVE',
    is_email_verified = true
WHERE email = @admin_email;
