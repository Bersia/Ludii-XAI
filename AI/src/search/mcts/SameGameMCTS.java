package search.mcts;

import game.Game;
import other.RankUtils;
import other.context.Context;
import other.move.Move;
import other.state.State;
import other.trial.Trial;
import search.mcts.backpropagation.AlphaGoBackprop;
import search.mcts.backpropagation.BackpropagationStrategy;
import search.mcts.finalmoveselection.FinalMoveSelectionStrategy;
import search.mcts.finalmoveselection.GameStateScore;
import search.mcts.finalmoveselection.RobustChild;
import search.mcts.nodes.BaseNode;
import search.mcts.playout.HeuristicSampingPlayout;
import search.mcts.playout.PlayoutStrategy;
import search.mcts.selection.SameGameSelect;
import search.mcts.selection.SelectionStrategy;
import search.mcts.selection.UCB1;
import utils.AIUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SameGameMCTS extends MCTS {

    private double maxScoreEncountered = 0;
    /**
     * Constructor with arguments for all strategies
     *
     * @param selectionStrategy
     * @param playoutStrategy
     * @param backpropagationStrategy
     * @param finalMoveSelectionStrategy
     */
    public SameGameMCTS(SelectionStrategy selectionStrategy, PlayoutStrategy playoutStrategy, BackpropagationStrategy backpropagationStrategy, FinalMoveSelectionStrategy finalMoveSelectionStrategy) {
        super(selectionStrategy, playoutStrategy, backpropagationStrategy, finalMoveSelectionStrategy);
    }

    public SameGameMCTS() {
        this(new SameGameSelect(), new HeuristicSampingPlayout(), new AlphaGoBackprop(), new GameStateScore());
    }

    public double normalizeScore(Context contextRef) {
        double score = contextRef.score(1);
        if (score > maxScoreEncountered) {
            this.maxScoreEncountered = score;
        }
        return score / maxScoreEncountered;
    }

    @Override
    public Move selectAction
            (
                    final Game game,
                    final Context context,
                    final double maxSeconds,
                    final int maxIterations,
                    final int maxDepth
            )
    {
        final long startTime = System.currentTimeMillis();
        long stopTime = (maxSeconds > 0.0) ? startTime + (long) (maxSeconds * 1000) : Long.MAX_VALUE;
        final int maxIts = (maxIterations >= 0) ? maxIterations : Integer.MAX_VALUE;

        while (numThreadsBusy.get() != 0 && System.currentTimeMillis() < Math.min(stopTime, startTime + 1000L))
        {
            // Give threads in thread pool some more time to clean up after themselves from previous iteration
        }

        // We'll assume all threads are really done now and just reset to 0
        numThreadsBusy.set(0);

        final AtomicInteger numIterations = new AtomicInteger();

        // Find or create root node
        if (treeReuse && rootNode != null)
        {
            // Want to reuse part of existing search tree

            // Need to traverse parts of old tree corresponding to
            // actions played in the real game
            final List<Move> actionHistory = context.trial().generateCompleteMovesList();
            int offsetActionToTraverse = actionHistory.size() - lastActionHistorySize;

            if (offsetActionToTraverse < 0)
            {
                // Something strange happened, probably forgot to call
                // initAI() for a newly-started game. Won't be a good
                // idea to reuse tree anyway
                rootNode = null;
            }

            while (offsetActionToTraverse > 0)
            {
                final Move move = actionHistory.get(actionHistory.size() - offsetActionToTraverse);
                rootNode = rootNode.findChildForMove(move);

                if (rootNode == null)
                {
                    // Didn't have a node in tree corresponding to action
                    // played, so can't reuse tree
                    break;
                }

                --offsetActionToTraverse;
            }
        }

        if (rootNode == null || !treeReuse)
        {
            // Need to create a fresh root
            rootNode = createNode(this, null, null, null, context);
            //System.out.println("NO TREE REUSE");
        }
        else
        {
            //System.out.println("successful tree reuse");

            // We're reusing a part of previous search tree
            // Clean up unused parts of search tree from memory
            //
            // rootNode.setParent(null);

            // TODO in nondeterministic games + OpenLoop MCTS, we'll want to
            // decay statistics gathered in the entire subtree here
        }

        if (heuristicStats != null)
        {
            // Clear all heuristic stats
            for (int p = 1; p < heuristicStats.length; ++p)
            {
                heuristicStats[p].init(0, 0.0, 0.0);
            }
        }

        rootNode.rootInit(context);

        if (rootNode.numLegalMoves() == 1)
        {
            // play faster if we only have one move available anyway
            if (autoPlaySeconds >= 0.0 && autoPlaySeconds < maxSeconds)
                stopTime = startTime + (long) (autoPlaySeconds * 1000);
        }

        lastActionHistorySize = context.trial().numMoves();

        lastNumPlayoutActions = 0;	// TODO if this variable actually becomes important, may want to make it Atomic

        // Store this in a separate variable because threading weirdness sometimes sets the class variable to null
        // even though some threads here still want to do something with it.
        final BaseNode rootThisCall = rootNode;

        // For each thread, queue up a job
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final long finalStopTime = stopTime;	// Need this to be final for use in inner lambda
        for (int thread = 0; thread < numThreads; ++thread)
        {
            threadPool.submit
                    (
                            () ->
                            {
                                try
                                {
                                    numThreadsBusy.incrementAndGet();

                                    // Search until we have to stop
                                    while (numIterations.get() < maxIts && System.currentTimeMillis() < finalStopTime && !wantsInterrupt)
                                    {
                                        /*********************
                                         Selection Phase
                                         *********************/
                                        BaseNode current = rootThisCall;
                                        current.addVirtualVisit();
                                        current.startNewIteration(context);

                                        Context playoutContext = null;

                                        while (current.contextRef().trial().status() == null)
                                        {
                                            BaseNode prevNode = current;
                                            prevNode.getLock().lock();

                                            try
                                            {
                                                final int selectedIdx = selectionStrategy.select(this, current);
                                                BaseNode nextNode = current.childForNthLegalMove(selectedIdx);

                                                final Context newContext = current.traverse(selectedIdx);

                                                if (nextNode == null)
                                                {
                                                    /*********************
                                                     Expand
                                                     *********************/
                                                    nextNode =
                                                            createNode
                                                                    (
                                                                            this,
                                                                            current,
                                                                            newContext.trial().lastMove(),
                                                                            current.nthLegalMove(selectedIdx),
                                                                            newContext
                                                                    );

                                                    current.addChild(nextNode, selectedIdx);
                                                    current = nextNode;
                                                    current.addVirtualVisit();
                                                    current.updateContextRef();

                                                    if ((expansionFlags & HEURISTIC_INIT) != 0)
                                                    {
                                                        assert (heuristicFunction != null);
                                                        nextNode.setHeuristicValueEstimates
                                                                (
                                                                        AIUtils.heuristicValueEstimates(nextNode.playoutContext(), heuristicFunction)
                                                                );
                                                    }

                                                    playoutContext = current.playoutContext();

                                                    break;	// stop Selection phase
                                                }

                                                current = nextNode;
                                                current.addVirtualVisit();
                                                current.updateContextRef();
                                            }
                                            catch (final ArrayIndexOutOfBoundsException e)
                                            {
                                                System.err.println(describeMCTS());
                                                throw e;
                                            }
                                            finally
                                            {
                                                prevNode.getLock().unlock();
                                            }
                                        }

                                        Trial endTrial = current.contextRef().trial();
                                        int numPlayoutActions = 0;

                                        if (!endTrial.over() && playoutValueWeight > 0.0)
                                        {
                                            // Did not reach a terminal game state yet

                                            /********************************
                                             Play-out
                                             ********************************/

                                            final int numActionsBeforePlayout = current.contextRef().trial().numMoves();

                                            endTrial = playoutStrategy.runPlayout(this, playoutContext);
                                            numPlayoutActions = (endTrial.numMoves() - numActionsBeforePlayout);

                                            lastNumPlayoutActions +=
                                                    (playoutContext.trial().numMoves() - numActionsBeforePlayout);
                                        }
                                        else
                                        {
                                            // Reached a terminal game state
                                            playoutContext = current.contextRef();
                                        }

                                        /***************************
                                         Backpropagation Phase
                                         ***************************/
                                        final double[] outcome = RankUtils.agentUtilities(playoutContext);
                                        backpropagationStrategy.update(this, current, playoutContext, outcome, numPlayoutActions);

                                        numIterations.incrementAndGet();
                                    }

                                    rootThisCall.cleanThreadLocals();
                                }
                                catch (final Exception e)
                                {
//                                    System.err.println("MCTS error in game: " + context.game().name());
//                                    e.printStackTrace();	// Need to do this here since we don't retrieve runnable's Future result
                                }
                                finally
                                {
                                    numThreadsBusy.decrementAndGet();
                                    latch.countDown();
                                }
                            }
                    );
        }

        try
        {
            latch.await(stopTime - startTime + 2000L, TimeUnit.MILLISECONDS);
        }
        catch (final InterruptedException e)
        {
            e.printStackTrace();
        }

        lastNumMctsIterations = numIterations.get();

        final Move returnMove = finalMoveSelectionStrategy.selectMove(this, rootThisCall);
        int playedChildIdx = -1;

        if (!wantsInterrupt)
        {
            int moveVisits = -1;

            for (int i = 0; i < rootThisCall.numLegalMoves(); ++i)
            {
                final BaseNode child = rootThisCall.childForNthLegalMove(i);

                if (child != null)
                {
                    if (rootThisCall.nthLegalMove(i).equals(returnMove))
                    {
                        final State state = rootThisCall.deterministicContextRef().state();
//                        final int moverAgent = state.playerToAgent(state.mover());
                        moveVisits = child.numVisits();
//                        lastReturnedMoveValueEst = child.expectedScore(moverAgent);
                        lastReturnedMoveValueEst = normalizeScore(context);
                        playedChildIdx = i;

                        break;
                    }
                }
            }

            final int numRootIts = rootThisCall.numVisits();

            analysisReport =
                    friendlyName +
                            " made move after " +
                            numRootIts +
                            " iterations (selected child visits = " +
                            moveVisits +
                            ", value = " +
                            lastReturnedMoveValueEst +
                            ").";
            BaseNode child = this.rootNode;
//			analysisReport += displayTree(child, "",true);
        }
        else
        {
            analysisReport = null;
        }

        // We can already try to clean up a bit of memory here
        // NOTE: from this point on we have to use rootNode instead of rootThisCall again!
        if (!preserveRootNode)
        {
            if (!treeReuse)
            {
                rootNode = null;	// clean up entire search tree
            }
            else if (!wantsInterrupt)	// only clean up if we didn't pause the AI / interrupt it
            {
                if (playedChildIdx >= 0)
                    rootNode = rootThisCall.childForNthLegalMove(playedChildIdx);
                else
                    rootNode = null;

                if (rootNode != null)
                {
                    rootNode.setParent(null);
                    ++lastActionHistorySize;
                }
            }
        }

        if (globalActionStats != null)
        {
            if (!treeReuse)
            {
                // Completely clear statistics if we're not reusing the tree
                globalActionStats.clear();
            }
            else
            {
                // Otherwise, decay statistics
                final Set<Map.Entry<MoveKey, ActionStatistics>> entries = globalActionStats.entrySet();
                final Iterator<Map.Entry<MoveKey, ActionStatistics>> it = entries.iterator();

                while (it.hasNext())
                {
                    final Map.Entry<MoveKey, ActionStatistics> entry = it.next();
                    final ActionStatistics stats = entry.getValue();
                    stats.visitCount *= globalActionDecayFactor;

                    if (stats.visitCount < 10.f)
                        it.remove();
                    else
                        stats.accumulatedScore *= globalActionDecayFactor;
                }
            }
        }

        if (globalNGramActionStats != null)
        {

            if (!treeReuse)
            {
                // Completely clear statistics if we're not reusing the tree
                globalNGramActionStats.clear();
            }
            else
            {
                // Otherwise, decay statistics
                final Set<Map.Entry<NGramMoveKey, ActionStatistics>> entries = globalNGramActionStats.entrySet();
                final Iterator<Map.Entry<NGramMoveKey, ActionStatistics>> it = entries.iterator();

                while (it.hasNext())
                {
                    final Map.Entry<NGramMoveKey, ActionStatistics> entry = it.next();
                    final ActionStatistics stats = entry.getValue();
                    stats.visitCount *= globalActionDecayFactor;

                    if (stats.visitCount < 10.f)
                        it.remove();
                    else
                        stats.accumulatedScore *= globalActionDecayFactor;
                }
            }
        }

        //System.out.println(numIterations + " MCTS iterations");
        return returnMove;
    }
}
