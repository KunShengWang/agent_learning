package com.agent.demo8.project_workspace.src.main.java.com.example.workspace;

public class Main {

    public static void main(String[] args) {
        GreetingService greetingService = new GreetingService();
        DiscountService discountService = new DiscountService();

        System.out.println("Theme: " + AppConfig.DEFAULT_THEME);
        System.out.println(greetingService.greetUser("Java learner"));
        System.out.println("Payable amount: " + discountService.calculatePayableAmount(320, true));
    }
}
