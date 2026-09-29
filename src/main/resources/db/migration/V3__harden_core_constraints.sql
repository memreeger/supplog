ALTER TABLE public.role
    ALTER COLUMN name SET NOT NULL;

ALTER TABLE public.role
    ADD CONSTRAINT uk_role_name UNIQUE (name);


ALTER TABLE public.users
    ALTER COLUMN birth_date SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN e_mail SET NOT NULL,
    ALTER COLUMN first_name SET NOT NULL,
    ALTER COLUMN is_deleted SET NOT NULL,
    ALTER COLUMN last_name SET NOT NULL,
    ALTER COLUMN password SET NOT NULL,
    ALTER COLUMN score SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL,
    ALTER COLUMN user_name SET NOT NULL;


ALTER TABLE public.support_relationships
    ADD CONSTRAINT chk_support_relationship_not_self
    CHECK (supported_user_id <> supporter_user_id);