-- Demo account so `docker compose up` gives a system you can log into immediately.
-- Credentials: demo / demo123   (hash is {bcrypt}, see SecurityConfig#passwordEncoder)
INSERT INTO users (username, password_hash, first_name, last_name, email, created_at)
VALUES ('demo',
        '{bcrypt}$2a$10$jsU1MU1WwLvJtnGZg7641e4dBNeVkQ.Qqhs6oGGg3HIVMy8pQ7Rna',
        'Demo', 'User', 'demo@example.com', CURRENT_TIMESTAMP);
