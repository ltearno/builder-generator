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

/**
 * Generates a Builder class for constructors annotated with {@link UseBuilderGenerator}.
 * 
 * <p>
 * The generated builder supports mandatory parameters, which should be annotated with {@link Mandatory}.
 * 
 * <p>
 * The generated builders conform to the pattern described here : .
 * 
 * @author Arnaud Tournier www.lteconsulting.fr github.com/ltearno @ltearno
 *
 */
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

	private void processConstructor( ExecutableElement element )
	{
		// prepare lists of mandatory and optional parameters
		List<ParameterInformation> mandatoryParameters = new ArrayList<>();
		List<ParameterInformation> optionalParameters = new ArrayList<>();

		// split the constructor parameters into mandatory and optional
		analyzeParametersAndFeedLists( element, mandatoryParameters, optionalParameters );

		// prepare and do code generation
		String packageName = getPackageName( element );
		String builderClassName = getEnclosingTypeName( element ) + "Builder";
		String builderClassFqn = packageName + "." + builderClassName;

		StringBuilder sb = new StringBuilder();

		generateBuilderClassCode( element, packageName, builderClassName, mandatoryParameters, optionalParameters, sb );

		saveBuilderClass( element, builderClassFqn, sb );
	}

	private void analyzeParametersAndFeedLists( ExecutableElement element, List<ParameterInformation> mandatoryParameters, List<ParameterInformation> optionalParameters )
	{
		for( VariableElement parameter : element.getParameters() )
		{
			String parameterName = parameter.getSimpleName().toString();
			TypeMirror parameterType = parameter.asType();
			Mandatory mandatoryAnnotation = parameter.getAnnotation( Mandatory.class );

			ParameterInformation paramInfo = new ParameterInformation( parameterName, parameterType );
			(mandatoryAnnotation != null ? mandatoryParameters : optionalParameters).add( paramInfo );
		}
	}

	private void generateBuilderClassCode( ExecutableElement element, String packageName, String builderClassName, List<ParameterInformation> mandatoryParameters,
			List<ParameterInformation> optionalParameters, StringBuilder sb )
	{
		sb.append( "package " + packageName + ";\r\n" );
		sb.append( "\r\n" );
		sb.append( "public class " + builderClassName + " {\r\n" );

		generateMandatoryParametersInterfaces( mandatoryParameters, sb );
		generateOptionalParametersInterface( element, optionalParameters, sb );
		generateBuilderImplementation( element, mandatoryParameters, optionalParameters, sb );
		generateBootstrapMethod( mandatoryParameters, sb );

		sb.append( "}\r\n" );
	}

	private void generateMandatoryParametersInterfaces( List<ParameterInformation> mandatoryParameters, StringBuilder sb )
	{
		for( int i = 0; i < mandatoryParameters.size(); i++ )
		{
			ParameterInformation paramInfo = mandatoryParameters.get( i );
			String nextInterfaceName = i < mandatoryParameters.size() - 1 ? mandatoryParameters.get( i + 1 ).interfaceName : "OptionalParameters";

			String capitalized = capitalize( paramInfo.parameterName );
			sb.append( tab + "public interface " + paramInfo.interfaceName + " {\r\n" );
			sb.append( tab + tab + nextInterfaceName + " with" + capitalized + "(" + paramInfo.parameterType + " " + paramInfo.parameterName + ");\r\n" );
			sb.append( tab + "}\r\n" );
			sb.append( "\r\n" );
		}
	}

	private void generateOptionalParametersInterface( ExecutableElement element, List<ParameterInformation> optionalParameters, StringBuilder sb )
	{
		sb.append( tab + "public interface OptionalParameters {\r\n" );
		sb.append( tab + tab + getEnclosingTypeName( element ) + " build();\r\n" );
		for( ParameterInformation info : optionalParameters )
		{
			sb.append( tab + tab + "OptionalParameters with" + capitalize( info.parameterName ) + "(" + info.parameterType + " " + info.parameterName + ");\r\n" );
		}
		sb.append( tab + "}\r\n" );
		sb.append( "\r\n" );
	}

	private void generateBuilderImplementation( ExecutableElement element, List<ParameterInformation> mandatoryParameters, List<ParameterInformation> optionalParameters,
			StringBuilder sb )
	{
		sb.append( tab + "private static class BuilderInternal implements OptionalParameters" );
		for( ParameterInformation info : mandatoryParameters )
			sb.append( ", " + info.interfaceName );
		sb.append( " {\r\n" );

		generatePrivateFields( element, sb );
		generateBuildMethod( element, sb );
		generateMandatorySetters( mandatoryParameters, sb );
		generateOptionalSetters( optionalParameters, sb );

		sb.append( tab + "}\r\n" );
		sb.append( "\r\n" );
	}

	private void generatePrivateFields( ExecutableElement element, StringBuilder sb )
	{
		for( VariableElement parameter : element.getParameters() )
		{
			sb.append( tab + tab + "private " + parameter.asType() + " " + parameter.getSimpleName().toString() + ";\r\n" );
		}
		sb.append( "\r\n" );
	}

	private void generateBuildMethod( ExecutableElement element, StringBuilder sb )
	{
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
	}

	private void generateMandatorySetters( List<ParameterInformation> mandatoryParameters, StringBuilder sb )
	{
		for( int i = 0; i < mandatoryParameters.size(); i++ )
		{
			ParameterInformation paramInfo = mandatoryParameters.get( i );
			String nextInterfaceName = i < mandatoryParameters.size() - 1 ? mandatoryParameters.get( i + 1 ).interfaceName : "OptionalParameters";

			String capitalized = capitalize( paramInfo.parameterName );
			sb.append( tab + tab + "@Override public " + nextInterfaceName + " with" + capitalized + "(" + paramInfo.parameterType + " " + paramInfo.parameterName + ") {\r\n" );
			sb.append( tab + tab + tab + "this." + paramInfo.parameterName + " = " + paramInfo.parameterName + ";\r\n" );
			sb.append( tab + tab + tab + "return this;\r\n" );
			sb.append( tab + tab + "}\r\n" );
			sb.append( "\r\n" );
		}
	}

	private void generateOptionalSetters( List<ParameterInformation> optionalParameters, StringBuilder sb )
	{
		for( ParameterInformation info : optionalParameters )
		{
			sb.append( tab + tab + "@Override public OptionalParameters with" + capitalize( info.parameterName ) + "(" + info.parameterType + " " + info.parameterName + ") {\r\n" );
			sb.append( tab + tab + tab + "this." + info.parameterName + " = " + info.parameterName + ";\r\n" );
			sb.append( tab + tab + tab + "return this;\r\n" );
			sb.append( tab + tab + "}\r\n" );
			sb.append( "\r\n" );
		}
	}

	private void generateBootstrapMethod( List<ParameterInformation> mandatoryParameters, StringBuilder sb )
	{
		if( !mandatoryParameters.isEmpty() )
		{
			ParameterInformation info = mandatoryParameters.get( 0 );
			String nextInterfaceName = mandatoryParameters.size() > 1 ? mandatoryParameters.get( 1 ).interfaceName : "OptionalParameters";

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
	}

	private void saveBuilderClass( ExecutableElement element, String builderClassFqn, StringBuilder sb )
	{
		try
		{
			JavaFileObject jfo = processingEnv.getFiler().createSourceFile( builderClassFqn, element );

			OutputStream os = jfo.openOutputStream();
			PrintWriter pw = new PrintWriter( os );
			pw.print( sb.toString() );
			pw.close();
			os.close();

			processingEnv.getMessager().printMessage( Kind.NOTE, "Builder generated for this constructor: " + builderClassFqn, element );
		}
		catch( IOException e )
		{
			e.printStackTrace();
			processingEnv.getMessager().printMessage( Kind.ERROR, "Error generating builder, a builder may already exist (" + builderClassFqn + ") !" + e, element );
		}
	}

	private static class ParameterInformation
	{
		String parameterName;
		TypeMirror parameterType;
		String interfaceName;

		public ParameterInformation( String parameterName, TypeMirror parameterType )
		{
			this.parameterName = parameterName;
			this.parameterType = parameterType;
			this.interfaceName = "MandatoryParameter" + capitalize( parameterName );
		}
	}

	private static String getPackageName( Element element )
	{
		while( element != null && !(element instanceof PackageElement) )
			element = element.getEnclosingElement();
		if( element == null )
			return null;
		return ((PackageElement) element).getQualifiedName().toString();
	}

	private static String getEnclosingTypeName( Element element )
	{
		while( element != null && !(element instanceof TypeElement) )
			element = element.getEnclosingElement();
		if( element == null )
			return null;
		return ((TypeElement) element).getSimpleName().toString();
	}

	private static String capitalize( String value )
	{
		return value.substring( 0, 1 ).toUpperCase() + value.substring( 1 );
	}
}