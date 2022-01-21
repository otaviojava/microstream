package one.microstream.communication.binarydynamic;

public class ComHandlerSendMessageNewType implements ComHandlerSend<ComMessageNewType>
{
	///////////////////////////////////////////////////////////////////////////
	// instance fields //
	////////////////////
	
	private final ComChannelDynamic<?> comChannel;
	
	
	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////
	
	public ComHandlerSendMessageNewType(
		final ComChannelDynamic<?> channel
	)
	{
		super();
		this.comChannel = channel;
	}
	

	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////

	@Override
	public Void sendMessage(final ComMessageNewType message)
	{					
		this.comChannel.persistenceManager.store(message);		
		return null;
	}

	@Override
	public Object sendMessage(final Object messageObject)
	{
		final ComMessageNewType message = (ComMessageNewType)messageObject;
		return this.sendMessage(message);
	}
	
}
