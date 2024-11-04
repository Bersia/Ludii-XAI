package embeddings.features;

import embeddings.distance.Euclidean;
import other.context.Context;

public class EmptyColumns extends Feature {

    private byte emptycolumns = 0;

    public EmptyColumns(Context context) {

        byte BOARD_SIZE = (byte)Math.sqrt(context.board().graph().faces().size());

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
}
