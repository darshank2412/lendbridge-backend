package com.lendbridge.service;

import com.lendbridge.util.EmiCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EMI Calculator Unit Tests")
class EmiCalculatorTest {

    @Test
    @DisplayName("Standard EMI: ₹1,00,000 @ 12% for 12 months ≈ ₹8,884.88")
    void standardEmi() {
        BigDecimal emi = EmiCalculator.calculateEmi(
                new BigDecimal("100000"), new BigDecimal("12"), 12);
        assertThat(emi.doubleValue()).isCloseTo(8884.88, within(0.02));
    }

    @Test
    @DisplayName("Total repayment must always exceed principal")
    void totalRepaymentExceedsPrincipal() {
        BigDecimal emi   = EmiCalculator.calculateEmi(new BigDecimal("100000"), new BigDecimal("12"), 12);
        BigDecimal total = emi.multiply(BigDecimal.valueOf(12));
        assertThat(total).isGreaterThan(new BigDecimal("100000"));
    }

    @Test
    @DisplayName("Zero interest rate returns simple division")
    void zeroInterestRate() {
        BigDecimal emi = EmiCalculator.calculateEmi(new BigDecimal("120000"), BigDecimal.ZERO, 12);
        assertThat(emi.setScale(0, RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("Higher interest rate produces higher EMI")
    void higherRateHigherEmi() {
        BigDecimal lowEmi  = EmiCalculator.calculateEmi(new BigDecimal("100000"), new BigDecimal("8"),  12);
        BigDecimal highEmi = EmiCalculator.calculateEmi(new BigDecimal("100000"), new BigDecimal("24"), 12);
        assertThat(highEmi).isGreaterThan(lowEmi);
    }

    @Test
    @DisplayName("Longer tenure produces lower EMI")
    void longerTenureLowerEmi() {
        BigDecimal shortEmi = EmiCalculator.calculateEmi(new BigDecimal("100000"), new BigDecimal("12"), 12);
        BigDecimal longEmi  = EmiCalculator.calculateEmi(new BigDecimal("100000"), new BigDecimal("12"), 60);
        assertThat(longEmi).isLessThan(shortEmi);
    }

    @ParameterizedTest(name = "principal={0}, rate={1}%, tenure={2}m → EMI > 0")
    @CsvSource({
        "10000,  8, 6",
        "500000, 24, 60",
        "50000,  12, 12",
        "1000,   10, 6",
        "5000000, 20, 48"
    })
    @DisplayName("EMI is always positive for valid inputs")
    void emiAlwaysPositive(double principal, double rate, int tenure) {
        BigDecimal emi = EmiCalculator.calculateEmi(
                BigDecimal.valueOf(principal), BigDecimal.valueOf(rate), tenure);
        assertThat(emi).isGreaterThan(BigDecimal.ZERO);
    }
}
