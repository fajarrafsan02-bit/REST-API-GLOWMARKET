package com.projekfajar.util;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class OtpGenerator {
    public String generate4Digit() {
        return String.format("%04d", new SecureRandom().nextInt(10000));
    }
}
