package one.microstream.communication.binarydynamic;

import java.nio.ByteBuffer;

import one.microstream.chars.VarString;
import one.microstream.chars.XChars;
import one.microstream.collections.BulkList;
import one.microstream.collections.types.XGettingSequence;
import one.microstream.communication.types.ComConnection;
import one.microstream.memory.XMemory;
import one.microstream.persistence.binary.types.Binary;
import one.microstream.persistence.types.PersistenceTypeDefinition;
import one.microstream.persistence.types.PersistenceTypeDescription;
import one.microstream.persistence.types.PersistenceTypeDictionaryAssembler;
import one.microstream.persistence.types.PersistenceTypeDictionaryView;
import one.microstream.persistence.types.PersistenceTypeHandler;
import one.microstream.persistence.types.PersistenceTypeHandlerManager;

/**
 * This class handles the matching of types that have been modified on either the client or the host side
 * Including the necessary data transfer during the initialization of the ComChannels.
 *
 */
public class ComTypeMappingResolver 
{	
	///////////////////////////////////////////////////////////////////////////
	// constants //
	//////////////
	
	protected static final int LENGTH_CHAR_COUNT = 8;
	
	
	///////////////////////////////////////////////////////////////////////////
	// instance fields //
	////////////////////
	
	protected final PersistenceTypeDictionaryAssembler    typeDictionaryAssembler;
	protected final ComConnection                         connection;
	protected final PersistenceTypeDictionaryView         hostTypeDictionary;
	protected final PersistenceTypeHandlerManager<Binary> typeHandlerManager;
	protected final ComTypeDefinitionBuilder              typeDefinitionBuilder;
		
	
	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////
	
	/**
	 * Constructs a ComTypeMappingResolver instance
	 * 
	 * @param typeDictionaryAssembler PersistenceTypeDictionaryAssembler
	 * @param connection			  ComConnection
	 * @param hostTypeDictionary      PersistenceTypeDictionaryView
	 * @param typeHandlerManager      PersistenceTypeHandlerManager
	 * @param typeDefinitionBuilder   ComTypeDefinitionBuilder
	 */
	public ComTypeMappingResolver(
		final PersistenceTypeDictionaryAssembler    typeDictionaryAssembler,
		final ComConnection                         connection, 
		final PersistenceTypeDictionaryView         hostTypeDictionary,
		final PersistenceTypeHandlerManager<Binary> typeHandlerManager,
		final ComTypeDefinitionBuilder              typeDefinitionBuilder
	) 
	{
		super();
		this.typeDictionaryAssembler = typeDictionaryAssembler;
		this.connection              = connection;
		this.hostTypeDictionary      = hostTypeDictionary;
		this.typeHandlerManager      = typeHandlerManager;
		this.typeDefinitionBuilder   = typeDefinitionBuilder;
	}

	
	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////
	
	/**
	 * Handle the client's side of the communication type mapping during connection initialization phase.
	 * This is collection all type definition that belong to the clients classes that needs to be mapped by the host
	 * and transferring those to the host. 
	 */
	public void resolveClient()
	{						
		this.sendNewTypeDefintionsToHost(
			this.assembleTypeDefinitions(
				this.findHostTypeDefinitions()));
	}
	
	/**
	 * Handle the host's side of the communication type mapping during connection initialization phase.
	 * This is receiving the client's type definitions and creating the required legacy type handlers.  
	 */
	public void resolveHost() 
	{
		this.applyHostTypeMapping(
			this.parseClientTypeDefinitions(
				this.receiveUpdatedDefintionsfromClient()));
	}
	
	private void sendNewTypeDefintionsToHost(final byte[] assembledTypeDefinitions) 
	{
		final ByteBuffer dbb = XMemory.allocateDirectNative(assembledTypeDefinitions.length);
		final long dbbAddress = XMemory.getDirectByteBufferAddress(dbb);
		XMemory.copyArrayToAddress(assembledTypeDefinitions, dbbAddress);
		
		this.connection.writeCompletely(dbb);	
	}

	private byte[] assembleTypeDefinitions(final BulkList<PersistenceTypeDescription> newDefinitions) 
	{
		final VarString vs = VarString.New(10_000);		
		
		vs
		.reset()
		.repeat(LENGTH_CHAR_COUNT, '0');
		
		newDefinitions.forEach(definition -> {
			vs.add(this.assembleTypeDefintion(definition));
		});
		
		final char[] lengthString = XChars.readChars(XChars.String(vs.length()));
		vs.setChars(LENGTH_CHAR_COUNT - lengthString.length, lengthString);
		
		return vs.encode();
	}

	private VarString assembleTypeDefintion(final PersistenceTypeDescription definition) 
	{
		final VarString vc = VarString.New();
		this.typeDictionaryAssembler.assembleTypeDescription(vc, definition);
		return vc;
	}

	private BulkList<PersistenceTypeDescription> findHostTypeDefinitions()
	{
		final BulkList<PersistenceTypeDescription> newTypeDescriptions = BulkList.New();
		
		this.typeHandlerManager.iterateLegacyTypeHandlers(legacyTypeHandler -> {					
			final PersistenceTypeHandler<Binary, ?> currentHandler = this.typeHandlerManager.lookupTypeHandler(legacyTypeHandler.type());
			if(this.hostTypeDictionary.lookupTypeById(currentHandler.typeId()) == null)
			{
				newTypeDescriptions.add(currentHandler);
			}
		});
		
		return newTypeDescriptions;
	}
	
	private XGettingSequence<PersistenceTypeDefinition> parseClientTypeDefinitions(final ByteBuffer buffer) 
	{
		buffer.position(1);
		final char[] typeDefinitionsChars = XChars.standardCharset().decode(buffer).array();
		
		final String typeDefintions = XChars.String(typeDefinitionsChars);
		return this.typeDefinitionBuilder.buildTypeDefinitions(typeDefintions);	
	}

	private ByteBuffer receiveUpdatedDefintionsfromClient() 
	{
		final ByteBuffer lengthBuffer = XMemory.allocateDirectNative(LENGTH_CHAR_COUNT);
		this.connection.read(lengthBuffer, LENGTH_CHAR_COUNT);
		
		lengthBuffer.position(0);
		final String lengthDigits = XChars.standardCharset().decode(lengthBuffer).toString();
		final int    length       = Integer.parseInt(lengthDigits);
		
		final ByteBuffer typeDefinitionsBuffer = XMemory.allocateDirectNative(length - LENGTH_CHAR_COUNT);
		this.connection.read(typeDefinitionsBuffer, length - LENGTH_CHAR_COUNT);
		
		return typeDefinitionsBuffer;
	}
	
	private void applyHostTypeMapping(final XGettingSequence<PersistenceTypeDefinition> typeDefinitions)
	{
		typeDefinitions.forEach( typeDefinition -> {		
			final PersistenceTypeHandler<Binary, ?> currentHandler = this.typeHandlerManager.lookupTypeHandler(typeDefinition.type());	
			this.typeHandlerManager.ensureLegacyTypeHandler(typeDefinition, currentHandler);	
			this.typeHandlerManager.updateCurrentHighestTypeId(typeDefinition.typeId());
		});
	}
}
