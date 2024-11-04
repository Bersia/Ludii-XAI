package embeddings.features;

import other.context.Context;

public abstract class Feature {
//    protected Context context;
//
//    public Context getContext() {
//        return context;
//    }

    public abstract double distance(Feature other);

    public abstract String print();

    @Override
    public String toString() {
        return print();
    }
}
