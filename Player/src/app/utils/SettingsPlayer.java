package app.utils;

import java.awt.Point;
import java.util.Timer;

import app.PlayerApp;
import app.move.animation.AnimationParameters;
import app.move.animation.MoveAnimation;
import game.equipment.component.Component;
import other.context.Context;
import other.state.State;
import policies.softmax.SoftmaxFromMetadata;

/**
 * Settings for the current player.
 * 
 * @author Matthew and cambolbro
 */
public class SettingsPlayer
{
	
	/** Default X and Y values for placing the frame (loaded from preferences). */
	private int defaultX = -1;
	private int defaultY = -1;
	
	/** Whether the fame should be maximised (loaded from preferences). */
	private boolean frameMaximised = false;
	
	/** True if the Developer cursor tooltip is to be shown. */
	private boolean cursorTooltipDev = false;
	
	/** True if we should play a sound effect after each move. */
	private boolean moveSoundEffect = false;

	/** Font size for the text area. */
	private int tabFontSize = 13;
	
	/** Font size for the text area. */
	private int editorFontSize = 13;
	
	/** Format for printing moves (Move, Full, Short). */
	private String moveFormat = "Move";
	
	/** If pressing |< Button Pauses AI. */
	private boolean startButtonPausesAI = true;
	
	/** Strings entered into the TestLudemeDialog, to be saved in preferences. */
	private String testLudeme1 = "";
	private String testLudeme2 = "";
	private String testLudeme3 = "";
	private String testLudeme4 = "";
	
	/** If the zoomBox (magnifying glass) should be shown. */
	private boolean showZoomBox = false;
	
	/** If we should display moves using coord rather than index */
	private boolean moveCoord = true;
	
	/** number of walks to increment the drag piece image by when rotating. */
	private int currentWalkExtra = 0;
	/** Show the board. */
	private boolean showBoard = true;
	/** Show the pieces. */
	private boolean showPieces = true;
	/** Show the graph. */
	private boolean showGraph = false;
	/** Show the dual. */
	private boolean showConnections = false;
	/** Show the board axes. */
	private boolean showAxes = false;
	
	/** Tab currently selected. */
	private int tabSelected = 0;
	
	//-------------------------------------------------------------------------
	// User settings
	
	/** For puzzles whether to show the dialog, cycle through, or decide automatically. */
	private PuzzleSelectionType puzzleDialogOption = PuzzleSelectionType.Automatic;
	
	/** Visualize AI's distribution over moves on the game board */
	private boolean showAIDistribution = false;
	
	/** Visualize the last move made on the game board */
	private boolean showLastMove = false;
	/** Visualize any ending moves made on the game board */
	private boolean showEndingMove = true;
	
	private boolean swapRule = false;
	private boolean noRepetition = false;
	private boolean noRepetitionWithinTurn = false;
	
	/** Hide the moves of the AI if this is a hidden information game.
	Only applied in games where just one of the players is a human. */
	private boolean hideAiMoves = true;
	
	/** If true, whenever we save a trial, we also save a CSV file with heuristic evaluations of game states */
	private boolean saveHeuristics = false;
	
	/** If true, whenever the user clicks (or drags between) one or two sites, we print active features for matching moves */
	private boolean printMoveFeatures = false;
	
	/** If true, whenever the user clicks (or drags between) one or two sites, we print active feature instances for matching moves */
	private boolean printMoveFeatureInstances = false;
	
	/** Object we can use to compute active features for printing purposes */
	private final SoftmaxFromMetadata featurePrintingSoftmax = new SoftmaxFromMetadata(0.0);
	
	private boolean devMode = false;
	
	private boolean showAnimation = false;
	
	//-------------------------------------------------------------------------
	// Editor settings
	
	/** Whether the editor should show possible autocomplete options. */
	private boolean editorAutocomplete = true;
	
	//-------------------------------------------------------------------------
	// Animation settings
	
	private AnimationParameters animationParameters = null;
	
	/** Timer object used for animating moves. */
	private Timer animationTimer = new Timer();
	
	/** The number of frames still to go for the current animation. */
	protected int drawingMovingPieceTime = MoveAnimation.MOVE_PIECE_FRAMES;
	
	//-------------------------------------------------------------------------
	// Information about the component being dragged.
	
	/** Component currently being dragged. */
	private Component dragComponent = null;
	
	/** State of the dragged component. */
	private int dragComponentState = 1;
	
	/** Original mouse position at the start of the drag. */
	private Point oldMousePoint = new Point(0,0);
	
	/** If a component is currently selected. */
	private boolean componentIsSelected = false;
	
	//-------------------------------------------------------------------------
	// Other
	
	/** Whether illegal moves are allowed to be made. */
	private boolean illegalMovesValid = false;
	
	/** The last error message that was reported. Stored to prevent the same error message being repeated multiple times. */
	private String lastErrorMessage = "";
	
	/** If the trial should be saved after every move. */
	private boolean saveTrialAfterMove = false;
	
	/** If the last game was loaded successfully or not.
	  * Used to determine if trial and preferences should be saved. */
	private boolean loadSuccessful = false;
	
	/** saved match game description. */
	private String matchDescriptionFull = "";
	private String matchDescriptionShort = "";
	
	/** whether or not the preferences have been loaded successfully. */
	private boolean preferencesLoaded = false;
	
	/** most recent games that have been loaded, used for menu option. */
	private String[] recentGames = {null,null,null,null,null,null,null,null,null,null};
	
	/** If the last game was loaded from memory. */
	private boolean loadedFromMemory = false;
	
	private String savedStatusTabString = "";
	
	//-------------------------------------------------------------------------
	
	public int getIntermediateContextPlayerNumber(final PlayerApp app)
	{
		final Context context = app.manager().ref().context();
		final State state = context.state();
		int mover = state.mover();
		
		if (context.game().isDeductionPuzzle())
			return mover;
		
		if (app.manager().settingsNetwork().getNetworkPlayerNumber() > 0)
		{
			mover = app.manager().settingsNetwork().getNetworkPlayerNumber();
		}
		else if (hideAiMoves())
		{
			int humansFound = 0;
			int humanIndex = 0;
			for (int i = 1; i <= context.game().players().count(); i++)
			{
				if (app.manager().aiSelected()[i].ai() == null)
				{
					humansFound++;
					humanIndex = state.playerToAgent(i);
				}
			}
			
			if (humansFound == 1)
				mover = humanIndex;
		}
		
		return mover;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Add gameName to the list of recent games, or update its position in this list.
	 * These games can then be selected from the menu bar
	 * @param gameName
	 */
	public void updateRecentGames(final PlayerApp app, final String gameName)
	{
		String GameMenuName = gameName;
		
		if (!loadedFromMemory())
			GameMenuName = app.manager().savedLudName();
		
		int gameAlreadyIncluded = -1;
		
		// Check if the game is already included in our recent games list, and record its position.
		for (int i = 0; i < recentGames().length; i++)
		{
			if (recentGames()[i] != null && recentGames()[i].equals(GameMenuName))
			{
				gameAlreadyIncluded = i;
			}
		}

		// If game was not already in recent games list, record the last position on the list
		if (gameAlreadyIncluded == -1)
		{
			gameAlreadyIncluded = recentGames().length-1;
		}

		// Shift all games ahead of the recored position down a spot.
		for (int i = gameAlreadyIncluded; i > 0; i--)
		{
			recentGames()[i] = recentGames()[i-1];
		}
		
		// Add game at front of recent games list.
		recentGames()[0] = GameMenuName;
	}
	
	//-------------------------------------------------------------------------

	public boolean isMoveCoord()
	{
		return moveCoord;
	}

	public void setMoveCoord(final boolean moveCoord)
	{
		this.moveCoord = moveCoord;
	}

	public int defaultX()
	{
		return defaultX;
	}

	public void setDefaultX(final int x)
	{
		defaultX = x;
	}

	public int defaultY()
	{
		return defaultY;
	}

	public void setDefaultY(final int y)
	{
		defaultY = y;
	}

	public boolean frameMaximised()
	{
		return frameMaximised;
	}

	public void setFrameMaximised(final boolean max)
	{
		frameMaximised = max;
	}

	public boolean cursorTooltipDev()
	{
		return cursorTooltipDev;
	}

	public void setCursorTooltipDev(final boolean value)
	{
		cursorTooltipDev = value;
	}

	public int tabFontSize()
	{
		return tabFontSize;
	}

	public void setTabFontSize(final int size)
	{
		tabFontSize = size;
	}

	public int editorFontSize()
	{
		return editorFontSize;
	}

	public void setEditorFontSize(final int size)
	{
		editorFontSize = size;
	}

	public String moveFormat()
	{
		return moveFormat;
	}

	public void setMoveFormat(final String format)
	{
		moveFormat = format;
	}

	public boolean startButtonPausesAI()
	{
		return startButtonPausesAI;
	}

	public void setStartButtonPausesAI(final boolean start)
	{
		startButtonPausesAI = start;
	}

	public String testLudeme1()
	{
		return testLudeme1;
	}

	public void setTestLudeme1(final String test1)
	{
		testLudeme1 = test1;
	}

	public String testLudeme2()
	{
		return testLudeme2;
	}

	public void setTestLudeme2(final String test2)
	{
		testLudeme2 = test2;
	}

	public String testLudeme3()
	{
		return testLudeme3;
	}

	public void setTestLudeme3(final String test3)
	{
		testLudeme3 = test3;
	}

	public String testLudeme4()
	{
		return testLudeme4;
	}

	public void setTestLudeme4(final String test4)
	{
		testLudeme4 = test4;
	}

	public boolean showZoomBox()
	{
		return showZoomBox;
	}

	public void setShowZoomBox(final boolean show)
	{
		showZoomBox = show;
	}

	public int currentWalkExtra() 
	{
		return currentWalkExtra;
	}

	public void setCurrentWalkExtra(final int currentWalkExtra) 
	{
		this.currentWalkExtra = currentWalkExtra;
	}
	

	public boolean showBoard()
	{
		return showBoard;
	}

	public void setShowBoard(final boolean show)
	{
		showBoard = show;
	}

	public boolean showPieces()
	{
		return showPieces;
	}

	public void setShowPieces(final boolean show)
	{
		showPieces = show;
	}

	public boolean showGraph()
	{
		return showGraph;
	}

	public void setShowGraph(final boolean show)
	{
		showGraph = show;
	}

	public boolean showConnections()
	{
		return showConnections;
	}

	public void setShowConnections(final boolean show)
	{
		showConnections = show;
	}

	public boolean showAxes()
	{
		return showAxes;
	}

	public void setShowAxes(final boolean show)
	{
		showAxes = show;
	}
	
	public PuzzleSelectionType puzzleDialogOption()
	{
		return puzzleDialogOption;
	}

	public void setPuzzleDialogOption(final PuzzleSelectionType puzzleDialogOption)
	{
		this.puzzleDialogOption = puzzleDialogOption;
	}

	public boolean showAIDistribution()
	{
		return showAIDistribution;
	}

	public void setShowAIDistribution(final boolean show)
	{
		showAIDistribution = show;
	}

	public boolean showLastMove()
	{
		return showLastMove;
	}

	public void setShowLastMove(final boolean show)
	{
		showLastMove = show;
	}
	
	public boolean showEndingMove()
	{
		return showEndingMove;
	}

	public void setShowEndingMove(final boolean show)
	{
		showEndingMove = show;
	}

	public boolean swapRule()
	{
		return swapRule;
	}

	public void setSwapRule(final boolean swap)
	{
		swapRule = swap;
	}

	public boolean noRepetition()
	{
		return noRepetition;
	}

	public void setNoRepetition(final boolean no)
	{
		noRepetition = no;
	}

	public boolean noRepetitionWithinTurn()
	{
		return noRepetitionWithinTurn;
	}

	public void setNoRepetitionWithinTurn(final boolean no)
	{
		noRepetitionWithinTurn = no;
	}

	public boolean hideAiMoves()
	{
		return hideAiMoves;
	}

	public void setHideAiMoves(final boolean hide)
	{
		hideAiMoves = hide;
	}

	public boolean saveHeuristics()
	{
		return saveHeuristics;
	}

	public void setSaveHeuristics(final boolean save)
	{
		saveHeuristics = save;
	}
	
	/**
	 * @return Do we want to print move features?
	 */
	public boolean printMoveFeatures()
	{
		return printMoveFeatures;
	}
	
	/**
	 * @return Do we want to print move feature instances?
	 */
	public boolean printMoveFeatureInstances()
	{
		return printMoveFeatureInstances;
	}
	
	/**
	 * Sets whether we want to print move features
	 * @param val
	 */
	public void setPrintMoveFeatures(final boolean val)
	{
		printMoveFeatures = val;
	}
	
	/**
	 * Sets whether we want to print move feature instances
	 * @param val
	 */
	public void setPrintMoveFeatureInstances(final boolean val)
	{
		printMoveFeatureInstances = val;
	}
	
	/**
	 * @return The softmax object we can use for computing features to print
	 */
	public SoftmaxFromMetadata featurePrintingSoftmax()
	{
		return featurePrintingSoftmax;
	}

	public boolean devMode()
	{
		return devMode;
	}

	public void setDevMode(final boolean value)
	{
		devMode = value;
	}

	public Component dragComponent()
	{
		return dragComponent;
	}

	public void setDragComponent(final Component drag)
	{
		dragComponent = drag;
	}

	public int dragComponentState()
	{
		return dragComponentState;
	}

	public void setDragComponentState(final int drag)
	{
		dragComponentState = drag;
	}

	public Point oldMousePoint()
	{
		return oldMousePoint;
	}

	public void setOldMousePoint(final Point old)
	{
		oldMousePoint = old;
	}

	public boolean illegalMovesValid()
	{
		return illegalMovesValid;
	}

	public void setIllegalMovesValid(final boolean illegal)
	{
		illegalMovesValid = illegal;
	}

	public String lastErrorMessage()
	{
		return lastErrorMessage;
	}

	public void setLastErrorMessage(final String last)
	{
		lastErrorMessage = last;
	}

	public boolean editorAutocomplete()
	{
		return editorAutocomplete;
	}

	public void setEditorAutocomplete(final boolean editor)
	{
		editorAutocomplete = editor;
	}
	
	public boolean componentIsSelected() 
	{
		return componentIsSelected;
	}

	public void setComponentIsSelected(final boolean componentIsSelected) 
	{
		this.componentIsSelected = componentIsSelected;
	}

	public boolean isMoveSoundEffect() 
	{
		return moveSoundEffect;
	}

	public void setMoveSoundEffect(final boolean moveSoundEffect) 
	{
		this.moveSoundEffect = moveSoundEffect;
	}

	public boolean showAnimation() 
	{
		return showAnimation;
	}

	public void setShowAnimation(final boolean showAnimation) 
	{
		this.showAnimation = showAnimation;
	}
	
	public Timer getAnimationTimer()
	{
		return animationTimer;
	}

	public void setAnimationTimer(final Timer animationTimer)
	{
		this.animationTimer = animationTimer;
	}

	public int getDrawingMovingPieceTime()
	{
		return drawingMovingPieceTime;
	}

	public void setDrawingMovingPieceTime(final int drawingMovingPieceTime)
	{
		this.drawingMovingPieceTime = drawingMovingPieceTime;
	}

	public boolean saveTrialAfterMove() 
	{
		return saveTrialAfterMove;
	}

	public void setSaveTrialAfterMove(final boolean saveTrialAfterMove) 
	{
		this.saveTrialAfterMove = saveTrialAfterMove;
	}
	
	public boolean loadSuccessful()
	{
		return loadSuccessful;
	}

	public void setLoadSuccessful(final boolean loadSuccessful)
	{
		this.loadSuccessful = loadSuccessful;
	}
	
	public String matchDescriptionFull()
	{
		return matchDescriptionFull;
	}

	public void setMatchDescriptionFull(final String matchDescriptionFull)
	{
		this.matchDescriptionFull = matchDescriptionFull;
	}

	public String matchDescriptionShort()
	{
		return matchDescriptionShort;
	}

	public void setMatchDescriptionShort(final String matchDescriptionShort)
	{
		this.matchDescriptionShort = matchDescriptionShort;
	}
	
	public boolean preferencesLoaded()
	{
		return preferencesLoaded;
	}

	public void setPreferencesLoaded(final boolean preferencesLoaded)
	{
		this.preferencesLoaded = preferencesLoaded;
	}

	public String[] recentGames() 
	{
		return recentGames;
	}

	public void setRecentGames(final String[] recentGames) 
	{
		this.recentGames = recentGames;
	}
	
	public boolean loadedFromMemory()
	{
		return loadedFromMemory;
	}

	public void setLoadedFromMemory(final boolean loadedFromMemory)
	{
		this.loadedFromMemory = loadedFromMemory;
	}

	public String savedStatusTabString() 
	{
		return savedStatusTabString;
	}

	public void setSavedStatusTabString(final String savedStatusTabString) 
	{
		this.savedStatusTabString = savedStatusTabString;
	}

	public AnimationParameters animationParameters() 
	{
		return animationParameters;
	}

	public void setAnimationParameters(final AnimationParameters animationParameters) 
	{
		this.animationParameters = animationParameters;
	}
	
	public int tabSelected()
	{
		return tabSelected;
	}

	public void setTabSelected(final int tabSelected)
	{
		this.tabSelected = tabSelected;
	}
			
	//-------------------------------------------------------------------------

}