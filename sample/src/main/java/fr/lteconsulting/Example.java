package fr.lteconsulting;

import fr.lteconsulting.Mandatory;
import fr.lteconsulting.UseBuilderGenerator;

public class Example
{
	private final String a;

	private final String b;

	private String c;

	private String d;

	@UseBuilderGenerator
	public Example(@Mandatory String a, @Mandatory String b, String c, String d)
	{
		super();
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}

	@Override
	public String toString()
	{
		return "Example [a=" + a + ", b=" + b + ", c=" + c + ", d=" + d + "]";
	}
}
