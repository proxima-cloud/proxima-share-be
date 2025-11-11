-- Update constraint to user_roles table for autoremove in many to many
ALTER TABLE user_roles
DROP CONSTRAINT fkhfh9dx7w3ubf1co1vdev94g3f;

ALTER TABLE user_roles
ADD CONSTRAINT fkhfh9dx7w3ubf1co1vdev94g3f
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;