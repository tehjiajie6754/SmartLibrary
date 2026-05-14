/**
 * ============================================================
 * Entity Class: Book
 * ============================================================
 * Each Book object doubles as a BST node because it has
 * `left` and `right` pointers.  This keeps things simple —
 * one class serves both as data AND tree structure.
 *
 *        [ISBN 200]           ← root
 *        /        \
 *   [ISBN 100]  [ISBN 300]   ← left child < parent < right child
 * ============================================================
 */
public class Book {

    // --- Book data ---
    int isbn;
    String title;
    String author;

    // --- BST pointers (left child, right child) ---
    Book left;
    Book right;

    /** Constructor – simply stores the three fields */
    public Book(int isbn, String title, String author) {
        this.isbn   = isbn;
        this.title  = title;
        this.author = author;
        this.left   = null;   // no children yet
        this.right  = null;
    }

    /** Handy method to print book info */
    @Override
    public String toString() {
        return "ISBN: " + isbn + " | Title: " + title + " | Author: " + author;
    }
}
