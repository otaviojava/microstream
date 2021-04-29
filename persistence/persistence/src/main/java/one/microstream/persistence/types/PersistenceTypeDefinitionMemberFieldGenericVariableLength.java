package one.microstream.persistence.types;

import static one.microstream.X.mayNull;
import static one.microstream.X.notNull;
import static one.microstream.math.XMath.positive;

public interface PersistenceTypeDefinitionMemberFieldGenericVariableLength
extends PersistenceTypeDefinitionMemberFieldGeneric, PersistenceTypeDescriptionMemberFieldGenericVariableLength
{
	@Override
	public default PersistenceTypeDefinitionMemberFieldGenericVariableLength copyForName(final String name)
	{
		return this.copyForName(this.qualifier(), name);
	}
	
	@Override
	public PersistenceTypeDefinitionMemberFieldGenericVariableLength copyForName(String qualifier, String name);
	
	

	public static PersistenceTypeDefinitionMemberFieldGenericVariableLength.Default New(
		final PersistenceTypeDescriptionMemberFieldGenericVariableLength description
	)
	{
		return PersistenceTypeDefinitionMemberFieldGenericVariableLength.New(
			description.typeName()               ,
			description.name()                   ,
			description.persistentMinimumLength(),
			description.persistentMaximumLength()
		);
	}
	
	public static PersistenceTypeDefinitionMemberFieldGenericVariableLength.Default New(
		final String typeName               ,
		final String name                   ,
		final long   persistentMinimumLength,
		final long   persistentMaximumLength
	)
	{
		return New(typeName, null, name, persistentMinimumLength, persistentMaximumLength);
	}
	
	public static PersistenceTypeDefinitionMemberFieldGenericVariableLength.Default New(
		final String typeName               ,
		final String qualifier              ,
		final String name                   ,
		final long   persistentMinimumLength,
		final long   persistentMaximumLength
	)
	{
		return new PersistenceTypeDefinitionMemberFieldGenericVariableLength.Default(
			 notNull(typeName)               ,
			 mayNull(qualifier)              ,
			 notNull(name)                   ,
			         false                   ,
			positive(persistentMinimumLength),
			positive(persistentMaximumLength)
		);
	}

	public static PersistenceTypeDefinitionMemberFieldGenericVariableLength.Default Bytes(
		final String name                   ,
		final long   persistentMinimumLength,
		final long   persistentMaximumLength
	)
	{
		return Bytes(
			null                   ,
			name                   ,
			persistentMinimumLength,
			persistentMaximumLength
		);
	}
	
	public static PersistenceTypeDefinitionMemberFieldGenericVariableLength.Default Bytes(
		final String qualifier              ,
		final String name                   ,
		final long   persistentMinimumLength,
		final long   persistentMaximumLength
	)
	{
		return New(
			PersistenceTypeDictionary.Symbols.TYPE_BYTES,
			qualifier,
			name,
			persistentMinimumLength,
			persistentMaximumLength
		);
	}

	public static PersistenceTypeDefinitionMemberFieldGenericVariableLength.Default Chars(
		final String name                   ,
		final long   persistentMinimumLength,
		final long   persistentMaximumLength
	)
	{
		return Chars(null, name, persistentMinimumLength, persistentMaximumLength);
	}
	
	public static PersistenceTypeDefinitionMemberFieldGenericVariableLength.Default Chars(
		final String qualifier              ,
		final String name                   ,
		final long   persistentMinimumLength,
		final long   persistentMaximumLength
	)
	{
		return New(
			PersistenceTypeDictionary.Symbols.TYPE_CHARS,
			qualifier              ,
			name                   ,
			persistentMinimumLength,
			persistentMaximumLength
		);
	}



	public class Default
	extends PersistenceTypeDescriptionMemberFieldGenericVariableLength.Default
	implements PersistenceTypeDefinitionMemberFieldGenericVariableLength
	{
		///////////////////////////////////////////////////////////////////////////
		// constructors //
		/////////////////

		Default(
			final String  typeName               ,
			final String  qualifier              ,
			final String  name                   ,
			final boolean hasReferences          ,
			final long    persistentMinimumLength,
			final long    persistentMaximumLength
		)
		{
			super(typeName, qualifier, name, hasReferences, persistentMinimumLength, persistentMaximumLength);
		}



		///////////////////////////////////////////////////////////////////////////
		// methods //
		////////////
				
		@Override
		public final Class<?> type()
		{
			return null;
		}

		@Override
		public PersistenceTypeDefinitionMemberFieldGenericVariableLength copyForName(
			final String qualifier,
			final String name
		)
		{
			return new PersistenceTypeDefinitionMemberFieldGenericVariableLength.Default(
				this.typeName(),
				qualifier,
				name,
				this.isReference(),
				this.persistentMinimumLength(),
				this.persistentMaximumLength()
			);
		}

	}

}