import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
        this.copies      = new ArrayList<>();
        this.waitlist    = new LinkedList<>();
        this.left        = null;
        this.right       = null;
    }

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
        return String.format(
            "ISBN: %-6d | %-35s | %-22s | %-15s | Location: %-20s | Available: %d/%d | Borrows: %d",
            isbn, title, author, genre, location, availableCount(), totalCopies(), borrowCount
        );
    }
}
