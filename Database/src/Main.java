import embeddings.Features;
import game.functions.region.sites.simple.SitesOuter;
import model.ContextData;
import model.GameTrialData;
import other.context.Context;
import search.mcts.nodes.BaseNode;
import search.mcts.nodes.OpenLoopNode;

import javax.jdo.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        JDOEnhancer enhancer = JDOHelper.getEnhancer();
        enhancer.setVerbose(true);
        enhancer.addPersistenceUnit("PersistenceUnit");
        enhancer.enhance();
        PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory("PersistenceUnit");
        PersistenceManager pm = pmf.getPersistenceManager();
        Transaction tx = pm.currentTransaction();

        try {
            // Persist data
            for (int i = 0; i < 1; ++i)
            {
                tx.begin();
                GameTrialData gameTrialData = new GameTrialData(4, 3, 0, 1);
                pm.makePersistent(gameTrialData);
                do{
                    Context context = gameTrialData.getContext();
                    OpenLoopNode root = gameTrialData.rootNode();
                    ContextData contextData = new ContextData(context, root, gameTrialData.getID(), gameTrialData.getBoardSize());
                    pm.makePersistent(contextData);
//                    System.out.println(contextData.printBoard(context));

                }while(gameTrialData.step());

//                System.out.println("Score: " + gameTrialData.getContext().score(1));

                tx.commit();
            }

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            pm.close();
            pmf.close();
        }
    }

    public void queryData(PersistenceManager pm, Transaction tx) {
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

                Node.saveTreemap(contextsByID.get(games.get(i).getID()).get(j).getRoot());
            }
        }
    }
}
