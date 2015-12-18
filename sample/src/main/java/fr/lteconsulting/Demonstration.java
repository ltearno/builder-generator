package fr.lteconsulting;

import fr.lteconsulting.ApiClass.Operation;
import fr.lteconsulting.builders.ComplexClassBuilder;

public class Demonstration
{
	public static void main( String[] args )
	{
		ComplexClass instance = ComplexClassBuilder
				.withA( "this one is mandatory" )
				.bonjour( "monsieur" )
				.withC( "this one too" )
				.withE( "all this is generated !" )
				.withD( "an optional parameter" )
				.build();

		ComplexClassBuilder.prepare().withA( null ).bonjour( null ).withC( null ).withE( null ).build();

		SomeMethodeCaller.prepare().withImportantNote( 52 ).call();

		GetValeurCaller.prepare( instance ).withX( 12 ).withToto( 'a' ).call();

		// TODO : maybe since withValue() is a terminal method here, we may directly return the built instance instead of calling build()

		Operation op = OperationBuilder
				.withLeft( ValueBuilder.withValue( 5 ).build() )
				.withOperation( "+" )
				.withRight( ValueBuilder.withValue( 5 ).build() )
				.build();
		System.out.println( op.toString() );
	}
}
