package embeddings.features;

import embeddings.distance.Euclidean;
import org.json.JSONException;
import org.json.JSONObject;
import other.context.Context;

import java.util.*;

public class Colors extends Feature {

    private int numColors;
    private Set<Byte> colors;

    public Colors(Context context) {
        int BOARD_SIZE = (byte)Math.sqrt(context.board().graph().faces().size());

        //Initial board
        byte[] board = new byte[BOARD_SIZE*BOARD_SIZE];

        for(int i=0;i<BOARD_SIZE*BOARD_SIZE;i++){
            board[i] = (byte)context.state().containerStates()[0].stateCell(i);
        }

        // Count distinct colors
        colors = new HashSet<>();
        for(int c = 0; c < board.length; c++) {
            if(board[c] > 0) {
                colors.add(board[c]);
            }
        }
        this.numColors = colors.size();
    }

    public int getNumColors() {
        return numColors;
    }

    public List<Byte> getColorValues() {
        return colors.stream().toList();
    }

    @Override
    public double distance(Feature other) {
        return Euclidean.distance(numColors, ((Colors)other).numColors);
    }

    @Override
    public String print() {
        return "Number of colors: "+numColors;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("colors", this.colors);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }
}
