package one.microstream.communication.binarydynamic;

import one.microstream.com.ComException;

/**
 * Thrown when a typeDefinition received from the remote host
 * contains a type that can't be resolved to an exiting class 
 * on the local system. 
 *
 */
public class ComExceptionRemoteClassNotFound extends ComException 
{
	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////
	
	/**
     * Constructs a <code>ComExceptionRemoteClassNotFound</code> with no detail message.
     */
	public ComExceptionRemoteClassNotFound(String typeName) {
		super("Class not found: " + typeName);
	}

}
