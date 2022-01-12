package one.microstream.communication.typemapping.test;

import java.nio.ByteOrder;

import one.microstream.communication.binarydynamic.ComBinaryDynamic;
import one.microstream.communication.types.ComHost;
import one.microstream.meta.XDebug;

public class MainTestHost {

	public static void main(final String[] args)
	{
		final ComHost<?> host = ComBinaryDynamic.Foundation()
			.setHostByteOrder(ByteOrder.BIG_ENDIAN)
			.setHostChannelAcceptor(hostChannel ->
			{
				hostChannel.send(new ModificationTestClass());
				
				final Object o = hostChannel.receive();
				
				
				XDebug.println("HOST RECEIVED: " + o.toString());
				
				hostChannel.send("Goodby");
			})
			.setInactivityTimeout(000)
			.createHost()
		;

		// run the host, making it constantly listen for new connections and relaying them to the logic
		host.run();
	}
	
}
