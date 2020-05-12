package one.microstream.afs;

public interface ACreator
{
	public ADirectory createDirectory(ADirectory parent, String identifier);
	
	public AFile createFile(ADirectory parent, String identifier);
	
	public AFile createFile(ADirectory parent, String name, String type);
	
	public AFile createFile(ADirectory parent, String identifier, String name, String type);
}