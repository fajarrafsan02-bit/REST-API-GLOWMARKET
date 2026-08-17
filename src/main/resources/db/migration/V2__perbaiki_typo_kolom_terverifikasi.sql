-- Memperbaiki salah ketik nama kolom: "terferifikasi" -> "terverifikasi".
-- RENAME COLUMN mempertahankan seluruh data yang sudah ada.
ALTER TABLE users RENAME COLUMN terferifikasi TO terverifikasi;
