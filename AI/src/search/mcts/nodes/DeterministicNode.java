package search.mcts.nodes;

import main.collections.FVector;
import main.collections.FastArrayList;
import other.context.Context;
import other.move.Move;
import search.mcts.MCTS;

import javax.jdo.annotations.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract class for nodes for any deterministic game.
 * 
 * @author Dennis Soemers
 */
@PersistenceCapable
//@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
////@Discriminator(column = "DETERMINISTIC_NODE_TYPE", value = "DeterministicNode")
//@Discriminator(value = "DeterministicNode")
public abstract class DeterministicNode extends BaseNode
{
	
	//-------------------------------------------------------------------------
	
	/** Context for this node (contains game state) */
    @Persistent
	protected Context context;
    
    /** Array of child nodes. */
    @Persistent
    protected DeterministicNode[] children;
    
    /** Array of legal moves in this node's state */
    @Persistent
    protected Move[] legalMoves;
    
    /** Cached policy over the list of children */
    @NotPersistent
    protected FVector cachedPolicy = null;
    
    /** Indices of relevant children (for deterministic game, every child is always relevant) */
    @Persistent
    protected int[] childIndices;
    
    /** Number of (potential) children that we've never visited */
    @Persistent
    protected int numUnvisitedChildren = -1;
    
    //-------------------------------------------------------------------------
    
    /**
     * Constructor 
     * 
     * @param mcts
     * @param parent
     * @param parentMove
     * @param parentMoveWithoutConseq
     * @param context
     */
    public DeterministicNode
    (
    	final MCTS mcts, 
    	final BaseNode parent, 
    	final Move parentMove, 
    	final Move parentMoveWithoutConseq,
    	final Context context
    )
    {
    	super(mcts, parent, parentMove, parentMoveWithoutConseq, context.game());
    	this.context = context;
    	
    	if (context.trial().over())
    	{
    		// we just created a node for a terminal game state, 
    		// so create empty list of actions
    		legalMoves = new Move[0];
    	}
    	else
    	{
    		// non-terminal game state, so figure out list of actions we can 
    		// still take
    		final FastArrayList<Move> actions = context.game().moves(context).moves();
    		legalMoves = new Move[actions.size()];
    		actions.toArray(legalMoves);
    	}
    	
    	children = new DeterministicNode[legalMoves.length];
    	childIndices = new int[children.length];
    	
    	for (int i = 0; i < childIndices.length; ++i)
    	{
    		childIndices[i] = i;
    	}
    	
    	numUnvisitedChildren = children.length;
    }

    public DeterministicNode() {
        super();
    }

    //-------------------------------------------------------------------------

    @Override
    public void addChild(final BaseNode child, final int moveIdx)
    {
    	children[moveIdx] = (DeterministicNode) child;
    	--numUnvisitedChildren;
    	
    	if (numUnvisitedChildren == 0 && MCTS.NULL_UNDO_DATA)
    		context.trial().nullUndoData();		// Clear a bunch of memory we no longer need
    }

    @Override
    public List<BaseNode> getChildren() {
        if(children == null)
            return new ArrayList<>();
        List<BaseNode> children = Arrays.asList(this.children);
//        return List.copyOf(children); // defensive copy of children;
        return children;
        //TODO: check if null children are allowed and if copyOf is needed
    }
    
    @Override
    public DeterministicNode childForNthLegalMove(final int n)
    {
    	return children[n];
    }
    
    @Override
    public Context contextRef()
    {
    	return context;
    }
    
    @Override
    public Context deterministicContextRef()
    {
    	return context;
    }
    
    @Override
    public DeterministicNode findChildForMove(final Move move)
    {
    	DeterministicNode result = null;
		
		for (final DeterministicNode child : children)
		{
			if (child != null && child.parentMove().equals(move))
			{
				//System.out.println("found equal move: " + child.parentMove() + " equals " + move);
				result = child;
				break;
			}
//			else if (child != null)
//			{
//				System.out.println(child.parentMove() + " no match for: " + move);
//			}
		}
		
		return result;
    }
    
    @Override
    public FastArrayList<Move> movesFromNode()
    {
    	return new FastArrayList<Move>(legalMoves);
    }
    
    @Override
    public int nodeColour()
    {
    	return context.state().mover();
    }
    
    @Override
    public Move nthLegalMove(final int n)
    {
    	return legalMoves[n];
    }
    
    @Override
    public int numLegalMoves()
    {
    	return children.length;
    }
    
    @Override
    public Context playoutContext()
    {
    	// Need to copy context
    	return mcts.copyContext(context);
    }
    
    @Override
    public void rootInit(final Context cont)
    {
    	// Do nothing
    }
    
    @Override
    public void startNewIteration(final Context cont)
    {
    	// Do nothing
    }
    
    @Override
    public int sumLegalChildVisits()
    {
    	// Just the number of visits of this node
    	return numVisits;
    }
    
    @Override
    public Context traverse(final int moveIdx)
    {
    	final Context newContext;
    	
    	if (children[moveIdx] == null)
    	{
    		// Need to copy context
        	newContext = mcts.copyContext(context);
        	newContext.game().apply(newContext, legalMoves[moveIdx]);
    	}
    	else
    	{
    		newContext = children[moveIdx].context;
    	}
    	
    	return newContext;
    }
    
    @Override
    public void updateContextRef()
    {
    	// Do nothing
    }
    
    @Override
    public void cleanThreadLocals()
    {
    	// Do nothing
    }
    
    //-------------------------------------------------------------------------
    
    /**
     * @return Array of child nodes
     */
    public DeterministicNode[] children()
    {
    	return children;
    }
    
    /**
     * @return List of legal actions for this node's state
     */
    public Move[] legalActions()
    {
    	return legalMoves;
    }
    
    //-------------------------------------------------------------------------
    
    @Override
    public FVector learnedSelectionPolicy()
    {
    	// NOTE: by caching policy, we're assuming here that our learned policy 
    	// will never change in the middle of a single game. Have to change 
    	// this if we ever want to experiment with online learning in the 
    	// middle of a game.
    	if (cachedPolicy == null)
    	{
    		// didn't compute policy yet, so need to do so
    		cachedPolicy = 
    				mcts.learnedSelectionPolicy().computeDistribution(
    						context, new FastArrayList<Move>(legalMoves), true);
    	}
    	
    	return cachedPolicy;
    }
    
    //-------------------------------------------------------------------------

}
