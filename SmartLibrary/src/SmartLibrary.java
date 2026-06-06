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
 *   users.txt         – members + borrowed-key list + genre stats
 *   reservations.txt  – per-ISBN waitlists
 * ============================================================
 */
public class SmartLibrary implements LibraryADT {

    // --- File names ---
    private static final String LIBRARY_FILE      = "library.txt";
    private static final String USERS_FILE        = "users.txt";
    private static final String RESERVATIONS_FILE = "reservations.txt";

    // --- Business rules ---
    private static final int    LOAN_DAYS    = 7;
    private static final double FINE_PER_DAY = 1.0;   // RM 1 / day

    // --- Predefined genre list ---
    private static final String[] GENRES = {
        "Sci-Fi", "Fantasy", "Mystery", "Romance", "Classic",
        "Non-Fiction", "Horror", "Thriller", "History", "Philosophy",
        "Religion", "Biography"
    };

    // --- Data structures ---
    private final BookBST           catalogue = new BookBST();
    private final Map<String, User> users     = new HashMap<>();
    private final Map<String, Cart> carts     = new HashMap<>();
    private final BorrowStack       history   = new BorrowStack();

    // ===========================================================
    // LibraryADT – Member management
    // ===========================================================

    @Override
    public void registerUser(String userId, String name) {
        if (users.containsKey(userId)) {
            System.out.println("  User ID '" + userId + "' already exists.");
            return;
        }
        users.put(userId, new User(userId, name));
        System.out.println("  Member registered: " + userId + " (" + name + ")");
    }

    // ===========================================================
    // LibraryADT – Catalogue management (Admin)
    // ===========================================================

    @Override
    public void addBook(int isbn, String title, String author,
                        String location, String genre, int quantity) {
        Book existing = catalogue.search(isbn);
        if (existing != null) {
            int nextId = existing.totalCopies() + 1;
            for (int i = 0; i < quantity; i++) existing.addCopy(new Copy(nextId + i));
            System.out.printf("  Added %d copy/copies to ISBN %d. Total copies: %d%n",
                quantity, isbn, existing.totalCopies());
            return;
        }
        Book book = new Book(isbn, title, author, location, genre);
        for (int i = 1; i <= quantity; i++) book.addCopy(new Copy(i));
        catalogue.insert(book);
        System.out.printf("  Book registered: ISBN %d – \"%s\" by %s [%s] (%d copy/copies)%n",
            isbn, title, author, genre, quantity);
    }

    @Override
    public void removeBook(int isbn) {
        Book book = catalogue.search(isbn);
        if (book == null) {
            System.out.println("  Book not found: ISBN " + isbn);
            return;
        }
        for (Copy c : book.copies) {
            if (!c.isAvailable()) {
                System.out.println("  Cannot remove \"" + book.title +
                    "\" – one or more copies are still on loan.");
                return;
            }
        }
        catalogue.remove(isbn);
        System.out.println("  Book removed: ISBN " + isbn + " – \"" + book.title + "\"");
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
                    System.out.println("  ISBN must be a number.");
                    return;
                }
                break;
            case "title":  results = catalogue.searchByTitle(query);  break;
            case "author": results = catalogue.searchByAuthor(query); break;
            default:
                System.out.println("  Unknown search type: " + searchType);
                return;
        }

        if (results.isEmpty()) {
            System.out.println("  No results found for \"" + query + "\".");
            return;
        }

        System.out.println("  Found " + results.size() + " result(s):");
        System.out.println("  " + "-".repeat(110));
        for (Book b : results) {
            System.out.println("  " + b);
            if (!b.waitlist.isEmpty())
                System.out.println("    Waitlist: " + b.waitlist.size() + " user(s) waiting");
        }
        System.out.println("  " + "-".repeat(110));
    }

    // ===========================================================
    // LibraryADT – Cart
    // ===========================================================

    @Override
    public void addToCart(String userId, int isbn) {
        if (!users.containsKey(userId)) {
            System.out.println("  User not found: " + userId);
            return;
        }
        Book book = catalogue.search(isbn);
        if (book == null) {
            System.out.println("  Book not found: ISBN " + isbn);
            return;
        }
        Cart cart = carts.computeIfAbsent(userId, Cart::new);
        if (cart.add(isbn)) {
            System.out.println("  Added to cart: \"" + book.title + "\" (ISBN " + isbn + ")");
        } else {
            System.out.println("  ISBN " + isbn + " is already in your cart.");
        }
    }

    @Override
    public void removeFromCart(String userId, int isbn) {
        Cart cart = carts.get(userId);
        if (cart == null || cart.isEmpty()) { System.out.println("  Cart is empty."); return; }
        if (cart.remove(isbn)) {
            System.out.println("  Removed ISBN " + isbn + " from cart.");
        } else {
            System.out.println("  ISBN " + isbn + " was not in your cart.");
        }
    }

    @Override
    public void viewCart(String userId) {
        Cart cart = carts.get(userId);
        if (cart == null || cart.isEmpty()) { System.out.println("  (Cart is empty)"); return; }
        System.out.println("  --- Cart for " + userId + " ---");
        int i = 1;
        for (int isbn : cart.getItems()) {
            Book b = catalogue.search(isbn);
            System.out.println("  " + i++ + ". ISBN " + isbn + " – " +
                ((b != null) ? "\"" + b.title + "\"" : "(book not found)"));
        }
    }

    @Override
    public void checkout(String userId) {
        User user = users.get(userId);
        if (user == null) { System.out.println("  User not found: " + userId); return; }
        Cart cart = carts.get(userId);
        if (cart == null || cart.isEmpty()) { System.out.println("  Cart is empty."); return; }

        List<Integer> items      = new ArrayList<>(cart.getItems());
        boolean       anySuccess = false;

        for (int isbn : items) {
            if (!user.canBorrow()) {
                System.out.println("  Borrow limit reached (max " + User.MAX_BORROW +
                    "). Remaining cart items kept for later.");
                break;
            }
            Book book = catalogue.search(isbn);
            if (book == null) {
                System.out.println("  ISBN " + isbn + " no longer exists. Removed from cart.");
                cart.remove(isbn);
                continue;
            }
            Copy copy = book.getAvailableCopy();
            if (copy == null) {
                System.out.println("  No copies available for \"" + book.title + "\".");
                System.out.println("    Tip: use option [8] Join Waitlist to queue for this book.");
                continue;
            }

            copy.borrowerId  = userId;
            copy.borrowDate  = LocalDate.now();
            copy.dueDate     = LocalDate.now().plusDays(LOAN_DAYS);
            copy.renewedOnce = false;
            user.borrowedKeys.add(isbn + ":" + copy.copyId);
            cart.remove(isbn);

            // Analytics tracking
            book.borrowCount++;
            user.genreCounts.merge(book.genre, 1, Integer::sum);
            user.readHistory.add(isbn);

            history.push(userId, book.title, isbn, copy.copyId, LocalDate.now());
            System.out.printf("  Borrowed: \"%s\" (Copy #%d) | Due: %s%n",
                book.title, copy.copyId, copy.dueDate);
            anySuccess = true;
        }

        if (!anySuccess) System.out.println("  No books were borrowed in this session.");
    }

    // ===========================================================
    // LibraryADT – Borrowing lifecycle
    // ===========================================================

    @Override
    public void returnBook(String userId, int isbn) {
        User user = users.get(userId);
        if (user == null) { System.out.println("  User not found: " + userId); return; }
        Book book = catalogue.search(isbn);
        if (book == null) { System.out.println("  Book not found: ISBN " + isbn); return; }
        Copy copy = book.getCopyBorrowedBy(userId);
        if (copy == null) {
            System.out.println("  " + userId + " does not have a copy of \"" + book.title + "\".");
            return;
        }

        long   overdueDays = copy.daysOverdue();
        double fine        = overdueDays * FINE_PER_DAY;
        if (fine > 0)
            System.out.printf("  Overdue by %d day(s). Fine payable: RM %.2f%n", overdueDays, fine);

        user.borrowedKeys.remove(isbn + ":" + copy.copyId);
        copy.borrowerId  = null;
        copy.borrowDate  = null;
        copy.dueDate     = null;
        copy.renewedOnce = false;
        System.out.println("  Returned: \"" + book.title + "\" (Copy #" + copy.copyId + ")");

        processWaitlist(book, copy);
    }

    @Override
    public void renewBook(String userId, int isbn) {
        User user = users.get(userId);
        if (user == null) { System.out.println("  User not found: " + userId); return; }
        Book book = catalogue.search(isbn);
        if (book == null) { System.out.println("  Book not found: ISBN " + isbn); return; }
        Copy copy = book.getCopyBorrowedBy(userId);
        if (copy == null) {
            System.out.println("  " + userId + " does not have a copy of \"" + book.title + "\".");
            return;
        }
        if (copy.renewedOnce) {
            System.out.println("  Renewal limit reached. Each copy may only be renewed once.");
            return;
        }

        long overdueDays = copy.daysOverdue();
        if (overdueDays > 0)
            System.out.printf("  Book is overdue by %d day(s). Fine so far: RM %.2f. Clock reset from today.%n",
                overdueDays, overdueDays * FINE_PER_DAY);

        copy.borrowDate  = LocalDate.now();
        copy.dueDate     = LocalDate.now().plusDays(LOAN_DAYS);
        copy.renewedOnce = true;
        System.out.printf("  Renewed: \"%s\" | New due date: %s%n", book.title, copy.dueDate);
    }

    // ===========================================================
    // LibraryADT – Reservation queue
    // ===========================================================

    @Override
    public void joinWaitlist(String userId, int isbn) {
        if (!users.containsKey(userId)) { System.out.println("  User not found: " + userId); return; }
        Book book = catalogue.search(isbn);
        if (book == null) { System.out.println("  Book not found: ISBN " + isbn); return; }
        if (book.availableCount() > 0) {
            System.out.println("  Copies are available. Use Cart → Checkout to borrow directly.");
            return;
        }
        if (book.waitlist.contains(userId)) {
            System.out.println("  " + userId + " is already in the waitlist for \"" + book.title + "\".");
            return;
        }
        book.waitlist.add(userId);
        System.out.printf("  Added to waitlist for \"%s\" | Queue position: %d%n",
            book.title, book.waitlist.size());
    }

    // ===========================================================
    // LibraryADT – Analytics
    // ===========================================================

    @Override
    public void displayHottestBooks() {
        List<Book> all = new ArrayList<>();
        catalogue.getAllBooks(all);
        all.sort((a, b) -> b.borrowCount - a.borrowCount);

        if (all.isEmpty() || all.get(0).borrowCount == 0) {
            System.out.println("  No borrowing data yet.");
            return;
        }

        System.out.println("  --- Top 5 Hottest Books ---");
        System.out.println("  " + "-".repeat(110));
        int rank = 1;
        for (Book b : all) {
            if (rank > 5 || b.borrowCount == 0) break;
            System.out.printf("  #%-2d (%d borrow(s))  %s%n", rank++, b.borrowCount, b);
        }
        System.out.println("  " + "-".repeat(110));
    }

    @Override
    public void recommendBooks(String userId) {
        User user = users.get(userId);
        if (user == null) { System.out.println("  User not found: " + userId); return; }

        String topGenre = user.getTopGenre();
        if (topGenre == null) return;   // caller handles the no-history path

        List<Book> genreBooks = catalogue.searchByGenre(topGenre);
        List<Book> unread     = new ArrayList<>();
        for (Book b : genreBooks) {
            if (!user.readHistory.contains(b.isbn)) unread.add(b);
        }

        if (unread.isEmpty()) {
            System.out.println("  You've read all \"" + topGenre +
                "\" books in the catalogue! Try a new genre.");
            return;
        }

        unread.sort((a, b) -> b.borrowCount - a.borrowCount);
        int limit = Math.min(3, unread.size());

        System.out.printf("  Based on your most-borrowed genre: %s%n", topGenre);
        System.out.println("  " + "-".repeat(110));
        for (int i = 0; i < limit; i++)
            System.out.printf("  #%-2d %s%n", i + 1, unread.get(i));
        System.out.println("  " + "-".repeat(110));
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

    private void processWaitlist(Book book, Copy copy) {
        while (!book.waitlist.isEmpty()) {
            String nextId   = book.waitlist.peek();
            User   nextUser = users.get(nextId);

            if (nextUser == null) { book.waitlist.poll(); continue; }
            if (!nextUser.canBorrow()) {
                book.waitlist.poll();
                System.out.println("  Waitlist user " + nextId + " has reached borrow limit – skipped.");
                continue;
            }

            book.waitlist.poll();
            copy.borrowerId  = nextId;
            copy.borrowDate  = LocalDate.now();
            copy.dueDate     = LocalDate.now().plusDays(LOAN_DAYS);
            copy.renewedOnce = false;
            nextUser.borrowedKeys.add(book.isbn + ":" + copy.copyId);

            // Analytics tracking
            book.borrowCount++;
            nextUser.genreCounts.merge(book.genre, 1, Integer::sum);
            nextUser.readHistory.add(book.isbn);

            history.push(nextId, book.title, book.isbn, copy.copyId, LocalDate.now());
            System.out.printf("  Auto-assigned to waitlist user: %s (%s) | Due: %s%n",
                nextId, nextUser.name, copy.dueDate);
            break;
        }
    }

    /** Shows the top 3 most-borrowed books in a specific genre (no read filter). */
    private void showTopBooksByGenre(String genre) {
        List<Book> books = catalogue.searchByGenre(genre);
        if (books.isEmpty()) {
            System.out.println("  No books found for genre: \"" + genre + "\".");
            return;
        }
        books.sort((a, b) -> b.borrowCount - a.borrowCount);
        int limit = Math.min(3, books.size());
        System.out.printf("  Top %d book(s) in \"%s\":%n", limit, genre);
        System.out.println("  " + "-".repeat(110));
        for (int i = 0; i < limit; i++)
            System.out.printf("  #%-2d %s%n", i + 1, books.get(i));
        System.out.println("  " + "-".repeat(110));
    }

    /** Prints the genre list and returns the user's choice. */
    private String pickGenre(Scanner sc) {
        System.out.println("  Select Genre:");
        for (int i = 0; i < GENRES.length; i++) {
            System.out.printf("    %2d. %-15s", i + 1, GENRES[i]);
            if ((i + 1) % 3 == 0) System.out.println();
        }
        if (GENRES.length % 3 != 0) System.out.println();
        System.out.print("  Choice (1-" + GENRES.length + "): ");
        int g = readInt(sc);
        if (g < 1 || g > GENRES.length) {
            System.out.println("  Invalid choice. Defaulting to 'Uncategorized'.");
            return "Uncategorized";
        }
        return GENRES[g - 1];
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
         * New format:
         *   BOOK | isbn | title | author | location | genre | borrowCount
         *   COPY | copyId | borrowerId | borrowDate | dueDate | renewedOnce
         * Old format (backward compatible, no genre/borrowCount):
         *   BOOK | isbn | title | author | location
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
                    int    isbn        = Integer.parseInt(p[1].trim());
                    String title       = p[2].trim();
                    String author      = p[3].trim();
                    String location    = p[4].trim();
                    String genre       = (p.length >= 6) ? p[5].trim() : "Uncategorized";
                    int    borrowCount = 0;
                    if (p.length >= 7) {
                        try { borrowCount = Integer.parseInt(p[6].trim()); }
                        catch (NumberFormatException ignored) {}
                    }
                    Book book = new Book(isbn, title, author, location, genre);
                    book.borrowCount = borrowCount;
                    catalogue.insert(book);
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
            System.out.println("  Loaded " + count + " book(s) from " + LIBRARY_FILE);
        } catch (IOException e) {
            System.out.println("  Error loading library: " + e.getMessage());
        }
    }

    private void loadUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) return;
        /*
         * Format:
         *   userId | name | borrowedKeys | genreCounts | readHistory
         *   e.g.  25006805 | Alice | 101:1 | Classic:2,Sci-Fi:1 | 101,103
         * Older lines with fewer fields are handled gracefully.
         */
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int    count = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // Use limit -1 to keep trailing empty fields
                String[] p = line.split(" \\| ", -1);
                if (p.length < 2) continue;

                String uid  = p[0].trim();
                // strip any stray pipe chars that may have corrupted the name
                String name = p[1].replaceAll("\\|", "").trim();
                User   user = new User(uid, name);

                // field 3: borrowedKeys (isbn:copyId, validated)
                if (p.length >= 3 && !p[2].trim().isEmpty()) {
                    for (String key : p[2].trim().split(",")) {
                        String k = key.trim();
                        if (k.matches("\\d+:\\d+")) user.borrowedKeys.add(k);
                    }
                }

                // field 4: genreCounts (genre:count,...)
                if (p.length >= 4 && !p[3].trim().isEmpty()) {
                    for (String entry : p[3].trim().split(",")) {
                        int colon = entry.lastIndexOf(':');
                        if (colon > 0 && colon < entry.length() - 1) {
                            String genre = entry.substring(0, colon).trim();
                            try {
                                int cnt = Integer.parseInt(entry.substring(colon + 1).trim());
                                if (!genre.isEmpty()) user.genreCounts.put(genre, cnt);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                // field 5: readHistory (isbn,isbn,...)
                if (p.length >= 5 && !p[4].trim().isEmpty()) {
                    for (String isbnStr : p[4].trim().split(",")) {
                        try { user.readHistory.add(Integer.parseInt(isbnStr.trim())); }
                        catch (NumberFormatException ignored) {}
                    }
                }

                users.put(uid, user);
                count++;
            }
            System.out.println("  Loaded " + count + " user(s) from " + USERS_FILE);
        } catch (IOException e) {
            System.out.println("  Error loading users: " + e.getMessage());
        }
    }

    private void loadReservations() {
        File file = new File(RESERVATIONS_FILE);
        if (!file.exists()) return;
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
            System.out.println("  Reservations loaded from " + RESERVATIONS_FILE);
        } catch (IOException e) {
            System.out.println("  Error loading reservations: " + e.getMessage());
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
                pw.println("BOOK | " + b.isbn + " | " + b.title + " | " + b.author +
                    " | " + b.location + " | " + b.genre + " | " + b.borrowCount);
                for (Copy c : b.copies) {
                    pw.println("COPY | " + c.copyId + " | " + c.borrowerId +
                        " | " + c.borrowDate + " | " + c.dueDate + " | " + c.renewedOnce);
                }
            }
            System.out.println("  Library saved (" + allBooks.size() + " book(s)).");
        } catch (IOException e) {
            System.out.println("  Error saving library: " + e.getMessage());
        }
    }

    private void saveUsers() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(USERS_FILE))) {
            for (User u : users.values()) {
                // field 3: borrowed keys
                String borrowedKeysStr = String.join(",", u.borrowedKeys);

                // field 4: genre counts
                StringBuilder gcSb = new StringBuilder();
                for (Map.Entry<String, Integer> e : u.genreCounts.entrySet()) {
                    if (gcSb.length() > 0) gcSb.append(",");
                    gcSb.append(e.getKey()).append(":").append(e.getValue());
                }

                // field 5: read history
                StringBuilder rhSb = new StringBuilder();
                for (int isbn : u.readHistory) {
                    if (rhSb.length() > 0) rhSb.append(",");
                    rhSb.append(isbn);
                }

                pw.println(u.userId + " | " + u.name + " | " + borrowedKeysStr +
                    " | " + gcSb + " | " + rhSb);
            }
            System.out.println("  Users saved (" + users.size() + " user(s)).");
        } catch (IOException e) {
            System.out.println("  Error saving users: " + e.getMessage());
        }
    }

    private void saveReservations() {
        List<Book> allBooks = new ArrayList<>();
        catalogue.getAllBooks(allBooks);
        try (PrintWriter pw = new PrintWriter(new FileWriter(RESERVATIONS_FILE))) {
            for (Book b : allBooks) {
                if (!b.waitlist.isEmpty())
                    pw.println(b.isbn + " | " + String.join(",", b.waitlist));
            }
            System.out.println("  Reservations saved.");
        } catch (IOException e) {
            System.out.println("  Error saving reservations: " + e.getMessage());
        }
    }

    // ===========================================================
    // Console Menu
    // ===========================================================

    public void runMenu() {
        Scanner sc = new Scanner(System.in);

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║          SMART LIBRARY SYSTEM  v4.0              ║");
        System.out.println("║  BST | HashMap | Queue | Analytics               ║");
        System.out.println("╚══════════════════════════════════════════════════╝");

        loadDatabase();

        while (true) {
            printMenu();
            System.out.print("Enter your choice: ");

            if (!sc.hasNextInt()) {
                System.out.println("  Please enter a number (1-12).");
                sc.next();
                continue;
            }

            int choice = sc.nextInt();
            sc.nextLine();

            if (choice == 12) {
                System.out.print("\n  Save changes before exit? (Y/N): ");
                String yn = sc.nextLine().trim();
                if (yn.equalsIgnoreCase("Y")) {
                    saveDatabase();
                }
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
        System.out.println("│  [Analytics]                             │");
        System.out.println("│   9. Display Hottest Books (Top 5)       │");
        System.out.println("│  10. Recommend Books                     │");
        System.out.println("│  [History]                               │");
        System.out.println("│  11. View Borrowing History              │");
        System.out.println("│  12. Exit                                │");
        System.out.println("└──────────────────────────────────────────┘");
    }

    private void handleChoice(int choice, Scanner sc) {
        switch (choice) {

            case 1: {
                System.out.print("  Enter User ID  : ");
                String uid = sc.nextLine().trim();
                System.out.print("  Enter Full Name: ");
                String name = sc.nextLine().trim();
                registerUser(uid, name);
                break;
            }

            case 2: {
                System.out.print("  Enter ISBN (number): ");
                int isbn = readInt(sc);
                if (catalogue.search(isbn) != null) {
                    // existing ISBN — just add copies, no need to re-enter metadata
                    System.out.print("  Enter Quantity to add: ");
                    int qty = readInt(sc);
                    addBook(isbn, "", "", "", "", qty);
                } else {
                    // new book — full registration
                    System.out.print("  Enter Title   : ");
                    String title = sc.nextLine().trim();
                    System.out.print("  Enter Author  : ");
                    String author = sc.nextLine().trim();
                    System.out.print("  Enter Location (e.g. Aisle 1 / Shelf A): ");
                    String location = sc.nextLine().trim();
                    String genre = pickGenre(sc);
                    System.out.print("  Enter Quantity: ");
                    int qty = readInt(sc);
                    addBook(isbn, title, author, location, genre, qty);
                }
                break;
            }

            case 3: {
                System.out.print("  Enter ISBN to remove: ");
                removeBook(readInt(sc));
                break;
            }

            case 4: {
                System.out.println("  Search by:  1. ISBN   2. Title   3. Author");
                System.out.print("  Choice: ");
                int t = readInt(sc);
                String type = (t == 1) ? "isbn" : (t == 2) ? "title" : "author";
                System.out.print("  Enter search term: ");
                findBook(sc.nextLine().trim(), type);
                break;
            }

            case 5: {
                System.out.print("  Enter User ID: ");
                String uid = sc.nextLine().trim();
                if (!users.containsKey(uid)) {
                    System.out.println("  User not found: " + uid);
                    break;
                }
                cartMenu(uid, sc);
                break;
            }

            case 6: {
                System.out.print("  Enter User ID : ");
                String uid = sc.nextLine().trim();
                System.out.print("  Enter ISBN    : ");
                returnBook(uid, readInt(sc));
                break;
            }

            case 7: {
                System.out.print("  Enter User ID : ");
                String uid = sc.nextLine().trim();
                System.out.print("  Enter ISBN    : ");
                renewBook(uid, readInt(sc));
                break;
            }

            case 8: {
                System.out.print("  Enter User ID : ");
                String uid = sc.nextLine().trim();
                System.out.print("  Enter ISBN    : ");
                joinWaitlist(uid, readInt(sc));
                break;
            }

            case 9: {
                displayHottestBooks();
                break;
            }

            case 10: {
                System.out.print("  Enter User ID: ");
                String uid = sc.nextLine().trim();
                User   user = users.get(uid);
                if (user == null) {
                    System.out.println("  User not found: " + uid);
                    break;
                }
                if (user.getTopGenre() == null) {
                    // no borrow history — let them pick a genre manually
                    System.out.println("  No borrowing history found. Showing top books by your chosen genre:");
                    showTopBooksByGenre(pickGenre(sc));
                } else {
                    recommendBooks(uid);
                }
                break;
            }

            case 11: {
                viewHistory();
                break;
            }

            default:
                System.out.println("  Invalid option. Please choose 1-12.");
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
                case "a": System.out.print("    Enter ISBN: "); addToCart(userId, readInt(sc)); break;
                case "b": System.out.print("    Enter ISBN to remove: "); removeFromCart(userId, readInt(sc)); break;
                case "c": viewCart(userId); break;
                case "d": checkout(userId); break;
                case "e": return;
                default:  System.out.println("    Enter a, b, c, d, or e.");
            }
        }
    }

    private int readInt(Scanner sc) {
        while (!sc.hasNextInt()) {
            System.out.print("  Please enter a number: ");
            sc.next();
        }
        int v = sc.nextInt();
        sc.nextLine();
        return v;
    }
}
