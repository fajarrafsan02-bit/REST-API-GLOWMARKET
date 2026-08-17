ALTER TABLE payments ADD COLUMN ongkir_sumber varchar(30);
ALTER TABLE pesanan  ADD COLUMN ongkir_sumber varchar(30);

-- Cache hasil resolusi alamat -> ID kecamatan RajaOngkir, diisi lazy saat
-- pertama kali dipakai menghitung ongkir (lihat AlamatOngkirResolver), bukan
-- saat alamat disimpan -- supaya tidak boros kuota harian API untuk alamat
-- yang tidak pernah dipakai belanja.
ALTER TABLE alamat ADD COLUMN rajaongkir_destination_id varchar(20);
