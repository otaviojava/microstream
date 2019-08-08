package one.microstream.test.corp.main;

import one.microstream.X;
import one.microstream.persistence.types.Persistence;
import one.microstream.reflect.XReflect;
import one.microstream.storage.types.EmbeddedStorage;
import one.microstream.storage.types.EmbeddedStorageManager;
import one.microstream.test.corp.logic.Test;
import one.microstream.test.corp.logic.TestImportExport;


public class MainTestStoreEnum
{


	public static void main(final String[] args)
	{
//		if(System.currentTimeMillis() > 0)
//		{
//			printEnums();
//			System.exit(0);
//		}

		// creates and starts an embedded storage manager with all-default-settings.
		final EmbeddedStorageManager storage = EmbeddedStorage
			.Foundation()
//			.setRefactoringMappingProvider(
//				Persistence.RefactoringMapping(new File("Refactorings.csv"))
//			)
			.start()
		;
		
		// object graph with root either loaded on startup from an existing DB or required to be generated.
		if(storage.root() == null)
		{
			// first execution enters here (database creation)

			Test.print("Model data required.");
			storage.setRoot(
				createGraph()
			);
			
			Test.print("Storing ...");
			storage.storeRoot();
			Test.print("Storing completed.");
			TestImportExport.testExport(storage, Test.provideTimestampedDirectory("testExport"));
		}
		else
		{
			// subsequent executions enter here (database reading)

			Test.print("Model data loaded.");
			Test.print("Root instance: " + storage.root());
			
			Test.print("Exporting data ...");
			TestImportExport.testExport(storage, Test.provideTimestampedDirectory("testExport"));
			Test.print("Data export completed.");
		}
		
		// no shutdown required, the storage concept is inherently crash-safe
		System.exit(0);
	}
		
		
	static Object[] createGraph()
	{
		return X.array(
			SimpleEnum.TypeA,
			SimpleEnum.TypeB,
			SimpleEnum.TypeC,
			CrazyEnum.ShouldWorkNormal,
			CrazyEnum.ShouldWorkSpecial,
			CrazyEnumSpecialState.SpecialState,
			CrazyEnumSpecialState.Normal2,
			CrazyEnumSpecialState.Normal3,
			StatefulEnum.Type1,
			StatefulEnum.Type2,
			StatefulEnum.Type3
		);
	}
		
	static void printEnums()
	{
		StatefulEnum.Type1.state();
		System.out.println(StatefulEnum.Type1);
		System.out.println(StatefulEnum.Type1.getClass());
		System.out.println(StatefulEnum.Type1.getDeclaringClass());
		
		CrazyEnumSpecialState.SpecialState.crazyState();
		System.out.println(CrazyEnumSpecialState.SpecialState);
		System.out.println(CrazyEnumSpecialState.SpecialState.getClass());
		
		final Class<?> c = CrazyEnumSpecialState.SpecialState.getDeclaringClass();
		System.out.println(c);
		System.out.println(XReflect.isEnum(CrazyEnumSpecialState.SpecialState.getClass()));

		printMappedName(CrazyEnum.ShouldWorkSpecial.getClass());
		printMappedName(CrazyEnum.ShouldWorkNormal.getClass());
	}
	
	static void printMappedName(final Class<?> type)
	{
		final String persistentTypeName = Persistence.derivePersistentTypeName(type);
		final Class<?> reresolvedClass  = Persistence.resolveType(persistentTypeName);
		System.out.println(type.getName() + " -> " + persistentTypeName + " -> " + reresolvedClass.getName());
	}
	
}

enum SimpleEnum
{
	TypeA,
	TypeB,
	TypeC;
}

enum StatefulEnum
{
	Type1(100),
	Type2(200),
	Type3(300);
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// instance fields //
	////////////////////
	
	private int state;
	
	
	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////

	private StatefulEnum(final int state)
	{
		this.state = state;
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////
	
	public int state()
	{
		return this.state;
	}
	
	@Override
	public String toString()
	{
		return this.name() + "-" + this.state;
	}
	
}

enum CrazyEnum
{
	ShouldWorkNormal(0),
	ShouldWorkSpecial(1)
	{
		transient short mustBeDiscarded;
		
		@Override
		public long handleableState()
		{
			this.mustBeDiscarded++;
			return super.handleableState();
		}
		
		@Override
		public String toString()
		{
			return super.toString() + " (state queried " + this.mustBeDiscarded + " times)";
		}
	};
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// instance fields //
	////////////////////
	
	private long handleableState;
		
	
	
	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////

	private CrazyEnum(final long handleableState)
	{
		this.handleableState = handleableState;
	}

	
	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////

	public long handleableState()
	{
		return this.handleableState;
	}
	
	@Override
	public String toString()
	{
		return this.name() + "-" + this.handleableState;
	}
	
}

enum CrazyEnumSpecialState
{
	SpecialState(1111)
	{
		long crazySpecialHelperState;
		
		@Override
		public long crazyState()
		{
			this.crazySpecialHelperState++;
			return super.crazyState();
		}
		
		@Override
		public String toString()
		{
			return super.toString() + " (state queried " + this.crazySpecialHelperState + " times)";
		}
	},
	Normal2(2222),
	Normal3(3333);
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// instance fields //
	////////////////////
	
	private long crazyState;
		
	
	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////

	private CrazyEnumSpecialState(final long crazyState)
	{
		this.crazyState = crazyState;
	}

	
	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////

	public long crazyState()
	{
		return this.crazyState;
	}
	
	@Override
	public String toString()
	{
		return this.name() + "-" + this.crazyState;
	}
	
}