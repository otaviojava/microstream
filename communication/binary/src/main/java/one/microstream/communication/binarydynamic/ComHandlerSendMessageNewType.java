package one.microstream.communication.binarydynamic;

import one.microstream.collections.types.XGettingSequence;
import one.microstream.equality.Equalator;
import one.microstream.meta.XDebug;
import one.microstream.persistence.binary.types.Binary;
import one.microstream.persistence.types.PersistenceTypeDefinition;
import one.microstream.persistence.types.PersistenceTypeDescriptionMember;
import one.microstream.persistence.types.PersistenceTypeHandler;
import one.microstream.persistence.types.PersistenceTypeHandlerEnsurer;
import one.microstream.persistence.types.PersistenceTypeHandlerManager;

public class ComHandlerSendMessageNewType implements ComHandlerSend<ComMessageNewType>
{
	///////////////////////////////////////////////////////////////////////////
	// instance fields //
	////////////////////
	
	private final ComChannelDynamic<?>                  comChannel;
	private final PersistenceTypeHandlerManager<Binary> typeHandlerManager;
	private final ComTypeDefinitionBuilder 				typeDefintionBuilder;
	private final PersistenceTypeHandlerEnsurer<Binary> typeHandlerEnsurer;
	
	
	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////
	
	public ComHandlerSendMessageNewType(
		final ComChannelDynamic<?> channel,
		final PersistenceTypeHandlerManager<Binary> typeHandlerManager,
		final ComTypeDefinitionBuilder 				typeDefintionBuilder,
		final PersistenceTypeHandlerEnsurer<Binary> typeHandlerEnsurer
	)
	{
		super();
		this.comChannel           = channel;
		this.typeHandlerManager   = typeHandlerManager;
		this.typeDefintionBuilder = typeDefintionBuilder;
		this.typeHandlerEnsurer   = typeHandlerEnsurer;
	}
	

	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////
		
	@Override
	public Void sendMessage(final ComMessageNewType message)
	{
		//final ComMessageStatus answer = (ComMessageStatus)this.comChannel.requestUnhandled(message);
		
		final Object answer = this.comChannel.requestUnhandled(message);
				
		if(answer instanceof ComMessageClientTypeMismatch)
		{
			throw new ComExceptionTypeMismatch(
				((ComMessageClientTypeMismatch) answer).getTypeId(),
				((ComMessageClientTypeMismatch) answer).getType()
			);
		}
		
		if(answer instanceof ComMessageNewType) 
		{
			final ComMessageNewType typedAnswer = (ComMessageNewType) answer;
			
			XDebug.println("answer is new type (legacy type at client side): \n" + typedAnswer.typeEntry());
			
			final boolean result = this.registerLegacyHandler(typedAnswer.typeEntry(), message.typeDefinition());			
			
			this.comChannel.requestUnhandled( new ComMessageStatus(result));
		}
						
		else 
		{
			XDebug.println("answer: " + answer.toString());
		}
				
		return null;
	}
	
	@Override
	public Object sendMessage(final Object messageObject)
	{
		final ComMessageNewType message = (ComMessageNewType)messageObject;
		return this.sendMessage(message);
	}
		
	
	private final Equalator<PersistenceTypeDescriptionMember> memberValidator = (m1, m2) ->
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
	
	private boolean registerLegacyHandler(final String typeEntry, final PersistenceTypeDefinition persistenceTypeDefinition) 
	{
		try
		{
			final XGettingSequence<PersistenceTypeDefinition> defs = this.typeDefintionBuilder.buildTypeDefinitions(typeEntry);
								
			for (final PersistenceTypeDefinition ptd : defs)
			{
				//just create a typehandler for the current class version; no registration and so...
				final PersistenceTypeHandler<Binary, ?> th = this.typeHandlerEnsurer.ensureTypeHandler(ptd.type());							
								
				if(PersistenceTypeDescriptionMember.equalMembers(ptd.allMembers(), th.allMembers(), this.memberValidator))
				{
					this.typeHandlerManager.ensureTypeHandler(ptd.type());
				}
				else
				{				
					@SuppressWarnings("unchecked")
					final PersistenceTypeHandler<Binary, ?> current = (PersistenceTypeHandler<Binary, ?>) persistenceTypeDefinition;				
					this.typeHandlerManager.ensureLegacyTypeHandler(ptd, current);		
					this.typeHandlerManager.updateCurrentHighestTypeId(ptd.typeId());
				}
				
				System.out.println("typehandlers created");
			}
			
		}
		catch(final ComExceptionTypeMismatch e)
		{
			this.comChannel.send(new ComMessageClientTypeMismatch(e.getTypeId(), e.getType()));
			throw e;
		}
				
		return true;
	}

}
