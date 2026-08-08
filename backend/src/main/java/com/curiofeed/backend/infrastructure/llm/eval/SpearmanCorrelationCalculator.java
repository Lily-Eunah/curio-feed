package com.curiofeed.backend.infrastructure.llm.eval;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Calculates Spearman Rank Correlation Coefficient (r_s) between two sets of numerical scores.
 * Formula: r_s = 1 - (6 * sum(d_i^2)) / (n * (n^2 - 1))
 */
public class SpearmanCorrelationCalculator {

    public static double calculate(double[] x, double[] y) {
        if (x == null || y == null || x.length != y.length) {
            throw new IllegalArgumentException("Input arrays must be non-null and of equal length.");
        }
        int n = x.length;
        if (n < 2) {
            return 1.0;
        }

        double[] rankX = computeRanks(x);
        double[] rankY = computeRanks(y);

        double sumD2 = 0.0;
        for (int i = 0; i < n; i++) {
            double diff = rankX[i] - rankY[i];
            sumD2 += diff * diff;
        }

        double denominator = n * ((double) n * n - 1.0);
        if (denominator == 0) {
            return 1.0;
        }

        return 1.0 - (6.0 * sumD2 / denominator);
    }

    private static double[] computeRanks(double[] values) {
        int n = values.length;
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }

        Arrays.sort(indices, Comparator.comparingDouble(i -> values[i]));

        double[] ranks = new double[n];
        int i = 0;
        while (i < n) {
            int j = i;
            while (j < n - 1 && values[indices[j]] == values[indices[j + 1]]) {
                j++;
            }
            double rankSum = 0;
            for (int k = i; k <= j; k++) {
                rankSum += (k + 1);
            }
            double averageRank = rankSum / (j - i + 1);
            for (int k = i; k <= j; k++) {
                ranks[indices[k]] = averageRank;
            }
            i = j + 1;
        }
        return ranks;
    }
}
