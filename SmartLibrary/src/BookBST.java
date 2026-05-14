/**
 * ============================================================
 * TASK 1 – Catalogue Architect: Binary Search Tree (BST)
 * TASK 3 – Record Finder: Recursive Search in the BST
 * ============================================================
 * WHY a BST?
 *   - Searching by ISBN is O(log n) on average.
 *   - Each node's LEFT children have smaller ISBNs.
 *   - Each node's RIGHT children have larger ISBNs.
 *
 * Visual example after inserting ISBNs 200, 100, 300, 150:
 *
 *            [200]
 *           /     \
 *        [100]   [300]
 *            \
 *           [150]
 * ============================================================
 */
public class BookBST {

    private Book root;   // the top of the tree (starts as null = empty)

    // ---------------------------------------------------------
    // PUBLIC insert – the only method outside code calls
    // ---------------------------------------------------------
    public void insert(int isbn, String title, String author) {
        root = insertRecursive(root, isbn, title, author);
    }

    /**
     * PRIVATE recursive helper for insert.
     *
     * Logic (very simple):
     *   1. If we reach a null spot → create the new Book here.
     *   2. If isbn < current node → go LEFT.
     *   3. If isbn > current node → go RIGHT.
     *   4. If isbn == current node → duplicate, do nothing.
     *   5. Return the (possibly updated) node back up.
     */
    private Book insertRecursive(Book node, int isbn, String title, String author) {
        // Base case: empty spot found – place the new book here
        if (node == null) {
            return new Book(isbn, title, author);
        }

        if (isbn < node.isbn) {
            // ISBN is smaller → go to the left subtree
            node.left = insertRecursive(node.left, isbn, title, author);
        } else if (isbn > node.isbn) {
            // ISBN is larger → go to the right subtree
            node.right = insertRecursive(node.right, isbn, title, author);
        }
        // else: isbn == node.isbn → duplicate, we simply ignore it

        return node;  // return the unchanged (or updated) node
    }

    // ---------------------------------------------------------
    // PUBLIC search – TASK 3: Recursive search  O(log n)
    // ---------------------------------------------------------
    public Book search(int isbn) {
        return searchRecursive(root, isbn);
    }

    /**
     * PRIVATE recursive helper for search.
     *
     * Logic:
     *   1. If node is null → book not found, return null.
     *   2. If isbn matches → found it! return this node.
     *   3. If isbn < node → search LEFT.
     *   4. If isbn > node → search RIGHT.
     */
    private Book searchRecursive(Book node, int isbn) {
        // Base case 1: reached a dead end → not found
        if (node == null) {
            return null;
        }

        // Base case 2: found the book!
        if (isbn == node.isbn) {
            return node;
        }

        // Recursive step: decide which half to search
        if (isbn < node.isbn) {
            return searchRecursive(node.left, isbn);   // search left
        } else {
            return searchRecursive(node.right, isbn);  // search right
        }
    }

    // ---------------------------------------------------------
    // PUBLIC remove – needed for borrowing (TASK 5)
    // ---------------------------------------------------------
    public void remove(int isbn) {
        root = removeRecursive(root, isbn);
    }

    /**
     * PRIVATE recursive helper for remove.
     *
     * Three cases when we find the node to delete:
     *   Case 1: Leaf node (no children)        → just remove it.
     *   Case 2: One child                      → replace with that child.
     *   Case 3: Two children                   → replace with the
     *           smallest value in the right subtree (in-order successor).
     */
    private Book removeRecursive(Book node, int isbn) {
        if (node == null) {
            return null;  // ISBN not in tree
        }

        if (isbn < node.isbn) {
            node.left = removeRecursive(node.left, isbn);
        } else if (isbn > node.isbn) {
            node.right = removeRecursive(node.right, isbn);
        } else {
            // *** Found the node to remove ***

            // Case 1 & 2: zero or one child
            if (node.left == null) {
                return node.right;   // right child (or null) takes over
            }
            if (node.right == null) {
                return node.left;    // left child takes over
            }

            // Case 3: two children
            // Find the smallest ISBN in the RIGHT subtree
            Book successor = findMin(node.right);
            // Copy successor's data into this node
            node.isbn   = successor.isbn;
            node.title  = successor.title;
            node.author = successor.author;
            // Delete the successor from the right subtree
            node.right = removeRecursive(node.right, successor.isbn);
        }

        return node;
    }

    /** Find the leftmost (smallest) node in a subtree */
    private Book findMin(Book node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }
}
