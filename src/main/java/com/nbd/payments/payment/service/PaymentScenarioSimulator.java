
package com.nbd.payments.payment.service;

import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.Map;

@Component
public class PaymentScenarioSimulator {

    private static final Map<String, String> SCENARIOS = Map.ofEntries(
        
        Map.entry("4111111111111111", "SUCCESS"),
        Map.entry("4000000000000002", "INSUFFICIENT_FUNDS"),
        Map.entry("4000000000000010", "AUTHENTICATION_FAILED"),
        Map.entry("4000000000000028", "LIMIT_EXCEEDED"),
        Map.entry("4000000000000036", "FRAUD_SUSPECTED"),
        Map.entry("4000000000000044", "ACCOUNT_RESTRICTED"),
        Map.entry("4000000000000051", "TRANSACTION_NOT_ALLOWED"),
        Map.entry("4000000000000069", "BLACKLISTED_BIN"),
        Map.entry("4000000000000077", "SERVER_UNAVAILABLE"),
        Map.entry("4000000000000085", "NETWORK_TIMEOUT"),
        Map.entry("4000000000000093", "MERCHANT_CONFIGURATION_ERROR")
    );

    public String evaluate(
            String cardNumber,
            String expiry,
            String cvv
    ) {
        if (cardNumber == null ||
                !cardNumber.matches("\\d{16}")) {
            return "INVALID_CARD";
        }

        if (cvv == null || !cvv.matches("\\d{3}")) {
            return "INVALID_CARD";
        }

        if (expiry == null ||
                !expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            return "INVALID_CARD";
        }

        int month = Integer.parseInt(expiry.substring(0, 2));
        int year = 2000 + Integer.parseInt(expiry.substring(3));

        if (YearMonth.of(year, month).isBefore(YearMonth.now())) {
            return "EXPIRED_CARD";
        }

        return SCENARIOS.getOrDefault(
                cardNumber,
                "INVALID_CARD"
        );
    }

    public String messageFor(String result) {
        return switch (result) {
            
            case "SUCCESS" -> 
                    "Payment successful";

            case "INSUFFICIENT_FUNDS" -> 
                    "You do not have enough balance to make this transaction.";

            case "AUTHENTICATION_FAILED" ->
                    "Card authentication failed. Please retry.";

            case "LIMIT_EXCEEDED" -> 
                    "Your card transaction limit has been exceeded";

            case "FRAUD_SUSPECTED" ->
                    "Transaction declined by the bank";

            case "ACCOUNT_RESTRICTED" -> 
                    "Card is restricted";

            case "TRANSACTION_NOT_ALLOWED" ->
                    "Card is not enabled for this transaction";

            case "BLACKLISTED_BIN" -> 
                    "Card is not supported";

            case "SERVER_UNAVAILABLE" ->
                    "Payment service temporarily unavailable";

            case "NETWORK_TIMEOUT" -> 
                    "Payment request timed out";

            case "MERCHANT_CONFIGURATION_ERROR" ->
                    "Merchant is not configured for payments";

            case "EXPIRED_CARD" -> 
                    "Card has expired";

            default -> 
                    "Incorrect card details";
        };
    }
}
