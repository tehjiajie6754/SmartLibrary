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
    void removeMember(String userId);
    void listAllMembers();

    // --- Catalogue management (Admin) ---
    void addBook(int isbn, String title, String author, String location, String genre, int quantity);
    void removeBook(int isbn);
    void updateBook(int isbn, String newTitle, String newAuthor, String newLocation, String newGenre);

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
    void leaveWaitlist(String userId, int isbn);

    // --- Analytics ---
    void displayHottestBooks();
    void recommendBooks(String userId);
    void viewMonthlyReport(int year, int month);

    // --- Ratings ---
    void rateBook(String userId, int isbn, int stars);

    // --- Catalogue browsing ---
    void listAllBooks();
    void displayOverdueReport();

    // --- Profile ---
    void viewUserProfile(String userId);

    // --- History ---
    void viewHistory();
    void viewPersonalHistory(String userId);
}
