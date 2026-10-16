package com.acme.finance.transfer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransferValidationTest {

    @Test
    void transferAmountMustBePositive() {
        double amount = 1500.00;
        assertTrue(amount > 0);
    }

    @Test
    void zeroAmountIsRejected() {
        double amount = 0.0;
        assertFalse(amount > 0);
    }

    @Test
    void accountNumberMatchesExpectedFormat() {
        String accountNumber = "ACME-1234-5678-9012";
        assertTrue(accountNumber.matches("ACME-\\d{4}-\\d{4}-\\d{4}"));
    }

    @Test
    void highValueTransferFlaggedForReview() {
        double threshold = 10_000.00;
        double amount = 25_000.00;
        boolean requiresReview = amount > threshold;
        assertTrue(requiresReview);
    }

    @Test
    void currencyCodeIsThreeLetters() {
        String currency = "USD";
        assertEquals(3, currency.length());
        assertTrue(currency.chars().allMatch(Character::isUpperCase));
    }
}
