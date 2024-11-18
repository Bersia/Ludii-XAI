import app.DesktopApp;
import embeddings.Features;
import game.Game;
import other.AI;
import other.GameLoader;
import other.context.Context;
import other.model.Model;
import other.trial.Trial;
import search.mcts.MCTS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    private static final DesktopApp desktopApp = new DesktopApp();




    public static void main(final String[] args) {
        // Initialize the Java desktop application
//        desktopApp.createDesktopApp();

        // Start game programmatically
        final List<String> options = Arrays.asList("Board size/"+4, "Number of Colours/"+3);
        //final List<String> options = Arrays.asList("Board size/"+15, "Number of Colours/"+4);
        final Game game = GameLoader.loadGameFromName("SameGame.lud", options);
//        final Game game = GameLoader.loadGameFromName("Example_4x4_3col.lud");
        final Trial trial = new Trial(game);
        final Context context = new Context(game, trial);
        byte BOARD_SIZE = (byte)Math.sqrt(context.board().graph().faces().size());

        final List<AI> ais = new ArrayList<AI>();
        ais.add(null);
        for (int p = 1; p <= game.players().count(); ++p)
        {
            ais.add(MCTS.createUCT());
            //ais.add(new RandomAI());
        }

        for (int i = 0; i < 1; ++i)
        {
            game.start(context);

            for (int p = 1; p <= game.players().count(); ++p)
            {
                ais.get(p).initAI(game, p);
            }

            final Model model = context.model();

            System.out.println("Initial board:");
            printBoard(context);

            Features initial = new Features(null, context);
            Features f = initial;

            while (!trial.over())
            {
                System.out.println("Before:");
                printBoard(context);
                model.startNewStep(context, ais, 1.0);

                System.out.println("After:");
                printBoard(context);

                f = new Features(f, context);

                System.out.println(f.distance(initial));

            }


            final double[] ranking = trial.ranking();

            for (int p = 1; p <= game.players().count(); ++p)
            {
                System.out.println("Agent " + context.state().playerToAgent(p) + " achieved rank: " + ranking[p]);
                System.out.println("Agent " + context.state().playerToAgent(p) + " achieved score: " + context.score(p));
            }
        }
//        System.exit(0);
    }

    private static void printBoard(Context context) {
        byte BOARD_SIZE = (byte)Math.sqrt(context.board().graph().faces().size());
        byte[][] board = new byte[BOARD_SIZE][BOARD_SIZE];
        for(int j=board.length-1;j>=0;j--){
            for(int k=0;k<board.length;k++) {
                byte piece = (byte)context.state().containerStates()[0].stateCell(board.length * j + k);
                System.out.print(piece+" ");
                board[board.length-1-j][k] = piece;
            }
            System.out.println();
        }
        System.out.println();
    }

}
