package org.joel.kimwanyisacco.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyUtilTest {

    @Test
    void addsTwoPositiveAmounts() {
        assertEquals(
                new BigDecimal("300.00"),
                MoneyUtil.add(new BigDecimal("100.00"), new BigDecimal("200.00"))
        );
    }

    @Test
    void addThrowsOnNullArgument() {
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.add(null, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.add(BigDecimal.TEN, null));
    }

    @Test
    void subtractsAmountsAllowingNegativeResult() {
        assertEquals(
                new BigDecimal("-50.00"),
                MoneyUtil.subtract(new BigDecimal("100.00"), new BigDecimal("150.00"))
        );
    }

    @Test
    void subtractThrowsOnNullArgument() {
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.subtract(null, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.subtract(BigDecimal.TEN, null));
    }

    @Test
    void formatsAmountWithCurrencyCodeAndThousandsSeparator() {
        assertEquals("KES 1,250.00", MoneyUtil.format(new BigDecimal("1250.00")));
    }

    @Test
    void formatThrowsOnNullArgument() {
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.format(null));
    }
}
