# SmartLibrary – A Console-Based Library Management System

A **Java** console application that manages a full library workflow using core **OOP** and **DSA** concepts — no frameworks, no libraries beyond the Java standard library.

---

## What Does It Do?

SmartLibrary runs a full library system from your terminal. Members can search for books, build a cart, borrow, return, renew, and join a waitlist. Admins can register books and manage the catalogue. Analytics features surface the most popular books and personalised genre-based recommendations.

| Feature | Details |
|---|---|
| **Register Member** | Create a user account with a unique ID and name |
| **Add Book** | Register a new ISBN with title, author, location, genre, and quantity; or add more copies to an existing ISBN |
| **Remove Book** | Delete a book from the catalogue (blocked if any copy is on loan) |
| **Find Book** | Search by ISBN, partial title, or partial author name |
| **Cart** | Stage books before borrowing — add, remove, then checkout all at once |
| **Checkout** | Borrow up to **2 books** per member at a time |
| **Return Book** | Return a copy; calculates a fine of **RM 1/day** if overdue (loan period: 7 days) |
| **Renew Book** | Extend the loan by 7 days from today — allowed **once per borrow** |
| **Waitlist** | If a book has no available copies, join the queue; when a copy is returned it is **auto-assigned** to the next eligible person |
| **Hottest Books** | Display the top 5 most-borrowed books across all genres, sorted by total borrow count |
| **Recommend Books** | Personalised suggestions based on each member's most-borrowed genre — shows the top 3 unread books in that genre; new members with no history can input a genre manually |
| **Borrowing History** | Global log of all borrow events, most-recent first |
| **Persistence** | All data saved and loaded across sessions from three plain-text files; prompts to save on exit |

---

## Project Structure

```
SmartLibrary/
├── src/
│   ├── Main.java            # Entry point – launches the menu
│   ├── LibraryADT.java      # Interface – defines WHAT the library can do
│   ├── SmartLibrary.java    # Implementation – wires all structures together + UI
│   ├── Book.java            # Entity – ISBN metadata, List<Copy>, waitlist Queue, BST node
│   ├── BookBST.java         # BST (primary) + HashMap indexes (title/author/genre)
│   ├── Copy.java            # One physical copy of a book (borrow state, dates)
│   ├── User.java            # Registered member (userId, name, borrow list, genre stats)
│   ├── Cart.java            # Per-user staging cart before checkout
│   └── BorrowStack.java     # Stack – borrow history in LIFO order
├── library.txt              # Persisted book catalogue + copies
├── users.txt                # Persisted member accounts + borrow keys + genre history
└── reservations.txt         # Persisted per-ISBN waitlists
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

### 2. HashMap — Secondary Search Indexes
Three `HashMap`s sit alongside the BST for fast lookup by different attributes:

| Map | Key | Value | Use |
|---|---|---|---|
| `titleIndex` | lowercase title | ISBN | O(1) exact, O(k) partial title search |
| `authorIndex` | lowercase author | List\<ISBN\> | O(1) exact, O(k) partial author search |
| `genreIndex` | lowercase genre | List\<ISBN\> | O(1) exact genre lookup for recommendations |

All three maps stay in sync with the BST on every insert and remove — the classic **secondary index** pattern used in real databases.

### 3. Queue — Reservation Waitlist
Each `Book` node contains a `Queue<String>` of user IDs waiting for a copy. When a copy is returned, `processWaitlist()` polls the queue and **auto-assigns** the copy to the first eligible user (skips anyone who has since hit the 2-book limit).

```
Waitlist for "Harry Potter" (0/3 copies available):
  Front → [25006805] → [25006754] ← Rear
  When a copy is returned → 25006805 auto-assigned | Due: 2026-06-13
```

### 4. Stack (LIFO) — Borrowing History
Every borrow event is pushed onto a `Stack<BorrowRecord>`. Viewing history iterates from top to bottom — the **most recent borrow appears first**.

```
| 25006805 borrowed "Harry Potter" (Copy #2) on 2026-06-06 |  ← top
| 25006757 borrowed "Dune"         (Copy #1) on 2026-06-03 |
| 25006805 borrowed "The Hobbit"   (Copy #1) on 2026-06-01 |
```

### 5. Multiple Copies per ISBN
Each BST node (one ISBN) holds a `List<Copy>`. Each `Copy` tracks its own borrow state independently, so all three copies of Harry Potter can be loaned to three different members simultaneously.

### 6. Collections.sort() — Hottest Books Ranking
To rank books by popularity, `getAllBooks()` collects the full BST into a `List<Book>` via in-order traversal, then `Collections.sort()` sorts it by `borrowCount` descending in **O(n log n)**. The top 5 are displayed.

### 7. Genre-Based Recommendation
Each `User` maintains two analytics fields:

| Field | Type | Purpose |
|---|---|---|
| `genreCounts` | `Map<String, Integer>` | Tracks how many times each genre has been borrowed |
| `readHistory` | `Set<Integer>` | All ISBNs ever borrowed (persists after return) |

`getTopGenre()` does a linear scan of `genreCounts` to find the genre with the highest count. The `genreIndex` HashMap then retrieves all books in that genre in O(1), which are filtered against `readHistory` and sorted by `borrowCount` to produce the top 3 unread recommendations.

### 8. Interface / Information Hiding
`LibraryADT` declares **what** the library can do without revealing **how**. Callers never touch the BST, HashMaps, Stack, Queue, or file I/O directly.

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

### Menu
```
╔══════════════════════════════════════════════════╗
║          SMART LIBRARY SYSTEM  v4.0              ║
║  BST | HashMap | Queue | Analytics               ║
╚══════════════════════════════════════════════════╝

┌──────────────────────────────────────────┐
│  [Member]                                │
│   1. Register Member                     │
│  [Admin]                                 │
│   2. Add Book (Register / Add Copies)    │
│   3. Remove Book                         │
│  [Services]                              │
│   4. Find Book (ISBN / Title / Author)   │
│   5. Manage Cart                         │
│   6. Return Book                         │
│   7. Renew Book                          │
│   8. Join Waitlist                       │
│  [Analytics]                             │
│   9. Display Hottest Books (Top 5)       │
│  10. Recommend Books                     │
│  [History]                               │
│  11. View Borrowing History              │
│  12. Exit (prompts to save)              │
└──────────────────────────────────────────┘
```

---

## Example Walkthrough

```
1. Register Member    → ID: 25006001, Name: JITESH

2. Add Book           → ISBN: 1001, "Dune", Frank Herbert,
                        Aisle 1 / Shelf A, Genre: Sci-Fi, qty: 2

3. Find Book          → Search by title: "dune"
                        Found! Available: 2/2 | Borrows: 15

4. Manage Cart        → 25006001: add ISBN 1001 → checkout
                        Borrowed: "Dune" (Copy #2) | Due: 2026-06-13

5. Return Book        → 25006001 returns ISBN 1001
                        No fine (returned on time)

6. Join Waitlist      → 25006805 joins queue for ISBN 1004 (0/3 copies available)
                        Queue position: 1

7. Return Book        → 25006757 returns ISBN 1004 (Harry Potter)
                        Auto-assigned to waitlist user: 25006805 | Due: 2026-06-13

8. Hottest Books      → #1 Harry Potter (22 borrows)
                        #2 The Hobbit    (18 borrows)
                        #3 Sapiens       (16 borrows)
                        #4 Dune          (15 borrows)
                        #5 1984          (14 borrows)

9. Recommend Books    → User: 25006757 (TIONG TSUI JEFF)
                        Top genre: Non-Fiction
                        #1 Thinking, Fast and Slow – Daniel Kahneman

10. Exit              → Save changes before exit? (Y/N): Y
                        Library saved. Goodbye!
```

---

## Persistence Format

**library.txt**
```
BOOK | 1001 | Dune | Frank Herbert | Aisle 1 / Shelf A | Sci-Fi | 15
COPY | 1 | 25006757 | 2026-06-03 | 2026-06-10 | false
COPY | 2 | null | null | null | false
```
- `BOOK` fields: isbn | title | author | location | genre | borrowCount
- `COPY` fields: copyId | borrowerId (null if available) | borrowDate | dueDate | renewedOnce

**users.txt**
```
25006757 | TIONG TSUI JEFF | 1001:1,1004:1 | Non-Fiction:5,Sci-Fi:3 | 1011,1001,1002
```
- Fields: userId | name | borrowedKeys | genreCounts | readHistory
- `borrowedKeys` format: `isbn:copyId` pairs
- `genreCounts` format: `genre:count` pairs
- `readHistory` format: comma-separated ISBNs (persists after return)

**reservations.txt**
```
1004 | 25006805,25006754
1015 | 25006805
```
- Format: isbn | comma-separated userId queue (FIFO order, left = next to receive)

---

## Predefined Genres

When adding a book, the admin selects from 12 genres:

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
| `LibraryADT` | Interface — declares all 13 library operations |
| `SmartLibrary` | Implements `LibraryADT` — full business logic, menu UI, and file persistence |
| `Book` | Metadata entity + BST node; holds `List<Copy>`, `Queue<String>` waitlist, genre, and borrowCount |
| `BookBST` | BST by ISBN + HashMap secondary indexes for title, author, and genre search |
| `Copy` | One physical copy — tracks borrower, borrow date, due date, renewal flag |
| `User` | Registered member — userId, name, borrowed keys, genreCounts map, readHistory set |
| `Cart` | Per-user staging area — add/remove ISBNs, then checkout all at once |
| `BorrowStack` | Stack of `BorrowRecord` — push on borrow, display LIFO |
