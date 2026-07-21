package org.joel.kimwanyisacco.common.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public final class MoneyUtil {

    private static final String CURRENCY_CODE = "KES";
    private static final String DECIMAL_PATTERN = "#,##0.00";

    private MoneyUtil() {
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        requireNonNull(a, b);
        return a.add(b);
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        requireNonNull(a, b);
        return a.subtract(b);
    }

    public static String format(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        DecimalFormat decimalFormat = new DecimalFormat(DECIMAL_PATTERN);
        return CURRENCY_CODE + " " + decimalFormat.format(amount);
    }

    private static void requireNonNull(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }
    }
}
