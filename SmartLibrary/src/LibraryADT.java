/**
 * ============================================================
 * TASK 4 – ADT Designer: The Library Interface
 * ============================================================
 * This interface defines WHAT the library can do,
 * without revealing HOW it does it internally.
 * This is "Information Hiding" – the caller never needs
 * to know about the BST or Stack underneath.
 * ============================================================
 */
public interface LibraryADT {

    /** Add a new book to the catalogue */
    void addBook(int isbn, String title, String author);

    /** Search for a book by its ISBN */
    void searchBook(int isbn);

    /** Borrow a book (move it from catalogue to history) */
    void borrowBook(int isbn);

    /** View the borrowing history (most-recent first) */
    void viewLatestHistory();
}
