package cafeteriaapp;

import java.util.ArrayList;
import java.util.List;

public class Cart {

    private static final Cart instance = new Cart();
    private final List<CartItem> items = new ArrayList<>();

    private Cart() {}

    public static Cart getInstance() {
        return instance;
    }

    public void addItem(int menuItemId, String name, double price, int quantity) {
    for (CartItem item : items) {
        if (item.getMenuItemId() == menuItemId) {
            item.setQuantity(item.getQuantity() + quantity);
            return;
        }
    }
        items.add(new CartItem(menuItemId, name, price, quantity));
    }

    public void removeItem(int menuItemId) {
        items.removeIf(item -> item.getMenuItemId() == menuItemId);
    }

    public void updateQuantity(int menuItemId, int quantity) {
        if (quantity <= 0) {
            removeItem(menuItemId);
            return;
        }
        for (CartItem item : items) {
            if (item.getMenuItemId() == menuItemId) {
                item.setQuantity(quantity);
                return;
            }
        }
    }

    public List<CartItem> getItems() {
        return items;
    }

    public double getTotal() {
        return items.stream().mapToDouble(CartItem::getSubtotal).sum();
    }

    public int getItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}