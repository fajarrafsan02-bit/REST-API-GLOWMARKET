-- Menghapus tabel counter terjual_produk.
--
-- Tabel ini menyimpan jumlah terjual per produk yang harus dinaikkan manual
-- setiap penjualan, padahal angka yang sama sudah bisa dihitung dari catatan
-- transaksi di tabel produk_terjual. Dua sumber angka untuk hal yang sama
-- berarti keduanya bisa berbeda; sekarang hanya ada satu sumber kebenaran.
DROP TABLE IF EXISTS terjual_produk;
