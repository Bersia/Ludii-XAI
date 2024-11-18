package embeddings.features;

import org.json.JSONObject;

public abstract class Feature {
//    protected Context context;
//
//    public Context getContext() {
//        return context;
//    }

    public abstract double distance(Feature other);

    public abstract String print();

    public abstract JSONObject toJSON();
    
    public abstract double[] vectorize();

    @Override
    public String toString() {
        return print();
    }
}
