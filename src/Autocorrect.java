import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Autocorrect
 * <p>
 * A command-line tool to suggest similar words when given one not in the dictionary.
 * </p>
 * @author Zach Blick
 * @author Jacob Lowe
 */

public class Autocorrect {
    private String[] words;
    private int threshold;
    private HashMap<String, Long> wordFrequency;
    private static final int MAX_SUGGESTIONS = 10;
    private static final int LONG_WORD_LENGTH = 7;
    private static final int MEDIUM_WORD_LENGTH = 4;

    // Keyboard adjacency map for keys with their neighbors
    private static final HashMap<Character, String> KEYBOARD = new HashMap<>();
    static {
        KEYBOARD.put('q', "was");   KEYBOARD.put('w', "qeasd"); KEYBOARD.put('e', "wrsdf");
        KEYBOARD.put('r', "etdfg"); KEYBOARD.put('t', "ryfgh"); KEYBOARD.put('y', "tughj");
        KEYBOARD.put('u', "yihjk"); KEYBOARD.put('i', "uojkl"); KEYBOARD.put('o', "ipkl");
        KEYBOARD.put('p', "ol");    KEYBOARD.put('a', "qwszx"); KEYBOARD.put('s', "awedxz");
        KEYBOARD.put('d', "serfcx"); KEYBOARD.put('f', "dtgvc"); KEYBOARD.put('g', "fyhbv");
        KEYBOARD.put('h', "gynjb"); KEYBOARD.put('j', "huknm"); KEYBOARD.put('k', "jilm");
        KEYBOARD.put('l', "kop");   KEYBOARD.put('z', "asx");   KEYBOARD.put('x', "zsdc");
        KEYBOARD.put('c', "xdfv");  KEYBOARD.put('v', "cfgb");  KEYBOARD.put('b', "vghn");
        KEYBOARD.put('n', "bhjm");  KEYBOARD.put('m', "njk");
    }

    public static void main(String[] args) {

        // Load dictionary and autocorrect
        String[] words = loadDictionary("large");
        Autocorrect autocorrect = new Autocorrect(words, 2);
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter a word: ");

        while (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim().toLowerCase();

            // Calculate threshold based on word length
            autocorrect.setThreshold(calculateThreshold(input));

            String[] suggestions = autocorrect.runTest(input);

            if (suggestions.length ==0) {
                System.out.println("No suggestions found for \"" + input + "\"");
            } else {
                System.out.println("Suggestions for \"" + input + "\": ");
                for (String s : suggestions) {
                    System.out.println("   " + s);
                }
            }
            System.out.println("\nEnter a word: ");
        }
        scanner.close();
    }

    public static int calculateThreshold(String input) {

        // Short words get less threshold, long words get more tolerance
        if (input.length() >= LONG_WORD_LENGTH) {
            return 3;
        } else if (input.length() >= MEDIUM_WORD_LENGTH) {
            return 2;
        } else {
            return 1;
        }
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    /**
     * Constucts an instance of the Autocorrect class.
     * @param words The dictionary of acceptable words.
     * @param threshold The maximum number of edits a suggestion can have.
     */
    public Autocorrect(String[] words, int threshold) {
        this.words = words;
        this.threshold = threshold;
        this.wordFrequency = loadFrequencies("unigram_freq.csv");
    }

    /**
     * Runs a test from the tester file, AutocorrectTester.
     * @param typed The (potentially) misspelled word, provided by the user.
     * @return An array of all dictionary words with an edit distance less than or equal
     * to threshold, sorted by edit distance, then sorted alphabetically.
     */
    public String[] runTest(String typed) {
        ArrayList<String> results = new ArrayList<>();
        HashMap<String, Double> distanceMap = new HashMap<String, Double>();

        for (String word : words) {

            // Length distance check, skip words with distance > threshold
            if (Math.abs(word.length()- typed.length()) > threshold) continue;

            // Only add word if it's within edit distance threshold
            double distance = lev(typed, word);
            if (distance <= threshold) {
                results.add(word);
                distanceMap.put(word, distance);
            }
        }

        // Sort words based on weights and frequencies
        // Log10 compresses the large number frequencies into a smaller amount
        // The lower score means a better suggestion
        results.sort((a, b) -> {
            long frequencyA = wordFrequency.getOrDefault(a, 1L);
            long frequencyB = wordFrequency.getOrDefault(b, 1L);

            double scoreA = distanceMap.get(a) - (Math.log10(frequencyA) / 10.0);
            double scoreB = distanceMap.get(b) - (Math.log10(frequencyB) / 10.0);

            return Double.compare(scoreA, scoreB);
        });

        // Returns the top 10 suggestions
        return results.stream().limit(MAX_SUGGESTIONS).toArray(String[]::new);
    }

    /**
     * Loads the unigram frequency CSV into a HashMap.
     * Words not present in the CSV default to a frequency of 1 (lowest rank).
     * @param filepath
     * @return
     */
    private static HashMap<String, Long> loadFrequencies(String filepath) {
        HashMap<String, Long> map = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    map.put(parts[0].trim(), Long.parseLong(parts[1].trim()));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load frequency file: " + filepath, e);
        }
        return map;
    }

    /**
     * Determines Levenshtein edit distanhce between two strings using dynamic programming.
     * Substitution cost determines based on keyboard layout, near typos cost 0.5, while others cost 1.0.
     * @param a
     * @param b
     * @return
     */
    private double lev(String a, String b) {

        // Tabulation 2D Array
        double[][] tab = new double[a.length() + 1][b.length() + 1];

        // Deletion
        for (int i = 0; i <= a.length(); i++) {
            tab[i][0] = i;
        }

        // Insertion
        for (int j = 0; j <= b.length(); j++) {
            tab[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                double editCost = substitutionCost(a.charAt(i - 1), b.charAt(j - 1));

                // Up || Left
                double minUpLeft = Math.min(tab[i - 1][j] + 1, tab[i][j-1] + 1);

                // Diagonal
                tab[i][j] = Math.min(minUpLeft, tab[i - 1][j - 1] + editCost);
            }
        }
        return tab[a.length()][b.length()];
    }

    /**
     * Returns the substitution cost between two characters.
     * Adjacent keys on the keyboard cost 0.5, reasonable due to a typo
     * Other substitutions cost 1.0
     * @param a
     * @param b
     * @return
     */
    private double substitutionCost(char a, char b) {
        if (a == b) return 0.0;
        String neighbors = KEYBOARD.getOrDefault(a, "");

        // Return higher cost for farther letters
        if (neighbors.indexOf(b) >= 0) {
            return 0.5;
        } else {
            return 1.0;
        }
    }

    /**
     * Loads a dictionary of words from the provided textfiles in the dictionaries directory.
     * @param dictionary The name of the textfile, [dictionary].txt, in the dictionaries directory.
     * @return An array of Strings containing all words in alphabetical order.
     */
    private static String[] loadDictionary(String dictionary)  {
        try {
            String line;
            BufferedReader dictReader = new BufferedReader(new FileReader("dictionaries/" + dictionary + ".txt"));
            line = dictReader.readLine();

            // Update instance variables with test data
            int n = Integer.parseInt(line);
            String[] words = new String[n];

            for (int i = 0; i < n; i++) {
                line = dictReader.readLine();
                words[i] = line;
            }
            return words;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}