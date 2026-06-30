package com.example.workspace;

public class Main {

    public static void main(String[] args) {
        GreetingService service = new GreetingService();
        System.out.println(service.greetUser("Alice"));
    }
}
