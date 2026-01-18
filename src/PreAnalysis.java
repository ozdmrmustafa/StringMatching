/**
 * PreAnalysis interface for students to implement their algorithm selection logic
 * 
 * Students should analyze the characteristics of the text and pattern to determine
 * which algorithm would be most efficient for the given input.
 * 
 * The system will automatically use this analysis if the chooseAlgorithm method
 * returns a non-null value.
 */
public abstract class PreAnalysis {
    
    /**
     * Analyze the text and pattern to choose the best algorithm
     * 
     * @param text The text to search in
     * @param pattern The pattern to search for
     * @return The name of the algorithm to use (e.g., "Naive", "KMP", "RabinKarp", "BoyerMoore", "GoCrazy")
     *         Return null if you want to skip pre-analysis and run all algorithms
     * 
     * Tips for students:
     * - Consider the length of the text and pattern
     * - Consider the characteristics of the pattern (repeating characters, etc.)
     * - Consider the alphabet size
     * - Think about which algorithm performs best in different scenarios
     */
    public abstract String chooseAlgorithm(String text, String pattern);
    
    /**
     * Get a description of your analysis strategy
     * This will be displayed in the output
     */
    public abstract String getStrategyDescription();
}


/**
 * Default implementation that students should modify
 * This is where students write their pre-analysis logic
 */
class StudentPreAnalysis extends PreAnalysis {

    @Override
    public String chooseAlgorithm(String text, String pattern) {
        if (text == null) text = "";
        if (pattern == null) pattern = "";

        int n = text.length();
        int m = pattern.length();

        // Edge cases (benchmark'lerinde Empty Pattern'da KMP iyi çıkabiliyor)
        if (m == 0) return "KMP";
        if (n == 0) return "Naive";
        if (m > n) return "Naive";

        // Features (cheap + stable)
        int uniq = uniqueCharCount(pattern, 128);
        double uniqRatio = (double) uniq / (double) m;     // 0..1
        double runRatio = maxRunRatio(pattern);            // 0..1
        double overlapRatio = borderRatio(pattern);        // 0..1 (KMP-ish)

        // Score each algorithm (not a decision tree)
        double scoreNaive = 0;
        double scoreKMP = 0;
        double scoreRK = 0;
        double scoreBM = 0;
        double scoreGoCrazy = 0;

        // Naive: zero preprocess, short patterns & moderate inputs
        scoreNaive += 8.0;
        scoreNaive += (m <= 4 ? 6.0 : 0.0);
        scoreNaive += (n <= 200 ? 2.0 : 0.0);
        scoreNaive -= (n > 5000 ? 3.0 : 0.0);

        // KMP: benefits with self-overlap / repetitiveness
        scoreKMP += 4.0;
        scoreKMP += 10.0 * overlapRatio;   // strong boost if big border
        scoreKMP += 6.0 * runRatio;        // boost if long runs (aaaaa.. or abab..)
        scoreKMP -= (m <= 3 ? 3.0 : 0.0);  // overhead not worth it for tiny patterns

        // Rabin-Karp: good when text is large; hash amortization
        scoreRK += 3.0;
        scoreRK += (n > 1500 ? 5.0 : 0.0);
        scoreRK += (m >= 8 ? 2.0 : -1.0);
        scoreRK += (uniqRatio >= 0.6 ? 1.0 : 0.0); // less repetitive patterns are fine

        // Boyer-Moore: shines with larger alphabet & longer pattern
        scoreBM += 3.0;
        scoreBM += (m >= 10 ? 4.0 : -1.0);
        scoreBM += 8.0 * uniqRatio;              // more unique chars -> better skipping chance
        scoreBM += (n >= 800 ? 2.0 : 0.0);
        scoreBM -= (uniq <= 3 ? 3.0 : 0.0);      // tiny alphabet hurts BM

        // GoCrazy: your custom hybrid -> we treat it as "small alphabet + mid patterns"
        scoreGoCrazy += 2.0;
        scoreGoCrazy += (uniq <= 4 ? 5.0 : 0.0);
        scoreGoCrazy += (m >= 5 && m <= 20 ? 2.0 : 0.0);
        scoreGoCrazy += (n >= 200 ? 1.5 : 0.0);

        // Pick max score
        String best = "Naive";
        double bestScore = scoreNaive;

        if (scoreKMP > bestScore) { bestScore = scoreKMP; best = "KMP"; }
        if (scoreRK > bestScore)  { bestScore = scoreRK;  best = "RabinKarp"; }
        if (scoreBM > bestScore)  { bestScore = scoreBM;  best = "BoyerMoore"; }
        if (scoreGoCrazy > bestScore) { bestScore = scoreGoCrazy; best = "GoCrazy"; }

        return best;
    }

    @Override
    public String getStrategyDescription() {
        return "Score-based pre-analysis: compute pattern features (unique ratio, runs, border overlap) + sizes (n,m), then choose highest-scoring algorithm.";
    }

    // ---- feature helpers ----

    private int uniqueCharCount(String s, int cap) {
        java.util.HashSet<Character> set = new java.util.HashSet<>();
        for (int i = 0; i < s.length() && set.size() < cap; i++) {
            set.add(s.charAt(i));
        }
        return set.size();
    }

    // max run length / m  (e.g. "aaaaab" -> 5/6)
    private double maxRunRatio(String s) {
        int m = s.length();
        int best = 1, cur = 1;
        for (int i = 1; i < m; i++) {
            if (s.charAt(i) == s.charAt(i - 1)) cur++;
            else { best = Math.max(best, cur); cur = 1; }
        }
        best = Math.max(best, cur);
        return (double) best / (double) m;
    }

    // longest proper prefix which is also suffix (KMP border) / m
    // computed via prefix-function (pi array)
    private double borderRatio(String p) {
        int m = p.length();
        if (m <= 1) return 0.0;

        int[] pi = new int[m];
        int j = 0;
        for (int i = 1; i < m; i++) {
            while (j > 0 && p.charAt(i) != p.charAt(j)) j = pi[j - 1];
            if (p.charAt(i) == p.charAt(j)) j++;
            pi[i] = j;
        }
        int border = pi[m - 1];
        return (double) border / (double) m;
    }
}



/**
 * Example implementation showing how pre-analysis could work
 * This is for demonstration purposes
 */
class ExamplePreAnalysis extends PreAnalysis {

    @Override
    public String chooseAlgorithm(String text, String pattern) {
        int textLen = text.length();
        int patternLen = pattern.length();

        // Simple heuristic example
        if (patternLen <= 3) {
            return "Naive"; // For very short patterns, naive is often fastest
        } else if (hasRepeatingPrefix(pattern)) {
            return "KMP"; // KMP is good for patterns with repeating prefixes
        } else if (patternLen > 10 && textLen > 1000) {
            return "RabinKarp"; // RabinKarp can be good for long patterns in long texts
        } else {
            return "Naive"; // Default to naive for other cases
        }
    }

    private boolean hasRepeatingPrefix(String pattern) {
        if (pattern.length() < 2) return false;

        // Check if first character repeats
        char first = pattern.charAt(0);
        int count = 0;
        for (int i = 0; i < Math.min(pattern.length(), 5); i++) {
            if (pattern.charAt(i) == first) count++;
        }
        return count >= 3;
    }

    @Override
    public String getStrategyDescription() {
        return "Example strategy: Choose based on pattern length and characteristics";
    }
}

/**
 * Instructor's pre-analysis implementation (for testing purposes only)
 * Students should NOT modify this class
 */
class InstructorPreAnalysis extends PreAnalysis {

    @Override
    public String chooseAlgorithm(String text, String pattern) {
        // This is a placeholder for instructor testing
        // Students should focus on implementing StudentPreAnalysis
        return null;
    }

    @Override
    public String getStrategyDescription() {
        return "Instructor's testing implementation";
    }
}
