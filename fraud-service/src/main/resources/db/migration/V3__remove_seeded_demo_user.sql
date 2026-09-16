-- V2 seeded a demo account directly in a migration, which put a well-known
-- credential into every environment. Demo data is now created by
-- DemoDataSeeder only when the "demo" Spring profile is active.
-- This migration removes exactly the row V2 inserted (and anything it scored).
DELETE FROM predictions
 WHERE user_id IN (SELECT id FROM users WHERE username = 'demo' AND email = 'demo@example.com');

DELETE FROM users
 WHERE username = 'demo' AND email = 'demo@example.com';
