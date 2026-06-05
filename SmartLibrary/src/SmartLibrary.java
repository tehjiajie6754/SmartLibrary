import java.io.*;
import java.time.LocalDate;
import java.util.*;

/**
 * ============================================================
 * TASK 5 – Admin Logic + Console Interface
 * ============================================================
 * Implements LibraryADT and wires together:
 *   BST catalogue  (primary + HashMap secondary indexes)
 *   BorrowStack    (history, LIFO)
 *   User map       (member registry)
 *   Cart map       (per-user staging area)
 *   Reservation    (Queue<String> inside each Book node)
 *
 * Persistence  (3 separate files):
 *   library.txt       – books + copies
 *   users.txt         – members + borrowed-key list
 *   reservations.txt  – per-ISBN waitlists
 * ============================================================
 */
public class SmartLibrary implements LibraryADT {

    // --- File names ---
    private static final String LIBRARY_FILE      = "library.txt";
    private static final String USERS_FILE        = "users.txt";
    private static final String RESERVATIONS_FILE = "reservations.txt";

    // --- Business rules ---
    private static final int    LOAN_DAYS     = 7;
    private static final double FINE_PER_DAY  = 1.0;   // RM 1 / day

    // --- Data structures ---
    private final BookBST              catalogue = new BookBST();
    private final Map<String, User>    users     = new HashMap<>();
    private final Map<String, Cart>    carts     = new HashMap<>();
    private final BorrowStack          history   = new BorrowStack();

    // ===========================================================
    // LibraryADT – Member management
    // ===========================================================

    @Override
    public void registerUser(String userId, String name) {
        if (users.containsKey(userId)) {
            System.out.println("  ✗ User ID '" + userId + "' already exists.");
            return;
        }
        users.put(userId, new User(userId, name));
        System.out.println("  ✓ Member registered: " + userId + " (" + name + ")");
    }

    // ===========================================================
    // LibraryADT – Catalogue management (Admin)
    // ===========================================================

    @Override
    public void addBook(int isbn, String title, String author, String location, int quantity) {
        Book existing = catalogue.search(isbn);
        if (existing != null) {
            // Add more copies to an already-registered ISBN
            int nextId = existing.totalCopies() + 1;
            for (int i = 0; i < quantity; i++) {
                existing.addCopy(new Copy(nextId + i));
            }
            System.out.printf("  ✓ Added %d copy/copies to ISBN %d. Total copies: %d%n",
                quantity, isbn, existing.totalCopies());
            return;
        }
        Book book = new Book(isbn, title, author, location);
        for (int i = 1; i <= quantity; i++) {
            book.addCopy(new Copy(i));
        }
        catalogue.insert(book);
        System.out.printf("  ✓ Book registered: ISBN %d – \"%s\" by %s (%d copy/copies)%n",
            isbn, title, author, quantity);
    }

    @Override
    public void removeBook(int isbn) {
        Book book = catalogue.search(isbn);
        if (book == null) {
            System.out.println("  ✗ Book not found: ISBN " + isbn);
            return;
        }
        for (Copy c : book.copies) {
            if (!c.isAvailable()) {
                System.out.println("  ✗ Cannot remove \"" + book.title +
                    "\" – one or more copies are still on loan.");
                return;
            }
        }
        catalogue.remove(isbn);
        System.out.println("  ✓ Book removed: ISBN " + isbn + " – \"" + book.title + "\"");
    }

    // ===========================================================
    // LibraryADT – Discovery
    // ===========================================================

    @Override
    public void findBook(String query, String searchType) {
        List<Book> results = new ArrayList<>();
        switch (searchType.toLowerCase()) {
            case "isbn":
                try {
                    Book b = catalogue.search(Integer.parseInt(query.trim()));
                    if (b != null) results.add(b);
                } catch (NumberFormatException e) {
                    System.out.println("  ✗ ISBN must be a number.");
                    return;
                }
                break;
            case "title":
                results = catalogue.searchByTitle(query);
                break;
            case "author":
                results = catalogue.searchByAuthor(query);
                break;
            default:
                System.out.println("  ✗ Unknown search type: " + searchType);
                return;
        }

        if (results.isEmpty()) {
            System.out.println("  ✗ No results found for \"" + query + "\".");
            return;
        }

        System.out.println("  Found " + results.size() + " result(s):");
        System.out.println("  " + "-".repeat(100));
        for (Book b : results) {
            System.out.println("  " + b);
            if (!b.waitlist.isEmpty()) {
                System.out.println("    Waitlist: " + b.waitlist.size() + " user(s) waiting");
            }
        }
        System.out.println("  " + "-".repeat(100));
    }

    // ===========================================================
    // LibraryADT – Cart
    // ===========================================================

    @Override
    public void addToCart(String userId, int isbn) {
        if (!users.containsKey(userId)) {
            System.out.println("  ✗ User not found: " + userId);
            return;
        }
        Book book = catalogue.search(isbn);
        if (book == null) {
            System.out.println("  ✗ Book not found: ISBN " + isbn);
            return;
        }
        Cart cart = carts.computeIfAbsent(userId, Cart::new);
        if (cart.add(isbn)) {
            System.out.println("  ✓ Added to cart: \"" + book.title + "\" (ISBN " + isbn + ")");
        } else {
            System.out.println("  ✗ ISBN " + isbn + " is already in your cart.");
        }
    }

    @Override
    public void removeFromCart(String userId, int isbn) {
        Cart cart = carts.get(userId);
        if (cart == null || cart.isEmpty()) {
            System.out.println("  ✗ Cart is empty.");
            return;
        }
        if (cart.remove(isbn)) {
            System.out.println("  ✓ Removed ISBN " + isbn + " from cart.");
        } else {
            System.out.println("  ✗ ISBN " + isbn + " was not in your cart.");
        }
    }

    @Override
    public void viewCart(String userId) {
        Cart cart = carts.get(userId);
        if (cart == null || cart.isEmpty()) {
            System.out.println("  (Cart is empty)");
            return;
        }
        System.out.println("  --- Cart for " + userId + " ---");
        int i = 1;
        for (int isbn : cart.getItems()) {
            Book b    = catalogue.search(isbn);
            String bk = (b != null) ? "\"" + b.title + "\"" : "(book not found)";
            System.out.println("  " + i++ + ". ISBN " + isbn + " – " + bk);
        }
    }

    @Override
    public void checkout(String userId) {
        User user = users.get(userId);
        if (user == null) {
            System.out.println("  ✗ User not found: " + userId);
            return;
        }
        Cart cart = carts.get(userId);
        if (cart == null || cart.isEmpty()) {
            System.out.println("  ✗ Cart is empty.");
            return;
        }

        // Take a snapshot so we can iterate while modifying the cart
        List<Integer> items      = new ArrayList<>(cart.getItems());
        boolean       anySuccess = false;

        for (int isbn : items) {
            if (!user.canBorrow()) {
                System.out.println("  ✗ Borrow limit reached (max " + User.MAX_BORROW +
                    "). Remaining cart items kept for later.");
                break;
            }
            Book book = catalogue.search(isbn);
            if (book == null) {
                System.out.println("  ✗ ISBN " + isbn + " no longer exists. Removed from cart.");
                cart.remove(isbn);
                continue;
            }
            Copy copy = book.getAvailableCopy();
            if (copy == null) {
                System.out.println("  ✗ No copies available for \"" + book.title + "\".");
                System.out.println("    Tip: use option [8] Join Waitlist to queue for this book.");
                continue;
            }

            // Borrow the copy
            copy.borrowerId  = userId;
            copy.borrowDate  = LocalDate.now();
            copy.dueDate     = LocalDate.now().plusDays(LOAN_DAYS);
            copy.renewedOnce = false;
            user.borrowedKeys.add(isbn + ":" + copy.copyId);
            cart.remove(isbn);
            history.push(userId, book.title, isbn, copy.copyId, LocalDate.now());

            System.out.printf("  ✓ Borrowed: \"%s\" (Copy #%d) | Due: %s%n",
                book.title, copy.copyId, copy.dueDate);
            anySuccess = true;
        }

        if (!anySuccess) {
            System.out.println("  ✗ No books were borrowed in this session.");
        }
    }

    // ===========================================================
    // LibraryADT – Borrowing lifecycle
    // ===========================================================

    @Override
    public void returnBook(String userId, int isbn) {
        User user = users.get(userId);
        if (user == null) {
            System.out.println("  ✗ User not found: " + userId);
            return;
        }
        Book book = catalogue.search(isbn);
        if (book == null) {
            System.out.println("  ✗ Book not found: ISBN " + isbn);
            return;
        }
        Copy copy = book.getCopyBorrowedBy(userId);
        if (copy == null) {
            System.out.println("  ✗ " + userId + " does not have a copy of \"" + book.title + "\".");
            return;
        }

        // Fine calculation
        long   overdueDays = copy.daysOverdue();
        double fine        = overdueDays * FINE_PER_DAY;
        if (fine > 0) {
            System.out.printf("  ⚠  Overdue by %d day(s). Fine payable: RM %.2f%n",
                overdueDays, fine);
        }

        // Release the copy
        String returnedKey = isbn + ":" + copy.copyId;
        user.borrowedKeys.remove(returnedKey);
        copy.borrowerId  = null;
        copy.borrowDate  = null;
        copy.dueDate     = null;
        copy.renewedOnce = false;
        System.out.println("  ✓ Returned: \"" + book.title + "\" (Copy #" + copy.copyId + ")");

        // Auto-assign to next person in the reservation queue
        processWaitlist(book, copy);
    }

    @Override
    public void renewBook(String userId, int isbn) {
        User user = users.get(userId);
        if (user == null) {
            System.out.println("  ✗ User not found: " + userId);
            return;
        }
        Book book = catalogue.search(isbn);
        if (book == null) {
            System.out.println("  ✗ Book not found: ISBN " + isbn);
            return;
        }
        Copy copy = book.getCopyBorrowedBy(userId);
        if (copy == null) {
            System.out.println("  ✗ " + userId + " does not have a copy of \"" + book.title + "\".");
            return;
        }
        if (copy.renewedOnce) {
            System.out.println("  ✗ Renewal limit reached. Each copy may only be renewed once.");
            return;
        }

        // Show any accumulated fine before resetting the clock
        long overdueDays = copy.daysOverdue();
        if (overdueDays > 0) {
            System.out.printf("  ⚠  Book is overdue by %d day(s). Accumulated fine: RM %.2f%n",
                overdueDays, overdueDays * FINE_PER_DAY);
            System.out.println("     Fine is noted. Renewal clock reset from today.");
        }

        copy.borrowDate  = LocalDate.now();
        copy.dueDate     = LocalDate.now().plusDays(LOAN_DAYS);
        copy.renewedOnce = true;
        System.out.printf("  ✓ Renewed: \"%s\" | New due date: %s%n", book.title, copy.dueDate);
    }

    // ===========================================================
    // LibraryADT – Reservation queue
    // ===========================================================

    @Override
    public void joinWaitlist(String userId, int isbn) {
        if (!users.containsKey(userId)) {
            System.out.println("  ✗ User not found: " + userId);
            return;
        }
        Book book = catalogue.search(isbn);
        if (book == null) {
            System.out.println("  ✗ Book not found: ISBN " + isbn);
            return;
        }
        if (book.availableCount() > 0) {
            System.out.println("  ✗ Copies are available now. Use Cart → Checkout to borrow directly.");
            return;
        }
        if (book.waitlist.contains(userId)) {
            System.out.println("  ✗ " + userId + " is already in the waitlist for \"" + book.title + "\".");
            return;
        }
        book.waitlist.add(userId);
        System.out.printf("  ✓ Added to waitlist for \"%s\" | Queue position: %d%n",
            book.title, book.waitlist.size());
    }

    // ===========================================================
    // LibraryADT – History
    // ===========================================================

    @Override
    public void viewHistory() {
        history.show();
    }

    // ===========================================================
    // Private helpers
    // ===========================================================

    /**
     * When a copy is returned, walk the waitlist and auto-assign to the
     * first eligible user (skipping invalid / over-limit entries).
     */
    private void processWaitlist(Book book, Copy copy) {
        while (!book.waitlist.isEmpty()) {
            String nextId   = book.waitlist.peek();
            User   nextUser = users.get(nextId);

            if (nextUser == null) {
                book.waitlist.poll();           // stale entry – remove
                continue;
            }
            if (!nextUser.canBorrow()) {
                book.waitlist.poll();
                System.out.println("  ★ Waitlist user " + nextId +
                    " has reached borrow limit – skipped.");
                continue;
            }

            // Auto-assign
            book.waitlist.poll();
            copy.borrowerId  = nextId;
            copy.borrowDate  = LocalDate.now();
            copy.dueDate     = LocalDate.now().plusDays(LOAN_DAYS);
            copy.renewedOnce = false;
            nextUser.borrowedKeys.add(book.isbn + ":" + copy.copyId);
            history.push(nextId, book.title, book.isbn, copy.copyId, LocalDate.now());
            System.out.printf("  ★ Auto-assigned to waitlist user: %s (%s) | Due: %s%n",
                nextId, nextUser.name, copy.dueDate);
            break;
        }
    }

    // ===========================================================
    // Persistence – load
    // ===========================================================

    private void loadDatabase() {
        loadLibrary();
        loadUsers();
        loadReservations();
    }

    private void loadLibrary() {
        File file = new File(LIBRARY_FILE);
        if (!file.exists()) {
            System.out.println("  (No library database found – starting fresh.)");
            return;
        }
        /*
         * File format (one book block):
         *   BOOK | isbn | title | author | location
         *   COPY | copyId | borrowerId(or null) | borrowDate(or null) | dueDate(or null) | renewedOnce
         *   COPY | ...
         */
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            Book   currentBook = null;
            int    count       = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(" \\| ");
                if (p[0].equals("BOOK") && p.length >= 5) {
                    int    isbn     = Integer.parseInt(p[1].trim());
                    String title    = p[2].trim();
                    String author   = p[3].trim();
                    String location = p[4].trim();
                    Book   book     = new Book(isbn, title, author, location);
                    catalogue.insert(book);
                    // Always use the node actually stored in the BST
                    currentBook = catalogue.search(isbn);
                    count++;
                } else if (p[0].equals("COPY") && p.length >= 6 && currentBook != null) {
                    int       copyId      = Integer.parseInt(p[1].trim());
                    String    borrowerId  = p[2].trim().equals("null") ? null : p[2].trim();
                    LocalDate borrowDate  = p[3].trim().equals("null") ? null : LocalDate.parse(p[3].trim());
                    LocalDate dueDate     = p[4].trim().equals("null") ? null : LocalDate.parse(p[4].trim());
                    boolean   renewedOnce = Boolean.parseBoolean(p[5].trim());
                    Copy      copy        = new Copy(copyId);
                    copy.borrowerId  = borrowerId;
                    copy.borrowDate  = borrowDate;
                    copy.dueDate     = dueDate;
                    copy.renewedOnce = renewedOnce;
                    currentBook.addCopy(copy);
                }
            }
            System.out.println("  ✓ Loaded " + count + " book(s) from " + LIBRARY_FILE);
        } catch (IOException e) {
            System.out.println("  ✗ Error loading library: " + e.getMessage());
        }
    }

    private void loadUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) return;
        /*
         * File format:
         *   userId | name | isbn:copyId,isbn:copyId,...
         */
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int    count = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p    = line.split(" \\| ");
                if (p.length < 2) continue;
                String   uid  = p[0].trim();
                String   name = p[1].trim();
                User     user = new User(uid, name);
                if (p.length >= 3 && !p[2].trim().isEmpty()) {
                    for (String key : p[2].trim().split(",")) {
                        if (!key.trim().isEmpty()) user.borrowedKeys.add(key.trim());
                    }
                }
                users.put(uid, user);
                count++;
            }
            System.out.println("  ✓ Loaded " + count + " user(s) from " + USERS_FILE);
        } catch (IOException e) {
            System.out.println("  ✗ Error loading users: " + e.getMessage());
        }
    }

    private void loadReservations() {
        File file = new File(RESERVATIONS_FILE);
        if (!file.exists()) return;
        /*
         * File format:
         *   isbn | userId1,userId2,...
         */
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(" \\| ");
                if (p.length < 2) continue;
                int  isbn = Integer.parseInt(p[0].trim());
                Book book = catalogue.search(isbn);
                if (book == null) continue;
                for (String uid : p[1].trim().split(",")) {
                    if (!uid.trim().isEmpty()) book.waitlist.add(uid.trim());
                }
            }
            System.out.println("  ✓ Reservations loaded from " + RESERVATIONS_FILE);
        } catch (IOException e) {
            System.out.println("  ✗ Error loading reservations: " + e.getMessage());
        }
    }

    // ===========================================================
    // Persistence – save
    // ===========================================================

    private void saveDatabase() {
        saveLibrary();
        saveUsers();
        saveReservations();
    }

    private void saveLibrary() {
        List<Book> allBooks = new ArrayList<>();
        catalogue.getAllBooks(allBooks);
        try (PrintWriter pw = new PrintWriter(new FileWriter(LIBRARY_FILE))) {
            for (Book b : allBooks) {
                pw.println("BOOK | " + b.isbn + " | " + b.title + " | " + b.author + " | " + b.location);
                for (Copy c : b.copies) {
                    pw.println("COPY | " + c.copyId + " | " + c.borrowerId + " | " +
                        c.borrowDate + " | " + c.dueDate + " | " + c.renewedOnce);
                }
            }
            System.out.println("  ✓ Library saved (" + allBooks.size() + " book(s)).");
        } catch (IOException e) {
            System.out.println("  ✗ Error saving library: " + e.getMessage());
        }
    }

    private void saveUsers() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(USERS_FILE))) {
            for (User u : users.values()) {
                pw.println(u.userId + " | " + u.name + " | " +
                    String.join(",", u.borrowedKeys));
            }
            System.out.println("  ✓ Users saved (" + users.size() + " user(s)).");
        } catch (IOException e) {
            System.out.println("  ✗ Error saving users: " + e.getMessage());
        }
    }

    private void saveReservations() {
        List<Book> allBooks = new ArrayList<>();
        catalogue.getAllBooks(allBooks);
        try (PrintWriter pw = new PrintWriter(new FileWriter(RESERVATIONS_FILE))) {
            for (Book b : allBooks) {
                if (!b.waitlist.isEmpty()) {
                    pw.println(b.isbn + " | " + String.join(",", b.waitlist));
                }
            }
            System.out.println("  ✓ Reservations saved.");
        } catch (IOException e) {
            System.out.println("  ✗ Error saving reservations: " + e.getMessage());
        }
    }

    // ===========================================================
    // Console Menu
    // ===========================================================

    public void runMenu() {
        Scanner sc = new Scanner(System.in);

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║          SMART LIBRARY SYSTEM  v3.0              ║");
        System.out.println("║  BST Catalogue | HashMap Search | Queue Waitlist ║");
        System.out.println("╚══════════════════════════════════════════════════╝");

        loadDatabase();

        while (true) {
            printMenu();
            System.out.print("Enter your choice: ");

            if (!sc.hasNextInt()) {
                System.out.println("  ✗ Please enter a number (1-10).");
                sc.next();
                continue;
            }

            int choice = sc.nextInt();
            sc.nextLine();

            if (choice == 10) {
                saveDatabase();
                System.out.println("\n  Goodbye! Thank you for using SmartLibrary.");
                break;
            }

            handleChoice(choice, sc);
        }

        sc.close();
    }

    private void printMenu() {
        System.out.println();
        System.out.println("┌──────────────────────────────────────────┐");
        System.out.println("│  [Member]                                │");
        System.out.println("│   1. Register Member                     │");
        System.out.println("│  [Admin]                                 │");
        System.out.println("│   2. Add Book (Register / Add Copies)    │");
        System.out.println("│   3. Remove Book                         │");
        System.out.println("│  [Services]                              │");
        System.out.println("│   4. Find Book (ISBN / Title / Author)   │");
        System.out.println("│   5. Manage Cart                         │");
        System.out.println("│   6. Return Book                         │");
        System.out.println("│   7. Renew Book                          │");
        System.out.println("│   8. Join Waitlist                       │");
        System.out.println("│  [History]                               │");
        System.out.println("│   9. View Borrowing History              │");
        System.out.println("│  10. Exit                                │");
        System.out.println("└──────────────────────────────────────────┘");
    }

    private void handleChoice(int choice, Scanner sc) {
        switch (choice) {

            case 1: { // Register Member
                System.out.print("  Enter User ID  : ");
                String uid = sc.nextLine().trim();
                System.out.print("  Enter Full Name: ");
                String name = sc.nextLine().trim();
                registerUser(uid, name);
                break;
            }

            case 2: { // Add Book
                System.out.print("  Enter ISBN (number)               : ");
                int isbn = readInt(sc);
                System.out.print("  Enter Title                       : ");
                String title = sc.nextLine().trim();
                System.out.print("  Enter Author                      : ");
                String author = sc.nextLine().trim();
                System.out.print("  Enter Location (e.g. Aisle 1 / Shelf A): ");
                String location = sc.nextLine().trim();
                System.out.print("  Enter Quantity                    : ");
                int qty = readInt(sc);
                addBook(isbn, title, author, location, qty);
                break;
            }

            case 3: { // Remove Book
                System.out.print("  Enter ISBN to remove: ");
                removeBook(readInt(sc));
                break;
            }

            case 4: { // Find Book
                System.out.println("  Search by:  1. ISBN   2. Title   3. Author");
                System.out.print("  Choice: ");
                int t = readInt(sc);
                String type = (t == 1) ? "isbn" : (t == 2) ? "title" : "author";
                System.out.print("  Enter search term: ");
                String q = sc.nextLine().trim();
                findBook(q, type);
                break;
            }

            case 5: { // Manage Cart
                System.out.print("  Enter User ID: ");
                String uid = sc.nextLine().trim();
                if (!users.containsKey(uid)) {
                    System.out.println("  ✗ User not found: " + uid);
                    break;
                }
                cartMenu(uid, sc);
                break;
            }

            case 6: { // Return Book
                System.out.print("  Enter User ID : ");
                String uid = sc.nextLine().trim();
                System.out.print("  Enter ISBN    : ");
                int isbn = readInt(sc);
                returnBook(uid, isbn);
                break;
            }

            case 7: { // Renew Book
                System.out.print("  Enter User ID : ");
                String uid = sc.nextLine().trim();
                System.out.print("  Enter ISBN    : ");
                int isbn = readInt(sc);
                renewBook(uid, isbn);
                break;
            }

            case 8: { // Join Waitlist
                System.out.print("  Enter User ID : ");
                String uid = sc.nextLine().trim();
                System.out.print("  Enter ISBN    : ");
                int isbn = readInt(sc);
                joinWaitlist(uid, isbn);
                break;
            }

            case 9: { // View History
                viewHistory();
                break;
            }

            default:
                System.out.println("  ✗ Invalid option. Please choose 1-10.");
        }
    }

    private void cartMenu(String userId, Scanner sc) {
        while (true) {
            System.out.println();
            System.out.println("  ┌─ Cart Menu (" + userId + ") ─────────────────┐");
            System.out.println("  │  a. Add book to cart                    │");
            System.out.println("  │  b. Remove book from cart               │");
            System.out.println("  │  c. View cart                           │");
            System.out.println("  │  d. Checkout (borrow all)               │");
            System.out.println("  │  e. Back to main menu                   │");
            System.out.println("  └─────────────────────────────────────────┘");
            System.out.print("  Choice: ");
            String ch = sc.nextLine().trim().toLowerCase();

            switch (ch) {
                case "a": {
                    System.out.print("    Enter ISBN: ");
                    addToCart(userId, readInt(sc));
                    break;
                }
                case "b": {
                    System.out.print("    Enter ISBN to remove: ");
                    removeFromCart(userId, readInt(sc));
                    break;
                }
                case "c":
                    viewCart(userId);
                    break;
                case "d":
                    checkout(userId);
                    break;
                case "e":
                    return;
                default:
                    System.out.println("    ✗ Enter a, b, c, d, or e.");
            }
        }
    }

    /** Reads an int then consumes the trailing newline. */
    private int readInt(Scanner sc) {
        while (!sc.hasNextInt()) {
            System.out.print("  ✗ Please enter a number: ");
            sc.next();
        }
        int v = sc.nextInt();
        sc.nextLine();
        return v;
    }
}
