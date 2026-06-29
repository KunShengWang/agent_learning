package com.agent.demo9.project_workspace.src.main.java.com.example.workspace;

public class GreetingService {

    public String greetUser(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Hello, friend!";
        }
        return "Hello, " + name.trim() + "!";
    }
}
