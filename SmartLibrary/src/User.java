import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a registered library member.
 *
 * borrowedKeys  – "isbn:copyId" for copies currently on loan (max 2)
 * genreCounts   – how many books per genre this user has ever borrowed
 * readHistory   – every ISBN this user has ever borrowed (persists after return,
 *                 used to filter out already-read books from recommendations)
 */
public class User {

    static final int MAX_BORROW = 2;

    String               userId;
    String               name;
    List<String>         borrowedKeys;
    Map<String, Integer> genreCounts;
    Set<Integer>         readHistory;

    public User(String userId, String name) {
        this.userId       = userId;
        this.name         = name;
        this.borrowedKeys = new ArrayList<>();
        this.genreCounts  = new HashMap<>();
        this.readHistory  = new HashSet<>();
    }

    public boolean canBorrow() {
        return borrowedKeys.size() < MAX_BORROW;
    }

    public int borrowedCount() {
        return borrowedKeys.size();
    }

    /**
     * Returns the genre this user has borrowed the most,
     * or null if they have no borrow history.
     */
    public String getTopGenre() {
        if (genreCounts.isEmpty()) return null;
        String top = null;
        int    max = 0;
        for (Map.Entry<String, Integer> e : genreCounts.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                top = e.getKey();
            }
        }
        return top;
    }
}
