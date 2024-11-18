package embeddings.features;

import embeddings.distance.Euclidean;
import org.json.JSONException;
import org.json.JSONObject;
import other.context.Context;

public class ScoreOffset extends Feature {
    private int scoreOffset;

    public ScoreOffset(Context currentContext, Clusters previousClusters, Clusters currentClusters, RemovedCells removedCells) {
//        this.context = currentContext;
        this.scoreOffset = currentClusters.getScore() - previousClusters.getScore() + (int) Math.pow(removedCells.getRemovedCells()-1, 2);
    }

    @Override
    public double distance(Feature other) {
        return Euclidean.distance(scoreOffset, ((ScoreOffset)other).scoreOffset);
    }

    @Override
    public String print() {
        return "Score offset: " + scoreOffset;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("scoreOffset", this.scoreOffset);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    @Override
    public double[] vectorize() {
        return new double[scoreOffset];
    }
}
