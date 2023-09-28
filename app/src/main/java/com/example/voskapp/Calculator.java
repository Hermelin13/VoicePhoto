package com.example.voskapp;

public class Calculator {
    private boolean isCalculating = false;
    private CalculationCallback callback;

    public void startContinuousCalculations(CalculationCallback callback) {
        this.callback = callback;
        if (!isCalculating) {
            isCalculating = true;
            new Thread(() -> {
                while (isCalculating) {
                    // Perform continuous calculations here
                    double result = performHardCalculations();
                    callback.onCalculationCompleted(result);
                }
            }).start();
        }
    }

    public void stopContinuousCalculations() {
        isCalculating = false;
    }

    private double performHardCalculations() {
        int i = 0, r = 1;

        while (r > 0) {
            r = factorial (i);
        }
        return r;
    }

    public static int factorial(int n) {
        if (n < 0) {  return -1;  }
        if (n == 0 || n == 1) { return 1; }
        int r = 1;
        try {
            for (int i = 1; i <= n; ++i) {
                r = Math.multiplyExact (r, i);
            }
        } catch (ArithmeticException ex) {
            return -2;
        }
        return r;
    }
}