package common;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import game.Game;
import main.grammar.Description;
import other.GameLoader;

/**
 * A lightweight representation of a game and the chosen ruleset. It is
 * determined by the ludemeFile and the chosen List<String> ruleSet.
 * 
 * @author Markus
 *
 */
public class LudRul implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final File ludFile;
	private final List<String> ruleSetString;
	private final String nameCombiString;

	private static final HashMap<LudRul, String> expandedDescriptions = new HashMap<>();

	public static void releaseResources()
	{
		expandedDescriptions.clear();
	}

	/**
	 * 
	 * @param ludFile
	 * @param ruleSetString
	 */
	public LudRul(final File ludFile, final List<String> ruleSetString)
	{
		this.ludFile = ludFile;
		this.ruleSetString = new ArrayList<String>(ruleSetString);
		nameCombiString = ludFile.getName() + ruleSetString.toString();
	}

	/**
	 * if default ruleset is used
	 * 
	 * @param ludFile
	 */
	public LudRul(final File ludFile)
	{
		this.ludFile = ludFile;
		this.ruleSetString = new ArrayList<String>();
		nameCombiString = ludFile.getName() + ruleSetString.toString();
	}

	public File getFile()
	{
		return ludFile;
	}

	/**
	 * @return ruleset as used by the GameLoader
	 */
	public List<String> getRuleSet()
	{
		return ruleSetString;
	}

	/**
	 * compares the files and the ruleset strings. In the current implementation it
	 * is not certain if two equal relative paths starting from different folders,
	 * would be seen as equal.
	 * 
	 * @param object2
	 * @return true if both point to the same file and have the same ruleset strings (in the same order)
	 */
	public boolean equals(final LudRul object2)
	{

		if (!ludFile.equals(object2.getFile()))
			return false;
		return (ruleSetString.equals(object2.ruleSetString));

	}

	/**
	 * @return the name together the selected options <p>
	 * For example: "JurokuMusashi.lud[Variant/Described]"
	 */
	public String getGameNameIncludingOption()
	{
		return nameCombiString;
	}

	@Override
	public String toString()
	{
		return nameCombiString;

	}

	/**
	 * @return the name of the folder this game would be in.
	 */
	public String getCurrentFolderName()
	{
		return DistanceUtils.getCurrentFolderName(ludFile);

	}

	/**
	 * Lazy loads the expanded description and stores it for future retrival
	 * 
	 * @return the expanded description
	 */
	public String getDescriptionExpanded()
	{
		String s = expandedDescriptions.get(this);
		if (s != null)
			return s;

		final Game game = GameLoader.loadGameFromFile(this.ludFile, this.ruleSetString);
		s = game.description().expanded();
		expandedDescriptions.put(this, s);
		return s;
	}

	public Description getDescription()
	{
		return GameLoader.loadGameFromFile(this.ludFile, this.ruleSetString).description();

	}

	public Game getGame()
	{
		return GameLoader.loadGameFromFile(this.ludFile, this.ruleSetString);
	}

	public String[] getDescriptionSplit()
	{
		final String dataCleanA = cleanString(getDescriptionExpanded());
		final String[] wordsA = dataCleanA.split("\\s+");
		return wordsA;
	}
	
	
	/**
	 * @param contentData ...
	 * @return String with just single words and no double spaces.
	 */
	public String cleanString(final String contentData)
	{
		final String data = contentData;
		final String dataAlphabetic = data.replaceAll("[^A-Za-z0-9 ]", " ");

		// Maybe keep numbers, to ???
		// dataAlphabetic = data.replaceAll("[^A-Za-z ]"," ");

		final String dataSingleSpace = dataAlphabetic.trim().replaceAll(" +", " ");
		final String dataClean = dataSingleSpace.toLowerCase();

		return dataClean;
	}

}