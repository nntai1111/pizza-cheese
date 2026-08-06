-- name: findByEmail
SELECT id, username, email, password_hash, full_name, phone, avatar_url,
       is_email_verified AS email_verified, is_active AS active, created_at, updated_at
FROM users
WHERE LOWER(email) = LOWER(:email)

-- name: findByUsername
SELECT id, username, email, password_hash, full_name, phone, avatar_url,
       is_email_verified AS email_verified, is_active AS active, created_at, updated_at
FROM users
WHERE LOWER(username) = LOWER(:username)

-- name: findByEmailOrUsername
SELECT id, username, email, password_hash, full_name, phone, avatar_url,
       is_email_verified AS email_verified, is_active AS active, created_at, updated_at
FROM users
WHERE LOWER(email) = LOWER(:login) OR LOWER(username) = LOWER(:login)

-- name: findById
SELECT id, username, email, password_hash, full_name, phone, avatar_url,
       is_email_verified AS email_verified, is_active AS active, created_at, updated_at
FROM users
WHERE id = :id

-- name: findDisplayInfoByIds
SELECT id, full_name, email, phone
FROM users
WHERE id IN (:ids)

-- name: findRolesByUserId
SELECT r.name AS role
FROM user_roles ur
JOIN roles r ON r.id = ur.role_id
WHERE ur.user_id = :userId

-- name: existsByEmail
SELECT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = LOWER(:email))

-- name: existsByUsername
SELECT EXISTS (SELECT 1 FROM users WHERE LOWER(username) = LOWER(:username))

-- name: count
SELECT COUNT(*) FROM users

-- name: countStaffBase
SELECT COUNT(*)
FROM users u
WHERE COALESCE(u.is_deleted, FALSE) = FALSE
  AND EXISTS (
      SELECT 1
      FROM user_roles ur
      JOIN roles r ON r.id = ur.role_id
      WHERE ur.user_id = u.id
        AND r.name IN ('CASHIER', 'KITCHEN', 'DELIVERY', 'ADMIN')
  )

-- name: findStaffPageBase
SELECT u.id, u.username, u.email, u.password_hash, u.full_name, u.phone, u.avatar_url,
       u.is_email_verified AS email_verified, u.is_active AS active, u.created_at, u.updated_at
FROM users u
WHERE COALESCE(u.is_deleted, FALSE) = FALSE
  AND EXISTS (
      SELECT 1
      FROM user_roles ur
      JOIN roles r ON r.id = ur.role_id
      WHERE ur.user_id = u.id
        AND r.name IN ('CASHIER', 'KITCHEN', 'DELIVERY', 'ADMIN')
  )

-- name: countCustomersBase
SELECT COUNT(*)
FROM users u
WHERE COALESCE(u.is_deleted, FALSE) = FALSE
  AND EXISTS (
      SELECT 1
      FROM user_roles ur
      JOIN roles r ON r.id = ur.role_id
      WHERE ur.user_id = u.id
        AND r.name = 'CUSTOMER'
  )

-- name: countCustomersCreatedBetween
SELECT COUNT(*)
FROM users u
WHERE COALESCE(u.is_deleted, FALSE) = FALSE
  AND u.created_at >= :from
  AND u.created_at < :to
  AND EXISTS (
      SELECT 1
      FROM user_roles ur
      JOIN roles r ON r.id = ur.role_id
      WHERE ur.user_id = u.id
        AND r.name = 'CUSTOMER'
  )

-- name: findCustomersPageBase
SELECT u.id, u.username, u.email, u.password_hash, u.full_name, u.phone, u.avatar_url,
       u.is_email_verified AS email_verified, u.is_active AS active, u.created_at, u.updated_at
FROM users u
WHERE COALESCE(u.is_deleted, FALSE) = FALSE
  AND EXISTS (
      SELECT 1
      FROM user_roles ur
      JOIN roles r ON r.id = ur.role_id
      WHERE ur.user_id = u.id
        AND r.name = 'CUSTOMER'
  )

-- name: insert
INSERT INTO users (id, username, email, password_hash, full_name, phone, avatar_url, is_active, is_email_verified, created_at, updated_at)
VALUES (:id, :username, :email, :passwordHash, :fullName, :phone, :avatarUrl, :active, :emailVerified, :createdAt, :updatedAt)

-- name: update
UPDATE users
SET email = :email,
    password_hash = :passwordHash,
    full_name = :fullName,
    phone = :phone,
    avatar_url = :avatarUrl,
    is_active = :active,
    is_email_verified = :emailVerified,
    updated_at = :updatedAt
WHERE id = :id

-- name: deleteById
DELETE FROM users WHERE id = :id

-- name: deleteRolesByUserId
DELETE FROM user_roles WHERE user_id = :userId

-- name: insertRole
INSERT INTO user_roles (user_id, role_id)
SELECT :userId, id FROM roles WHERE name = :role
