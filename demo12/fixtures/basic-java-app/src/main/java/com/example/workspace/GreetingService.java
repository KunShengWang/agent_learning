public class GreetingService {

    public String greetUser(String name) {
        if (name == null || name.isBlank()) {
            return "Hello, friend!";
        }
        return "Hello, " + name.trim() + "!";
    }
}
