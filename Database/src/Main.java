import embeddings.Features;
import game.functions.region.sites.simple.SitesOuter;
import model.ContextData;
import model.GameTrialData;
import other.context.Context;
import search.mcts.nodes.BaseNode;
import search.mcts.nodes.OpenLoopNode;
import feature_mining.Node;
import search.mcts.nodes.BaseNode;
import search.mcts.nodes.StandardNode;

import javax.jdo.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        JDOEnhancer enhancer = JDOHelper.getEnhancer();
        enhancer.setVerbose(true);
        enhancer.addPersistenceUnit("PersistenceUnit");
        enhancer.enhance();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the number of games to generate for each combination of parameters: ");
        int numGames = scanner.nextInt();
        scanner.nextLine();
        System.out.println("Enter the desired board sizes (between 4 and 15) separated by spaces: ");
        String boardSizes = scanner.nextLine();
        ArrayList<Integer> boardSizesList = new ArrayList<>();
        String[] boardSizesArray = boardSizes.split(" ");
        for (int i = 0; i < boardSizesArray.length; i++) {
            if (Integer.parseInt(boardSizesArray[i]) < 4 || Integer.parseInt(boardSizesArray[i]) > 15) {
                System.out.println("Invalid board size: " + boardSizesArray[i] + " skipping...");
                continue;
            }
            boardSizesList.add(Integer.parseInt(boardSizesArray[i]));
        }
        System.out.println("Enter the desired number of colors (between 2 and 10) separated by spaces: ");
        String colors = scanner.nextLine();
        ArrayList<Integer> colorsList = new ArrayList<>();
        String[] colorsArray = colors.split(" ");
        for (int i = 0; i < colorsArray.length; i++) {
            if (Integer.parseInt(colorsArray[i]) < 2 || Integer.parseInt(colorsArray[i]) > 10) {
                System.out.println("Invalid board size: " + colorsArray[i] + " skipping...");
                continue;
            }
            colorsList.add(Integer.parseInt(colorsArray[i]));
        }
        System.out.println("Enter the reflection time: ");
        int reflectionTime = scanner.nextInt();
        scanner.nextLine();

        System.out.println("Save the AI trees? (y/n): ");
        String saveTrees = scanner.nextLine();
        boolean saveTreesBool = false;
        if (saveTrees.startsWith("y") || saveTrees.startsWith("Y")) {
            saveTreesBool = true;
        }

        System.out.println("Final parameters:\n"+
                "Number of games per parameter combination: "+numGames+"\n"+
                "Board sizes: "+boardSizesList+"\n"+
                "Colors: "+colorsList+"\n"+
                "Reflection time: "+reflectionTime+"\n"+
                "Total number of games: "+boardSizesList.size()*colorsList.size()*numGames+"\n"+
                "Save trees: "+saveTreesBool+"\n\nTo confirm and start the game generation process press enter, to cancel press Ctrl+C");
        if(!scanner.nextLine().equals("")){
            System.exit(0);
        }

        PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory("PersistenceUnit");
        Transaction tx = null;

        try(PersistenceManager pm = pmf.getPersistenceManager()) {

            tx = pm.currentTransaction();


            // Persist data
            for(int j = 0; j < colorsList.size(); j++){
                for(int i = 0; i < boardSizesList.size(); i++){
                    System.out.println("Generating "+numGames+" games for board size "+boardSizesList.get(i)+" and "+colorsList.get(j)+" colors...");
                    generateGames(pm, tx, numGames, boardSizesList.get(i), colorsList.get(j), reflectionTime, saveTreesBool);
                    System.out.println("Finished generating "+numGames+" games for board size "+boardSizesList.get(i)+" and "+colorsList.get(j)+" colors.");
                }
            }

//            queryData(pm, tx);

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            pmf.close();
        }
    }

    public static void queryData(PersistenceManager pm, Transaction tx) {
        // Query data
        tx.begin();
        Query<GameTrialData> query = pm.newQuery(GameTrialData.class);
        List<GameTrialData> games = query.executeList();
        for (GameTrialData game : games) {
            System.out.println(game);
        }
        tx.commit();

        // Query data
        tx.begin();
        Query<ContextData> query2 = pm.newQuery(ContextData.class);
        List<ContextData> contexts = query2.executeList();
        Map<Long, List<ContextData>> contextsByID = new HashMap<>();

        //Map by Game
        for (ContextData context : contexts) {
            System.out.println(context);
            if(contextsByID.get(context.getGameID()) == null)
                contextsByID.put(context.getGameID(), new ArrayList<ContextData>());
            contextsByID.get(context.getGameID()).add(context);
        }
        tx.commit();

        //Sort each game context by step
        for (Map.Entry<Long, List<ContextData>> entry : contextsByID.entrySet()) {
            entry.getValue().sort(Comparator.comparing(ContextData::getStep));
        }

        for(int i = 0; i < games.size(); ++i) {
            System.out.println("Game "+games.get(i).getID());
            Features initial = new Features(null, contextsByID.get(games.get(i).getID()).getFirst().getContext());
            Features f = initial;
            for(int j = 1; j < contextsByID.get(games.get(i).getID()).size(); ++j) {
                f = new Features(f, contextsByID.get(games.get(i).getID()).get(j).getContext());
                System.out.println(f.distance(initial));
                StandardNode root = contextsByID.get(games.get(i).getID()).get(j).getRoot();
                if(root!=null) Node.saveTreemap(root);
            }
        }
    }

    private static void generateGames(PersistenceManager pm, Transaction tx, int numGames, int boardSize, int numColours, int reflectionTime, boolean saveTrees) {
        for (int i = 0; i < numGames; ++i)
        {
            tx.begin();
            GameTrialData gameTrialData = new GameTrialData(boardSize, numColours, 0, reflectionTime);
//            pm.makePersistent(gameTrialData);
            tx.commit();
//            tx.begin();
            do{
                Context context = gameTrialData.getContext();
                StandardNode root = null;
                if(saveTrees)
                    root = gameTrialData.rootNode();
                ContextData contextData = new ContextData(context, root, gameTrialData.getID(), gameTrialData.getBoardSize());
                System.out.println(contextData);


                tx.begin();
                pm.makePersistent(contextData);
                tx.commit();



            }while(gameTrialData.step());
//            tx.commit();
        }
    }
//    private static void generateGames(PersistenceManager pm, Transaction tx, int numGames, int boardSize, int numColours, int reflectionTime, boolean saveTrees) {
//        try {
//            for (int i = 0; i < numGames; ++i) {
//                tx.begin();
//                GameTrialData gameTrialData = new GameTrialData(boardSize, numColours, 0, reflectionTime);
//                pm.makePersistent(gameTrialData);
//
//                List<ContextData> contextDataList = new ArrayList<>();
//                while (gameTrialData.step()) {
//                    Context context = gameTrialData.getContext();
//                    StandardNode root = saveTrees ? gameTrialData.rootNode() : null;
//                    contextDataList.add(new ContextData(context, root, gameTrialData.getID(), gameTrialData.getBoardSize()));
//                }
//                pm.makePersistentAll(contextDataList); // Batch persist
//                tx.commit();
//            }
//        } catch (Exception e) {
//            if (tx.isActive()) tx.rollback();
//            throw new RuntimeException("Error generating games", e);
//        }
//    }
}
