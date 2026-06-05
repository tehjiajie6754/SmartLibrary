import java.util.ArrayList;
import java.util.List;

/**
 * A staging area for a user's borrow session.
 * Books are added/removed here before the final checkout commits them.
 */
public class Cart {

    private final String       userId;
    private final List<Integer> items;  // ISBNs

    public Cart(String userId) {
        this.userId = userId;
        this.items  = new ArrayList<>();
    }

    /** Returns false if the ISBN is already in the cart. */
    public boolean add(int isbn) {
        if (items.contains(isbn)) return false;
        items.add(isbn);
        return true;
    }

    /** Returns false if the ISBN was not in the cart. */
    public boolean remove(int isbn) {
        return items.remove(Integer.valueOf(isbn));
    }

    public List<Integer> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
    }

    public String getUserId() {
        return userId;
    }
}
