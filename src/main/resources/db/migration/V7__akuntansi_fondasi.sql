-- Fondasi pembukuan berpasangan: harga modal, bagan akun, dan jurnal.
--
-- Sebelum ini sistem hanya tahu omzet, bukan laba, karena produk tidak menyimpan
-- harga modal sama sekali. Kolom harga_modal pada pesanan_item adalah salinan saat
-- barang terjual — sama seperti harga_satuan — supaya laba pesanan lama tidak ikut
-- berubah ketika harga modal produk diperbarui.
--
-- NOT NULL selalu berpasangan dengan DEFAULT: kedua tabel sudah berisi data.
ALTER TABLE produk ADD COLUMN harga_modal numeric(19,2) NOT NULL DEFAULT 0;
ALTER TABLE pesanan_item ADD COLUMN harga_modal numeric(19,2) NOT NULL DEFAULT 0;

CREATE TABLE akun (
    id bigserial PRIMARY KEY,
    kode varchar(10) NOT NULL UNIQUE,
    nama varchar(100) NOT NULL,
    tipe varchar(20) NOT NULL,
    saldo_normal varchar(10) NOT NULL,
    aktif boolean NOT NULL DEFAULT true
);

CREATE TABLE jurnal (
    id bigserial PRIMARY KEY,
    nomor varchar(30) NOT NULL UNIQUE,
    tanggal date NOT NULL,
    keterangan varchar(255) NOT NULL,
    sumber varchar(20) NOT NULL,
    referensi_id bigint,
    created_at timestamp(6) NOT NULL
);

CREATE INDEX idx_jurnal_tanggal ON jurnal(tanggal);
CREATE INDEX idx_jurnal_sumber ON jurnal(sumber, referensi_id);

CREATE TABLE jurnal_detail (
    id bigserial PRIMARY KEY,
    jurnal_id bigint NOT NULL REFERENCES jurnal(id) ON DELETE CASCADE,
    akun_id bigint NOT NULL REFERENCES akun(id),
    debit numeric(19,2) NOT NULL DEFAULT 0,
    kredit numeric(19,2) NOT NULL DEFAULT 0,
    keterangan varchar(255)
);

CREATE INDEX idx_jurnal_detail_jurnal ON jurnal_detail(jurnal_id);
CREATE INDEX idx_jurnal_detail_akun ON jurnal_detail(akun_id);

-- Bagan akun standar. Diletakkan di migrasi (bukan DataInitializer) agar database
-- lama dan database baru punya akun yang persis sama — kode akun dipakai langsung
-- oleh pencatatan otomatis, jadi tidak boleh berbeda antar lingkungan.
INSERT INTO akun (kode, nama, tipe, saldo_normal) VALUES
    ('1-100', 'Kas & Bank',              'ASET',       'DEBIT'),
    ('1-200', 'Persediaan Barang',       'ASET',       'DEBIT'),
    ('2-100', 'Utang Usaha',             'LIABILITAS', 'KREDIT'),
    ('3-100', 'Modal Pemilik',           'EKUITAS',    'KREDIT'),
    ('4-100', 'Pendapatan Penjualan',    'PENDAPATAN', 'KREDIT'),
    ('4-200', 'Pendapatan Ongkir',       'PENDAPATAN', 'KREDIT'),
    ('5-100', 'Harga Pokok Penjualan',   'HPP',        'DEBIT'),
    ('6-100', 'Beban Gaji',              'BEBAN',      'DEBIT'),
    ('6-200', 'Beban Sewa',              'BEBAN',      'DEBIT'),
    ('6-300', 'Beban Listrik & Air',     'BEBAN',      'DEBIT'),
    ('6-400', 'Beban Transport',         'BEBAN',      'DEBIT'),
    ('6-900', 'Beban Lain-lain',         'BEBAN',      'DEBIT');
