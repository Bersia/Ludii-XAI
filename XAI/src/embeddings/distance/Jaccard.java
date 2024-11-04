package embeddings.distance;

import java.util.ArrayList;
import java.util.HashSet;

public class Jaccard {
    public static double distance(ArrayList<Integer> a, ArrayList<Integer> b) {
        // Convert lists to sets to find the unique elements
        HashSet<Integer> setA = new HashSet<>(a);
        HashSet<Integer> setB = new HashSet<>(b);

        // Find the intersection of setA and setB
        HashSet<Integer> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        // Find the union of setA and setB
        HashSet<Integer> union = new HashSet<>(setA);
        union.addAll(setB);

        // Calculate Jaccard distance
        if (union.size() == 0) {
            return 0.0; // Define distance as 0 if both sets are empty
        }

        double jaccardSimilarity = (double) intersection.size() / union.size();
        return 1.0 - jaccardSimilarity; // Distance is 1 - similarity
    }
}
