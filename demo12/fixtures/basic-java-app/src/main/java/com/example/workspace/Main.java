public class Main {

    public static void main(String[] args) {
        GreetingService greetingService = new GreetingService();
        DiscountService discountService = new DiscountService();

        System.out.println(greetingService.greetUser("Alice"));
        System.out.println(discountService.applyDiscount(100.0, 0.2));
    }
}
