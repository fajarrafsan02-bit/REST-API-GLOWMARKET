-- Verifikasi kepemilikan email saat registrasi.
--
-- Kolom "terverifikasi" yang sudah ada BUKAN untuk ini — itu status aktif/nonaktif
-- pelanggan yang dikendalikan admin dan dicek saat login. Verifikasi email memakai
-- kolom sendiri agar keduanya tidak saling menimpa arti.
--
-- NOT NULL wajib disertai DEFAULT: tabel yang sudah berisi data menolak penambahan
-- kolom NOT NULL tanpa nilai default.
ALTER TABLE users ADD COLUMN email_terverifikasi boolean NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN email_terverifikasi_at timestamp(6);

-- Pengguna dan admin yang sudah terdaftar sebelum fitur ini dianggap terverifikasi,
-- supaya mereka tidak mendadak terkunci dari checkout.
UPDATE users SET email_terverifikasi = true, email_terverifikasi_at = now();

CREATE TABLE email_verification (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id),
    kode varchar(6) NOT NULL,
    percobaan integer NOT NULL DEFAULT 0,
    used boolean NOT NULL DEFAULT false,
    expired_at timestamp(6) NOT NULL,
    created_at timestamp(6) NOT NULL
);

CREATE INDEX idx_email_verification_user ON email_verification(user_id);
