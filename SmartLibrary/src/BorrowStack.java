import java.util.Stack;

/**
 * ============================================================
 * TASK 2 – Borrowing History: Stack (LIFO)
 * ============================================================
 * WHY a Stack?
 *   - The most-recently-borrowed book should appear FIRST
 *     when a student views their history.
 *   - Stack = Last-In, First-Out (LIFO) → perfect for this.
 *
 * Visual:
 *   push("Book C")   →   | Book C |  ← top (most recent)
 *   push("Book B")       | Book B |
 *   push("Book A")       | Book A |
 *                         ---------
 *   Viewing prints: Book C → Book B → Book A
 * ============================================================
 */
public class BorrowStack {

    // Java's built-in Stack class (internally uses a Vector/array)
    private Stack<Book> stack = new Stack<>();

    /** Push a borrowed book onto the top of the stack */
    public void push(Book book) {
        stack.push(book);
    }

    /**
     * Display all borrowed books, most-recent first.
     *
     * The Java Stack stores index 0 at the bottom and
     * index (size-1) at the top, so we iterate backwards
     * to show the newest book first.
     */
    public void show() {
        if (stack.isEmpty()) {
            System.out.println("  (No borrowing history yet.)");
            return;
        }

        System.out.println("  --- Borrowing History (most recent first) ---");
        // Walk from the TOP of the stack down to the BOTTOM
        for (int i = stack.size() - 1; i >= 0; i--) {
            Book book = stack.get(i);
            System.out.println("  " + (stack.size() - i) + ". " + book);
        }
    }
}
