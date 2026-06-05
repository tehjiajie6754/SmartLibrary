# SmartLibrary – A Console-Based Library Management System

A **Java** console application that manages a full library workflow using core **OOP** and **DSA** concepts — no frameworks, no libraries beyond the Java standard library.

---

## What Does It Do?

SmartLibrary runs a full library system from your terminal. Members can search for books, build a cart, borrow, return, renew, and join a waitlist. Admins can register books and manage the catalogue.

| Feature | Details |
|---|---|
| **Register Member** | Create a user account with a unique ID and name |
| **Add Book** | Register a new ISBN with location and quantity, or add more copies to an existing one |
| **Remove Book** | Delete a book from the catalogue (blocked if copies are on loan) |
| **Find Book** | Search by ISBN, partial title, or partial author name |
| **Cart** | Stage books before borrowing — add, remove, then checkout all at once |
| **Checkout** | Borrow up to **2 books** per member at a time |
| **Return Book** | Return a copy; calculates a fine of **RM 1/day** if overdue (loan period: 7 days) |
| **Renew Book** | Extend the loan by 7 days from today — allowed **once per borrow** |
| **Waitlist** | If a book has no available copies, join the queue; when a copy is returned it is **auto-assigned** to the next person |
| **Borrowing History** | Global log of all borrow events, most-recent first |
| **Persistence** | All data is saved across sessions to three text files |

---

## Project Structure

```
SmartLibrary/
├── src/
│   ├── Main.java            # Entry point – launches the menu
│   ├── LibraryADT.java      # Interface – defines WHAT the library can do
│   ├── SmartLibrary.java    # Implementation – wires all structures together + UI
│   ├── Book.java            # Entity – ISBN metadata, List<Copy>, waitlist Queue, BST node
│   ├── BookBST.java         # BST (primary) + HashMap indexes (secondary) for search
│   ├── Copy.java            # One physical copy of a book (borrow state, dates)
│   ├── User.java            # Registered member (userId, name, borrowed list)
│   ├── Cart.java            # Per-user staging cart before checkout
│   └── BorrowStack.java     # Stack – borrow history in LIFO order
├── library.txt              # Persisted book catalogue + copies
├── users.txt                # Persisted member accounts + borrow keys
└── reservations.txt         # Persisted per-ISBN waitlists
```

---

## Key DSA Concepts

### 1. Binary Search Tree (BST) — Primary Catalogue Index
Books are keyed by ISBN. Smaller ISBNs go **left**, larger go **right**, giving **O(log n)** average search, insert, and remove.

```
        [200]
       /     \
    [100]   [300]
        \
       [150]
```

### 2. HashMap — Secondary Search Indexes
Two `HashMap`s sit alongside the BST to enable fast title and author lookup:

| Map | Key | Value | Lookup |
|---|---|---|---|
| `titleIndex` | lowercase title | ISBN | O(1) exact, O(k) partial |
| `authorIndex` | lowercase author | List\<ISBN\> | O(1) exact, O(k) partial |

Both maps are kept in sync with the BST on every insert and remove — the classic **secondary index** pattern used in real databases.

### 3. Queue — Reservation Waitlist
Each `Book` node contains a `Queue<String>` of user IDs waiting for a copy. When a copy is returned, `processWaitlist()` polls the queue and **auto-assigns** the copy to the first eligible user.

```
Waitlist for "Clean Code" (0 copies available):
  Front → [STU002] → [STU005] → [STU009] ← Rear
  When a copy is returned → STU002 gets it automatically
```

### 4. Stack (LIFO) — Borrowing History
Every borrow event is pushed onto a `Stack<BorrowRecord>`. Viewing history iterates from top to bottom — the **most recent borrow appears first**.

```
| STU003 borrowed "Refactoring" on 2026-06-05 |  ← top (most recent)
| STU001 borrowed "Clean Code"  on 2026-06-04 |
| STU002 borrowed "Design Patterns" on 2026-06-03 |
```

### 5. Multiple Copies per ISBN
Each BST node (one ISBN) holds a `List<Copy>`. Each `Copy` tracks its own borrow state independently, so three copies of the same book can be loaned to three different members simultaneously.

### 6. Interface / Information Hiding
`LibraryADT` declares **what** the library can do without revealing **how**. Callers never touch the BST, HashMaps, Stack, Queue, or file I/O directly.

---

## How to Run

### Prerequisites
- **Java JDK 16+** installed

### Compile & Run
```bash
# From the project root (SmartLibrary/)
javac -d out src/*.java
java -cp out Main
```

### Menu
```
╔══════════════════════════════════════════════════╗
║          SMART LIBRARY SYSTEM  v3.0              ║
║  BST Catalogue | HashMap Search | Queue Waitlist ║
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
│  [History]                               │
│   9. View Borrowing History              │
│  10. Exit                                │
└──────────────────────────────────────────┘
```

---

## Example Walkthrough

```
1. Register Member  → ID: STU001, Name: Alice
2. Add Book         → ISBN: 1001, "Clean Code", Robert Martin, Aisle 1 / Shelf A, qty: 2
3. Find Book        → Search by title: "clean"  →  Found! Available: 2/2
4. Manage Cart      → STU001: add ISBN 1001 → checkout
                    → Borrowed: "Clean Code" (Copy #1) | Due: 2026-06-12
5. Return Book      → STU001 returns ISBN 1001
                    →  No fine (returned on time)
6. Join Waitlist    → STU002 joins queue for ISBN 1001 (0 copies available)
7. Return Book      → STU001 returns last copy
                    →  Auto-assigned to STU002 from waitlist | Due: 2026-06-12
```

---

## Persistence Format

**library.txt**
```
BOOK | 1001 | Clean Code | Robert Martin | Aisle 1 / Shelf A
COPY | 1 | STU001 | 2026-06-05 | 2026-06-12 | false
COPY | 2 | null | null | null | false
```

**users.txt**
```
STU001 | Alice | 1001:1
STU002 | Bob |
```

**reservations.txt**
```
1001 | STU003,STU004
```

---

## Class Responsibilities

| Class | Role |
|---|---|
| `Main` | Entry point — creates `SmartLibrary` and starts the menu |
| `LibraryADT` | Interface — declares all 12 library operations |
| `SmartLibrary` | Implements `LibraryADT` — full business logic, menu UI, and file persistence |
| `Book` | Metadata entity + BST node; holds `List<Copy>` and `Queue<String>` waitlist |
| `BookBST` | BST by ISBN + HashMap secondary indexes for title/author search |
| `Copy` | One physical copy — tracks borrower, borrow date, due date, renewal flag |
| `User` | Registered member — userId, name, list of borrowed keys, max-2 limit |
| `Cart` | Per-user staging area — add/remove ISBNs, then checkout all at once |
| `BorrowStack` | Stack of `BorrowRecord` — push on borrow, display LIFO |
