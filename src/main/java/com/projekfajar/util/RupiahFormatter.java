package com.projekfajar.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Format rupiah tunggal untuk email dan chatbot, supaya angka yang dilihat
 * pelanggan tidak berbeda-beda tergantung dari saluran mana ia datang.
 */
public final class RupiahFormatter {

    private RupiahFormatter() {
    }

    public static String format(BigDecimal nilai) {
        BigDecimal angka = nilai != null ? nilai : BigDecimal.ZERO;

        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        format.setMaximumFractionDigits(0);

        return format.format(angka);
    }
}
