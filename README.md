# SmartLibrary

SmartLibrary is a console-based Library Management System developed in Java. It allows administrators and members to manage library operations such as registering members, adding books, borrowing books, returning books, managing waitlists, tracking fines, viewing borrowing history, and generating reports.

The project demonstrates core Object-Oriented Programming (OOP) and Data Structures and Algorithms (DSA) concepts such as Binary Search Tree, HashMap, Queue, Stack, ArrayList, Set, and file handling.

---

## Features

### Admin Features

* Admin login using PIN
* Register new members
* Remove members
* View all registered members
* Add new books
* Add multiple copies of the same book
* Remove books
* Update book details
* Search books by ISBN, title, author, or genre
* Manage member borrowing cart
* Process book returns
* Renew borrowed books
* Manage book waitlists
* View overdue book report
* Generate monthly activity report
* View global borrowing history

### Member Features

* Member login using User ID
* Search books by ISBN, title, author, or genre
* View hottest books
* Get book recommendations
* View full book catalogue
* View personal profile
* View active loans and due dates
* View outstanding fines
* View personal borrowing history
* View waitlist status

---

## Technologies Used

* Java
* Java Standard Library
* File I/O
* Object-Oriented Programming
* Data Structures and Algorithms

---

## Data Structures Used

| Data Structure     | Usage                                          |
| ------------------ | ---------------------------------------------- |
| Binary Search Tree | Stores books sorted by ISBN                    |
| HashMap            | Fast search by title, author, and genre        |
| Queue              | Manages reservation waitlists                  |
| Stack              | Stores borrowing history in LIFO order         |
| ArrayList          | Stores book copies and collections of data     |
| Set                | Stores user read history                       |
| Map                | Stores genre counts, ratings, users, and carts |

---

## Project Structure

```text
SmartLibrary/
├── SmartLibrary/
│   ├── src/
│   │   ├── Main.java
│   │   ├── LibraryADT.java
│   │   ├── SmartLibrary.java
│   │   ├── Book.java
│   │   ├── BookBST.java
│   │   ├── Copy.java
│   │   ├── User.java
│   │   ├── Cart.java
│   │   └── BorrowStack.java
│   ├── bin/
│   └── README.md
├── library.txt
├── users.txt
├── reservations.txt
├── history.txt
└── fines.txt
```

---

## Class Responsibilities

| Class          | Responsibility                                                  |
| -------------- | --------------------------------------------------------------- |
| `Main`         | Program entry point                                             |
| `LibraryADT`   | Interface defining library operations                           |
| `SmartLibrary` | Main system logic, menus, borrowing, reports, and file handling |
| `Book`         | Represents a book and stores copies, ratings, and waitlist      |
| `BookBST`      | Manages books using BST and secondary search indexes            |
| `Copy`         | Represents one physical copy of a book                          |
| `User`         | Represents a library member                                     |
| `Cart`         | Stores books before checkout                                    |
| `BorrowStack`  | Stores borrowing history and supports reports                   |

---

## How to Run

### Prerequisites

Make sure Java JDK is installed.

Check Java version:

```bash
java -version
javac -version
```

---

### Compile the Program

From the project folder:

```bash
cd SmartLibrary
javac -d bin src/*.java
```

---

### Run the Program

```bash
java -cp bin Main
```

---

## Login Information

### Admin Login

```text
Admin PIN: 1234
```

### Member Login

Members log in using their registered User ID.

Example:

```text
User ID: 25006757
```

---

## Main System Menu

```text
[1] Admin Login
[2] Member Login
[3] Exit
```

---

## Admin Dashboard

```text
[Member Management]
1. Register Member
2. Remove Member
3. List All Members

[Catalogue Management]
4. Add Book
5. Remove Book
6. Update Book Details

[Transactions]
7. Manage Cart
8. Return Book
9. Renew Book
10. Join Waitlist
11. Leave Waitlist

[Discovery]
12. Find Book
13. List All Books

[Reports]
14. Overdue Report
15. Monthly Activity Report

[History]
16. View Borrowing History
17. Logout
```

---

## Member Dashboard

```text
[Discovery]
1. Find Book
2. Display Hottest Books
3. Recommend Books
4. List All Books

[My Account]
5. View My Profile
6. View My Borrowing History
7. Logout
```

---

## Business Rules

| Rule          | Description                                              |
| ------------- | -------------------------------------------------------- |
| Loan period   | 7 days                                                   |
| Fine rate     | RM 1.00 per overdue day                                  |
| Borrow limit  | 2 books per member                                       |
| Renewal limit | Each borrowed copy can be renewed once                   |
| Rating        | Members can rate books from 1 to 5 stars after return    |
| Waitlist      | Members can queue when all copies of a book are borrowed |

---

## File Persistence

SmartLibrary stores data in plain-text files so that records remain available after the program exits.

| File               | Purpose                                                                      |
| ------------------ | ---------------------------------------------------------------------------- |
| `library.txt`      | Stores book details, copies, borrow status, and ratings                      |
| `users.txt`        | Stores member details, borrowed books, genre counts, read history, and fines |
| `reservations.txt` | Stores waitlists for books                                                   |
| `history.txt`      | Stores borrowing history                                                     |
| `fines.txt`        | Stores fine records                                                          |

---

## Example Workflow

1. Admin logs in using PIN `1234`.
2. Admin registers a new member.
3. Admin adds a new book with ISBN, title, author, location, genre, and quantity.
4. Admin manages the member cart and checks out a book.
5. The system assigns a due date automatically.
6. Member returns the book.
7. If the book is overdue, the system calculates the fine.
8. Admin can view overdue reports and monthly activity reports.
9. Member can view profile, borrowing history, and recommendations.

---

## Key DSA Concepts Explained

### Binary Search Tree

Books are stored by ISBN inside a Binary Search Tree. This allows books to be arranged in sorted order and searched efficiently by ISBN.

### HashMap Indexes

The system also uses HashMaps to support faster searching by:

* Title
* Author
* Genre

This avoids depending only on ISBN search.

### Queue for Waitlist

When all copies of a book are borrowed, members can join a waitlist. The first member who joins the queue will receive the next available copy first.

### Stack for Borrowing History

Borrowing records are stored using a stack. The most recent borrowing activity appears first when viewing history.

### Recommendation System

The recommendation system checks a member’s most borrowed genre and suggests unread books from that genre.

---

## Sample Data Format

### `library.txt`

```text
BOOK | 1001 | Dune | Frank Herbert | Aisle 1 / Shelf A | Sci-Fi | 15
COPY | 1 | 25006757 | 2026-06-03 | 2026-06-10 | false
COPY | 2 | null | null | null | false
RATING | 25006757 | 5
```

### `users.txt`

```text
25006757 | TIONG TSUI JEFF | 1001:1,1004:1 | Sci-Fi:3,Non-Fiction:5 | 1001,1002,1004 | 0.00
```

### `reservations.txt`

```text
1004 | 25006805,25006754
```

### `history.txt`

```text
25006757 | Dune | 1001 | 1 | 2026-06-03 | Sci-Fi
```

### `fines.txt`

```text
2026-06-03 | 4.00
```

---

## Predefined Genres

The system supports the following genres:

* Sci-Fi
* Fantasy
* Mystery
* Romance
* Classic
* Non-Fiction
* Horror
* Thriller
* History
* Philosophy
* Religion
* Biography

---
