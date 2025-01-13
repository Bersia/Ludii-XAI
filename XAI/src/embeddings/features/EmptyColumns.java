package embeddings.features;

import embeddings.distance.Euclidean;
import org.json.JSONException;
import org.json.JSONObject;
import other.context.Context;

public class EmptyColumns extends Feature {

    private byte emptycolumns = 0;

    public EmptyColumns(Context context, int board_size) {

        byte BOARD_SIZE = (byte)board_size;//Math.sqrt(context.board().graph().faces().size());

        for(int i=BOARD_SIZE-1;i>=0;i--){
            if((byte)context.state().containerStates()[0].stateCell(i)==0){
                emptycolumns++;
            }
            else{
                break;
            }
        }
    }

    @Override
    public double distance(Feature other) {
        return Euclidean.distance(emptycolumns, ((EmptyColumns)other).emptycolumns);
    }

    @Override
    public String print() {
        return "Number of empty columns: "+emptycolumns;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("emptyColumns", this.emptycolumns);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    @Override
    public double[] vectorize() {
        return new double[emptycolumns];
    }
}
