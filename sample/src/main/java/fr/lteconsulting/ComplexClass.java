package fr.lteconsulting;

public class ComplexClass
{
	private String a;

	private String b;

	private String c;

	private String d;

	private String e;

	@UseBuilderGenerator(builderPackage = "fr.lteconsulting.builders")
	public ComplexClass(@Mandatory String a, @Parameter(mandatory = true, name = "bonjour") String b,
			@Mandatory String c, String d, @Mandatory String e)
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
	}

	@UseBuilderGenerator
	public static String someMethode(@Parameter(mandatory = true, name = "withImportantNote") int a, int b)
	{
		return null;
	}

	@UseBuilderGenerator
	public Integer getValeur(int p1, int c2, @Mandatory int x, int y, int z, char toto)
	{
		return 5;
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
