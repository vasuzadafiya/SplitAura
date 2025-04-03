package com.example.splitaura;

import com.example.splitaura.models.OweRelationship;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DebtSettlementCalculator {
    /**
     * Calculates the minimal set of transactions needed to settle debts among users.
     *
     * @param netBalances A map with user IDs as keys and net balances as values.
     *                    (Positive values indicate credit; negative values indicate debt.)
     * @return A list of OweRelationship transactions representing the simplified settlements.
     */
    public static List<OweRelationship> calculateMinimalOwe(Map<String, Double> netBalances) {
        List<OweRelationship> transactions = new ArrayList<>();

        // Separate users into creditors and debtors.
        List<SimpleEntry<String, Double>> creditors = new ArrayList<>();
        List<SimpleEntry<String, Double>> debtors = new ArrayList<>();

        for (Map.Entry<String, Double> entry : netBalances.entrySet()) {
            double balance = entry.getValue();
            if (balance > 0.0) {
                creditors.add(new SimpleEntry<>(entry.getKey(), balance));
            } else if (balance < 0.0) {
                // Store absolute value for ease of calculation.
                debtors.add(new SimpleEntry<>(entry.getKey(), -balance));
            }
        }

        int i = 0;
        int j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            SimpleEntry<String, Double> debtor = debtors.get(i);
            SimpleEntry<String, Double> creditor = creditors.get(j);
            double settledAmount = Math.min(debtor.getValue(), creditor.getValue());

            // Add a transaction only if the settled amount is greater than a negligible threshold.
            if (settledAmount > 1e-6) {
                transactions.add(new OweRelationship(debtor.getKey(), creditor.getKey(), settledAmount));
            }

            // Update remaining amounts.
            debtor.setValue(debtor.getValue() - settledAmount);
            creditor.setValue(creditor.getValue() - settledAmount);

            // Move to the next debtor or creditor if fully settled.
            if (Math.abs(debtor.getValue()) < 1e-6) {
                i++;
            }
            if (Math.abs(creditor.getValue()) < 1e-6) {
                j++;
            }
        }

        return transactions;
    }
}