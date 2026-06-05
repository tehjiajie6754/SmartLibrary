import java.util.ArrayList;
import java.util.List;

/**
 * Represents a registered library member.
 * borrowedKeys stores "isbn:copyId" strings so we can locate
 * exactly which physical copy belongs to this user.
 */
public class User {

    static final int MAX_BORROW = 2;

    String       userId;
    String       name;
    List<String> borrowedKeys;  // format: "isbn:copyId"

    public User(String userId, String name) {
        this.userId       = userId;
        this.name         = name;
        this.borrowedKeys = new ArrayList<>();
    }

    public boolean canBorrow() {
        return borrowedKeys.size() < MAX_BORROW;
    }

    public int borrowedCount() {
        return borrowedKeys.size();
    }
}
