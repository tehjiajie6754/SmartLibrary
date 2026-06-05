import java.time.LocalDate;

/**
 * Represents one physical copy of a book.
 * A single ISBN can have many Copy objects (e.g. 3 copies of the same title).
 */
public class Copy {

    int copyId;
    String    borrowerId;  // null = available
    LocalDate borrowDate;
    LocalDate dueDate;
    boolean   renewedOnce;

    public Copy(int copyId) {
        this.copyId      = copyId;
        this.borrowerId  = null;
        this.borrowDate  = null;
        this.dueDate     = null;
        this.renewedOnce = false;
    }

    public boolean isAvailable() {
        return borrowerId == null;
    }

    /** Returns how many days this copy is past its due date (0 if not overdue). */
    public long daysOverdue() {
        if (dueDate == null) return 0;
        long diff = LocalDate.now().toEpochDay() - dueDate.toEpochDay();
        return diff > 0 ? diff : 0;
    }
}
