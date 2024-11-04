package embeddings.distance;

import embeddings.features.BoardDistribution;
import embeddings.features.Feature;
import embeddings.distance.utils.HungarianAlgorithm;

public class DTW {
    /**
     * Calculates the dynamic time warping distance between two matrices
     * a and b. Implemented from the paper
     * https://content.iospress.com/download/intelligent-data-analysis/ida215908?id=intelligent-data-analysis%2Fida215908
     *
     * @param A the first tensor
     * @param B the second tensor
     * @return the dynamic time warping distance between a and b
     */
    public static double distance(byte[][][] A, byte[][][] B) {
        double[][] results = new double[A.length][B.length];

        for(int i = 1; i < A.length; i++){
            for (int j=1; j < B.length; j++){
                results[i][j] = distance(A[i], B[j]);
//                System.out.print(results[i][j]+" ");
            }
//            System.out.println();
        }

        HungarianAlgorithm algorithm = new HungarianAlgorithm(results);
        int [] opt = algorithm.execute();
//        System.out.println(Arrays.toString(opt));
//        System.out.println("Optimal assignment:");
        double sum=0;
        for (int i = 0; i < opt.length; i++) {
//            System.out.println("Row " + i + " assigned to column " + opt[i]);
            sum+=results[i][opt[i]];
        }
        
        return sum;
    }

    /**
     * Calculates the dynamic time warping distance between two matrices
     * a and b. Implemented from the paper
     * https://content.iospress.com/download/intelligent-data-analysis/ida215908?id=intelligent-data-analysis%2Fida215908
     *
     * @param T the first matrix
     * @param M the second matrix
     * @return the dynamic time warping distance between a and b
     */
    private static double distance(byte[][] T, byte[][] M) {
        //T r x q
        //M p x q

        if(T.length!=M.length || T[0].length!=M[0].length || T[0].length!=T.length || M[0].length!=M.length){
            byte[][][] squarified = squarify(T, M);
            T = squarified[0];
            M = squarified[1];
        }
//        System.out.println(Arrays.deepToString(T));
//        System.out.println(Arrays.deepToString(M));

        double[][] distance = new double[T.length][M.length];
        for (int i = 0; i < M.length; i++) {
            for (int j = 0; j < T.length; j++) {
                for (int k = 0; k < T[0].length; k++) {
                    distance[j][i] += Math.pow(T[j][k] - M[i][k],2);
                }
            }
        }

//        System.out.println(Arrays.deepToString(distance));

        return dtw(distance)/(T[0].length*T.length*M[0].length);
        //uncomment for the frobenius cube
//        double[][][] cuboid = new double[A.length][A.length][A.length];
//        for (int i = 0; i < B[0].length; i++) { // p
//            for (int j = 0; j < A[0].length; j++) { // r
//                for (int k = 0; k < B.length; k++) { // q
//                    cuboid[j][i][k] = Math.sqrt(Math.pow(A[j][k] - B[i][k], 2.0));
//                }
//            }
//        }
//        return cuboid;
    }

    private static byte[][][] squarify(byte[][] t, byte[][] m) {
        int max = Math.max(Math.max(t.length, t[0].length), Math.max(m.length, m[0].length));

        // Squarify matrix t
        if (t.length != max || t[0].length != max) {
            byte[][] paddedT = new byte[max][max];
            for (int i = max-t.length; i < max; i++) {
                for (int j = 0; j < t[0].length; j++) {
                    paddedT[i][j] = t[i-max+t.length][j];
                }
            }
            t = paddedT;
        }

        // Squarify matrix m
        if (m.length != max || m[0].length != max) {
            byte[][] paddedM = new byte[max][max];
            for (int i = max-m.length; i < max; i++) {
                for (int j = 0; j < m[0].length; j++) {
                    paddedM[i][j] = m[i-max+m.length][j];
                }
            }
            m = paddedM;
        }
        return new byte[][][]{t,m};
    }


    /**
     * Calculates the dynamic time warping distance between two arrays
     * a and b.
     *
     * @param a the first array
     * @param b the second array
     * @return the dynamic time warping distance between a and b
     */
    private static double distance(byte[] a, byte[] b) {
        double[][]lL = new double[a.length][b.length];
        for(int i = 0; i < a.length; i++){
            for (int j = 0; j < b.length; j++) {
                lL[i][j] = Math.abs(a[i]-b[j]);
            }
        }
        return dtw(lL);
    }

    private static double dtw(double[][] lL) {
        // Declare Iteration Constants.
        final int lN = lL.length;
        final int lM = lL[0].length;

        // Initialize the Global.
        double[][] lG = new double[lN][lM];
        lG[0][0] = lL[0][0];

        // Allocate the Warping Path. (Math.max(N, M) <= K < (N + M).
        final int[][] lWarpingPath  = new int[lN + lM][2];

        // Declare the MinimaBuffer.
        final double[]   lMinimaBuffer = new double[3];

        // Define the Scalar Qualifier.
        int lK = 1;

        for(int i = 1; i < lN; i++) {
            lG[i][0] = lL[i][0] + lG[i - 1][0];
        }

        for(int j = 1; j < lM; j++) {
            lG[0][j] = lL[0][j] + lG[0][j - 1];
        }

        for (int i = 1; i < lN; i++) {
            for (int j = 1; j < lM; j++) {
                // Accumulate the path.
                lG[i][j] = (Math.min(Math.min(lG[i-1][j], lG[i-1][j-1]), lG[i][j-1])) + lL[i][j];
            }
        }

        // Update iteration varaibles.
        int i = lWarpingPath[lK - 1][0] = (lN - 1);
        int j = lWarpingPath[lK - 1][1] = (lM - 1);

        // Whilst there are samples to process...
        while ((i + j) != 0) {
            // Handle the offset.
            if(i == 0) {
                // Decrement the iteration variable.
                j -= 1;
            }
            else if(j == 0) {
                // Decrement the iteration variable.
                i -= 1;
            }
            else {
                // Update the contents of the MinimaBuffer.
                lMinimaBuffer[0] = lG[i - 1][j];
                lMinimaBuffer[1] = lG[i][j - 1];
                lMinimaBuffer[2] = lG[i - 1][j - 1];
                // Calculate the Index of the Minimum.
                final int lMinimumIndex = getMinimumIndex(lMinimaBuffer);
                // Declare booleans.
                final boolean lMinIs0 = (lMinimumIndex == 0);
                final boolean lMinIs1 = (lMinimumIndex == 1);
                final boolean lMinIs2 = (lMinimumIndex == 2);
                // Update the iteration components.
                i -= (lMinIs0 || lMinIs2) ? 1 : 0;
                j -= (lMinIs1 || lMinIs2) ? 1 : 0;
            }
            // Increment the qualifier.
            lK++;
            // Update the Warping Path.
            lWarpingPath[lK - 1][0] = i;
            lWarpingPath[lK - 1][1] = j;
        }

//        int[][] path = reverse(lWarpingPath, lK);
//
//        System.out.println(Arrays.deepToString(path));

        // Return the Result. (Calculate the Warping Path and the Distance.)
        return ((lG[lN - 1][lM - 1]) / lK);
    }

    /** Finds the index of the minimum element from the given array. */
    private static int getMinimumIndex(final double[] pArray) {
        // Declare iteration variables.
        int lIndex = 0;
        double lValue = pArray[0];
        // Iterate the Array.
        for (int i = 1; i < pArray.length; i++) {
            // .Is the current value smaller?
            final boolean lIsSmaller = pArray[i] < lValue;
            // Update the search metrics.
            lValue = lIsSmaller ? pArray[i] : lValue;
            lIndex = lIsSmaller ? i : lIndex;
        }
        // Return the Index.
        return lIndex;
    }

    /** Changes the order of the warping path, in increasing order. */
    private static int[][] reverse(final int[][] pPath, final int pK) {
        // Allocate the Path.
        final int[][] lPath = new int[pK][2];
        // Iterate.
        for(int i = 0; i < pK; i++) {
            // Iterate.
            for (int j = 0; j < 2; j++) {
                // Update the Path.
                lPath[i][j] = pPath[pK - i - 1][j]+1;
            }
        }
        // Return the Allocated Path.
        return lPath;
    }
}
