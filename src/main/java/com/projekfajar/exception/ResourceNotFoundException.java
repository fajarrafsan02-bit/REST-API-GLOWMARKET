package com.projekfajar.exception;

/**
 * Data yang diminta tidak ada. Dipetakan ke HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
