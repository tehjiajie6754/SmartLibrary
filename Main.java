import java.util.Scanner;

class Node {
    int digit;
    Node prev, next;

    Node(int digit) {
        this.digit = digit;
    }
}

class LargeNumber {
    Node head, tail;
    boolean negative;
    int size;

    LargeNumber() {}

    LargeNumber(String s) {
        int i = 0;
        if (!s.isEmpty() && s.charAt(0) == '-') { negative = true; i = 1; }
        // skip leading zeros but keep at least one digit
        while (i < s.length() - 1 && s.charAt(i) == '0') i++;
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c >= '0' && c <= '9') appendLSD(c - '0');
        }
        if (size == 0) appendLSD(0);
    }

    // Add digit as new least-significant (appends to tail)
    void appendLSD(int d) {
        Node n = new Node(d);
        if (tail == null) { head = tail = n; }
        else { tail.next = n; n.prev = tail; tail = n; }
        size++;
    }

    // Add digit as new most-significant (prepends to head)
    void prependMSD(int d) {
        Node n = new Node(d);
        if (head == null) { head = tail = n; }
        else { n.next = head; head.prev = n; head = n; }
        size++;
    }

    boolean isZero() { return size == 1 && head.digit == 0; }

    void trimLeadingZeros() {
        while (head != null && head != tail && head.digit == 0) {
            head = head.next;
            head.prev = null;
            size--;
        }
    }

    static LargeNumber copyOf(LargeNumber a) {
        LargeNumber c = new LargeNumber();
        c.negative = a.negative;
        for (Node n = a.head; n != null; n = n.next) c.appendLSD(n.digit);
        return c;
    }

    @Override
    public String toString() {
        if (size == 0) return "0";
        StringBuilder sb = new StringBuilder();
        if (negative && !isZero()) sb.append('-');
        for (Node n = head; n != null; n = n.next) sb.append(n.digit);
        return sb.toString();
    }

    // ── Magnitude helpers ────────────────────────────────────────────────────

    // >0 if |a|>|b|, <0 if |a|<|b|, 0 if equal
    static int cmpMag(LargeNumber a, LargeNumber b) {
        if (a.size != b.size) return Integer.compare(a.size, b.size);
        Node ca = a.head, cb = b.head;
        while (ca != null) {
            if (ca.digit != cb.digit) return Integer.compare(ca.digit, cb.digit);
            ca = ca.next; cb = cb.next;
        }
        return 0;
    }

    // |a| + |b|
    static LargeNumber addMag(LargeNumber a, LargeNumber b) {
        LargeNumber r = new LargeNumber();
        Node ca = a.tail, cb = b.tail;
        int carry = 0;
        while (ca != null || cb != null || carry != 0) {
            int s = carry;
            if (ca != null) { s += ca.digit; ca = ca.prev; }
            if (cb != null) { s += cb.digit; cb = cb.prev; }
            r.prependMSD(s % 10);
            carry = s / 10;
        }
        return r;
    }

    // |a| - |b|, requires |a| >= |b|
    static LargeNumber subMag(LargeNumber a, LargeNumber b) {
        LargeNumber r = new LargeNumber();
        Node ca = a.tail, cb = b.tail;
        int borrow = 0;
        while (ca != null) {
            int d = ca.digit - borrow - (cb != null ? cb.digit : 0);
            if (d < 0) { d += 10; borrow = 1; } else borrow = 0;
            r.prependMSD(d);
            ca = ca.prev;
            if (cb != null) cb = cb.prev;
        }
        r.trimLeadingZeros();
        return r;
    }

    // |a| * |b|  (grade-school, LSB-indexed arrays)
    static LargeNumber mulMag(LargeNumber a, LargeNumber b) {
        int na = a.size, nb = b.size;
        int[] d = new int[na + nb];
        int[] ad = new int[na], bd = new int[nb];
        Node n = a.tail;
        for (int i = 0; i < na; i++, n = n.prev) ad[i] = n.digit;
        n = b.tail;
        for (int i = 0; i < nb; i++, n = n.prev) bd[i] = n.digit;
        for (int i = 0; i < na; i++)
            for (int j = 0; j < nb; j++)
                d[i + j] += ad[i] * bd[j];
        for (int i = 0; i < d.length - 1; i++) { d[i + 1] += d[i] / 10; d[i] %= 10; }
        LargeNumber r = new LargeNumber();
        int s = d.length - 1;
        while (s > 0 && d[s] == 0) s--;
        for (int i = s; i >= 0; i--) r.appendLSD(d[i]);
        return r;
    }

    // Long division of magnitudes; returns {quotient, remainder}
    static LargeNumber[] divMag(LargeNumber a, LargeNumber b) {
        if (b.isZero()) throw new ArithmeticException("Division by zero");
        if (cmpMag(a, b) < 0) return new LargeNumber[]{ new LargeNumber("0"), copyOf(a) };

        LargeNumber quotient = new LargeNumber();
        LargeNumber cur = new LargeNumber("0");
        LargeNumber ten = new LargeNumber("10");

        for (Node n = a.head; n != null; n = n.next) {
            // bring down next digit
            cur = addMag(mulMag(cur, ten), new LargeNumber(String.valueOf(n.digit)));

            // find largest q in [0,9] s.t. q*b <= cur
            int q = 0;
            LargeNumber qProd = new LargeNumber("0");
            for (int t = 9; t >= 1; t--) {
                LargeNumber prod = mulMag(b, new LargeNumber(String.valueOf(t)));
                if (cmpMag(cur, prod) >= 0) { q = t; qProd = prod; break; }
            }
            quotient.appendLSD(q);
            cur = subMag(cur, qProd);
        }
        quotient.trimLeadingZeros();
        return new LargeNumber[]{ quotient, cur };
    }

    // ── Public arithmetic ─────────────────────────────────────────────────────

    public static LargeNumber add(LargeNumber a, LargeNumber b) {
        LargeNumber r;
        if (a.negative == b.negative) {
            r = addMag(a, b);
            r.negative = a.negative;
        } else {
            int c = cmpMag(a, b);
            if (c >= 0) { r = subMag(a, b); r.negative = a.negative; }
            else        { r = subMag(b, a); r.negative = b.negative; }
        }
        if (r.isZero()) r.negative = false;
        return r;
    }

    public static LargeNumber subtract(LargeNumber a, LargeNumber b) {
        LargeNumber nb = copyOf(b);
        if (!b.isZero()) nb.negative = !b.negative;
        return add(a, nb);
    }

    public static LargeNumber multiply(LargeNumber a, LargeNumber b) {
        LargeNumber r = mulMag(a, b);
        r.negative = a.negative != b.negative && !r.isZero();
        return r;
    }

    public static String divide(LargeNumber a, LargeNumber b) {
        if (b.isZero()) return "Error: division by zero";
        boolean neg = a.negative != b.negative && !a.isZero();

        LargeNumber aa = copyOf(a); aa.negative = false;
        LargeNumber ba = copyOf(b); ba.negative = false;

        LargeNumber[] qr = divMag(aa, ba);
        StringBuilder sb = new StringBuilder();
        if (neg) sb.append('-');
        sb.append(qr[0]);

        LargeNumber rem = qr[1];
        if (!rem.isZero()) {
            sb.append('.');
            LargeNumber ten = new LargeNumber("10");
            for (int i = 0; i < 20 && !rem.isZero(); i++) {
                rem = mulMag(rem, ten);
                LargeNumber[] dr = divMag(rem, ba);
                sb.append(dr[0]);   // always a single digit 0-9
                rem = dr[1];
            }
        }
        return sb.toString();
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter m: ");
        LargeNumber m = new LargeNumber(sc.nextLine().trim());
        System.out.print("Enter n: ");
        LargeNumber n = new LargeNumber(sc.nextLine().trim());
        sc.close();

        System.out.println("addition       = " + LargeNumber.add(m, n));
        System.out.println("subtraction    = " + LargeNumber.subtract(m, n));
        System.out.println("multiplication = " + LargeNumber.multiply(m, n));
        System.out.println("division       = " + LargeNumber.divide(m, n));
    }
}
