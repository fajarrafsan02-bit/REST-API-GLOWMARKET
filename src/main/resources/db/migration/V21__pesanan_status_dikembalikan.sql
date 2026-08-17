-- Status pesanan DIKEMBALIKAN dipakai saat admin menyetujui pengembalian.
-- Constraint lama hanya mengenal PENDING/DIKEMAS/DIKIRIM/SELESAI/DIBATALKAN,
-- jadi UPDATE ke DIKEMBALIKAN ditolak PostgreSQL.

ALTER TABLE pesanan DROP CONSTRAINT pesanan_status_check;

ALTER TABLE pesanan ADD CONSTRAINT pesanan_status_check CHECK (
    status IN ('PENDING', 'DIKEMAS', 'DIKIRIM', 'SELESAI', 'DIBATALKAN', 'DIKEMBALIKAN')
);
