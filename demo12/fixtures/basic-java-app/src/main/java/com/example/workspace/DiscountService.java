public class DiscountService {

    public double applyDiscount(double price, double discountRate) {
        if (price < 0) {
            throw new IllegalArgumentException("price must not be negative");
        }
        return price * (1 - discountRate);
    }
}
