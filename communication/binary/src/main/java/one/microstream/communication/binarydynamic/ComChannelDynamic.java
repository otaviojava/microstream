package one.microstream.communication.binarydynamic;

import org.slf4j.Logger;

import one.microstream.communication.types.ComChannel;
import one.microstream.communication.types.ComProtocol;
import one.microstream.persistence.types.PersistenceManager;
import one.microstream.util.logging.Logging;

public abstract class ComChannelDynamic<C> implements ComChannel
{
	///////////////////////////////////////////////////////////////////////////
	// constants //
	//////////////
	
	private final static Logger logger = Logging.getLogger(Default.class);

	///////////////////////////////////////////////////////////////////////////
	// instance fields //
	////////////////////
	
	protected final PersistenceManager<?> persistenceManager;
	protected final C                     connection;
	protected final ComProtocol           protocol;
	protected final ComHandlerRegistry    handlers = new ComHandlerRegistry.Default();

	
	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////
	
	public ComChannelDynamic(
		final PersistenceManager<?> persistenceManager,
		final C                     connection,
		final ComProtocol           protocol)
	{
		this.connection         = connection;
		this.persistenceManager = persistenceManager;
		this.protocol           = protocol;
	}

	
	
	
	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////
	
	//Bypass Handlers to avoid recursion if called inside an handler ...
	public Object requestUnhandled(final Object object)
	{
		this.persistenceManager.store(object);
		return this.persistenceManager.get();
	}
	
	@Override
	public final void send(final Object graphRoot)
	{
		logger.trace("sending data");
		
		ComHandlerSend<?> handler = null;
		
		if(graphRoot != null)
		{
			handler = this.handlers.lookupSend(graphRoot.getClass());
		}
		
		if(handler != null )
		{
			handler.sendMessage(graphRoot);
		}
		else
		{
			this.persistenceManager.store(new ComMessageData(graphRoot));
		}
		
		logger.trace("sended data successfully");
	}

	@Override
	public final Object receive()
	{
		Object received = null;
		
		while(null == received)
		{
			logger.trace("waiting for data");
			received = this.persistenceManager.get();
			this.persistenceManager.objectRegistry().clear();
	
			final ComHandlerReceive<?> handler = this.handlers.lookupReceive(received.getClass());
			if(handler != null )
			{
				received = handler.processMessage(received);
			
				if(!handler.continueReceiving())
				{
					break;
				}
			}
		}
		
		logger.trace("data received successfully");
		
		return received;
	}

	@Override
	public final void close()
	{
		logger.trace("closing ComChannel");
		this.persistenceManager.close();
	}
}
