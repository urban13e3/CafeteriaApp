package cafeteriaapp;

public class CartItem {

    private int menuItemId;
    private String name;
    private double price;
    private int quantity;

    public CartItem(int menuItemId, String name, double price, int quantity) {
        this.menuItemId = menuItemId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public int getMenuItemId() { return menuItemId; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getSubtotal() {
        return price * quantity;
    }
}