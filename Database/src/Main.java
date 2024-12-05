import model.ContextData;
import model.GameTrialData;
import other.context.Context;
import search.mcts.nodes.BaseNode;
import search.mcts.nodes.OpenLoopNode;

import javax.jdo.*;
import java.util.List;

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
            tx.begin();
            // Persist data
            for (int i = 0; i < 2; ++i)
            {
                GameTrialData gameTrialData = new GameTrialData(4, 3, 0, 1);
                pm.makePersistent(gameTrialData);
                do{
                    Context context = gameTrialData.getContext();
                    OpenLoopNode root = gameTrialData.rootNode();
                    ContextData contextData = new ContextData(context, root, gameTrialData.getID(), gameTrialData.getBoardSize());
                    pm.makePersistent(contextData);
                    System.out.println(contextData.printBoard(context));

                }while(gameTrialData.step());

                System.out.println("Score: " + gameTrialData.getContext().score(1));
            }
            tx.commit();

            // Query data
            tx.begin();
            Query<GameTrialData> query = pm.newQuery(GameTrialData.class);
            List<GameTrialData> games = query.executeList();
            for (GameTrialData game : games) {
                System.out.println(game);
//                System.out.println(game.getAITree());
            }
            tx.commit();

            // Query data
            tx.begin();
            Query<ContextData> query2 = pm.newQuery(ContextData.class);
            List<ContextData> contexts = query2.executeList();
            for (ContextData context : contexts) {
                System.out.println(context);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            pm.close();
            pmf.close();
        }
    }
}
