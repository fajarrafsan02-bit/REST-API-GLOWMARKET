-- Akun penampung selisih persediaan.
--
-- Stok bisa berubah di luar pembelian dan penjualan: admin memperbaiki angka
-- yang salah hitung, barang rusak, atau produk baru dibuat dengan stok awal.
-- Tanpa akun ini perubahan tersebut tidak punya lawan jurnal, dan saldo
-- Persediaan di neraca perlahan menyimpang dari stok yang sebenarnya ada.
--
-- Hanya dipakai saat stok berkurang (barang hilang/rusak = kerugian nyata).
-- Stok yang bertambah dikreditkan ke Modal Pemilik, bukan ke sini, supaya
-- penambahan stok tidak pernah tampak sebagai keuntungan.
INSERT INTO akun (kode, nama, tipe, saldo_normal) VALUES
    ('6-500', 'Selisih Persediaan', 'BEBAN', 'DEBIT');
