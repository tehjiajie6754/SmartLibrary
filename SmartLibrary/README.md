# SmartLibrary – A Console-Based Library Management System

A **Java** console application that manages a full library workflow using core **OOP** and **DSA** concepts — no frameworks, no libraries beyond the Java standard library.

---

## What Does It Do?

SmartLibrary runs a full library system from your terminal with a **role-based login system**. Admins log in with a PIN and get a 17-option management dashboard. Members log in with their User ID and get a personalised 7-option dashboard. All data persists across sessions via 5 plain-text files.

---

## Role-Based Dashboards

### Admin Dashboard (PIN: `1234`)

| # | Feature | Details |
|---|---|---|
| 1 | **Register Member** | Create a member account with a unique ID and name |
| 2 | **Remove Member** | Delete a member (blocked if any loans are outstanding) |
| 3 | **List All Members** | All registered members sorted by User ID |
| 4 | **Add Book** | Register a new ISBN or add copies to an existing one |
| 5 | **Remove Book** | Delete a book (blocked if any copy is on loan) |
| 6 | **Update Book Details** | Edit title, author, location, or genre; all search indexes stay in sync |
| 7 | **Manage Cart** | Operate the cart on behalf of any member |
| 8 | **Return Book** | Process a return; prompts for a rating (1-5★) automatically |
| 9 | **Renew Book** | Extend a member's loan by 7 days (once per borrow) |
| 10 | **Join Waitlist** | Queue a member for a fully-borrowed book |
| 11 | **Leave Waitlist** | Remove a member from a book's reservation queue |
| 12 | **Find Book** | Search by ISBN, title, author, or genre |
| 13 | **List All Books** | Full aligned catalogue table with ratings, availability, and borrow counts |
| 14 | **Overdue Report** | Every overdue copy system-wide, sorted by most days overdue |
| 15 | **Monthly Activity Report** | Total borrows, genre popularity, and fines for any month |
| 16 | **View Borrowing History** | Global borrow log, most-recent first |
| 17 | **Logout** | Return to the role-selection screen |

### Member Dashboard (login by User ID)

| # | Feature | Details |
|---|---|---|
| 1 | **Find Book** | Search by ISBN, title, author, or genre; results sorted by popularity |
| 2 | **Display Hottest Books** | Top 5 most-borrowed books across all genres |
| 3 | **Recommend Books** | Top 3 unread books from your most-borrowed genre |
| 4 | **List All Books** | Full catalogue table |
| 5 | **View My Profile** | Active loans, due dates, genre stats, read history, waitlist positions, outstanding fine |
| 6 | **View My Borrowing History** | Personal borrow log, most-recent first |
| 7 | **Logout** | Return to the role-selection screen |

---

## Key Features

### Fine Tracking
- Loan period: **7 days**; fine rate: **RM 1.00 / overdue day**
- Fine is calculated on return and added to the member's `outstandingFine` balance
- Overdue report shows per-copy fines and a system-wide total
- Monthly Activity Report aggregates fines generated during the period

### Book Ratings
- After any return, the admin is prompted to rate the book on a **1–5 star** scale
- Each user's rating is stored per-book; the book displays an average and count
- Ratings persist across sessions in `library.txt`

### Monthly Activity Report
- Shows total borrows, fines generated, and genre popularity for any calendar month
- Can view the current month or any historical month
- Genre counts are derived from the persisted borrow-history stack

### Cart System
- Members stage books in a cart before borrowing
- Checkout processes all cart items at once; stops if the 2-book limit is reached
- Remaining cart items are kept for the next session

### Recommendation Engine
- Uses each member's top genre (highest borrow count) to filter unread books
- Sorted by popularity (most borrowed first); skips books already in `readHistory`
- First-time members with no history can pick a genre manually

---

## Project Structure

```
SmartLibrary/
├── src/
│   ├── Main.java            # Entry point – launches the menu
│   ├── LibraryADT.java      # Interface – defines WHAT the library can do (23 operations)
│   ├── SmartLibrary.java    # Implementation – business logic, dual menus, file persistence
│   ├── Book.java            # Entity – metadata, List<Copy>, waitlist Queue, ratings Map, BST node
│   ├── BookBST.java         # BST by ISBN + HashMap indexes (title/author/genre) + reindex()
│   ├── Copy.java            # One physical copy – borrow state, dates, overdue calculation
│   ├── User.java            # Member – userId, name, borrowedKeys, genreCounts, readHistory, outstandingFine
│   ├── Cart.java            # Per-user staging cart before checkout
│   └── BorrowStack.java     # Stack – LIFO borrow history; persists to history.txt
├── library.txt              # Books + copies + ratings
├── users.txt                # Members + borrow keys + genre stats + outstanding fine
├── reservations.txt         # Per-ISBN waitlists
├── history.txt              # Borrow records (oldest → newest; newest ends on top of stack)
└── fines.txt                # Fine events (date + amount generated)
```

---

## Key DSA Concepts

### 1. Binary Search Tree (BST) — Primary Catalogue Index
Books are keyed by ISBN. Smaller ISBNs go **left**, larger go **right**, giving **O(log n)** average search, insert, and remove.

```
        [1004]
       /      \
   [1001]    [1009]
       \
      [1003]
```

`getAllBooks()` does an **in-order traversal** collecting books into a `List<Book>` — naturally sorted by ISBN, no extra sort needed.

### 2. HashMap — Secondary Search Indexes
Three `HashMap`s sit alongside the BST for fast lookup by different attributes:

| Map | Key | Value | Use |
|---|---|---|---|
| `titleIndex` | lowercase title | ISBN | O(1) exact, O(k) partial title search |
| `authorIndex` | lowercase author | List\<ISBN\> | O(1) exact, O(k) partial author search |
| `genreIndex` | lowercase genre | List\<ISBN\> | O(1) genre lookup for search and recommendations |

All three maps stay in sync with the BST on every insert, remove, and **update** via `reindex()`. When a book's title, author, or genre changes, `reindex()` removes the stale entries and inserts fresh ones without touching the BST structure.

### 3. Queue — Reservation Waitlist
Each `Book` node holds a `Queue<String>` of user IDs waiting for a copy. When a copy is returned, `processWaitlist()` polls the queue and **auto-assigns** the copy to the first eligible user. Members can leave at any time via `Queue.remove(userId)` — removes by value without disturbing the order of remaining entries.

```
Waitlist for "Harry Potter" (0/3 copies available):
  Front → [25006805] → [25006754] ← Rear
  On return → 25006805 auto-assigned | Due: 2026-06-14
```

### 4. Stack (LIFO) — Borrowing History
Every borrow event (checkout and waitlist auto-assign) is pushed onto a `Stack<BorrowRecord>`. Each record stores: userId, title, ISBN, copyId, date, and genre. The stack is iterated top-to-bottom for display — **most recent first**.

The stack is persisted to `history.txt` (oldest record first) so history survives restarts.

```
| 25006805 borrowed "The Da Vinci Code" (Copy #2) on 2026-06-06 |  ← top
| 25006001 borrowed "Harry Potter"      (Copy #2) on 2026-06-05 |
| 25006739 borrowed "Harry Potter"      (Copy #3) on 2026-06-04 |
```

### 5. Multiple Copies per ISBN
Each BST node holds a `List<Copy>`. Each `Copy` tracks its own borrow state independently, so all three copies of Harry Potter can be on loan to three different members simultaneously.

### 6. Sorting — Popularity Ranking
Used in multiple places:

| Where | How |
|---|---|
| **Find Book** (title/author/genre) | Results sorted by `borrowCount` descending |
| **Hottest Books** | All books collected via in-order BST traversal, sorted by `borrowCount` descending |
| **Recommendations** | Genre books filtered, sorted by `borrowCount` descending |

### 7. Genre-Based Recommendation Engine
Each `User` maintains two analytics fields:

| Field | Type | Purpose |
|---|---|---|
| `genreCounts` | `Map<String, Integer>` | Counts borrows per genre |
| `readHistory` | `Set<Integer>` | All ISBNs ever borrowed (persists after return) |

`getTopGenre()` scans `genreCounts` to find the highest-count genre. The `genreIndex` HashMap retrieves all books in that genre in O(1), filtered against `readHistory`, and sorted by `borrowCount`.

### 8. Stack-Based Monthly Analytics
The `BorrowStack` exposes two analytics methods that scan the stack linearly:

| Method | Returns |
|---|---|
| `countBorrowsInMonth(year, month)` | Total borrow events in that period |
| `genreCountsForMonth(year, month)` | `Map<genre, count>` for the period |

These power the **Monthly Activity Report** without any extra data structures.

### 9. Interface / Information Hiding
`LibraryADT` declares **23 operations** that describe what the library can do without revealing the BST, HashMaps, Stack, Queue, or file I/O underneath.

---

## How to Run

### Prerequisites
- **Java JDK 16+** installed

### Compile & Run
```bash
# From the project root (SmartLibrary/)
javac -d bin src/*.java
java -cp bin Main
```

### Login Screen
```
╔══════════════════════════════════════════════════╗
║          SMART LIBRARY SYSTEM  v4.0              ║
║  BST | HashMap | Queue | Analytics               ║
╚══════════════════════════════════════════════════╝

┌─────────────────────────────┐
│  [1] Admin Login            │
│  [2] Member Login           │
│  [3] Exit                   │
└─────────────────────────────┘
```

### Admin Dashboard
```
┌──────────────────────────────────────────┐
│           ADMIN DASHBOARD                │
│  [Member Management]                     │
│   1. Register Member                     │
│   2. Remove Member                       │
│   3. List All Members                    │
│  [Catalogue Management]                  │
│   4. Add Book (Register / Add Copies)    │
│   5. Remove Book                         │
│   6. Update Book Details                 │
│  [Transactions]                          │
│   7. Manage Cart                         │
│   8. Return Book                         │
│   9. Renew Book                          │
│  10. Join Waitlist                       │
│  11. Leave Waitlist                      │
│  [Discovery]                             │
│  12. Find Book (ISBN/Title/Author/Genre) │
│  13. List All Books                      │
│  [Reports]                               │
│  14. Overdue Report                      │
│  15. Monthly Activity Report             │
│  [History]                               │
│  16. View Borrowing History (Global)     │
│  17. Logout                              │
└──────────────────────────────────────────┘
```

### Member Dashboard
```
┌──────────────────────────────────────────┐
│  MEMBER DASHBOARD  TIONG TSUI JEFF       │
│  [Discovery]                             │
│   1. Find Book (ISBN/Title/Author/Genre) │
│   2. Display Hottest Books               │
│   3. Recommend Books                     │
│   4. List All Books                      │
│  [My Account]                            │
│   5. View My Profile                     │
│   6. View My Borrowing History           │
│   7. Logout                              │
└──────────────────────────────────────────┘
```

---

## Example Walkthrough

```
1.  Admin login       → PIN: 1234

2.  Register Member   → ID: 25006001, Name: JITESH

3.  Add Book          → ISBN: 1001, "Dune", Frank Herbert,
                        Aisle 1 / Shelf A, Genre: Sci-Fi, qty: 2

4.  Manage Cart       → Member: 25006001 | Add ISBN 1001 → Checkout
                        Borrowed: "Dune" (Copy #1) | Due: 2026-06-14

5.  Return Book       → Member: 25006001 returns ISBN 1001
                        No fine (returned on time)
                        Rate this book? (1-5 stars, 0 to skip): 5
                        Rating saved: 5 star(s) — new average: 5.0/5 (1 rating(s))

6.  Join Waitlist     → Member: 25006805 | ISBN: 1004 (0/3 copies available)
                        Queue position: 1

7.  Return Book       → Member: 25006757 returns ISBN 1004
                        Returned: "Harry Potter" (Copy #1)
                        Auto-assigned to waitlist user: 25006805 | Due: 2026-06-14

8.  Leave Waitlist    → Member: 25006754 leaves queue for ISBN 1004
                        Removed from waitlist. Remaining queue: 0

9.  Overdue Report    → 1 overdue copy
                        Title                  | ISBN | Copy# | Borrower  | Due Date   | Days | Fine (RM)
                        1984                   | 1009 | 1     | 25006739  | 2026-06-03 | 4    | 4.00
                        Total outstanding fines: RM 4.00

10. Monthly Report    → June 2026
                        Total borrows       : 14
                        Total fines generated: RM 0.00
                        Genre Popularity:
                          #1  Fantasy         : 5 borrow(s)
                          #2  Thriller        : 4 borrow(s)
                          #3  Classic         : 2 borrow(s)

11. Member login      → ID: 25006757

12. View My Profile   → User: 25006757 (TIONG TSUI JEFF)
                        Loans: 2/2 | Outstanding Fine: RM 0.00
                        Currently borrowed: "Dune" (Copy #1) | Due: 2026-06-10 | On time
                                            "Harry Potter..." (Copy #1) | Due: 2026-06-09 | On time
                        Top genre: Non-Fiction (5 borrows)

13. Recommend Books   → Based on: Non-Fiction
                        #1  Thinking, Fast and Slow — Daniel Kahneman | Borrows: 9

14. List All Books    →
  ISBN    Title                                Author               Genre         Location           Avail    Borrows  Rating
  ---------------------------------------------------------------------------------------------------------------
  1001    Dune                                 Frank Herbert        Sci-Fi        Aisle 1 / Shelf A  1/2      15       4.3/5 (3)
  1002    The Hitchhiker's Guide to the Gala~  Douglas Adams        Sci-Fi        Aisle 1 / Shelf A  1/1      8        3.5/5 (2)
  ...

15. Logout            → Return to role-selection screen

16. Exit              → Save changes before exit? (Y/N): Y
                        Library saved (20 book(s)). Goodbye!
```

---

## Persistence Format

### library.txt
```
BOOK | 1001 | Dune | Frank Herbert | Aisle 1 / Shelf A | Sci-Fi | 15
COPY | 1 | 25006757 | 2026-06-03 | 2026-06-10 | false
COPY | 2 | null | null | null | false
RATING | 25006757 | 5
RATING | 25006805 | 3
```
- `BOOK` fields: isbn | title | author | location | genre | borrowCount
- `COPY` fields: copyId | borrowerId (`null` if available) | borrowDate | dueDate | renewedOnce
- `RATING` fields: userId | stars (1-5)

### users.txt
```
25006757 | TIONG TSUI JEFF | 1001:1,1004:1 | Sci-Fi:3,Non-Fiction:5 | 1001,1002,1004,1011,1017,1018 | 0.00
```
- Fields: userId | name | borrowedKeys | genreCounts | readHistory | outstandingFine
- `borrowedKeys`: `isbn:copyId` pairs (comma-separated)
- `genreCounts`: `genre:count` pairs (comma-separated)
- `readHistory`: comma-separated ISBNs (persists after return)
- `outstandingFine`: accumulated overdue fine balance (RM)

### reservations.txt
```
1004 | 25006805,25006754
```
- Format: isbn | comma-separated userId queue (FIFO — leftmost user receives the next available copy)

### history.txt
```
25006757 | The Hitchhiker's Guide to the Galaxy | 1002 | 1 | 2026-04-10 | Sci-Fi
25006805 | 1984 | 1009 | 2 | 2026-04-15 | Classic
```
- Fields: userId | title | isbn | copyId | date | genre
- Written oldest-first so that the last line becomes the top of the stack on reload

### fines.txt
```
2026-05-11 | 3.00
2026-06-03 | 4.00
```
- Fields: date | amount
- Each line is one fine-generation event (triggered on overdue return)

---

## Predefined Genres

When adding or updating a book, the admin selects from 12 genres:

| # | Genre | # | Genre | # | Genre |
|---|---|---|---|---|---|
| 1 | Sci-Fi | 5 | Classic | 9 | History |
| 2 | Fantasy | 6 | Non-Fiction | 10 | Philosophy |
| 3 | Mystery | 7 | Horror | 11 | Religion |
| 4 | Romance | 8 | Thriller | 12 | Biography |

---

## Class Responsibilities

| Class | Role |
|---|---|
| `Main` | Entry point — creates `SmartLibrary` and starts the menu |
| `LibraryADT` | Interface — declares all 23 library operations |
| `SmartLibrary` | Implements `LibraryADT` — full business logic, role-based dual menus, and 5-file persistence |
| `Book` | Metadata entity + BST node; holds `List<Copy>`, `Queue<String>` waitlist, `Map<userId,stars>` ratings |
| `BookBST` | BST by ISBN + HashMap secondary indexes for title, author, and genre; `reindex()` keeps indexes in sync |
| `Copy` | One physical copy — tracks borrower, borrow date, due date, renewal flag, and `daysOverdue()` |
| `User` | Registered member — userId, name, borrowedKeys, genreCounts map, readHistory set, outstandingFine |
| `Cart` | Per-user staging area — add/remove ISBNs, then checkout all at once |
| `BorrowStack` | Stack of `BorrowRecord` (userId, title, isbn, copyId, date, genre) — push on borrow, LIFO display, monthly analytics, persisted to history.txt |
