package com.projekfajar.exception;

public class ProdukNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ProdukNotFoundException(String message) {
        super(message);
    }
}
