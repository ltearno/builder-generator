package fr.lteconsulting;

import com.toto.SimpleBuilder;

public class Demonstration
{
	public static void main(String[] args)
	{
		ComplexClass instance = SimpleBuilder
				.withA("this one is mandatory")
				.bonjour("monsieur")
				.withC("this one too")
				.withE("all this is generated !")
				.withD("an optional parameter")
				.build();
		System.out.println(instance.toString());

		SimpleBuilder.prepare().withA(null).bonjour(null).withC(null).withE(null).build();

		PeteBurneBuilder.withA("khkjh").withB(null).call();

		ComplexMethodCaller.prepare().withImportantNote(52).call();
	}
}
