package com.projekfajar.exception;

/**
 * Aturan bisnis yang dilanggar oleh permintaan pengguna — misalnya stok tidak
 * cukup atau keranjang kosong.
 *
 * Dipetakan ke HTTP 400 supaya frontend bisa membedakannya dari kegagalan
 * server (500). Sebelumnya semua kasus ini memakai RuntimeException biasa
 * sehingga statusnya tidak konsisten.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
