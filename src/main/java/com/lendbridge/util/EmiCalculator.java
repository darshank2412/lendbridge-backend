package com.lendbridge.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class EmiCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    /**
     * EMI = P * r * (1+r)^n / ((1+r)^n - 1)
     * where r = annualRate / 12 / 100
     */
    public static BigDecimal calculateEmi(BigDecimal principal,
                                          BigDecimal annualRatePercent,
                                          int tenureMonths) {
        BigDecimal r = annualRatePercent
                .divide(BigDecimal.valueOf(1200), 10, RM);

        if (r.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), SCALE, RM);
        }

        BigDecimal onePlusR = BigDecimal.ONE.add(r);
        BigDecimal pow = onePlusR.pow(tenureMonths, new MathContext(10));
        BigDecimal numerator = principal.multiply(r).multiply(pow);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, SCALE, RM);
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, RM);
    }
}
