package fr.lteconsulting;

import fr.lteconsulting.Mandatory;
import fr.lteconsulting.UseBuilderGenerator;

public class MegaRelou
{
	private String a;

	private String b;

	private String c;

	private String d;

	private String e;

	@UseBuilderGenerator
	public MegaRelou(@Mandatory String a, String b, @Mandatory String c, String d, @Mandatory String e)
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
	}

	public String getA()
	{
		return a;
	}

	public void setA(String a)
	{
		this.a = a;
	}

	public String getB()
	{
		return b;
	}

	public void setB(String b)
	{
		this.b = b;
	}

	public String getC()
	{
		return c;
	}

	public void setC(String c)
	{
		this.c = c;
	}

	public String getD()
	{
		return d;
	}

	public void setD(String d)
	{
		this.d = d;
	}

	public String getE()
	{
		return e;
	}

	public void setE(String e)
	{
		this.e = e;
	}

}
