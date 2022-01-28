package one.microstream.communication.binarydynamic;

import one.microstream.collections.types.XGettingSequence;
import one.microstream.equality.Equalator;
import one.microstream.persistence.binary.types.Binary;
import one.microstream.persistence.types.PersistenceTypeDefinition;
import one.microstream.persistence.types.PersistenceTypeDescriptionMember;
import one.microstream.persistence.types.PersistenceTypeHandler;
import one.microstream.persistence.types.PersistenceTypeHandlerEnsurer;
import one.microstream.persistence.types.PersistenceTypeHandlerManager;

public class ComHandlerReceiveMessageNewType implements ComHandlerReceive<ComMessageNewType>
{
	///////////////////////////////////////////////////////////////////////////
	// instance fields //
	////////////////////
	
	private final PersistenceTypeHandlerManager<Binary> typeHandlerManager;
	private final ComTypeDefinitionBuilder 				typeDefintionBuilder;
	private final ComChannelDynamic<?>			 		comChannel;
	private final PersistenceTypeHandlerEnsurer<Binary> typeHandlerEnsurer;
	
	
	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////
	
	public ComHandlerReceiveMessageNewType(
		final ComChannelDynamic<?>					comClientChannelDynamic,
		final PersistenceTypeHandlerManager<Binary> typeHandlerManager,
		final ComTypeDefinitionBuilder 				typeDefintionBuilder,
		final PersistenceTypeHandlerEnsurer<Binary> typeHandlerEnsurer
	)
	{
		super();
		this.comChannel 				= comClientChannelDynamic;
		this.typeHandlerManager   		= typeHandlerManager;
		this.typeDefintionBuilder		= typeDefintionBuilder;
		this.typeHandlerEnsurer         = typeHandlerEnsurer;
	}
	
	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////
	
	final Equalator<PersistenceTypeDescriptionMember> memberValidator = (m1, m2) ->
	{
		if(m1 == null || m2 == null)
		{
			return false;
		}

		if(m1.equalsStructure(m2))
		{
			return true;
		}

		return false;
	};
	
	@Override
	public Void processMessage(final ComMessageNewType message)
	{
		final String typeEntry = message.typeEntry();
		//XDebug.println("new type: " + typeEntry);
		
		final XGettingSequence<PersistenceTypeDefinition> defs = this.typeDefintionBuilder.buildTypeDefinitions(typeEntry);
		for (final PersistenceTypeDefinition ptd : defs)
		{
			if(ptd.type() != null) 
			{
				final PersistenceTypeHandler<Binary, ?> handler = this.typeHandlerManager.lookupTypeHandler(ptd.type());
				
				if(handler != null) 
				{
					//XDebug.println("handler found " + handler.typeId());
										
					if(PersistenceTypeDescriptionMember.equalMembers(ptd.allMembers(), handler.allMembers(), this.memberValidator))
					{
						//XDebug.println("Exsiting handler matches " + handler.typeId());
					}
					else
					{
						//XDebug.println("Creating legacy handler for exiting");
						this.typeHandlerManager.updateCurrentHighestTypeId(ptd.typeId());
						this.typeHandlerManager.ensureLegacyTypeHandler(ptd, handler);																							
					}				
				} 
				else 
				{
					//XDebug.println("no handler found");
										
					final PersistenceTypeHandler<Binary, ?> th = this.typeHandlerEnsurer.ensureTypeHandler(ptd.type());
									
					if(PersistenceTypeDescriptionMember.equalMembers(ptd.allMembers(), th.allMembers(), this.memberValidator))
					{
						//XDebug.println("Creating new handler for new type");
						this.typeHandlerManager.ensureTypeHandler(ptd.type());
					}
					else
					{
						//XDebug.println("Legacy handler for new type");
						this.typeHandlerManager.updateCurrentHighestTypeId(ptd.typeId());
						this.typeHandlerManager.ensureLegacyTypeHandler(ptd, th);																							
					}									
				}								
			} 
			else
			{
				throw new ComExceptionRemoteClassNotFound(ptd.typeName());
			}
		}
	
		return null;
	}
	
	@Override
	public Object processMessage(final Object messageObject)
	{
		final ComMessageNewType message = (ComMessageNewType)messageObject;
		return this.processMessage(message);
	}

	@Override
	public boolean continueReceiving()
	{
		return true;
	}
}
