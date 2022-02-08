package one.microstream.communication.tls;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;

import one.microstream.communication.types.ComConnection;
import one.microstream.communication.types.ComConnectionListener;
import one.microstream.util.logging.Logging;

public class ComTLSConnectionListener extends ComConnectionListener.Default
{
	///////////////////////////////////////////////////////////////////////////
	// constants //
	//////////////
	
	private final static Logger logger = Logging.getLogger(ComConnectionListener.class);


	///////////////////////////////////////////////////////////////////////////
	// instance fields //
	////////////////////
		
	private final SSLContext sslContext;
	private final TLSParametersProvider sslParameters;

	
	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////
	
	public ComTLSConnectionListener(
		final ServerSocketChannel serverSocketChannel,
		final SSLContext context,
		final TLSParametersProvider tlsParameterProvider)
	{
		super(serverSocketChannel);
		this.sslContext = context;
		this.sslParameters = tlsParameterProvider;
	}

	
	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////
	
	@Override
	public ComConnection createConnection(final SocketChannel channel)
	{
		final ComConnection connection = new ComTLSConnection(channel, this.sslContext, this.sslParameters, false);
		logger.debug("created new ComConnection {}", connection);
		return connection;
	}
}
