package embeddings.distance;

import embeddings.features.Feature;

public interface FeatureDistance {

//    double distance(int[][] a, int[][] b);
//
//    double distance(int[] a, int[] b);

    double distance(Feature a, Feature b);
}
