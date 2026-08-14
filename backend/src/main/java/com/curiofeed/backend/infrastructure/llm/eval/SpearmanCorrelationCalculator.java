package com.curiofeed.backend.infrastructure.llm.eval;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Calculates Spearman Rank Correlation Coefficient (r_s) between two sets of numerical scores.
 * Uses Pearson correlation coefficient on fractional rank vectors to correctly handle ties.
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

        double meanX = 0.0, meanY = 0.0;
        for (int i = 0; i < n; i++) {
            meanX += rankX[i];
            meanY += rankY[i];
        }
        meanX /= n;
        meanY /= n;

        double num = 0.0, denX = 0.0, denY = 0.0;
        for (int i = 0; i < n; i++) {
            double dx = rankX[i] - meanX;
            double dy = rankY[i] - meanY;
            num += dx * dy;
            denX += dx * dx;
            denY += dy * dy;
        }

        double denominator = Math.sqrt(denX * denY);
        if (denominator == 0.0) {
            return 0.0;
        }

        return num / denominator;
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
