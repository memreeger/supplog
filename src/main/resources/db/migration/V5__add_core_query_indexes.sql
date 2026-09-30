CREATE INDEX idx_users_active_created
    ON public.users (is_deleted, created_at DESC);

CREATE INDEX idx_users_roles_role_user
    ON public.users_roles (role_id, user_id);

CREATE INDEX idx_supplements_user_active_created
    ON public.supplements (inserted_by_user_id, is_deleted, created_at DESC);

CREATE INDEX idx_routines_user_active_created
    ON public.routines (user_id, is_deleted, created_at DESC);

CREATE INDEX idx_routines_supplement_active
    ON public.routines (supplement_id, is_deleted);

CREATE INDEX idx_executions_date_status
    ON public.routine_executions (scheduled_date, status);

CREATE INDEX idx_executions_scheduled_at
    ON public.routine_executions (scheduled_at DESC);

CREATE INDEX idx_support_supported_status_requested
    ON public.support_relationships (supported_user_id, status, requested_at DESC);

CREATE INDEX idx_support_supporter_status_requested
    ON public.support_relationships (supporter_user_id, status, requested_at DESC);

CREATE INDEX idx_support_permissions_routine
    ON public.support_routine_permissions (routine_id);
