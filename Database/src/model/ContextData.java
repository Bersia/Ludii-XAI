package model;

import other.context.Context;
import search.mcts.nodes.BaseNode;
import search.mcts.nodes.OpenLoopNode;

import javax.jdo.annotations.*;

@PersistenceCapable
public class ContextData {
    private static int stepCounter = 0;

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.INCREMENT)
    private long id;

    @Persistent
    private long gameId;
    @Persistent
    private int step;
    @Persistent
    private byte boardSize;


    @Persistent
    private Context context;
    @Persistent
    private OpenLoopNode root;

    public ContextData(Context context, OpenLoopNode root, long gameId, byte boardSize) {
        this.step = stepCounter++;
        this.gameId = gameId;
        this.context = context;
        this.root = root;
        this.boardSize = boardSize;
    }

    public static void resetStepCounter() {
        stepCounter = 0;
    }

    public Context getContext() {
        return context;
    }

    public OpenLoopNode getRoot() {
        return root;
    }

    @Override
    public String toString() {
        return "ContextData{" + "id=" + id + ", gameId=" + gameId + ", step=" + step + ", context=" + context +", score=" + context.score(1) + ", root=" + root + '\n' + printBoard(context) + "}";
    }

    public String printBoard(Context context) {
        StringBuilder sb = new StringBuilder();
        for(int j = boardSize -1; j>=0; j--){
            for(int k = 0; k< boardSize; k++) {
                byte piece = (byte)context.state().containerStates()[0].stateCell(boardSize * j + k);
                sb.append(piece+" ");
            }
            sb.append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    public long getGameID() {
        return gameId;
    }

    public long getStep() {
        return step;
    }
}
