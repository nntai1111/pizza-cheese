-- name: findByEmail
SELECT id, username, email, password_hash, full_name, created_at, updated_at
FROM users
WHERE email = :email

-- name: findById
SELECT id, username, email, password_hash, full_name, created_at, updated_at
FROM users
WHERE id = :id

-- name: findRolesByUserId
SELECT r.name AS role
FROM user_roles ur
JOIN roles r ON r.id = ur.role_id
WHERE ur.user_id = :userId

-- name: existsByEmail
SELECT EXISTS (SELECT 1 FROM users WHERE email = :email)

-- name: existsByUsername
SELECT EXISTS (SELECT 1 FROM users WHERE username = :username)

-- name: count
SELECT COUNT(*) FROM users

-- name: insert
INSERT INTO users (id, username, email, password_hash, full_name, created_at, updated_at)
VALUES (:id, :username, :email, :passwordHash, :fullName, :createdAt, :updatedAt)

-- name: update
UPDATE users
SET email = :email, password_hash = :passwordHash, full_name = :fullName, updated_at = :updatedAt
WHERE id = :id

-- name: deleteRolesByUserId
DELETE FROM user_roles WHERE user_id = :userId

-- name: insertRole
INSERT INTO user_roles (user_id, role_id)
SELECT :userId, id FROM roles WHERE name = :role
