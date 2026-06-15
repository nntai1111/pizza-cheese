-- name: findByEmail
SELECT id, email, password, name, created_at, updated_at
FROM users
WHERE email = :email

-- name: findById
SELECT id, email, password, name, created_at, updated_at
FROM users
WHERE id = :id

-- name: findRolesByUserId
SELECT role
FROM user_roles
WHERE user_id = :userId

-- name: existsByEmail
SELECT EXISTS (SELECT 1 FROM users WHERE email = :email)

-- name: count
SELECT COUNT(*) FROM users

-- name: insert
INSERT INTO users (email, password, name, created_at, updated_at)
VALUES (:email, :password, :name, :createdAt, :updatedAt)

-- name: update
UPDATE users
SET email = :email, password = :password, name = :name, updated_at = :updatedAt
WHERE id = :id

-- name: deleteRolesByUserId
DELETE FROM user_roles WHERE user_id = :userId

-- name: insertRole
INSERT INTO user_roles (user_id, role) VALUES (:userId, :role)
