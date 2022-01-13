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
	public Object processMessage(final ComMessageNewType message)
	{
		final String typeEntry = message.typeEntry();
		
		try
		{
			final XGettingSequence<PersistenceTypeDefinition> defs = this.typeDefintionBuilder.buildTypeDefinitions(typeEntry);
								
			for (final PersistenceTypeDefinition ptd : defs)
			{
				if(ptd.type() != null)
				{
					final PersistenceTypeHandler<Binary, ?> th = this.typeHandlerEnsurer.ensureTypeHandler(ptd.type());
									
					if(PersistenceTypeDescriptionMember.equalMembers(ptd.allMembers(), th.allMembers(), this.memberValidator))
					{
						this.typeHandlerManager.ensureTypeHandler(ptd.type());
					}
					else
					{
						this.typeHandlerManager.updateCurrentHighestTypeId(ptd.typeId());
						this.typeHandlerManager.ensureLegacyTypeHandler(ptd, th);
						this.typeHandlerManager.ensureTypeHandler(ptd.type());
														
						final ComMessageStatus answer = (ComMessageStatus)this.comChannel.requestUnhandled(new ComMessageNewType(this.typeHandlerManager.lookupTypeHandler(ptd.type())));
						
						if(answer.status() != true) 
						{
							throw new ComExceptionTypeMismatch(ptd.typeId(), ptd.type().getName());
						}
					}
				}
				else
				{
					throw new ComExceptionRemoteClassNotFound(ptd.typeName());
				}
			}
												
		}
		catch(final ComExceptionTypeMismatch e)
		{
			this.comChannel.send(new ComMessageClientTypeMismatch(e.getTypeId(), e.getType()));
			throw e;
		}
		
		this.comChannel.send(new ComMessageStatus(true));
		
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
