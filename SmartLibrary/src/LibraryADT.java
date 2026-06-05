/**
 * ============================================================
 * TASK 4 – ADT Designer: The Library Interface
 * ============================================================
 * Defines WHAT the library can do without revealing HOW.
 * Callers never need to know about the BST, HashMap indexes,
 * Stack, Queue, or file I/O underneath.
 * ============================================================
 */
public interface LibraryADT {

    // --- Member management ---
    void registerUser(String userId, String name);

    // --- Catalogue management (Admin) ---
    void addBook(int isbn, String title, String author, String location, int quantity);
    void removeBook(int isbn);

    // --- Discovery ---
    /** searchType: "isbn" | "title" | "author" */
    void findBook(String query, String searchType);

    // --- Cart (staging area) ---
    void addToCart(String userId, int isbn);
    void removeFromCart(String userId, int isbn);
    void viewCart(String userId);
    void checkout(String userId);

    // --- Borrowing lifecycle ---
    void returnBook(String userId, int isbn);
    void renewBook(String userId, int isbn);

    // --- Reservation queue ---
    void joinWaitlist(String userId, int isbn);

    // --- History ---
    void viewHistory();
}
