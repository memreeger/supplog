ALTER TABLE public.routines
    ADD COLUMN missed_grace_period varchar(30) NOT NULL DEFAULT 'THIRTY_MINUTES';

ALTER TABLE public.routines
    ADD CONSTRAINT routines_missed_grace_period_check CHECK (
        missed_grace_period IN (
            'THIRTY_MINUTES',
            'SIXTY_MINUTES',
            'NINETY_MINUTES',
            'TWO_HOURS'
        )
    );

ALTER TABLE public.routine_executions
    ADD COLUMN missed_at timestamp(6) with time zone;

UPDATE public.routine_executions
SET missed_at = scheduled_at + INTERVAL '30 minutes'
WHERE missed_at IS NULL;

ALTER TABLE public.routine_executions
    ALTER COLUMN missed_at SET NOT NULL;

CREATE INDEX idx_executions_pending_missed_at
    ON public.routine_executions (missed_at)
    WHERE status = 'PENDING';
