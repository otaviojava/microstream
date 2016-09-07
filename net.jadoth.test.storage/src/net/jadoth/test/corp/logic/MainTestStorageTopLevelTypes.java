package net.jadoth.test.corp.logic;

import java.util.Arrays;

import net.jadoth.reference.Reference;
import net.jadoth.storage.types.EmbeddedStorage;
import net.jadoth.storage.types.Storage;


public class MainTestStorageTopLevelTypes
{
	public static void main(final String[] args)
	{
		net.jadoth.reference.Reference<Object>               root         = Reference.New(null);

		net.jadoth.persistence.types.PersistenceRootResolver rootResolver = Storage.RootResolver(root);
		
		net.jadoth.storage.types.EmbeddedStorageManager      storage      = EmbeddedStorage
			.createStorageManager(rootResolver)
			.start()
		;
		
		net.jadoth.storage.types.StorageConnection           connection  = storage.createConnection();
		
		net.jadoth.persistence.types.Storer                  storer      = connection.createStorer();
				
		java.util.List<TestPerson> entityGraph = Arrays.asList(
			new TestPerson(),
			new TestPerson(),
			new TestPerson()
		);
		
		storer.storeRequired(entityGraph);
		storer.commit();
	}
	
}

class TestPerson
{
	java.lang.String                        firstName;
	java.lang.String                        lastName ;
	net.jadoth.swizzling.types.Lazy<String> hugeText ;
}


