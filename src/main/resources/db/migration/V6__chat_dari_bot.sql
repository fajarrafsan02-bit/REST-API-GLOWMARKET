-- Menandai pesan chat yang dijawab otomatis oleh bot.
--
-- Bot membalas atas nama admin agar riwayat percakapan tetap utuh, tetapi
-- pelanggan berhak tahu ia sedang dijawab mesin — dan admin perlu langsung
-- melihat mana yang sudah dijawab bot sebelum ia menyusul membalas.
--
-- NOT NULL wajib berpasangan dengan DEFAULT: tabel ini sudah berisi data.
ALTER TABLE chat_messages ADD COLUMN dari_bot boolean NOT NULL DEFAULT false;
