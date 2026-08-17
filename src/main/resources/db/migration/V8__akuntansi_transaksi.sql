-- Sisi pengeluaran: pembelian stok ke pemasok dan biaya operasional.
--
-- Sebelum ini uang keluar tidak tercatat di mana pun, sehingga laporan hanya bisa
-- menunjukkan omzet. Dua tabel di bawah melengkapi sisi kredit kas: pembelian
-- menambah persediaan, beban langsung mengurangi laba.
--
-- Baris tidak pernah dihapus, hanya ditandai dibatalkan, karena jurnal yang sudah
-- tercatat menunjuk ke sini lewat referensi_id — riwayat pembukuan harus tetap
-- bisa ditelusuri balik ke transaksi asalnya.

CREATE TABLE pembelian (
    id bigserial PRIMARY KEY,
    nomor varchar(30) NOT NULL UNIQUE,
    tanggal date NOT NULL,
    pemasok varchar(150),
    total numeric(19,2) NOT NULL,
    catatan varchar(255),
    dibatalkan boolean NOT NULL DEFAULT false,
    dibatalkan_at timestamp(6),
    created_at timestamp(6) NOT NULL
);

CREATE INDEX idx_pembelian_tanggal ON pembelian(tanggal);

CREATE TABLE pembelian_item (
    id bigserial PRIMARY KEY,
    pembelian_id bigint NOT NULL REFERENCES pembelian(id) ON DELETE CASCADE,
    produk_id bigint NOT NULL REFERENCES produk(id),
    nama_produk varchar(255),
    qty integer NOT NULL,
    harga_beli numeric(19,2) NOT NULL,
    subtotal numeric(19,2) NOT NULL
);

CREATE INDEX idx_pembelian_item_pembelian ON pembelian_item(pembelian_id);
CREATE INDEX idx_pembelian_item_produk ON pembelian_item(produk_id);

CREATE TABLE beban (
    id bigserial PRIMARY KEY,
    tanggal date NOT NULL,
    akun_id bigint NOT NULL REFERENCES akun(id),
    keterangan varchar(255) NOT NULL,
    jumlah numeric(19,2) NOT NULL,
    dibatalkan boolean NOT NULL DEFAULT false,
    dibatalkan_at timestamp(6),
    created_at timestamp(6) NOT NULL
);

CREATE INDEX idx_beban_tanggal ON beban(tanggal);
CREATE INDEX idx_beban_akun ON beban(akun_id);
