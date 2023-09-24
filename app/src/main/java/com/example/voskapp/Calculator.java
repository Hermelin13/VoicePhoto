package com.example.voskapp;

public class Calculator {
    private volatile boolean isCalculating = false; // Flag to control calculations
    private CalculationCallback callback;

    public void startContinuousCalculations(CalculationCallback callback) {
        this.callback = callback;
        if (!isCalculating) {
            isCalculating = true;
            new Thread(this::continuousCalculations).start();
        }
    }

    public void stopContinuousCalculations() {
        isCalculating = false;
    }

    private void continuousCalculations() {
        while (isCalculating) {
            double result = performHardCalculations();
            callback.onCalculationCompleted(result);

            // Sleep for a while to control the calculation rate
            try {
                Thread.sleep(1000); // Sleep for 1 second (adjust as needed)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private double performHardCalculations() {
        // Your continuous calculations go here
        // Replace this with your actual calculation logic
        double result = 0;
        for (int i = 0; i < 1000000; i++) {
            result += Math.sin(i);
        }
        return result;
    }
}