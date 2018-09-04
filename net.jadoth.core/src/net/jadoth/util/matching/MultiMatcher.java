package net.jadoth.util.matching;

import net.jadoth.chars.XChars;
import net.jadoth.collections.types.XGettingCollection;
import net.jadoth.equality.Equalator;
import net.jadoth.functional.Similator;


/**
 * Logic for bidirectionally and exclusively linking all matching elements from two collections according
 * to equality and/or sufficient similarity.
 * <p>
 * Exclusviely means each element in both collections can at most be linked with one element from the other collection.
 * Bidirectionally means the link between two elements always has two directions. If element A is linked to element B,
 * element B is inherently linked to element A as well.
 * <p>
 * Equality and similarity are defined by {@link Equalator} and {@link Similator} functions that can be passed at
 * creation time. All values controlling the matching algorithm can be optionally configured in the factory class
 * if the default configuration is not desired. Additionally, a callback function for deciding found matches with
 * questionable similarity can be injected.
 * <p>
 * This is a powerful general purpose means of building associations of two sets of similar but not equal elements.<br>
 * A very simple use case is the formal recognition of a changed table column structure (for which this class
 * was originally developed).
 * <p>
 * For example given the following two hypothetical definitions (old and new) of column names:<br>
 * <br>
 * <u><b>Old</b></u>:
 * <ul>
 * <li>Name</li>
 * <li>Firstname</li>
 * <li>Age</li>
 * <li>Address</li>
 * <li>Freetext</li>
 * <li>Email</li>
 * <li>OtherAddress</li>
 * </ul>
 * and<br>
 * <br>
 * <u><b>New</b></u>:
 * <ul>
 * <li>firstname</li>
 * <li>lastname</li>
 * <li>age</li>
 * <li>emailAddress</li>
 * <li>postalAddress</li>
 * <li>noteLink</li>
 * <li>newColumn1</li>
 * <li>someMiscAddress</li>
 * </ul>
 * When using a case insensitive modified Levenshtein {@link Similator}
 * (see {@link XChars#levenshteinSubstringSimilarity}) the algorithm produces the following associations:
 * <pre>
 * firstname       <-1.00-> Firstname
 * lastname        <-0.75-> Name
 * age             <-1.00-> Age
 * emailAddress    <-0.71-> Email
 * postalAddress   <-0.77-> Address
 * noteLink        [new]
 * newColumn1      [new]
 * someMiscAddress <-0.56-> OtherAddress
 *                        X Freetext
 * </pre>
 *
 * @author Thomas Muenz
 *
 * @param <E> the type of the elements being matched.
 */
public interface MultiMatcher<E>
{
	public double similarityThreshold();
	
	public double singletonPrecedenceThreshold();
	
	public double singletonPrecedenceBonus();
	
	public double noiseFactor();

	public Equalator<? super E> equalator();
	
	public Similator<? super E> similator();
	
	public MatchValidator<? super E> matchCallback();
	

	public MultiMatcher<E> setSimilarityThreshold(double similarityThreshold);
	
	public MultiMatcher<E> setSingletonPrecedenceThreshold(double singletonPrecedenceThreshold);
	
	public MultiMatcher<E> setSingletonPrecedenceBonus(double singletonPrecedenceBonus);
	
	public MultiMatcher<E> setNoisefactor(double noiseFactor);

	public MultiMatcher<E> setSimilator(Similator<? super E> similator);
	
	public MultiMatcher<E> setEqualator(Equalator<? super E> equalator);
	
	public MultiMatcher<E> setMatchCallback(MatchValidator<? super E> decisionCallback);
	

	public MultiMatch<E> match(XGettingCollection<? extends E> source, XGettingCollection<? extends E> target);


	
	public static <E> Equalator<E> defaultEqualator()
	{
		return Equalator.value();
	}
	
	public static double defaultSimilarityThreshold()
	{
		return 0.50;
	}
	
	public static double defaultSingletonPrecedenceThreshold()
	{
		return 0.75;
	}
	
	public static double defaultSingletonPrecedenceBonus()
	{
		return 0.75;
	}
	
	public static double defaultNoiseFactor()
	{
		return 0.50;
	}
	
	public static int calculateMatchCount(final int[] s2tMapping)
	{
		int matchCount = 0;
		for(int i = 0; i < s2tMapping.length; i++)
		{
			if(s2tMapping[i] < 0)
			{
				continue;
			}
			matchCount++;
		}
		return matchCount;
	}

	public static int maxTargetQuantifier(final int[] sTargets)
	{
		int maxQuantifier = 0;
		for(int t = 0; t < sTargets.length; t++)
		{
			if(sTargets[t] > maxQuantifier)
			{
				maxQuantifier = sTargets[t];
			}
		}
		return maxQuantifier;
	}

	public static int maxSourceQuantifier(final int[][] quantifiers, final int t)
	{
		int maxQuantifier = 0;
		for(int s = 0; s < quantifiers.length; s++)
		{
			if(quantifiers[s][t] > maxQuantifier)
			{
				maxQuantifier = quantifiers[s][t];
			}
		}
		return maxQuantifier;
	}

	public class Implementation<E> implements MultiMatcher<E>
	{
		/* (04.08.2011 TM)TOD0: JavaDoc
		 * (04.09.2018 TM)NOTE: I'll leave that here for nostalgia's sake and as a foundation stone timestamp.
		 * (of course, EVERYTHING needs JavaDoc... )
		 */

		///////////////////////////////////////////////////////////////////////////
		// instance fields  //
		/////////////////////

		private Equalator<? super E>         equalator     = defaultEqualator();
		private Similator<? super E>         similator    ;
		private MatchValidator<? super E> matchCallback;

		private double similarityThreshold          = defaultSimilarityThreshold();
		private double singletonPrecedenceThreshold = defaultSingletonPrecedenceThreshold();
		private double singletonPrecedenceBonus     = defaultSingletonPrecedenceBonus();
		private double noiseFactor                  = defaultNoiseFactor();



		///////////////////////////////////////////////////////////////////////////
		// getters          //
		/////////////////////

		@Override
		public double similarityThreshold()
		{
			return this.similarityThreshold;
		}

		@Override
		public double singletonPrecedenceThreshold()
		{
			return this.singletonPrecedenceThreshold;
		}

		@Override
		public double singletonPrecedenceBonus()
		{
			return this.singletonPrecedenceBonus;
		}

		@Override
		public double noiseFactor()
		{
			return this.noiseFactor;
		}

		@Override
		public Equalator<? super E> equalator()
		{
			return this.equalator;
		}

		@Override
		public Similator<? super E> similator()
		{
			return this.similator;
		}

		@Override
		public MatchValidator<? super E> matchCallback()
		{
			return this.matchCallback;
		}



		///////////////////////////////////////////////////////////////////////////
		// setters          //
		/////////////////////

		@Override
		public MultiMatcher<E> setSimilarityThreshold(final double similarityThreshold)
		{
			this.similarityThreshold = similarityThreshold;
			return this;
		}

		@Override
		public MultiMatcher<E> setSingletonPrecedenceThreshold(final double singletonPrecedenceThreshold)
		{
			this.singletonPrecedenceThreshold = singletonPrecedenceThreshold;
			return this;
		}

		@Override
		public MultiMatcher<E> setSingletonPrecedenceBonus(final double singletonPrecedenceBonus)
		{
			this.singletonPrecedenceBonus = singletonPrecedenceBonus;
			return this;
		}

		@Override
		public MultiMatcher<E> setNoisefactor(final double noiseFactor)
		{
			this.noiseFactor = noiseFactor;
			return this;
		}

		@Override
		public MultiMatcher<E> setSimilator(final Similator<? super E> similator)
		{
			this.similator = similator;
			return this;
		}

		@Override
		public MultiMatcher<E> setEqualator(final Equalator<? super E> equalator)
		{
			this.equalator = equalator;
			return this;
		}

		@Override
		public MultiMatcher<E> setMatchCallback(final MatchValidator<? super E> suspiciousMatchDecider)
		{
			this.matchCallback = suspiciousMatchDecider;
			return this;
		}



		///////////////////////////////////////////////////////////////////////////
		// methods //
		////////////

		@SuppressWarnings("unchecked")
		@Override
		public MultiMatch<E> match(
			final XGettingCollection<? extends E> source,
			final XGettingCollection<? extends E> target
		)
		{
			return new MultiMatch.Implementation<>(
				this,
				(E[])source.toArray(),
				(E[])target.toArray()
			).match();
		}
		
	}

}
