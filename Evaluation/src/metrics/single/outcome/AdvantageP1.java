package metrics.single.outcome;

import org.apache.commons.rng.RandomProviderState;

import game.Game;
import metrics.Metric;
import other.concept.Concept;
import other.trial.Trial;

/**
 * Percentage of games where player 1 won.
 * 
 * @author matthew.stephenson
 */
public class AdvantageP1 extends Metric
{

	//-------------------------------------------------------------------------

	/**
	 * Constructor
	 */
	public AdvantageP1()
	{
		super
		(
			"AdvantageP1", 
			"Percentage of games where player 1 won.", 
			0.0, 
			1.0,
			Concept.AdvantageP1
		);
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public double apply
	(
			final Game game,
			final Trial[] trials,
			final RandomProviderState[] randomProviderStates
	)
	{
		// Count number of wins for P1
		double p1Wins = 0.0;
		for (final Trial trial : trials)
			if (trial.status().winner() == 1)
				p1Wins++;

		return p1Wins / trials.length;
	}

	//-------------------------------------------------------------------------

}