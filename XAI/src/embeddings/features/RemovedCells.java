package embeddings.features;

import embeddings.distance.Euclidean;
import other.context.Context;

public class RemovedCells extends Feature {
    private int numRemovedCells;

    public RemovedCells(Context currentContext, Clusters previousClusters, Clusters currentClusters) {
//        this.context = currentContext;
        this.numRemovedCells = previousClusters.getNumCells() - currentClusters.getNumCells();
    }

    @Override
    public double distance(Feature other) {
        return Euclidean.distance(numRemovedCells, ((RemovedCells) other).numRemovedCells);
    }

    @Override
    public String print() {
        return "Removed Cells: " + numRemovedCells;
    }

    public int getRemovedCells() {
        return numRemovedCells;
    }
}
