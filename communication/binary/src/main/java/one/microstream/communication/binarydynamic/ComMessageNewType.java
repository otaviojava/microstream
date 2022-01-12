package one.microstream.communication.binarydynamic;

import one.microstream.chars.VarString;
import one.microstream.persistence.types.PersistenceTypeDefinition;
import one.microstream.persistence.types.PersistenceTypeDictionaryAssembler;

public class ComMessageNewType implements ComMessage
{
	///////////////////////////////////////////////////////////////////////////
	// instance fields //
	////////////////////
	
	private String typeEntry;
	private transient PersistenceTypeDefinition typeDefinition;
	
	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////
	
	public ComMessageNewType(final PersistenceTypeDefinition typeDefinition)
	{
		this.typeDefinition = typeDefinition;
		this.typeEntry = "";
		final PersistenceTypeDictionaryAssembler assembler = PersistenceTypeDictionaryAssembler.New();
		
		final VarString vc = VarString.New();
		assembler.assembleTypeDescription(vc, typeDefinition);
		this.typeEntry = vc.toString();
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////
	
	public String typeEntry()
	{
		return this.typeEntry;
	}


	public PersistenceTypeDefinition typeDefinition() 
	{
		return this.typeDefinition;
	}
	
	
}
