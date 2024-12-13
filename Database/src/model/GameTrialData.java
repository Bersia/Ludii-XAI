package model;


import compiler.Compiler;
import game.Game;
import main.grammar.Description;
import other.AI;
import other.GameLoader;
import other.context.Context;
import other.model.Model;
import other.trial.Trial;
import search.mcts.MCTS;
import search.mcts.SameGameMCTS;
import search.mcts.nodes.StandardNode;
import main.grammar.Report;

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
//        final Game game = GameLoader.loadGameFromName("SameGame.lud", options);
        final Game game = getGame(boardSize, numColours);
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

    private Game getGame(int boardSize, int numColours) {
        String desc =
                "(game \"SameGame\"  \n" +
                        "    (players 1)  \n" +
                        "    (equipment { \n" +
                        "        (board (square "+boardSize+" diagonals:Implied)) \n" +
                        "        (piece \"Ball\" Neutral maxState:"+numColours+") \n" +
                        "    })  \n" +
                        "    (rules \n" +
                        "       (start { \n" +
                        "           (forEach Site (sites Empty) \n" +
                        "               (place Random {\"Ball0\"} state:(value Random (range 1 "+numColours+"))) \n" +
                        "           ) \n" +
                        "       }) \n" +

                        "                (play (forEach Site (sites Occupied by:Neutral) (move Select (from (site) if: ((toBool (count Sites in: (intersection (sites Direction from:(site) Orthogonal distance:1) (sites State (state at: (site))) ) ) )) ) (then (seq { ((do (and (set Var \"state\" (state at:(last To))) (set Var \"size\" 0)) next: (do (forEach Site (sites Group at:(last To) Orthogonal if:(= (state at:(to)) (var \"state\"))) (remove (site)) (then (set Var \"size\" (+ 1 (var \"size\")))) ) next:(addScore Mover ((^ (- (var \"size\") 2) 2  ) )) (then (set Var \"size\" 0)) ) ) ) ((do (set Var \"row\" 0) next: (while (!= (var \"row\") "+boardSize+") (do ((forEach Site (sites Row  (var \"row\")) (if (is Occupied (site)) (if (!= 0 (count Sites in:(sites LineOfSight Farthest at:(site) S))) (fromTo (from (site)) (to (sites LineOfSight Farthest at:(site) S))) ) ) )) next: (set Var \"row\" (+ (var \"row\") 1)) ) ) )) ((seq { (set Var \"countEmpty\" (count Sites in: (intersection (sites Empty) (sites Row 0) ) )) (while (!= (var \"countEmpty\") 0) (seq { (set Var \"col\" 0) (while (!= (var \"col\") "+boardSize+") (seq { ((if (is Empty (coord row:0 column:((var \"col\")))) (seq { (set Var \"row\" 0) (while (!= (var \"row\") "+boardSize+") (seq { (fromTo (from (coord row:(var \"row\") column:(+ 1 ((var \"col\"))))) (to (coord row:(var \"row\") column:((var \"col\"))))) (set Var \"row\" (+ (var \"row\") 1)) } ) ) } ) ) ) (set Var \"col\" (+ (var \"col\") 1)) } ) ) (set Var \"countEmpty\" (- (var \"countEmpty\") 1)) } ) )  } )) (if (= 0 (count Sites in:(sites Occupied by:Neutral))) (addScore Mover 1000) ) } ) ) ) ) )\n" +

                        "        (end { (if (= 0 (count Sites in:(sites Occupied by:Neutral) ) ) (result Mover Win) )  (if (no Moves Next) (result Mover Loss) )  } )\n" +
                        "    )\n" +
                        ")";

        final Description gameDescription = new Description(desc);
        final Report report = new Report();
        Game game=null;
        try
        {
            game = (Game)Compiler.compile(gameDescription, null, report, false);
            return game;
        }
        catch (final Exception e)
        {
            return game;
        }

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
