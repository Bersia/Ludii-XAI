package embeddings;

import embeddings.features.*;
import org.json.JSONObject;
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

    public Features(Features previous_features, Context current_context, int board_size) {
        this.previous_features = previous_features;
        this.current_context = current_context;

        features.put("Colors", new Colors(current_context, board_size));
        features.put("Clusters", new Clusters(current_context, (Colors) features.get("Colors"), board_size));
        features.put("EmptyColumns", new EmptyColumns(current_context, board_size));
        features.put("BoardDistribution", new BoardDistribution(current_context, (Colors) features.get("Colors"), board_size));

        if(previous_features != null) {
            features.put("RemovedCells", new RemovedCells(current_context, (Clusters) previous_features.features.get("Clusters"), (Clusters) features.get("Clusters")));
            features.put("ScoreOffset", new ScoreOffset(current_context, (Clusters) previous_features.features.get("Clusters"), (Clusters) features.get("Clusters"), (RemovedCells) features.get("RemovedCells")));
        }
        //System.out.println(this);
//        System.out.println("JSON:");
//        System.out.println(toJSON());
    }

    @Override
    public String toString() {
        StringBuilder print = new StringBuilder();
        for (Feature feature : features.values()) {
            print.append(feature).append("\n");
        }
        return print.toString();
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        for(Map.Entry<String, Feature> entry : features.entrySet()) {
            String key = entry.getKey();
            JSONObject value = entry.getValue().toJSON();
            for(String k : JSONObject.getNames(value))
            {
                json.put(k, value.get(k));
            }
        }
        return json;
    }

    public String distance(Features other) {
        StringBuilder print = new StringBuilder("Distance:\n");
        for(Map.Entry<String, Feature> entry : features.entrySet()) {
            String key = entry.getKey();
            Feature value = entry.getValue();
            if(other.features.containsKey(key))
                print.append(key).append(": ").append(value.distance(other.features.get(key))).append("\n");
        }
        return print.toString();
    }
}
