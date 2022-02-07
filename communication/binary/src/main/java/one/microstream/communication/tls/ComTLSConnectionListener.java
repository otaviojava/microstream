package one.microstream.communication.tls;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLContext;

import one.microstream.communication.types.ComConnection;
import one.microstream.communication.types.ComConnectionListener;

public class ComTLSConnectionListener extends ComConnectionListener.Default
{
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
		return new ComTLSConnection(channel, this.sslContext, this.sslParameters, false);
	}
}
