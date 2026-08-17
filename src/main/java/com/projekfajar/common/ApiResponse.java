package com.projekfajar.common;

/**
 * Bentuk respons seragam untuk seluruh API: {success, message, data}.
 *
 * Sebelumnya tiap controller menyusun Map sendiri dan ada yang mengembalikan
 * list mentah, sehingga frontend harus menebak bentuknya dengan pola seperti
 * {@code Array.isArray(res.data) ? res.data : res.data?.data}.
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
