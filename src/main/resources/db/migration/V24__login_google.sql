-- Login dengan akun Google. google_id (sub token Google) unik per akun,
-- boleh kosong untuk user yang mendaftar manual (email + password).
ALTER TABLE public.users
    ADD COLUMN google_id character varying(255);

ALTER TABLE public.users
    ADD CONSTRAINT users_google_id_key UNIQUE (google_id);

-- Password boleh kosong untuk akun yang hanya pernah login lewat Google.
ALTER TABLE public.users
    ALTER COLUMN password DROP NOT NULL;
