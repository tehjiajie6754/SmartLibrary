import java.util.Scanner;

/**
 * ============================================================
 * TASK 5 – Admin Logic + Console Interface
 * ============================================================
 * SmartLibrary implements the LibraryADT interface.
 * It wires together the BST (catalogue) and the Stack (history)
 * and provides the menu-driven console UI.
 *
 * Flow:
 * addBook → inserts into BST
 * searchBook→ searches BST (O(log n))
 * borrowBook→ searches BST, removes from BST, pushes to Stack
 * viewHistory→ prints stack top-to-bottom
 * ============================================================
 */
public class SmartLibrary implements LibraryADT {

    // Internal data structures – hidden from outside (Information Hiding)
    private BookBST catalogue = new BookBST(); // BST for fast search
    private BorrowStack history = new BorrowStack(); // Stack for LIFO history

    // ===========================================================
    // Interface method implementations
    // ===========================================================

    @Override
    public void addBook(int isbn, String title, String author) {
        catalogue.insert(isbn, title, author);
        System.out.println("  ✓ Book added: ISBN " + isbn + " – " + title + " by " + author);
    }

    @Override
    public void searchBook(int isbn) {
        Book found = catalogue.search(isbn); // O(log n) recursive search
        if (found != null) {
            System.out.println("  ✓ Found: " + found);
        } else {
            System.out.println("  ✗ No book found with ISBN " + isbn);
        }
    }

    @Override
    public void borrowBook(int isbn) {
        Book found = catalogue.search(isbn); // first, find the book
        if (found != null) {
            // Step 1: Push a copy to the history stack
            history.push(new Book(found.isbn, found.title, found.author));
            // Step 2: Remove from catalogue so it can't be borrowed again
            catalogue.remove(isbn);
            System.out.println("  ✓ Borrowed: " + found.title + " (ISBN " + isbn + ")");
        } else {
            System.out.println("  ✗ Book not found in catalogue. Cannot borrow.");
        }
    }

    @Override
    public void viewLatestHistory() {
        history.show();
    }

    // ===========================================================
    // Console Menu
    // ===========================================================

    /** Entry point: runs the interactive menu loop */
    public void runMenu() {
        Scanner sc = new Scanner(System.in);

        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║      SMART LIBRARY SYSTEM v1.0        ║");
        System.out.println("║  BST Catalogue + Stack History        ║");
        System.out.println("╚═══════════════════════════════════════╝");

        while (true) {
            printMenu();
            System.out.print("Enter your choice: ");

            // Guard against non-integer input
            if (!sc.hasNextInt()) {
                System.out.println("  ✗ Please enter a number (1-5).");
                sc.next(); // discard bad input
                continue;
            }

            int choice = sc.nextInt();
            sc.nextLine(); // consume leftover newline

            if (choice == 5) {
                System.out.println("\n  Goodbye! Thank you for using SmartLibrary.");
                break;
            }

            handleChoice(choice, sc);
        }

        sc.close();
    }

    /** Print the menu options */
    private void printMenu() {
        System.out.println("\n┌───────────────────────────────┐");
        System.out.println("│  1. Add Book                  │");
        System.out.println("│  2. Search Book (BST)         │");
        System.out.println("│  3. Borrow Book (-> Stack)    │");
        System.out.println("│  4. View Borrowing History    │");
        System.out.println("│  5. Exit                      │");
        System.out.println("└───────────────────────────────┘");
    }

    /** Dispatch user choice to the right action */
    private void handleChoice(int choice, Scanner sc) {
        switch (choice) {
            case 1: // --- Add Book ---
                System.out.print("  Enter ISBN (number): ");
                int isbn = sc.nextInt();
                sc.nextLine(); // consume newline
                System.out.print("  Enter Title: ");
                String title = sc.nextLine();
                System.out.print("  Enter Author: ");
                String author = sc.nextLine();
                addBook(isbn, title, author);
                break;

            case 2: // --- Search Book ---
                System.out.print("  Enter ISBN to search: ");
                searchBook(sc.nextInt());
                sc.nextLine();
                break;

            case 3: // --- Borrow Book ---
                System.out.print("  Enter ISBN to borrow: ");
                borrowBook(sc.nextInt());
                sc.nextLine();
                break;

            case 4: // --- View History ---
                viewLatestHistory();
                break;

            default:
                System.out.println("  ✗ Invalid option. Please choose 1-5.");
        }
    }
}
