package one.microstream.test.legacy;

import one.microstream.chars.XChars;

public class SimpleClass
{
	int    first ;
	char   third ;
	float  second;
	Object fourth;
	String fifth ;

	public SimpleClass()
	{
		super();
	}
		
	public SimpleClass(final int first, final float second, final char third)
	{
		super();
		this.first  = first ;
		this.second = second;
		this.third  = third ;
	}
	
	@Override
	public String toString()
	{
		return XChars.systemString(this)
			+ ": first = " + this.first
			+ ", second = " + this.second
			+ ", third = " + this.third
			+ ", fourth = " + this.fourth
		;
	}
	
}