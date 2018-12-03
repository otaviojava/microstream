package com.mysql.dev.sakila;

import java.io.File;
import java.util.ArrayList;

import net.jadoth.reference.Reference;
import net.jadoth.storage.types.EmbeddedStorage;
import net.jadoth.storage.types.EmbeddedStorageManager;
import net.jadoth.storage.types.Storage;
import net.jadoth.test.corp.model.ClientCorporation;

public class MainTestStoreCustomers
{
	/**
	 * Since the Jetstream storing performance considerably depends on the disk speed, it is important to use
	 * an SSD to get significant results.
	 */
	static final File DIRECTORY = new File("C:/" + MainTestStoreCustomers.class.getSimpleName());
	
	/**
	 * Using multiple channels (a combination of a thread with an exclusive storage directory) is significant for
	 * any question concerning performance.
	 */
	static final int CHANNEL_COUNT = 4;

	static final Reference<ClientCorporation> ROOT = Reference.New(null);

	// create a storage manager, link the root, start the "embedded" database
	static final EmbeddedStorageManager STORAGE = EmbeddedStorage
		.Foundation(
			DIRECTORY                                        , // location for the database files
			Storage.ChannelCountProvider(CHANNEL_COUNT)      , // amount of storage channels (parallel database threads)
			Storage.HousekeepingController(1000, 10_000_000) , // housekeeping time config (file cleanup, cache checks, etc.)
			Storage.DataFileEvaluator()                      , // evalutator for dissolving old files
			Storage.EntityCacheEvaluatorCustomTimeout(10_000)  // evalutator for unloading entities from the cache
		)
		.start()
	;
	// (03.12.2018 TM)FIXME: set root

	private static final int RUNS  = 1000;
	private static final int COUNT = 1_000_000;
	
	
	public static void main(final String[] args)
	{
		final ArrayList<Customer> customers = generateEntities();
		
		for(int r = 1; r <= RUNS; r++)
		{
			long tStart, tStop;
			resetTest();
			tStart = System.nanoTime();
			// (03.12.2018 TM)FIXME: store root
			tStop = System.nanoTime();
			System.out.println(
				"#" + r
				+ " Elapsed Time: " + new java.text.DecimalFormat("00,000,000,000").format(tStop - tStart)
				+ " (" + (tStop - tStart) / COUNT + " per instance)"
			);
			System.gc();
		}
	}
	
	static ArrayList<Customer> generateEntities()
	{
		// (03.12.2018 TM)FIXME: generate customer instances
		throw new net.jadoth.meta.NotImplementedYetError();
	}
	
	static void resetTest()
	{
		// the central object registry holding the internal object<->objectId associations must be reset
		STORAGE.persistenceManager().objectRegistry().truncate();
		System.gc();
	}
	

}