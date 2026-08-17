-- Nilai uang dipindahkan dari double precision ke numeric(19,2).
--
-- double precision adalah bilangan pecahan biner: 0,1 tidak bisa diwakili
-- persis, sehingga penjumlahan berulang bisa menghasilkan selisih receh.
-- numeric menyimpan angka desimal apa adanya, jadi total rupiah selalu tepat.
-- PostgreSQL mengonversi nilai lama secara otomatis tanpa kehilangan data.

ALTER TABLE produk          ALTER COLUMN harga           TYPE numeric(19,2);
ALTER TABLE ongkir          ALTER COLUMN tarif           TYPE numeric(19,2);

ALTER TABLE payments        ALTER COLUMN amount          TYPE numeric(19,2);
ALTER TABLE payments        ALTER COLUMN ongkir          TYPE numeric(19,2);

ALTER TABLE pesanan         ALTER COLUMN total_harga     TYPE numeric(19,2);
ALTER TABLE pesanan         ALTER COLUMN ongkir          TYPE numeric(19,2);

ALTER TABLE pesanan_item    ALTER COLUMN harga_satuan    TYPE numeric(19,2);
ALTER TABLE pesanan_item    ALTER COLUMN subtotal        TYPE numeric(19,2);

ALTER TABLE produk_terjual  ALTER COLUMN harga_saat_beli TYPE numeric(19,2);
ALTER TABLE produk_terjual  ALTER COLUMN total           TYPE numeric(19,2);
