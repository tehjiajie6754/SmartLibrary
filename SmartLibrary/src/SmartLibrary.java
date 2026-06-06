import java.io.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.Locale;

/**
 * ============================================================
 * TASK 5 – Admin Logic + Console Interface
 * ============================================================
 * Implements LibraryADT and wires together:
 * BST catalogue (primary + HashMap secondary indexes)
 * BorrowStack (history, LIFO)
 * User map (member registry)
 * Cart map (per-user staging area)
 * Reservation (Queue<String> inside each Book node)
 *
 * Persistence (3 separate files):
 * library.txt – books + copies
 * users.txt – members + borrowed-key list + genre stats
 * reservations.txt – per-ISBN waitlists
 * ============================================================
 */
public class SmartLibrary implements LibraryADT {

    // --- File names ---
    private static final String LIBRARY_FILE = "library.txt";
    private static final String USERS_FILE = "users.txt";
    private static final String RESERVATIONS_FILE = "reservations.txt";
    private static final String HISTORY_FILE = "history.txt";
    private static final String FINES_FILE = "fines.txt";

    // --- Fine event log (date + amount generated on return) ---
    private static class FineEvent {
        LocalDate date;
        double amount;

        FineEvent(LocalDate date, double amount) {
            this.date = date;
            this.amount = amount;
        }
    }

    private final List<FineEvent> fineLog = new ArrayList<>();

    // --- Business rules ---
    private static final int LOAN_DAYS = 7;
    private static final double FINE_PER_DAY = 1.0; // RM 1 / day
    private static final String ADMIN_PIN = "1234";

    // --- Predefined genre list ---
    private static final String[] GENRES = {
            "Sci-Fi", "Fantasy", "Mystery", "Romance", "Classic",
            "Non-Fiction", "Horror", "Thriller", "History", "Philosophy",
            "Religion", "Biography"
    };

    // --- Data structures ---
    private final BookBST catalogue = new BookBST();
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Cart> carts = new HashMap<>();
    private final BorrowStack history = new BorrowStack();

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

    @Override
    public void removeMember(String userId) {
        User user = users.get(userId);
        if (user == null) {
            System.out.println("  Member not found: " + userId);
            return;
        }
        if (!user.borrowedKeys.isEmpty()) {
            System.out.println("  Cannot remove \"" + user.name + "\" — " +
                    user.borrowedKeys.size() + " active loan(s). Return all books first.");
            return;
        }
        // Remove from all waitlists
        List<Book> allBooks = new ArrayList<>();
        catalogue.getAllBooks(allBooks);
        for (Book b : allBooks)
            b.waitlist.remove(userId);
        carts.remove(userId);
        users.remove(userId);
        System.out.println("  Member removed: " + userId + " (" + user.name + ")");
    }

    @Override
    public void listAllMembers() {
        if (users.isEmpty()) {
            System.out.println("  No members registered.");
            return;
        }
        List<User> all = new ArrayList<>(users.values());
        all.sort((a, b) -> a.userId.compareTo(b.userId));
        System.out.printf("  --- Member List (%d member(s)) ---%n", all.size());
        System.out.println("  " + "-".repeat(35));
        System.out.printf("  %-15s  %s%n", "User ID", "Name");
        System.out.println("  " + "-".repeat(35));
        for (User u : all)
            System.out.printf("  %-15s  %s%n", u.userId, u.name);
        System.out.println("  " + "-".repeat(35));
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
            for (int i = 0; i < quantity; i++)
                existing.addCopy(new Copy(nextId + i));
            System.out.printf("  Added %d copy/copies to ISBN %d. Total copies: %d%n",
                    quantity, isbn, existing.totalCopies());
            return;
        }
        Book book = new Book(isbn, title, author, location, genre);
        for (int i = 1; i <= quantity; i++)
            book.addCopy(new Copy(i));
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

    @Override
    public void updateBook(int isbn, String newTitle, String newAuthor,
            String newLocation, String newGenre) {
        Book book = catalogue.search(isbn);
        if (book == null) {
            System.out.println("  Book not found: ISBN " + isbn);
            return;
        }
        String oldTitle = book.title;
        String oldAuthor = book.author;
        String oldGenre = book.genre;
        book.title = newTitle;
        book.author = newAuthor;
        book.location = newLocation;
        book.genre = newGenre;
        catalogue.reindex(book, oldTitle, oldAuthor, oldGenre);
        System.out.println("  Book updated: ISBN " + isbn);
        System.out.println("    Title   : " + book.title);
        System.out.println("    Author  : " + book.author);
        System.out.println("    Location: " + book.location);
        System.out.println("    Genre   : " + book.genre);
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
                    if (b != null)
                        results.add(b);
                } catch (NumberFormatException e) {
                    System.out.println("  ISBN must be a number.");
                    return;
                }
                break;
            case "title":
                results = catalogue.searchByTitle(query);
                break;
            case "author":
                results = catalogue.searchByAuthor(query);
                break;
            case "genre":
                results = catalogue.searchByGenre(query);
                break;
            default:
                System.out.println("  Unknown search type: " + searchType);
                return;
        }

        // Sort by popularity (most borrowed first) for multi-result searches
        if (!searchType.equalsIgnoreCase("isbn")) {
            results.sort((a, b) -> b.borrowCount - a.borrowCount);
        }

        if (results.isEmpty()) {
            System.out.println("  No results found for \"" + query + "\".");
            return;
        }

        System.out.println("  Found " + results.size() + " result(s):");
        System.out.println("  " + "-".repeat(144));
        for (Book b : results) {
            System.out.println("  " + b);
            if (!b.waitlist.isEmpty())
                System.out.println("    Waitlist: " + b.waitlist.size() + " user(s) waiting");
        }
        System.out.println("  " + "-".repeat(144));
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
        if (cart == null || cart.isEmpty()) {
            System.out.println("  Cart is empty.");
            return;
        }
        if (cart.remove(isbn)) {
            System.out.println("  Removed ISBN " + isbn + " from cart.");
        } else {
            System.out.println("  ISBN " + isbn + " was not in your cart.");
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
            Book b = catalogue.search(isbn);
            System.out.println("  " + i++ + ". ISBN " + isbn + " | " +
                    ((b != null) ? "\"" + b.title + "\"" : "(book not found)"));
        }
    }

    @Override
    public void checkout(String userId) {
        User user = users.get(userId);
        if (user == null) {
            System.out.println("  User not found: " + userId);
            return;
        }
        Cart cart = carts.get(userId);
        if (cart == null || cart.isEmpty()) {
            System.out.println("  Cart is empty.");
            return;
        }

        List<Integer> items = new ArrayList<>(cart.getItems());
        boolean anySuccess = false;

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

            copy.borrowerId = userId;
            copy.borrowDate = LocalDate.now();
            copy.dueDate = LocalDate.now().plusDays(LOAN_DAYS);
            copy.renewedOnce = false;
            user.borrowedKeys.add(isbn + ":" + copy.copyId);
            cart.remove(isbn);

            // Analytics tracking
            book.borrowCount++;
            user.genreCounts.merge(book.genre, 1, Integer::sum);
            user.readHistory.add(isbn);

            history.push(userId, book.title, isbn, copy.copyId, LocalDate.now(), book.genre);
            System.out.printf("  Borrowed: \"%s\" (Copy #%d) | Due: %s%n",
                    book.title, copy.copyId, copy.dueDate);
            anySuccess = true;
        }

        if (!anySuccess)
            System.out.println("  No books were borrowed in this session.");
    }

    // ===========================================================
    // LibraryADT – Borrowing lifecycle
    // ===========================================================

    @Override
    public void returnBook(String userId, int isbn) {
        User user = users.get(userId);
        if (user == null) {
            System.out.println("  User not found: " + userId);
            return;
        }
        Book book = catalogue.search(isbn);
        if (book == null) {
            System.out.println("  Book not found: ISBN " + isbn);
            return;
        }
        Copy copy = book.getCopyBorrowedBy(userId);
        if (copy == null) {
            System.out.println("  " + userId + " does not have a copy of \"" + book.title + "\".");
            return;
        }

        long overdueDays = copy.daysOverdue();
        double fine = overdueDays * FINE_PER_DAY;
        if (fine > 0) {
            System.out.printf("  Overdue by %d day(s). Fine payable: RM %.2f%n", overdueDays, fine);
            user.outstandingFine += fine;
            fineLog.add(new FineEvent(LocalDate.now(), fine));
            System.out.printf("  Total outstanding fine for %s: RM %.2f%n", userId, user.outstandingFine);
        }

        user.borrowedKeys.remove(isbn + ":" + copy.copyId);
        copy.borrowerId = null;
        copy.borrowDate = null;
        copy.dueDate = null;
        copy.renewedOnce = false;
        System.out.println("  Returned: \"" + book.title + "\" (Copy #" + copy.copyId + ")");

        processWaitlist(book, copy);
    }

    @Override
    public void renewBook(String userId, int isbn) {
        User user = users.get(userId);
        if (user == null) {
            System.out.println("  User not found: " + userId);
            return;
        }
        Book book = catalogue.search(isbn);
        if (book == null) {
            System.out.println("  Book not found: ISBN " + isbn);
            return;
        }
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

        copy.borrowDate = LocalDate.now();
        copy.dueDate = LocalDate.now().plusDays(LOAN_DAYS);
        copy.renewedOnce = true;
        System.out.printf("  Renewed: \"%s\" | New due date: %s%n", book.title, copy.dueDate);
    }

    // ===========================================================
    // LibraryADT – Reservation queue
    // ===========================================================

    @Override
    public void joinWaitlist(String userId, int isbn) {
        if (!users.containsKey(userId)) {
            System.out.println("  User not found: " + userId);
            return;
        }
        Book book = catalogue.search(isbn);
        if (book == null) {
            System.out.println("  Book not found: ISBN " + isbn);
            return;
        }
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

    @Override
    public void leaveWaitlist(String userId, int isbn) {
        if (!users.containsKey(userId)) {
            System.out.println("  User not found: " + userId);
            return;
        }
        Book book = catalogue.search(isbn);
        if (book == null) {
            System.out.println("  Book not found: ISBN " + isbn);
            return;
        }
        if (!book.waitlist.contains(userId)) {
            System.out.printf("  %s is not in the waitlist for \"%s\".%n", userId, book.title);
            return;
        }
        book.waitlist.remove(userId);
        System.out.printf("  %s removed from the waitlist for \"%s\".%n", userId, book.title);
        if (!book.waitlist.isEmpty())
            System.out.println("  Remaining queue size: " + book.waitlist.size());
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
        System.out.println("  " + "-".repeat(144));
        int rank = 1;
        for (Book b : all) {
            if (rank > 5 || b.borrowCount == 0)
                break;
            System.out.printf("  #%-2d (%2d borrow(s))  %s%n", rank++, b.borrowCount, b);
        }
        System.out.println("  " + "-".repeat(144));
    }

    @Override
    public void recommendBooks(String userId) {
        User user = users.get(userId);
        if (user == null) {
            System.out.println("  User not found: " + userId);
            return;
        }

        String topGenre = user.getTopGenre();
        if (topGenre == null)
            return; // caller handles the no-history path

        List<Book> genreBooks = catalogue.searchByGenre(topGenre);
        List<Book> unread = new ArrayList<>();
        for (Book b : genreBooks) {
            if (!user.readHistory.contains(b.isbn))
                unread.add(b);
        }

        if (unread.isEmpty()) {
            System.out.println("  You've read all \"" + topGenre +
                    "\" books in the catalogue! Try a new genre.");
            return;
        }

        unread.sort((a, b) -> b.borrowCount - a.borrowCount);
        int limit = Math.min(3, unread.size());

        System.out.printf("  Based on your most-borrowed genre: %s%n", topGenre);
        System.out.println("  " + "-".repeat(144));
        for (int i = 0; i < limit; i++)
            System.out.printf("  #%-2d %s%n", i + 1, unread.get(i));
        System.out.println("  " + "-".repeat(144));
    }

    // ===========================================================
    // LibraryADT – Catalogue browsing
    // ===========================================================

    @Override
    public void listAllBooks() {
        List<Book> all = new ArrayList<>();
        catalogue.getAllBooks(all);
        if (all.isEmpty()) {
            System.out.println("  Catalogue is empty.");
            return;
        }
        final int SEP = 131;
        System.out.printf("  --- Catalogue (%d book(s), sorted by ISBN) ---%n", all.size());
        System.out.println("  " + "-".repeat(SEP));
        System.out.printf("  %-6s  %-36s  %-20s  %-12s  %-17s  %-7s  %-7s  %-10s%n",
                "ISBN", "Title", "Author", "Genre", "Location", "Avail", "Borrows", "Rating");
        System.out.println("  " + "-".repeat(SEP));
        for (Book b : all) {
            String t = b.title.length()  > 36 ? b.title.substring(0, 35)  + "~" : b.title;
            String a = b.author.length() > 20 ? b.author.substring(0, 19) + "~" : b.author;
            String avail  = b.availableCount() + "/" + b.totalCopies();
            String rating = b.ratingCount() == 0 ? "No ratings"
                    : String.format("%.1f/5 (%d)", b.averageRating(), b.ratingCount());
            System.out.printf("  %-6d  %-36s  %-20s  %-12s  %-17s  %-7s  %-7d  %s%n",
                    b.isbn, t, a, b.genre, b.location, avail, b.borrowCount, rating);
            if (!b.waitlist.isEmpty())
                System.out.printf("  %8s  >> Waitlist: %d user(s) waiting%n",
                        "", b.waitlist.size());
        }
        System.out.println("  " + "-".repeat(SEP));
    }

    @Override
    public void displayOverdueReport() {
        List<Book> all = new ArrayList<>();
        catalogue.getAllBooks(all);

        List<String[]> entries = new ArrayList<>();
        for (Book b : all) {
            for (Copy c : b.copies) {
                long days = c.daysOverdue();
                if (days > 0) {
                    entries.add(new String[] {
                            b.title, String.valueOf(b.isbn), String.valueOf(c.copyId),
                            c.borrowerId, c.dueDate.toString(),
                            String.valueOf(days), String.format("%.2f", days * FINE_PER_DAY)
                    });
                }
            }
        }

        if (entries.isEmpty()) {
            System.out.println("  No overdue books at this time.");
            return;
        }

        // Sort by days overdue descending (most overdue first)
        entries.sort((a, b) -> Long.compare(Long.parseLong(b[5]), Long.parseLong(a[5])));

        System.out.printf("  --- Overdue Report (%d overdue copy/copies) ---%n", entries.size());
        System.out.println("  " + "-".repeat(110));
        System.out.printf("  %-30s %-8s %-6s %-12s %-12s %-8s %s%n",
                "Title", "ISBN", "Copy#", "Borrower", "Due Date", "Days", "Fine (RM)");
        System.out.println("  " + "-".repeat(110));
        for (String[] e : entries) {
            String displayTitle = e[0].length() > 29 ? e[0].substring(0, 28) + "…" : e[0];
            System.out.printf("  %-30s %-8s %-6s %-12s %-12s %-8s %s%n",
                    displayTitle, e[1], e[2], e[3], e[4], e[5], e[6]);
        }
        System.out.println("  " + "-".repeat(110));

        double total = 0;
        for (String[] e : entries)
            total += Double.parseDouble(e[6]);
        System.out.printf("  Total outstanding fines: RM %.2f%n", total);
    }

    // ===========================================================
    // LibraryADT – Ratings
    // ===========================================================

    @Override
    public void rateBook(String userId, int isbn, int stars) {
        Book book = catalogue.search(isbn);
        if (book == null || !users.containsKey(userId))
            return;
        if (stars < 1 || stars > 5) {
            System.out.println("  Rating must be 1-5.");
            return;
        }
        book.ratings.put(userId, stars);
        System.out.printf("  Rating saved: %d star(s) for \"%s\" — new average: %.1f/5 (%d rating(s))%n",
                stars, book.title, book.averageRating(), book.ratingCount());
    }

    // ===========================================================
    // LibraryADT – Monthly Report
    // ===========================================================

    @Override
    public void viewMonthlyReport(int year, int month) {
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        int totalBorrows = history.countBorrowsInMonth(year, month);
        Map<String, Integer> genreCounts = history.genreCountsForMonth(year, month);

        double finesGenerated = 0;
        for (FineEvent e : fineLog) {
            if (e.date.getYear() == year && e.date.getMonthValue() == month)
                finesGenerated += e.amount;
        }

        System.out.printf("  --- Monthly Activity Report: %s %d ---%n", monthName, year);
        System.out.println("  " + "-".repeat(50));
        System.out.printf("  Total borrows       : %d%n", totalBorrows);
        System.out.printf("  Total fines generated: RM %.2f%n", finesGenerated);
        System.out.println();
        System.out.println("  Genre Popularity (this month):");
        if (genreCounts.isEmpty()) {
            System.out.println("    (no borrow data for this period)");
        } else {
            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(genreCounts.entrySet());
            sorted.sort((a, b) -> b.getValue() - a.getValue());
            for (int i = 0; i < sorted.size(); i++)
                System.out.printf("    #%-2d %-15s : %d borrow(s)%n",
                        i + 1, sorted.get(i).getKey(), sorted.get(i).getValue());
        }
        System.out.println("  " + "-".repeat(50));
    }

    // ===========================================================
    // LibraryADT – Profile
    // ===========================================================

    @Override
    public void viewUserProfile(String userId) {
        User user = users.get(userId);
        if (user == null) {
            System.out.println("  User not found: " + userId);
            return;
        }

        System.out.println("  ╔══════════════════════════════════════════════╗");
        System.out.printf("  ║  User Profile: %-34s║%n", userId);
        System.out.println("  ╚══════════════════════════════════════════════╝");
        System.out.printf("  Name            : %s%n", user.name);
        System.out.printf("  Loans           : %d / %d slot(s) used%n", user.borrowedCount(), User.MAX_BORROW);
        System.out.printf("  Outstanding Fine: RM %.2f%n", user.outstandingFine);

        // --- Section 1: Currently borrowed ---
        System.out.println();
        System.out.println("  --- Currently Borrowed ---");
        if (user.borrowedKeys.isEmpty()) {
            System.out.println("  (none)");
        } else {
            for (String key : user.borrowedKeys) {
                String[] parts = key.split(":");
                int isbn = Integer.parseInt(parts[0]);
                int copyId = Integer.parseInt(parts[1]);
                Book book = catalogue.search(isbn);
                if (book == null) {
                    System.out.println("  ISBN " + isbn + " (removed from catalogue)");
                    continue;
                }
                Copy copy = null;
                for (Copy c : book.copies) {
                    if (c.copyId == copyId) {
                        copy = c;
                        break;
                    }
                }
                if (copy == null)
                    continue;
                long overdue = copy.daysOverdue();
                String status = overdue > 0
                        ? String.format("OVERDUE by %d day(s)  |  Fine: RM %.2f", overdue, overdue * FINE_PER_DAY)
                        : "On time";
                System.out.printf("  - \"%s\" (Copy #%d)  |  Due: %s  |  %s%n",
                        book.title, copyId, copy.dueDate, status);
            }
        }

        // --- Section 2: Genre stats ---
        System.out.println();
        System.out.println("  --- Genre Reading Stats ---");
        if (user.genreCounts.isEmpty()) {
            System.out.println("  (no borrowing history)");
        } else {
            List<Map.Entry<String, Integer>> genres = new ArrayList<>(user.genreCounts.entrySet());
            genres.sort((a, b) -> b.getValue() - a.getValue());
            String topGenre = genres.get(0).getKey();
            for (Map.Entry<String, Integer> e : genres)
                System.out.printf("  %-15s : %d book(s)%s%n",
                        e.getKey(), e.getValue(),
                        e.getKey().equals(topGenre) ? "  <-- top genre" : "");
        }

        // --- Section 3: Read history ---
        System.out.println();
        System.out.printf("  --- Read History (%d book(s) total) ---%n", user.readHistory.size());
        if (user.readHistory.isEmpty()) {
            System.out.println("  (none)");
        } else {
            List<Integer> sorted = new ArrayList<>(user.readHistory);
            Collections.sort(sorted);
            for (int isbn : sorted) {
                Book b = catalogue.search(isbn);
                String info = (b != null) ? "\"" + b.title + "\" by " + b.author : "(removed from catalogue)";
                System.out.println("  - ISBN " + isbn + "  |  " + info);
            }
        }

        // --- Section 4: Active waitlists ---
        System.out.println();
        System.out.println("  --- Waitlists ---");
        List<Book> allBooks = new ArrayList<>();
        catalogue.getAllBooks(allBooks);
        boolean onAny = false;
        for (Book b : allBooks) {
            if (b.waitlist.contains(userId)) {
                List<String> wl = new ArrayList<>(b.waitlist);
                int pos = wl.indexOf(userId) + 1;
                System.out.printf("  - \"%s\" (ISBN %d)  |  Queue position: %d of %d%n",
                        b.title, b.isbn, pos, b.waitlist.size());
                onAny = true;
            }
        }
        if (!onAny)
            System.out.println("  (not on any waitlist)");
    }

    // ===========================================================
    // LibraryADT – History
    // ===========================================================

    @Override
    public void viewHistory() {
        history.show();
    }

    @Override
    public void viewPersonalHistory(String userId) {
        history.showForUser(userId);
    }

    // ===========================================================
    // Private helpers
    // ===========================================================

    private void processWaitlist(Book book, Copy copy) {
        while (!book.waitlist.isEmpty()) {
            String nextId = book.waitlist.peek();
            User nextUser = users.get(nextId);

            if (nextUser == null) {
                book.waitlist.poll();
                continue;
            }
            if (!nextUser.canBorrow()) {
                book.waitlist.poll();
                System.out.println("  Waitlist user " + nextId + " has reached borrow limit – skipped.");
                continue;
            }

            book.waitlist.poll();
            copy.borrowerId = nextId;
            copy.borrowDate = LocalDate.now();
            copy.dueDate = LocalDate.now().plusDays(LOAN_DAYS);
            copy.renewedOnce = false;
            nextUser.borrowedKeys.add(book.isbn + ":" + copy.copyId);

            // Analytics tracking
            book.borrowCount++;
            nextUser.genreCounts.merge(book.genre, 1, Integer::sum);
            nextUser.readHistory.add(book.isbn);

            history.push(nextId, book.title, book.isbn, copy.copyId, LocalDate.now(), book.genre);
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
        System.out.println("  " + "-".repeat(144));
        for (int i = 0; i < limit; i++)
            System.out.printf("  #%-2d %s%n", i + 1, books.get(i));
        System.out.println("  " + "-".repeat(144));
    }

    /** Prints the genre list and returns the user's choice. */
    private String pickGenre(Scanner sc) {
        System.out.println("  Select Genre:");
        for (int i = 0; i < GENRES.length; i++) {
            System.out.printf("    %2d. %-15s", i + 1, GENRES[i]);
            if ((i + 1) % 3 == 0)
                System.out.println();
        }
        if (GENRES.length % 3 != 0)
            System.out.println();
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
        loadHistory();
        loadFines();
    }

    private void loadLibrary() {
        File file = new File(LIBRARY_FILE);
        if (!file.exists()) {
            System.out.println("  (No library database found – starting fresh.)");
            return;
        }
        /*
         * New format:
         * BOOK | isbn | title | author | location | genre | borrowCount
         * COPY | copyId | borrowerId | borrowDate | dueDate | renewedOnce
         * Old format (backward compatible, no genre/borrowCount):
         * BOOK | isbn | title | author | location
         */
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            Book currentBook = null;
            int count = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                String[] p = line.split(" \\| ");
                if (p[0].equals("BOOK") && p.length >= 5) {
                    int isbn = Integer.parseInt(p[1].trim());
                    String title = p[2].trim();
                    String author = p[3].trim();
                    String location = p[4].trim();
                    String genre = (p.length >= 6) ? p[5].trim() : "Uncategorized";
                    int borrowCount = 0;
                    if (p.length >= 7) {
                        try {
                            borrowCount = Integer.parseInt(p[6].trim());
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    Book book = new Book(isbn, title, author, location, genre);
                    book.borrowCount = borrowCount;
                    catalogue.insert(book);
                    currentBook = catalogue.search(isbn);
                    count++;
                } else if (p[0].equals("RATING") && p.length >= 3 && currentBook != null) {
                    try {
                        currentBook.ratings.put(p[1].trim(), Integer.parseInt(p[2].trim()));
                    } catch (NumberFormatException ignored) {
                    }
                } else if (p[0].equals("COPY") && p.length >= 6 && currentBook != null) {
                    int copyId = Integer.parseInt(p[1].trim());
                    String borrowerId = p[2].trim().equals("null") ? null : p[2].trim();
                    LocalDate borrowDate = p[3].trim().equals("null") ? null : LocalDate.parse(p[3].trim());
                    LocalDate dueDate = p[4].trim().equals("null") ? null : LocalDate.parse(p[4].trim());
                    boolean renewedOnce = Boolean.parseBoolean(p[5].trim());
                    Copy copy = new Copy(copyId);
                    copy.borrowerId = borrowerId;
                    copy.borrowDate = borrowDate;
                    copy.dueDate = dueDate;
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
        if (!file.exists())
            return;
        /*
         * Format:
         * userId | name | borrowedKeys | genreCounts | readHistory
         * e.g. 25006805 | Alice | 101:1 | Classic:2,Sci-Fi:1 | 101,103
         * Older lines with fewer fields are handled gracefully.
         */
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                // Use limit -1 to keep trailing empty fields
                String[] p = line.split(" \\| ", -1);
                if (p.length < 2)
                    continue;

                String uid = p[0].trim();
                // strip any stray pipe chars that may have corrupted the name
                String name = p[1].replaceAll("\\|", "").trim();
                User user = new User(uid, name);

                // field 3: borrowedKeys (isbn:copyId, validated)
                if (p.length >= 3 && !p[2].trim().isEmpty()) {
                    for (String key : p[2].trim().split(",")) {
                        String k = key.trim();
                        if (k.matches("\\d+:\\d+"))
                            user.borrowedKeys.add(k);
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
                                if (!genre.isEmpty())
                                    user.genreCounts.put(genre, cnt);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }

                // field 5: readHistory (isbn,isbn,...)
                if (p.length >= 5 && !p[4].trim().isEmpty()) {
                    for (String isbnStr : p[4].trim().split(",")) {
                        try {
                            user.readHistory.add(Integer.parseInt(isbnStr.trim()));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                // field 6: outstandingFine
                if (p.length >= 6 && !p[5].trim().isEmpty()) {
                    try {
                        user.outstandingFine = Double.parseDouble(p[5].trim());
                    } catch (NumberFormatException ignored) {
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
        if (!file.exists())
            return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                String[] p = line.split(" \\| ");
                if (p.length < 2)
                    continue;
                int isbn = Integer.parseInt(p[0].trim());
                Book book = catalogue.search(isbn);
                if (book == null)
                    continue;
                for (String uid : p[1].trim().split(",")) {
                    if (!uid.trim().isEmpty())
                        book.waitlist.add(uid.trim());
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
        saveHistory();
        saveFines();
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
                for (Map.Entry<String, Integer> r : b.ratings.entrySet())
                    pw.println("RATING | " + r.getKey() + " | " + r.getValue());
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
                    if (gcSb.length() > 0)
                        gcSb.append(",");
                    gcSb.append(e.getKey()).append(":").append(e.getValue());
                }

                // field 5: read history
                StringBuilder rhSb = new StringBuilder();
                for (int isbn : u.readHistory) {
                    if (rhSb.length() > 0)
                        rhSb.append(",");
                    rhSb.append(isbn);
                }

                pw.println(u.userId + " | " + u.name + " | " + borrowedKeysStr +
                        " | " + gcSb + " | " + rhSb + " | " + String.format("%.2f", u.outstandingFine));
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

    private void saveHistory() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(HISTORY_FILE))) {
            history.saveToFile(pw);
            System.out.println("  Borrowing history saved.");
        } catch (IOException e) {
            System.out.println("  Error saving history: " + e.getMessage());
        }
    }

    private void loadHistory() {
        java.io.File file = new java.io.File(HISTORY_FILE);
        if (!file.exists())
            return;
        try (BufferedReader br = new BufferedReader(new FileReader(HISTORY_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                String[] p = line.split(" \\| ", 6);
                if (p.length < 6)
                    continue;
                try {
                    history.loadRecord(p[0].trim(), p[1].trim(),
                            Integer.parseInt(p[2].trim()), Integer.parseInt(p[3].trim()),
                            LocalDate.parse(p[4].trim()), p[5].trim());
                } catch (Exception ignored) {
                }
            }
            System.out.println("  Borrowing history loaded from " + HISTORY_FILE);
        } catch (IOException e) {
            System.out.println("  Error loading history: " + e.getMessage());
        }
    }

    private void saveFines() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FINES_FILE))) {
            for (FineEvent e : fineLog)
                pw.println(e.date + " | " + String.format("%.2f", e.amount));
            System.out.println("  Fine log saved (" + fineLog.size() + " event(s)).");
        } catch (IOException e) {
            System.out.println("  Error saving fines: " + e.getMessage());
        }
    }

    private void loadFines() {
        java.io.File file = new java.io.File(FINES_FILE);
        if (!file.exists())
            return;
        try (BufferedReader br = new BufferedReader(new FileReader(FINES_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                String[] p = line.split(" \\| ");
                if (p.length < 2)
                    continue;
                try {
                    fineLog.add(new FineEvent(LocalDate.parse(p[0].trim()),
                            Double.parseDouble(p[1].trim())));
                } catch (Exception ignored) {
                }
            }
            System.out.println("  Fine log loaded (" + fineLog.size() + " event(s)).");
        } catch (IOException e) {
            System.out.println("  Error loading fines: " + e.getMessage());
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
            System.out.println();
            System.out.println("┌─────────────────────────────┐");
            System.out.println("│  [1] Admin Login            │");
            System.out.println("│  [2] Member Login           │");
            System.out.println("│  [3] Exit                   │");
            System.out.println("└─────────────────────────────┘");
            System.out.print("  Choice: ");

            if (!sc.hasNextInt()) {
                sc.next();
                continue;
            }
            int role = sc.nextInt();
            sc.nextLine();

            if (role == 3) {
                System.out.print("\n  Save changes before exit? (Y/N): ");
                if (sc.nextLine().trim().equalsIgnoreCase("Y"))
                    saveDatabase();
                System.out.println("\n  Goodbye! Thank you for using SmartLibrary.");
                break;
            } else if (role == 1) {
                System.out.print("  Enter Admin PIN: ");
                String pin = sc.nextLine().trim();
                if (!pin.equals(ADMIN_PIN)) {
                    System.out.println("  Incorrect PIN. Access denied.");
                    continue;
                }
                System.out.println("  Access granted. Welcome, Admin!");
                runAdminMenu(sc);
            } else if (role == 2) {
                System.out.print("  Enter User ID: ");
                String uid = sc.nextLine().trim();
                User member = users.get(uid);
                if (member == null) {
                    System.out.println("  User not found: " + uid);
                    continue;
                }
                System.out.println("  Welcome, " + member.name + "!");
                runMemberMenu(uid, sc);
            }
        }

        sc.close();
    }

    private void runAdminMenu(Scanner sc) {
        while (true) {
            printAdminMenu();
            System.out.print("  Enter choice: ");
            if (!sc.hasNextInt()) {
                System.out.println("  Please enter a number (1-17).");
                sc.next();
                continue;
            }
            int choice = sc.nextInt();
            sc.nextLine();
            if (choice == 17) {
                System.out.println("  Logged out.");
                break;
            }
            handleAdminChoice(choice, sc);
        }
    }

    private void runMemberMenu(String userId, Scanner sc) {
        User user = users.get(userId);
        while (true) {
            printMemberMenu(user.name);
            System.out.print("  Enter choice: ");
            if (!sc.hasNextInt()) {
                System.out.println("  Please enter a number (1-7).");
                sc.next();
                continue;
            }
            int choice = sc.nextInt();
            sc.nextLine();
            if (choice == 7) {
                System.out.println("  Logged out. Goodbye, " + user.name + "!");
                break;
            }
            handleMemberChoice(choice, userId, sc);
        }
    }

    private void printAdminMenu() {
        System.out.println();
        System.out.println("┌──────────────────────────────────────────┐");
        System.out.println("│           ADMIN DASHBOARD                │");
        System.out.println("│  [Member Management]                     │");
        System.out.println("│   1. Register Member                     │");
        System.out.println("│   2. Remove Member                       │");
        System.out.println("│   3. List All Members                    │");
        System.out.println("│  [Catalogue Management]                  │");
        System.out.println("│   4. Add Book (Register / Add Copies)    │");
        System.out.println("│   5. Remove Book                         │");
        System.out.println("│   6. Update Book Details                 │");
        System.out.println("│  [Transactions]                          │");
        System.out.println("│   7. Manage Cart                         │");
        System.out.println("│   8. Return Book                         │");
        System.out.println("│   9. Renew Book                          │");
        System.out.println("│  10. Join Waitlist                       │");
        System.out.println("│  11. Leave Waitlist                      │");
        System.out.println("│  [Discovery]                             │");
        System.out.println("│  12. Find Book (ISBN/Title/Author/Genre) │");
        System.out.println("│  13. List All Books                      │");
        System.out.println("│  [Reports]                               │");
        System.out.println("│  14. Overdue Report                      │");
        System.out.println("│  15. Monthly Activity Report             │");
        System.out.println("│  [History]                               │");
        System.out.println("│  16. View Borrowing History (Global)     │");
        System.out.println("│  17. Logout                              │");
        System.out.println("└──────────────────────────────────────────┘");
    }

    private void printMemberMenu(String name) {
        System.out.println();
        System.out.println("┌──────────────────────────────────────────┐");
        System.out.printf("│  MEMBER DASHBOARD  %-21s │%n",
                name.length() > 20 ? name.substring(0, 19) + "…" : name);
        System.out.println("│  [Discovery]                             │");
        System.out.println("│   1. Find Book (ISBN/Title/Author/Genre) │");
        System.out.println("│   2. Display Hottest Books               │");
        System.out.println("│   3. Recommend Books                     │");
        System.out.println("│   4. List All Books                      │");
        System.out.println("│  [My Account]                            │");
        System.out.println("│   5. View My Profile                     │");
        System.out.println("│   6. View My Borrowing History           │");
        System.out.println("│   7. Logout                              │");
        System.out.println("└──────────────────────────────────────────┘");
    }

    private void handleAdminChoice(int choice, Scanner sc) {
        switch (choice) {

            // ── Member Management ────────────────────────────────
            case 1: {
                System.out.print("  Enter User ID  : ");
                String uid = sc.nextLine().trim();
                System.out.print("  Enter Full Name: ");
                String name = sc.nextLine().trim();
                registerUser(uid, name);
                break;
            }
            case 2: {
                System.out.print("  Enter User ID to remove: ");
                removeMember(sc.nextLine().trim());
                break;
            }
            case 3: {
                listAllMembers();
                break;
            }

            // ── Catalogue Management ─────────────────────────────
            case 4: {
                System.out.print("  Enter ISBN (number): ");
                int isbn = readInt(sc);
                if (catalogue.search(isbn) != null) {
                    System.out.print("  Enter Quantity to add: ");
                    int qty = readInt(sc);
                    addBook(isbn, "", "", "", "", qty);
                } else {
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
            case 5: {
                System.out.print("  Enter ISBN to remove: ");
                removeBook(readInt(sc));
                break;
            }
            case 6: {
                System.out.print("  Enter ISBN to update: ");
                int isbn = readInt(sc);
                Book book = catalogue.search(isbn);
                if (book == null) {
                    System.out.println("  Book not found: ISBN " + isbn);
                    break;
                }
                System.out.println("  Current details:");
                System.out.println("    Title   : " + book.title);
                System.out.println("    Author  : " + book.author);
                System.out.println("    Location: " + book.location);
                System.out.println("    Genre   : " + book.genre);
                String newTitle = book.title;
                String newAuthor = book.author;
                String newLocation = book.location;
                String newGenre = book.genre;
                boolean editing = true;
                while (editing) {
                    System.out.println("  Edit:  1.Title  2.Author  3.Location  4.Genre  5.Save & Exit");
                    System.out.print("  Choice: ");
                    switch (readInt(sc)) {
                        case 1:
                            System.out.print("  New Title   : ");
                            newTitle = sc.nextLine().trim();
                            break;
                        case 2:
                            System.out.print("  New Author  : ");
                            newAuthor = sc.nextLine().trim();
                            break;
                        case 3:
                            System.out.print("  New Location: ");
                            newLocation = sc.nextLine().trim();
                            break;
                        case 4:
                            newGenre = pickGenre(sc);
                            break;
                        case 5:
                            editing = false;
                            break;
                        default:
                            System.out.println("  Enter 1–5.");
                    }
                }
                updateBook(isbn, newTitle, newAuthor, newLocation, newGenre);
                break;
            }

            // ── Transactions ─────────────────────────────────────
            case 7: {
                System.out.print("  Enter Member ID: ");
                String uid = sc.nextLine().trim();
                if (!users.containsKey(uid)) {
                    System.out.println("  User not found: " + uid);
                    break;
                }
                cartMenu(uid, sc);
                break;
            }
            case 8: {
                System.out.print("  Enter Member ID: ");
                String uid = sc.nextLine().trim();
                System.out.print("  Enter ISBN     : ");
                int isbn = readInt(sc);
                returnBook(uid, isbn);
                User rUser = users.get(uid);
                Book rBook = catalogue.search(isbn);
                if (rUser != null && rBook != null && rUser.readHistory.contains(isbn)) {
                    System.out.print("  Rate this book? (1-5 stars, 0 to skip): ");
                    int stars = readInt(sc);
                    if (stars >= 1 && stars <= 5)
                        rateBook(uid, isbn, stars);
                }
                break;
            }
            case 9: {
                System.out.print("  Enter Member ID: ");
                String uid = sc.nextLine().trim();
                System.out.print("  Enter ISBN     : ");
                renewBook(uid, readInt(sc));
                break;
            }
            case 10: {
                System.out.print("  Enter Member ID: ");
                String uid = sc.nextLine().trim();
                System.out.print("  Enter ISBN     : ");
                joinWaitlist(uid, readInt(sc));
                break;
            }
            case 11: {
                System.out.print("  Enter Member ID: ");
                String uid = sc.nextLine().trim();
                System.out.print("  Enter ISBN     : ");
                leaveWaitlist(uid, readInt(sc));
                break;
            }

            // ── Discovery ────────────────────────────────────────
            case 12: {
                System.out.println("  Search by:  1. ISBN   2. Title   3. Author   4. Genre");
                System.out.print("  Choice: ");
                int t = readInt(sc);
                if (t == 4) {
                    findBook(pickGenre(sc), "genre");
                } else {
                    String type = (t == 1) ? "isbn" : (t == 2) ? "title" : "author";
                    System.out.print("  Enter search term: ");
                    findBook(sc.nextLine().trim(), type);
                }
                break;
            }
            case 13: {
                listAllBooks();
                break;
            }

            // ── Reports ──────────────────────────────────────────
            case 14: {
                displayOverdueReport();
                break;
            }
            case 15: {
                System.out.println("  1. Current month   2. Choose a specific month");
                System.out.print("  Choice: ");
                int opt = readInt(sc);
                int year, month;
                if (opt == 2) {
                    System.out.print("  Enter year  (e.g. 2026): ");
                    year = readInt(sc);
                    System.out.print("  Enter month (1-12)     : ");
                    month = readInt(sc);
                    if (month < 1 || month > 12) {
                        System.out.println("  Invalid month.");
                        break;
                    }
                } else {
                    year = LocalDate.now().getYear();
                    month = LocalDate.now().getMonthValue();
                }
                viewMonthlyReport(year, month);
                break;
            }

            // ── History ──────────────────────────────────────────
            case 16: {
                viewHistory();
                break;
            }

            default:
                System.out.println("  Invalid option. Please choose 1-17.");
        }
    }

    private void handleMemberChoice(int choice, String userId, Scanner sc) {
        switch (choice) {

            // ── Discovery ────────────────────────────────────────
            case 1: {
                System.out.println("  Search by:  1. ISBN   2. Title   3. Author   4. Genre");
                System.out.print("  Choice: ");
                int t = readInt(sc);
                if (t == 4) {
                    findBook(pickGenre(sc), "genre");
                } else {
                    String type = (t == 1) ? "isbn" : (t == 2) ? "title" : "author";
                    System.out.print("  Enter search term: ");
                    findBook(sc.nextLine().trim(), type);
                }
                break;
            }
            case 2: {
                displayHottestBooks();
                break;
            }
            case 3: {
                User user = users.get(userId);
                if (user.getTopGenre() == null) {
                    System.out.println("  No borrowing history yet. Showing top books by your chosen genre:");
                    showTopBooksByGenre(pickGenre(sc));
                } else {
                    recommendBooks(userId);
                }
                break;
            }
            case 4: {
                listAllBooks();
                break;
            }

            // ── My Account ───────────────────────────────────────
            case 5: {
                viewUserProfile(userId);
                break;
            }
            case 6: {
                viewPersonalHistory(userId);
                break;
            }

            default:
                System.out.println("  Invalid option. Please choose 1-7.");
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
                case "a":
                    System.out.print("    Enter ISBN: ");
                    addToCart(userId, readInt(sc));
                    break;
                case "b":
                    System.out.print("    Enter ISBN to remove: ");
                    removeFromCart(userId, readInt(sc));
                    break;
                case "c":
                    viewCart(userId);
                    break;
                case "d":
                    checkout(userId);
                    break;
                case "e":
                    return;
                default:
                    System.out.println("    Enter a, b, c, d, or e.");
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
