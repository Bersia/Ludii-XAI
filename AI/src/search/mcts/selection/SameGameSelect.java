package search.mcts.selection;

import other.state.State;
import search.mcts.MCTS;
import search.mcts.SameGameMCTS;
import search.mcts.nodes.BaseNode;

import java.util.concurrent.ThreadLocalRandom;

public final class SameGameSelect implements SelectionStrategy {

    private double explorationConstant;

    public SameGameSelect() {
        this(Math.sqrt(2.0));
    }

    public SameGameSelect(double explorationConstant) {
        this.explorationConstant = explorationConstant;
    }

    @Override
    public int select(MCTS mcts, BaseNode current) {
        int bestIdx = 0;
        double bestValue = Double.NEGATIVE_INFINITY;
        int numBestFound = 0;

        final double parentLog = Math.log(Math.max(1, current.sumLegalChildVisits()));
        final int numChildren = current.numLegalMoves();
        final State state = current.contextRef().state();
        final int moverAgent = state.playerToAgent(state.mover());
        final double unvisitedValueEstimate = current.valueEstimateUnvisitedChildren(moverAgent);

        for (int i = 0; i < numChildren; ++i)
        {
            final BaseNode child = current.childForNthLegalMove(i);
            final double exploit;
            final double explore;

            if (child == null)
            {
                exploit = unvisitedValueEstimate;
                explore = Math.sqrt(parentLog);
            }
            else
            {
                if (mcts instanceof SameGameMCTS) {
                    SameGameMCTS sameGameMCTS = (SameGameMCTS) mcts;
                    exploit = sameGameMCTS.normalizeScore(child.contextRef());
                }
                else exploit = child.exploitationScore(moverAgent);
                final int numVisits = Math.max(child.numVisits() + child.numVirtualVisits(), 1);
                explore = Math.sqrt(parentLog / numVisits);
            }

            final double ucb1Value = exploit + explorationConstant * explore;
            //System.out.println("ucb1Value = " + ucb1Value);
            //System.out.println("exploit = " + exploit);
            //System.out.println("explore = " + explore);

            if (ucb1Value > bestValue)
            {
                bestValue = ucb1Value;
                bestIdx = i;
                numBestFound = 1;
            }
            else if
            (
                    ucb1Value == bestValue
                            &&
                            ThreadLocalRandom.current().nextInt() % ++numBestFound == 0
            )
            {
                bestIdx = i;
            }
        }

        return bestIdx;
    }

    @Override
    public int backpropFlags() {
        return 0;
    }

    @Override
    public int expansionFlags() {
        return 0;
    }

    @Override
    public void customise(String[] inputs) {

    }
}
