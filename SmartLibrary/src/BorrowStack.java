import java.time.LocalDate;
import java.util.Stack;

/**
 * ============================================================
 * TASK 2 – Borrowing History: Stack (LIFO)
 * ============================================================
 * Each push records who borrowed which copy of which book
 * and on what date.  Viewing prints most-recent-first.
 * ============================================================
 */
public class BorrowStack {

    private static class BorrowRecord {
        String    userId;
        String    title;
        int       isbn;
        int       copyId;
        LocalDate date;

        BorrowRecord(String userId, String title, int isbn, int copyId, LocalDate date) {
            this.userId = userId;
            this.title  = title;
            this.isbn   = isbn;
            this.copyId = copyId;
            this.date   = date;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s borrowed \"%s\" (ISBN %d, Copy #%d)",
                date, userId, title, isbn, copyId);
        }
    }

    private final Stack<BorrowRecord> stack = new Stack<>();

    public void push(String userId, String title, int isbn, int copyId, LocalDate date) {
        stack.push(new BorrowRecord(userId, title, isbn, copyId, date));
    }

    public void show() {
        if (stack.isEmpty()) {
            System.out.println("  (No borrowing history yet.)");
            return;
        }
        System.out.println("  --- Borrowing History (most recent first) ---");
        for (int i = stack.size() - 1; i >= 0; i--) {
            System.out.println("  " + (stack.size() - i) + ". " + stack.get(i));
        }
    }
}
