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
        // Your hard calculations go here
        // Replace this with your actual calculation logic
        double result = 0;
        for (int i = 0; i < 1000000; i++) {
            result += Math.sin(i);
        }
        return result;
    }
}