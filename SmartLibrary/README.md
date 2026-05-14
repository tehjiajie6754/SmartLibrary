# 📚 SmartLibrary – A Console-Based Library Management System

A **Java** console application that manages a book catalogue using a **Binary Search Tree (BST)** for fast lookups and a **Stack** for borrowing history – demonstrating core data-structure concepts with a clean, menu-driven interface.

---

## ✨ What Does It Do?

SmartLibrary lets you run a mini library right from your terminal. You can:

| Action | What Happens Under the Hood |
|---|---|
| **Add a Book** | The book is inserted into a BST, sorted by ISBN. |
| **Search for a Book** | The BST is searched recursively – averaging **O(log n)** time. |
| **Borrow a Book** | The book moves from the BST catalogue → onto a Stack (and can't be borrowed twice). |
| **View Borrowing History** | The Stack is printed top-to-bottom, showing the **most recent borrow first** (LIFO). |

---

## 🏗️ Project Structure

```
SmartLibrary/
├── src/
│   ├── Main.java          # Entry point – launches the menu
│   ├── LibraryADT.java    # Interface – defines WHAT the library can do
│   ├── SmartLibrary.java  # Implementation – wires BST + Stack together
│   ├── Book.java          # Entity class – holds ISBN, title, author (also acts as a BST node)
│   ├── BookBST.java       # Binary Search Tree – insert, search, remove
│   └── BorrowStack.java   # Stack – push borrowed books, display history
└── out/                   # Compiled class files
```

---

## 🔑 Key Concepts Demonstrated

### 1. Binary Search Tree (BST)
Books are organised by ISBN. Smaller ISBNs go **left**, larger go **right** — making search very efficient.

```
        [200]
       /     \
    [100]   [300]
        \
       [150]
```

### 2. Stack (LIFO)
Every time you borrow a book, it's **pushed** onto the history stack. Viewing history shows the **most recent borrow first**, just like the "undo" button on your keyboard.

```
| Book C |  ← top (most recent)
| Book B |
| Book A |
---------
```

### 3. Interface / Information Hiding
`LibraryADT` defines *what* operations are available (`addBook`, `searchBook`, `borrowBook`, `viewLatestHistory`) without exposing *how* they work internally. The BST and Stack are private implementation details.

---

## 🚀 How to Run

### Prerequisites
- **Java JDK 16+** installed

### Compile & Run
```bash
# From the project root
javac -d out src/*.java
java -cp out Main
```

You'll see an interactive menu:

```
╔═══════════════════════════════════════╗
║      SMART LIBRARY SYSTEM v1.0       ║
║  BST Catalogue + Stack History       ║
╚═══════════════════════════════════════╝

┌───────────────────────────────┐
│  1. Add Book                  │
│  2. Search Book (BST)         │
│  3. Borrow Book (→ Stack)     │
│  4. View Borrowing History    │
│  5. Exit                      │
└───────────────────────────────┘
Enter your choice:
```

---

## 📖 Example Walkthrough

```
1. Add Book   → ISBN: 200, Title: "Data Structures", Author: "John"
2. Add Book   → ISBN: 100, Title: "Algorithms", Author: "Jane"
3. Search     → ISBN: 200  →  ✓ Found!
4. Borrow     → ISBN: 200  →  Moved to history stack, removed from catalogue
5. Search     → ISBN: 200  →  ✗ Not found (already borrowed)
6. View History →  1. Data Structures (most recent)
```

---

## 🧩 Class Responsibilities

| Class | Role |
|---|---|
| `Main` | Entry point — creates `SmartLibrary` and starts the menu. |
| `LibraryADT` | Java interface — declares the four library operations. |
| `SmartLibrary` | Implements `LibraryADT` — coordinates BST & Stack, runs the console UI. |
| `Book` | Data entity — stores ISBN, title, author; also serves as a BST node (`left`/`right` pointers). |
| `BookBST` | Binary Search Tree — recursive `insert`, `search`, and `remove` by ISBN. |
| `BorrowStack` | Stack wrapper — `push` borrowed books, `show` history in LIFO order. |
