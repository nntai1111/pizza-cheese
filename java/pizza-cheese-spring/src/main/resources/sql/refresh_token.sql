-- name: findByToken
SELECT id, token, user_id, expires_at, created_at
FROM refresh_tokens
WHERE token = :token

-- name: insert
INSERT INTO refresh_tokens (token, user_id, expires_at, created_at)
VALUES (:token, :userId, :expiresAt, :createdAt)

-- name: deleteById
DELETE FROM refresh_tokens WHERE id = :id

-- name: deleteByUserId
DELETE FROM refresh_tokens WHERE user_id = :userId
