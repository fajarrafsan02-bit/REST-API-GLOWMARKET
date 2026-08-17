-- Pembelian stok bisa tunai (kas keluar sekarang) atau kredit (utang ke pemasok).
--
-- Baris lama semuanya tunai: kas sudah berkurang saat dicatat, jadi dilunasi = true.
-- Pembelian kredit baru dimulai dengan dilunasi = false sampai admin menekan Lunasi.

ALTER TABLE pembelian ADD COLUMN metode varchar(10) NOT NULL DEFAULT 'TUNAI';
ALTER TABLE pembelian ADD COLUMN dilunasi boolean NOT NULL DEFAULT true;
ALTER TABLE pembelian ADD COLUMN dilunasi_at timestamp(6);
