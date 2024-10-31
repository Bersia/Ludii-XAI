package embeddings;

import embeddings.features.*;
import other.context.Context;

import java.util.HashMap;
import java.util.Map;

public class Features {
    private Map<String, Feature> features = new HashMap<String, Feature>();
    private Features previous_features;
    private Context current_context;

    public Context getCurrent_context() {
        return current_context;
    }

    public Map<String, Feature> getFeatures() {
        return features;
    }

    public Features(Features previous_features, Context current_context) {
        this.previous_features = previous_features;
        this.current_context = current_context;


        features.put("Clusters", new Clusters(current_context));
        features.put("Colors", new Colors(current_context));
        features.put("EmptyColumns", new EmptyColumns(current_context));
        features.put("BoardDistribution", new BoardDistribution(current_context, (Colors) features.get("Colors")));

        if(previous_features != null) {
            features.put("RemovedCells", new RemovedCells(current_context, (Clusters) previous_features.features.get("Clusters"), (Clusters) features.get("Clusters")));
            features.put("ScoreOffset", new ScoreOffset(current_context, (Clusters) previous_features.features.get("Clusters"), (Clusters) features.get("Clusters"), (RemovedCells) features.get("RemovedCells")));
        }

        for(Map.Entry<String, Feature> entry : features.entrySet()) {
            String key = entry.getKey();
            Feature value = entry.getValue();
            System.out.println(value);
        }
    }
}
