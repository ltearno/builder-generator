package fr.lteconsulting;

public class Demonstration
{
	public static void main(String[] args)
	{
		MegaRelou instance = MegaRelouBuilder.withA("lkjlkj").withC("kjhkjh").build();
		System.out.println(instance.toString());
	}
}
