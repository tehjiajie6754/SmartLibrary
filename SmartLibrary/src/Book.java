import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * ============================================================
 * Entity Class: Book  (also serves as BST node)
 * ============================================================
 * Each Book represents one ISBN (title + metadata).
 * Multiple physical copies are stored in the `copies` list.
 * Users waiting for an unavailable book queue in `waitlist`.
 *
 * BST ordering is still by ISBN:
 *        [ISBN 200]
 *        /        \
 *   [ISBN 100]  [ISBN 300]
 * ============================================================
 */
public class Book {

    // --- Book metadata ---
    int    isbn;
    String title;
    String author;
    String location;
    String genre;
    int    borrowCount;   // total times this book has ever been borrowed

    // --- Member ratings: userId → stars (1-5) ---
    Map<String, Integer> ratings;

    // --- Multiple physical copies ---
    List<Copy>    copies;

    // --- Reservation queue (userIds waiting) ---
    Queue<String> waitlist;

    // --- BST pointers ---
    Book left;
    Book right;

    public Book(int isbn, String title, String author, String location, String genre) {
        this.isbn        = isbn;
        this.title       = title;
        this.author      = author;
        this.location    = location;
        this.genre       = genre;
        this.borrowCount = 0;
        this.ratings     = new HashMap<>();
        this.copies      = new ArrayList<>();
        this.waitlist    = new LinkedList<>();
        this.left        = null;
        this.right       = null;
    }

    public double averageRating() {
        if (ratings.isEmpty()) return 0;
        int sum = 0;
        for (int s : ratings.values()) sum += s;
        return (double) sum / ratings.size();
    }

    public int ratingCount() { return ratings.size(); }

    public void addCopy(Copy copy) {
        copies.add(copy);
    }

    public int totalCopies() {
        return copies.size();
    }

    public int availableCount() {
        int n = 0;
        for (Copy c : copies) if (c.isAvailable()) n++;
        return n;
    }

    /** Returns the first available Copy, or null if all are borrowed. */
    public Copy getAvailableCopy() {
        for (Copy c : copies) if (c.isAvailable()) return c;
        return null;
    }

    /** Returns the Copy currently borrowed by the given userId, or null. */
    public Copy getCopyBorrowedBy(String userId) {
        for (Copy c : copies) {
            if (userId.equals(c.borrowerId)) return c;
        }
        return null;
    }

    @Override
    public String toString() {
        String t = title.length()  > 36 ? title.substring(0, 35)  + "~" : title;
        String a = author.length() > 20 ? author.substring(0, 19) + "~" : author;
        String ratingStr = ratingCount() == 0
                ? "No ratings"
                : String.format("%.1f/5 (%d)", averageRating(), ratingCount());
        return String.format(
                "ISBN %-6d | %-36s | %-20s | %-12s | %-17s | %-5s | Borrows: %-3d | %s",
                isbn, t, a, genre, location,
                availableCount() + "/" + totalCopies(),
                borrowCount, ratingStr);
    }
}
