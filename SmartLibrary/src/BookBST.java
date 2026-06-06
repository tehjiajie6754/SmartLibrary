import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * TASK 1 / TASK 3  –  Binary Search Tree (BST) Catalogue
 * ============================================================
 * Primary index  : BST keyed by ISBN              O(log n)
 * Secondary indexes (HashMaps):
 *   titleIndex  : lowercase title  → ISBN         O(1) exact / O(k) partial
 *   authorIndex : lowercase author → List<ISBN>
 *   genreIndex  : lowercase genre  → List<ISBN>   O(1) genre lookup
 *
 * All three maps stay in sync on every insert / remove.
 * ============================================================
 */
public class BookBST {

    private Book root;

    private final Map<String, Integer>       titleIndex  = new HashMap<>();
    private final Map<String, List<Integer>> authorIndex = new HashMap<>();
    private final Map<String, List<Integer>> genreIndex  = new HashMap<>();

    // ---------------------------------------------------------
    // INSERT
    // ---------------------------------------------------------
    public void insert(Book book) {
        root = insertRecursive(root, book);
        addToIndex(book);
    }

    private Book insertRecursive(Book node, Book book) {
        if (node == null) return book;
        if (book.isbn < node.isbn)      node.left  = insertRecursive(node.left,  book);
        else if (book.isbn > node.isbn) node.right = insertRecursive(node.right, book);
        // duplicate isbn: silently ignore
        return node;
    }

    private void addToIndex(Book book) {
        titleIndex.put(book.title.toLowerCase(), book.isbn);
        authorIndex.computeIfAbsent(book.author.toLowerCase(), k -> new ArrayList<>()).add(book.isbn);
        genreIndex .computeIfAbsent(book.genre.toLowerCase(),  k -> new ArrayList<>()).add(book.isbn);
    }

    private void removeFromIndex(Book book) {
        titleIndex.remove(book.title.toLowerCase());

        List<Integer> byAuthor = authorIndex.get(book.author.toLowerCase());
        if (byAuthor != null) {
            byAuthor.remove(Integer.valueOf(book.isbn));
            if (byAuthor.isEmpty()) authorIndex.remove(book.author.toLowerCase());
        }

        List<Integer> byGenre = genreIndex.get(book.genre.toLowerCase());
        if (byGenre != null) {
            byGenre.remove(Integer.valueOf(book.isbn));
            if (byGenre.isEmpty()) genreIndex.remove(book.genre.toLowerCase());
        }
    }

    // ---------------------------------------------------------
    // SEARCH by ISBN  (primary key, O(log n))
    // ---------------------------------------------------------
    public Book search(int isbn) {
        return searchRecursive(root, isbn);
    }

    private Book searchRecursive(Book node, int isbn) {
        if (node == null)      return null;
        if (isbn == node.isbn) return node;
        if (isbn < node.isbn)  return searchRecursive(node.left,  isbn);
        return searchRecursive(node.right, isbn);
    }

    // ---------------------------------------------------------
    // SEARCH by Title  (secondary index, O(1) exact / O(k) partial)
    // ---------------------------------------------------------
    public List<Book> searchByTitle(String query) {
        List<Book> results = new ArrayList<>();
        String q = query.toLowerCase();
        for (Map.Entry<String, Integer> e : titleIndex.entrySet()) {
            if (e.getKey().contains(q)) {
                Book b = search(e.getValue());
                if (b != null) results.add(b);
            }
        }
        return results;
    }

    // ---------------------------------------------------------
    // SEARCH by Author  (secondary index)
    // ---------------------------------------------------------
    public List<Book> searchByAuthor(String query) {
        List<Book> results = new ArrayList<>();
        String q = query.toLowerCase();
        for (Map.Entry<String, List<Integer>> e : authorIndex.entrySet()) {
            if (e.getKey().contains(q)) {
                for (int isbn : e.getValue()) {
                    Book b = search(isbn);
                    if (b != null) results.add(b);
                }
            }
        }
        return results;
    }

    // ---------------------------------------------------------
    // SEARCH by Genre  (secondary index, O(1) exact genre lookup)
    // ---------------------------------------------------------
    public List<Book> searchByGenre(String genre) {
        List<Book> results = new ArrayList<>();
        List<Integer> isbns = genreIndex.get(genre.toLowerCase());
        if (isbns != null) {
            for (int isbn : isbns) {
                Book b = search(isbn);
                if (b != null) results.add(b);
            }
        }
        return results;
    }

    // ---------------------------------------------------------
    // REMOVE
    // ---------------------------------------------------------
    public void remove(int isbn) {
        Book toRemove = search(isbn);
        if (toRemove != null) removeFromIndex(toRemove);
        root = removeRecursive(root, isbn);
    }

    private Book removeRecursive(Book node, int isbn) {
        if (node == null) return null;
        if (isbn < node.isbn) {
            node.left  = removeRecursive(node.left,  isbn);
        } else if (isbn > node.isbn) {
            node.right = removeRecursive(node.right, isbn);
        } else {
            if (node.left  == null) return node.right;
            if (node.right == null) return node.left;

            // Two children: replace with in-order successor
            Book successor    = findMin(node.right);
            node.isbn         = successor.isbn;
            node.title        = successor.title;
            node.author       = successor.author;
            node.location     = successor.location;
            node.genre        = successor.genre;
            node.borrowCount  = successor.borrowCount;
            node.copies       = successor.copies;
            node.waitlist     = successor.waitlist;
            node.right        = removeRecursive(node.right, successor.isbn);
        }
        return node;
    }

    private Book findMin(Book node) {
        while (node.left != null) node = node.left;
        return node;
    }

    // ---------------------------------------------------------
    // Collect all books in sorted ISBN order (for persistence + hottest)
    // ---------------------------------------------------------
    public void getAllBooks(List<Book> list) {
        collectInOrder(root, list);
    }

    private void collectInOrder(Book node, List<Book> list) {
        if (node == null) return;
        collectInOrder(node.left, list);
        list.add(node);
        collectInOrder(node.right, list);
    }
}
