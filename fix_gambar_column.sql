-- Fix for 'Data too long for column gambar' error
-- Execute this SQL script in your MySQL database (phpMyAdmin, MySQL Workbench, or XAMPP Shell)

USE data_fajar_gold;

-- Change the gambar column from VARCHAR to TEXT to support longer URLs
ALTER TABLE produk MODIFY COLUMN gambar TEXT;

-- Verify the change
DESCRIBE produk;
