import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

public class Main {
    /** Holds a single root (xᵢ, yᵢ) */
    static class Root {
        final int x;
        final BigInteger y;

        Root(int x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) throws IOException {
        // 1) Load JSON text
        Path jsonPath = Paths.get("input.json");
        if (!Files.exists(jsonPath) || !Files.isReadable(jsonPath)) {
            System.err.println("Cannot read input.json at " + jsonPath.toAbsolutePath());
            System.exit(1);
        }
        String json = Files.readString(jsonPath);

        // 2) Extract n and k
        String keysObj = extractObject(json, "keys");
        int n = extractInt(keysObj, "n");
        int k = extractInt(keysObj, "k");

        // 3) Collect the first k roots
        List<Root> roots = new ArrayList<>();
        for (int i = 1; i <= n && roots.size() < k; i++) {
            String key = String.valueOf(i);
            if (!json.contains("\"" + key + "\""))
                continue;
            String obj = extractObject(json, key);

            // base is quoted, so extract as string then parse
            String baseStr = extractString(obj, "base");
            int base = Integer.parseInt(baseStr);

            String val = extractString(obj, "value");
            BigInteger y = decodeBaseToBigInteger(val, base);

            roots.add(new Root(i, y));
        }
        if (roots.size() < k) {
            System.err.println("Found only " + roots.size() + " roots, but k = " + k);
            System.exit(2);
        }

        // 4) Compute secret = P(0)
        BigInteger secret = lagrangeInterpolationAtZero(roots);

        // 5) Print only the secret
        System.out.println(secret);
    }

    // ——— JSON Helpers ——————————————————————————————————

    /** Extracts the {...} block immediately following "key" */
    private static String extractObject(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0)
            throw new IllegalArgumentException("Missing key: " + key);
        int start = json.indexOf("{", idx);
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{')
                depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(start, i + 1);
                }
            }
        }
        throw new IllegalArgumentException("Unmatched braces for key: " + key);
    }

    /** Finds "key": 123 (possibly with spaces) */
    private static int extractInt(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(json);
        if (m.find())
            return Integer.parseInt(m.group(1));
        throw new IllegalArgumentException("Unable to extract integer for key: " + key);
    }

    /** Finds "key": "someString" (possibly with spaces) */
    private static String extractString(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (m.find())
            return m.group(1);
        throw new IllegalArgumentException("Unable to extract string for key: " + key);
    }

    // ——— Base-36 Decoder ——————————————————————————————————

    /** Decodes s in base-[2..36] into a BigInteger */
    private static BigInteger decodeBaseToBigInteger(String s, int base) {
        if (base < Character.MIN_RADIX || base > Character.MAX_RADIX) {
            throw new IllegalArgumentException("Base must be 2..36, got " + base);
        }
        BigInteger result = BigInteger.ZERO;
        BigInteger b = BigInteger.valueOf(base);
        BigInteger place = BigInteger.ONE;

        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            int digit = Character.isDigit(c)
                    ? c - '0'
                    : Character.toUpperCase(c) - 'A' + 10;
            if (digit >= base) {
                throw new IllegalArgumentException(
                        "Invalid digit '" + c + "' for base " + base);
            }
            result = result.add(place.multiply(BigInteger.valueOf(digit)));
            place = place.multiply(b);
        }
        return result;
    }

    // ——— Lagrange Interpolation at x = 0 ——————————————————————————————————

    /**
     * Computes P(0) exactly given k roots (xᵢ, yᵢ) of a degree-(k-1) polynomial
     * via:
     * P(0) = Σᵢ [ yᵢ · ∏_{j≠i} (0 - xⱼ)/(xᵢ - xⱼ) ]
     */
    private static BigInteger lagrangeInterpolationAtZero(List<Root> roots) {
        BigInteger sum = BigInteger.ZERO;
        int k = roots.size();

        for (int i = 0; i < k; i++) {
            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;
            int xi = roots.get(i).x;

            for (int j = 0; j < k; j++) {
                if (i == j)
                    continue;
                int xj = roots.get(j).x;
                num = num.multiply(BigInteger.valueOf(-xj));
                den = den.multiply(BigInteger.valueOf(xi - xj));
            }

            // term = yᵢ * (num/den)
            sum = sum.add(roots.get(i).y.multiply(num).divide(den));
        }

        return sum;
    }
}


//Output for input.json is 3
