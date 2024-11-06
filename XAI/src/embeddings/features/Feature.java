package embeddings.features;

import org.json.JSONObject;
import other.context.Context;

public abstract class Feature {
//    protected Context context;
//
//    public Context getContext() {
//        return context;
//    }

    public abstract double distance(Feature other);

    public abstract String print();

    public abstract JSONObject toJSON();

    @Override
    public String toString() {
        return print();
    }
}
