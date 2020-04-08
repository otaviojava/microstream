
package one.microstream.cache;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Random;

import one.microstream.X;
import one.microstream.collections.EqHashTable;
import one.microstream.functional.Aggregator;
import one.microstream.math.XMath;
import one.microstream.typing.KeyValue;


public class MainTestEviction
{
	static final Comparator<KeyValue<Integer, Entry>> COMPARATOR =
		(e1, e2) -> Long.compare(e1.value().timestamp, e2.value().timestamp);
	static final Random                               RANDOM     = new Random();
	
	public static void main(final String[] args)
	{
		long                              start          = System.currentTimeMillis();
		
		final int                         maxCacheSize   = 100_000;
		final EqHashTable<Integer, Entry> cache          = EqHashTable.New();
		final int                         durationMillis = (int)Duration.ofDays(7).toMillis();
		for(long i = 0; i < maxCacheSize; i++)
		{
			cache.put(XMath.random(maxCacheSize), new Entry(start - XMath.random(durationMillis), "Entry" + i));
		}
		
		System.out.println("Cache filled, " + cache.size()
			+ " entries in " + (System.currentTimeMillis() - start));
		
		final Entry minEntry = cache.min(COMPARATOR).value();
		final Entry maxEntry = cache.max(COMPARATOR).value();
		System.out.println("Min: " + Instant.ofEpochMilli(minEntry.timestamp));
		System.out.println("Max: " + Instant.ofEpochMilli(maxEntry.timestamp));
		
		start = System.currentTimeMillis();
		
		final int sampleCount      = 1000;
		double    approximationSum = 0.0;
		for(int i = 1; i <= sampleCount; i++)
		{
			approximationSum += sample(cache, minEntry, maxEntry);
		}
		
		final long duration = System.currentTimeMillis() - start;
		System.out.println("Approximation: " + new BigDecimal(approximationSum).divide(new BigDecimal(sampleCount)));
		System.out.println("Average time: " + duration / sampleCount);
	}
	
	static double sample(final EqHashTable<Integer, Entry> cache, final Entry minEntry, final Entry maxEntry)
	{
		// final long start = System.currentTimeMillis();
		
		final Entry smallestEntry;
		
		final int   cacheSize = cache.intSize();
		if(cacheSize < 10_000)
		{
			smallestEntry = cache.min(COMPARATOR).value();
		}
		else
		{
			final int minSampleSize = 15;
			final int maxSampleSize = 100;
			final int optSampleSize = (int)(cacheSize * 0.002);
			final int sampleSize    = optSampleSize < minSampleSize
				? minSampleSize
				: optSampleSize > maxSampleSize
					? maxSampleSize
					: optSampleSize;
			final int offset        = RANDOM.nextInt(cacheSize - sampleSize - 1);
			smallestEntry = cache.iterate(new RangeMin<>(offset, sampleSize, COMPARATOR)).yield().value();
		}
		
		final double approximation =
			(double)(smallestEntry.timestamp - minEntry.timestamp)
				/ (double)(maxEntry.timestamp - minEntry.timestamp);
		
		// System.out.println("Val: " + Instant.ofEpochMilli(smallestEntry.timestamp) + ", approximation " +
		// approximation);
		
		return approximation;
	}
	
	static class Entry
	{
		final long   timestamp;
		final String value;
		
		Entry(final long timestamp, final String value)
		{
			super();
			
			this.timestamp = timestamp;
			this.value     = value;
		}
		
	}
	
	static class RangeMin<E> implements Aggregator<E, E>
	{
		private final long                  offset, length;
		private final Comparator<? super E> order;
		private long                        iterationOffset, iterationLength;
		private E                           iterationElement;
		
		RangeMin(final long offset, final long length, final Comparator<? super E> order)
		{
			super();
			
			this.offset = offset;
			this.length = length;
			this.order  = order ;
			
			this.reset();
		}
		
		@Override
		public final RangeMin<E> reset()
		{
			this.iterationElement = null;
			this.iterationOffset  = this.offset;
			this.iterationLength  = this.length;
			
			return this;
		}

		@Override
		public final void accept(final E element)
		{
			if(this.iterationOffset > 0)
			{
				this.iterationOffset--;
				return;
			}
			
			if(this.iterationLength <= 0)
			{
				throw X.BREAK();
			}
			
			if(this.iterationLength-- == this.length)
			{
				this.iterationElement = element;
				return;
			}
					
			if(this.order.compare(element, this.iterationElement) < 0)
			{
				this.iterationElement = element;
			}
		}
		
		@Override
		public final E yield()
		{
			return this.iterationElement;
		}
		
	}
	
}