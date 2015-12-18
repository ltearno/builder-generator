package fr.lteconsulting;

public class Demonstration
{
	public static void main(String[] args)
	{
		ComplexClass instance = ComplexClassBuilder
				.withA("this one is mandatory")
				.withC("this one too")
				.withE("all this is generated !")
				.withD("an optional parameter")
				.build();
		System.out.println(instance.toString());
	}
}
