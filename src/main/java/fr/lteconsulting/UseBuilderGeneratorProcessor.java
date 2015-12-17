package fr.lteconsulting;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes( UseBuilderGeneratorProcessor.AnnotationFqn )
@SupportedSourceVersion( SourceVersion.RELEASE_8 )
public class UseBuilderGeneratorProcessor extends AbstractProcessor
{
	private final static String tab = "    ";
	public final static String AnnotationFqn = "fr.lteconsulting.UseBuilderGenerator";

	@Override
	public boolean process( Set<? extends TypeElement> annotations, RoundEnvironment roundEnv )
	{
		for( ExecutableElement element : ElementFilter.constructorsIn( roundEnv.getElementsAnnotatedWith( UseBuilderGenerator.class ) ) )
		{
			processConstructor( element );
		}

		roundEnv.errorRaised();

		return true;
	}

	String getPackageName( Element element )
	{
		while( element != null && !(element instanceof PackageElement) )
			element = element.getEnclosingElement();
		if( element == null )
			return null;
		return ((PackageElement) element).getQualifiedName().toString();
	}

	String getEnclosingTypeName( Element element )
	{
		while( element != null && !(element instanceof TypeElement) )
			element = element.getEnclosingElement();
		if( element == null )
			return null;
		return ((TypeElement) element).getSimpleName().toString();
	}

	static String capitalize( String value )
	{
		return value.substring( 0, 1 ).toUpperCase() + value.substring( 1 );
	}

	static class ParameterInfo
	{
		String parameterName;
		TypeMirror parameterType;
		Mandatory mandatoryAnnotation;
		String interfaceName;

		public ParameterInfo( String parameterName, TypeMirror parameterType, Mandatory mandatoryAnnotation )
		{
			this.parameterName = parameterName;
			this.parameterType = parameterType;
			this.mandatoryAnnotation = mandatoryAnnotation;
			this.interfaceName = "MandatoryParameter" + capitalize( parameterName );
		}
	}

	private void processConstructor( ExecutableElement element )
	{
		String packageName = getPackageName( element );
		String builderClassName = getEnclosingTypeName( element ) + "Builder";

		StringBuilder sb = new StringBuilder();
		sb.append( "package " + packageName + ";\r\n" );
		sb.append( "\r\n" );
		sb.append( "public class " + builderClassName + " {\r\n" );

		List<ParameterInfo> mandatoryParameters = new ArrayList<>();
		List<ParameterInfo> optionalParameters = new ArrayList<>();

		for( VariableElement parameter : element.getParameters() )
		{
			String parameterName = parameter.getSimpleName().toString();
			TypeMirror parameterType = parameter.asType();
			Mandatory mandatoryAnnotation = parameter.getAnnotation( Mandatory.class );

			ParameterInfo paramInfo = new ParameterInfo( parameterName, parameterType, mandatoryAnnotation );
			(mandatoryAnnotation != null ? mandatoryParameters : optionalParameters).add( paramInfo );
		}

		// Mandatory parameters interfaces
		for( int i = 0; i < mandatoryParameters.size(); i++ )
		{
			ParameterInfo paramInfo = mandatoryParameters.get( i );
			String nextInterfaceName = i < mandatoryParameters.size() - 1 ? mandatoryParameters.get( i + 1 ).interfaceName : "OptionalParameters";

			String capitalized = capitalize( paramInfo.parameterName );
			sb.append( tab + "public interface " + paramInfo.interfaceName + " {\r\n" );
			sb.append( tab + tab + nextInterfaceName + " with" + capitalized + "(" + paramInfo.parameterType + " " + paramInfo.parameterName + ");\r\n" );
			sb.append( tab + "}\r\n" );
			sb.append( "\r\n" );
		}

		// Optional parameters interface
		sb.append( tab + "public interface OptionalParameters {\r\n" );
		sb.append( tab + tab + getEnclosingTypeName( element ) + " build();\r\n" );
		for( ParameterInfo info : optionalParameters )
		{
			sb.append( tab + tab + "OptionalParameters with" + capitalize( info.parameterName ) + "(" + info.parameterType + " " + info.parameterName + ");\r\n" );
		}
		sb.append( tab + "}\r\n" );
		sb.append( "\r\n" );

		// Builder implementation
		sb.append( tab + "private static class BuilderInternal implements OptionalParameters" );
		for( ParameterInfo info : mandatoryParameters )
			sb.append( ", " + info.interfaceName );
		sb.append( " {\r\n" );
		for( VariableElement parameter : element.getParameters() )
		{
			sb.append( tab + tab + "private " + parameter.asType() + " " + parameter.getSimpleName().toString() + ";\r\n" );
		}
		sb.append( "\r\n" );

		sb.append( tab + tab + "@Override public " + getEnclosingTypeName( element ) + " build() {\r\n" );
		sb.append( tab + tab + tab + "return new " + getEnclosingTypeName( element ) + "(" );
		boolean first = true;
		for( VariableElement parameter : element.getParameters() )
		{
			if( !first )
				sb.append( ", " );
			else
				first = false;
			sb.append( parameter.getSimpleName().toString() );
		}
		sb.append( ");\r\n" );
		sb.append( tab + tab + "}\r\n" );
		sb.append( "\r\n" );

		for( int i = 0; i < mandatoryParameters.size(); i++ )
		{
			ParameterInfo paramInfo = mandatoryParameters.get( i );
			String nextInterfaceName = i < mandatoryParameters.size() - 1 ? mandatoryParameters.get( i + 1 ).interfaceName : "OptionalParameters";

			String capitalized = capitalize( paramInfo.parameterName );
			sb.append( tab + tab + "@Override public " + nextInterfaceName + " with" + capitalized + "(" + paramInfo.parameterType + " " + paramInfo.parameterName + ") {\r\n" );
			sb.append( tab + tab + tab + "this." + paramInfo.parameterName + " = " + paramInfo.parameterName + ";\r\n" );
			sb.append( tab + tab + tab + "return this;\r\n" );
			sb.append( tab + tab + "}\r\n" );
			sb.append( "\r\n" );
		}
		for( ParameterInfo info : optionalParameters )
		{
			sb.append( tab + tab + "@Override public OptionalParameters with" + capitalize( info.parameterName ) + "(" + info.parameterType + " " + info.parameterName + ") {\r\n" );
			sb.append( tab + tab + tab + "this." + info.parameterName + " = " + info.parameterName + ";\r\n" );
			sb.append( tab + tab + tab + "return this;\r\n" );
			sb.append( tab + tab + "}\r\n" );
			sb.append( "\r\n" );
		}

		sb.append( tab + "}\r\n" );
		sb.append( "\r\n" );

		// Bootstrap method
		if( !mandatoryParameters.isEmpty() )
		{
			ParameterInfo info = mandatoryParameters.get( 0 );
			String nextInterfaceName = mandatoryParameters.size() > 1 ? mandatoryParameters.get( mandatoryParameters.size() - 1 ).interfaceName : "OptionalParameters";

			String capitalized = capitalize( info.parameterName );
			sb.append( tab + "public static " + nextInterfaceName + " with" + capitalized + "(" + info.parameterType + " " + info.parameterName + ") {\r\n" );
			sb.append( tab + tab + "return new BuilderInternal().with" + capitalized + "(" + info.parameterName + ");\r\n" );
			sb.append( tab + "}\r\n" );
		}
		else
		{
			sb.append( tab + "public static OptionalParameters create() {\r\n" );
			sb.append( tab + tab + "return new BuilderInternal();\r\n" );
			sb.append( tab + "}\r\n" );
		}

		sb.append( "}\r\n" );

		try
		{
			JavaFileObject jfo = processingEnv.getFiler().createSourceFile( packageName + "." + builderClassName, element );

			OutputStream os = jfo.openOutputStream();
			PrintWriter pw = new PrintWriter( os );
			pw.print( sb.toString() );
			pw.close();
			os.close();

			processingEnv.getMessager().printMessage( Kind.NOTE, "AutoThreaded généré !", element );
		}
		catch( IOException e )
		{
			e.printStackTrace();
			processingEnv.getMessager().printMessage( Kind.ERROR, "AutoThreaded non généré !" + e, element );
		}
	}
}
