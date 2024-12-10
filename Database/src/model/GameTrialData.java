package model;

import game.Game;
import other.AI;
import other.GameLoader;
import other.context.Context;
import other.model.Model;
import other.trial.Trial;
import search.mcts.MCTS;
import search.mcts.SameGameMCTS;
import search.mcts.backpropagation.AlphaGoBackprop;
import search.mcts.finalmoveselection.RobustChild;
import search.mcts.nodes.BaseNode;
import search.mcts.nodes.OpenLoopNode;
import search.mcts.nodes.StandardNode;
import search.mcts.playout.HeuristicSampingPlayout;
import search.mcts.selection.UCB1;

import javax.jdo.annotations.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@PersistenceCapable
public class GameTrialData {
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.INCREMENT)
    private long id;

    // Game options
    @Persistent
    private int scoringMethod;

    // AI options
    @Persistent
    private String AIType;

    @Persistent
    private int reflectionTime;

    // Game data
    @Persistent
    private int boardSize;

    @Persistent
    private int numColours;

    @NotPersistent
    private Trial trial;

    @NotPersistent
    private Context context;

    @NotPersistent
    private Model model;

    @NotPersistent
    private final List<AI> ais = new ArrayList<>();

    public GameTrialData(int boardSize, int numColours, int scoringMethod, int reflectionTime) {
        ContextData.resetStepCounter();
        this.boardSize = boardSize;
        this.numColours = numColours;
        this.scoringMethod = scoringMethod;
        this.reflectionTime = reflectionTime;

        final List<String> options = Arrays.asList("Board size/"+boardSize, "Number of Colours/"+numColours);
        final Game game = GameLoader.loadGameFromName("SameGame.lud", options);

        trial = new Trial(game);
        context = new Context(game, trial);

        ais.add(null);
        for (int p = 1; p <= game.players().count(); ++p)
        {
//            ais.add(MCTS.createHybridMCTS());
            ais.add(new SameGameMCTS());
            AIType = "SameGameMCTS";
        }

        game.start(context);

        for (int p = 1; p <= game.players().count(); ++p)
        {
            ais.get(p).initAI(game, p);
        }

        model = context.model();
    }

    public Context getContext() {
        return context.deepCopy();
    }

    public boolean step(){
        if(trial.over()){
            return false;
        }
        else{
            model.startNewStep(context, ais, reflectionTime);
            return true;
        }
    }

    public long getID() {
        return id;
    }

    @Override
    public String toString() {
        return "GameTrialData{" +
                "id=" + id +
                ", boardSize=" + boardSize +
                ", numColours=" + numColours +
                ", scoringMethod=" + scoringMethod +
                ", AI=" + AIType +
                ", reflectionTime=" + reflectionTime +
                '}';
    }

//    public String getAITree() {
//        return ((MCTS)(ais.get(1))).getTreeReport();
//    }

    public StandardNode rootNode() {
        return (StandardNode) ((SameGameMCTS)(ais.get(1))).rootNode();
    }

    public byte getBoardSize() {
        return (byte)boardSize;
    }
}
