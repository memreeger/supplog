INSERT INTO public.role (name)
SELECT 'ROLE_USER'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.role
    WHERE name = 'ROLE_USER'
);

INSERT INTO public.role (name)
SELECT 'ROLE_ADMIN'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.role
    WHERE name = 'ROLE_ADMIN'
);