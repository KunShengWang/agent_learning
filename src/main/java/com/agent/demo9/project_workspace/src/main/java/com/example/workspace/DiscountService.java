package com.agent.demo9.project_workspace.src.main.java.com.example.workspace;

public class DiscountService {

    public double calculateDiscountRate(double totalAmount, boolean vip) {
        if (totalAmount >= 1000) {
            return 0.15;
        }
        if (vip) {
            return 0.10;
        }
        if (totalAmount >= 200) {
            return 0.05;
        }
        return 0.0;
    }

    public double calculatePayableAmount(double totalAmount, boolean vip) {
        double discountRate = calculateDiscountRate(totalAmount, vip);
        return totalAmount * (1 - discountRate);
    }
}
