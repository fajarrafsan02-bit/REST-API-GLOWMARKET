package com.projekfajar.util;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class OtpGenerator {
    public String generate4Digit() {
        return String.format("%04d", new SecureRandom().nextInt(10000));
    }

    /** Kode 6 digit untuk verifikasi email. */
    public String generate6Digit() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }
}
