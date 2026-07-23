-- Allow the approval workflow's PENDING state and future member states.
-- Existing values (ACTIVE, INACTIVE, SUSPENDED) are preserved.
ALTER TABLE members MODIFY COLUMN status VARCHAR(30) NOT NULL;
