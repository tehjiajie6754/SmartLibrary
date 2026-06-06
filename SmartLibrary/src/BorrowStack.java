import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * ============================================================
 * TASK 2 – Borrowing History: Stack (LIFO)
 * ============================================================
 * Each push records who borrowed which copy of which book,
 * on what date, and in which genre.
 * Viewing prints most-recent-first.
 * Persisted to history.txt across sessions.
 * ============================================================
 */
public class BorrowStack {

    private static class BorrowRecord {
        String    userId;
        String    title;
        int       isbn;
        int       copyId;
        LocalDate date;
        String    genre;

        BorrowRecord(String userId, String title, int isbn, int copyId,
                     LocalDate date, String genre) {
            this.userId = userId;
            this.title  = title;
            this.isbn   = isbn;
            this.copyId = copyId;
            this.date   = date;
            this.genre  = genre;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s borrowed \"%s\" (ISBN %d, Copy #%d) [%s]",
                date, userId, title, isbn, copyId, genre);
        }
    }

    private final Stack<BorrowRecord> stack = new Stack<>();

    public void push(String userId, String title, int isbn, int copyId,
                     LocalDate date, String genre) {
        stack.push(new BorrowRecord(userId, title, isbn, copyId, date, genre));
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

    /** Shows only the borrow events belonging to the given userId, most recent first. */
    public void showForUser(String userId) {
        List<String> lines = new ArrayList<>();
        for (int i = stack.size() - 1; i >= 0; i--) {
            BorrowRecord r = stack.get(i);
            if (r.userId.equals(userId)) lines.add(r.toString());
        }
        if (lines.isEmpty()) {
            System.out.println("  (No borrowing history for this user.)");
            return;
        }
        System.out.println("  --- Your Borrowing History (most recent first) ---");
        for (int i = 0; i < lines.size(); i++)
            System.out.println("  " + (i + 1) + ". " + lines.get(i));
    }

    // ---------------------------------------------------------
    // Monthly analytics
    // ---------------------------------------------------------

    public int countBorrowsInMonth(int year, int month) {
        int count = 0;
        for (int i = 0; i < stack.size(); i++) {
            BorrowRecord r = stack.get(i);
            if (r.date.getYear() == year && r.date.getMonthValue() == month) count++;
        }
        return count;
    }

    public Map<String, Integer> genreCountsForMonth(int year, int month) {
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < stack.size(); i++) {
            BorrowRecord r = stack.get(i);
            if (r.date.getYear() == year && r.date.getMonthValue() == month)
                counts.merge(r.genre, 1, Integer::sum);
        }
        return counts;
    }

    // ---------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------

    /** Saves all records oldest-first (bottom of stack first). */
    public void saveToFile(PrintWriter pw) {
        for (int i = 0; i < stack.size(); i++) {
            BorrowRecord r = stack.get(i);
            pw.println(r.userId + " | " + r.title + " | " + r.isbn + " | " +
                       r.copyId + " | " + r.date + " | " + r.genre);
        }
    }

    /** Loads a single record (call in file-read order; last line ends up on top). */
    public void loadRecord(String userId, String title, int isbn, int copyId,
                           LocalDate date, String genre) {
        stack.push(new BorrowRecord(userId, title, isbn, copyId, date, genre));
    }

    public static BorrowStack fromFile(String filename) {
        BorrowStack bs = new BorrowStack();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(" \\| ", 6);
                if (p.length < 6) continue;
                try {
                    String    userId = p[0].trim();
                    String    title  = p[1].trim();
                    int       isbn   = Integer.parseInt(p[2].trim());
                    int       copyId = Integer.parseInt(p[3].trim());
                    LocalDate date   = LocalDate.parse(p[4].trim());
                    String    genre  = p[5].trim();
                    bs.loadRecord(userId, title, isbn, copyId, date, genre);
                } catch (Exception ignored) {}
            }
        } catch (IOException ignored) {}
        return bs;
    }
}
