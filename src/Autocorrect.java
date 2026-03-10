import java.beans.PropertyEditorSupport;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Autocorrect
 * <p>
 * A command-line tool to suggest similar words when given one not in the dictionary.
 * </p>
 * @author Zach Blick
 * @author Jacob Lowe
 */
public class Autocorrect {
    String[] words;
    int threshold;
    /**
     * Constucts an instance of the Autocorrect class.
     * @param words The dictionary of acceptable words.
     * @param threshold The maximum number of edits a suggestion can have.
     */
    public Autocorrect(String[] words, int threshold) {
        this.words = words;
        this.threshold = threshold;
    }

    /**
     * Runs a test from the tester file, AutocorrectTester.
     * @param typed The (potentially) misspelled word, provided by the user.
     * @return An array of all dictionary words with an edit distance less than or equal
     * to threshold, sorted by edit distance, then sorted alphabetically.
     */
    public String[] runTest(String typed) {

        // Store an array of words within the edit distance threshold
        ArrayList<String> wordResults = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            if (lev(typed, words[i]) <= threshold) {
                wordResults.add(words[i]);
            }
        }

        // Sort by edit distance then alphabetically
        wordResults.sort((a, b) -> {

            // Compares edit distance
            int A = lev(typed, a);
            int B = lev(typed, b);
            if (A != B) {
                return A - B;
            }

            // Compares alphabetical location
            return a.compareTo(b);
        });

        // Converts arraylist to array
//        System.out.println(wordResults);
        return wordResults.toArray(new String[0]);
    }

    private int lev(String a, String b) {

        // Tabulation 2D Array
        int[][] tab = new int[a.length() + 1][b.length() + 1];

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
                int editDistance;

                // Characters match || Characters don't match
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    editDistance = 0;
                } else {
                    editDistance = 1;
                }

                // Up || Left
                int minUpLeft = Math.min(tab[i - 1][j] + 1, tab[i][j-1] + 1);

                // Diagonal
                tab[i][j] = Math.min(minUpLeft, tab[i - 1][j - 1] + editDistance);
            }
        }
        return tab[a.length()][b.length()];
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