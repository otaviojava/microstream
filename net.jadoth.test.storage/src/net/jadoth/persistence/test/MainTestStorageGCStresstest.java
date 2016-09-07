package net.jadoth.persistence.test;

import net.jadoth.Jadoth;
import net.jadoth.concurrent.JadothThreads;
import net.jadoth.math.JadothMath;
import net.jadoth.storage.types.DEBUGStorage;
import net.jadoth.storage.types.Storage;
import net.jadoth.storage.types.StorageConnection;
import net.jadoth.storage.types.StorageDataFileEvaluator;
import net.jadoth.swizzling.types.Lazy;


/*
 * Storage GC Stresstest
 *
 * Creates a test graph
 * Stored it in several steps to create multiple files
 * Clear the
 *
 * Storage.HousekeepingController(10, 7_000_000)
 * Storage.FileDissolver(100, 10_000, 0.75)
 */
public class MainTestStorageGCStresstest extends TestStorage
{
	static final int  RUNS      = 10000;
	static final long WAIT_TIME = 400;

	static final StorageDataFileEvaluator fileEvaluatorHard = Storage.DataFileEvaluator(100, 10_000_000, 0.99999);

	@SuppressWarnings("unchecked")
	public static void main(final String[] args)
	{
		final Lazy<Object[]> ref;
		if(ROOT.get() == null)
		{
			ref = Lazy.Reference(testGraphEvenMoreManyType());
			ROOT.set(ref);
		}
		else
		{
			ref = (Lazy<Object[]>)ROOT.get();
			ref.get();
		}

		final int size = ref.get().length;

		final StorageConnection connection = STORAGE.createConnection();
		connection.storeRequired(ROOT);

//		storageCleanup(connection);

		for(int i = 0; i < RUNS; i++)
		{
//			if(Math.random() < 0.1)
//			{
//				DEBUGStorage.println("#### GC #### (#"+i+") @ " + System.currentTimeMillis());
//				storageCleanup(connection);
//			}

			connection.storeFull(ref.get()[JadothMath.random(size)]);
			DEBUGStorage.println("stored #"+i);

			ref.clear();
			JadothThreads.sleep(WAIT_TIME);
//			connection.issueFullCacheCheck((a, b, c) -> true);
//			connection.issueFullFileCheck(fileEvaluatorHard);
			final Object o = ref.get();
			DEBUGStorage.println("loaded: "+Jadoth.systemString(o));
		}
		exit();
	}

	public static void storageCleanup(final StorageConnection connection, final Double dissolveRatio)
	{
		DEBUGStorage.println("GC#1");
		connection.issueFullGarbageCollection();
		DEBUGStorage.println("GC#2");
		connection.issueFullGarbageCollection();

		DEBUGStorage.println("cache check");
		connection.issueFullCacheCheck();

		DEBUGStorage.println("file check");
		connection.issueFullFileCheck(
			Storage.DataFileEvaluator(100, 10_000, 0.99999)
		);
		DEBUGStorage.println("Done cleanup");
	}

}