package embeddings.distance.utils;

import java.util.Arrays;

public class HungarianAlgorithm {
    private final double[][] costMatrix;
    private final int rows, cols, dim;
    private final double[] labelByWorker, labelByJob;
    private final int[] minSlackWorkerByJob;
    private final double[] minSlackValueByJob;
    private final int[] matchJobByWorker, matchWorkerByJob;
    private final boolean[] committedWorkers;

    public HungarianAlgorithm(double[][] costMatrix) {
        this.rows = costMatrix.length;
        this.cols = costMatrix[0].length;
        this.dim = Math.max(rows, cols);
        this.costMatrix = new double[dim][dim];
        for (int i = 0; i < dim; i++) {
            if (i < rows) {
                this.costMatrix[i] = Arrays.copyOf(costMatrix[i], dim);
            } else {
                Arrays.fill(this.costMatrix[i], 0);
            }
        }
        labelByWorker = new double[dim];
        labelByJob = new double[dim];
        minSlackWorkerByJob = new int[dim];
        minSlackValueByJob = new double[dim];
        committedWorkers = new boolean[dim];
        matchJobByWorker = new int[dim];
        Arrays.fill(matchJobByWorker, -1);
        matchWorkerByJob = new int[dim];
        Arrays.fill(matchWorkerByJob, -1);
    }

    public int[] execute() {
        reduce();
        computeInitialFeasibleSolution();
        greedyMatch();

        int w = fetchUnmatchedWorker();
        while (w < dim) {
            initializePhase(w);
            executePhase();
            w = fetchUnmatchedWorker();
        }
        int[] result = Arrays.copyOf(matchJobByWorker, rows);
        return result;
    }

    private void reduce() {
        for (int w = 0; w < dim; w++) {
            double min = Double.MAX_VALUE;
            for (int j = 0; j < dim; j++) {
                if (costMatrix[w][j] < min) {
                    min = costMatrix[w][j];
                }
            }
            for (int j = 0; j < dim; j++) {
                costMatrix[w][j] -= min;
            }
        }
        for (int j = 0; j < dim; j++) {
            double min = Double.MAX_VALUE;
            for (int w = 0; w < dim; w++) {
                if (costMatrix[w][j] < min) {
                    min = costMatrix[w][j];
                }
            }
            for (int w = 0; w < dim; w++) {
                costMatrix[w][j] -= min;
            }
        }
    }

    private void computeInitialFeasibleSolution() {
        for (int j = 0; j < dim; j++) {
            labelByJob[j] = Integer.MAX_VALUE;
        }
        for (int w = 0; w < dim; w++) {
            for (int j = 0; j < dim; j++) {
                if (costMatrix[w][j] < labelByJob[j]) {
                    labelByJob[j] = costMatrix[w][j];
                }
            }
        }
    }

    private void greedyMatch() {
        for (int w = 0; w < dim; w++) {
            for (int j = 0; j < dim; j++) {
                if (matchJobByWorker[w] == -1 && matchWorkerByJob[j] == -1 && costMatrix[w][j] - labelByWorker[w] - labelByJob[j] == 0) {
                    match(w, j);
                }
            }
        }
    }

    private void initializePhase(int w) {
        Arrays.fill(committedWorkers, false);
        committedWorkers[w] = true;
        for (int j = 0; j < dim; j++) {
            minSlackValueByJob[j] = costMatrix[w][j] - labelByWorker[w] - labelByJob[j];
            minSlackWorkerByJob[j] = w;
        }
    }

    private void executePhase() {
        while (true) {
            int minSlackWorker = -1, minSlackJob = -1;
            double minSlackValue = Double.MAX_VALUE;
            for (int j = 0; j < dim; j++) {
                if (matchWorkerByJob[j] == -1) {
                    if (minSlackValueByJob[j] < minSlackValue) {
                        minSlackValue = minSlackValueByJob[j];
                        minSlackWorker = minSlackWorkerByJob[j];
                        minSlackJob = j;
                    }
                }
            }
            if (minSlackValue > 0) {
                updateLabeling(minSlackValue);
            }
            match(minSlackWorker, minSlackJob);
            if (matchJobByWorker[minSlackWorker] != -1) {
                return;
            }
            initializePhase(minSlackWorker);
        }
    }

    private void updateLabeling(double slack) {
        for (int w = 0; w < dim; w++) {
            if (committedWorkers[w]) {
                labelByWorker[w] += slack;
            }
        }
        for (int j = 0; j < dim; j++) {
            if (matchWorkerByJob[j] != -1) {
                labelByJob[j] -= slack;
            } else {
                minSlackValueByJob[j] -= slack;
            }
        }
    }

    private void match(int w, int j) {
        matchJobByWorker[w] = j;
        matchWorkerByJob[j] = w;
    }

    private int fetchUnmatchedWorker() {
        for (int w = 0; w < dim; w++) {
            if (matchJobByWorker[w] == -1) {
                return w;
            }
        }
        return dim;
    }
}
