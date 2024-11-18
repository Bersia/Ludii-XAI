package embeddings.features;

import embeddings.distance.Euclidean;
import org.json.JSONException;
import org.json.JSONObject;
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

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("removedCells", this.numRemovedCells);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    @Override
    public double[] vectorize() {
        return new double[numRemovedCells];
    }
}
