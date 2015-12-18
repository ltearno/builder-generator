package fr.lteconsulting;

public class PeteBurne
{
	private String a;

	private String b;

	private String c;

	private String d;

	private String e;

	private String f;

	@UseBuilderGenerator(finalMethodName = "call")
	public PeteBurne(@Mandatory String a, @Mandatory String b, String c, @Mandatory String d, String e, String f)
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;
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

	public String getF()
	{
		return f;
	}

	public void setF(String f)
	{
		this.f = f;
	}

}
