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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

/**
 * Generates a Builder class for constructors annotated with {@link UseBuilderGenerator}.
 * 
 * <p>
 * The generated builder supports mandatory parameters, which should be annotated with {@link Mandatory}.
 * 
 * <p>
 * For more detailed customization, use the {@link Parameter} annotation.
 * 
 * <p>
 * The generated builders conform to the pattern described here : http://www.jayway.com/2012/02/07/builder-pattern-with-a-twist/.
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
		for( Element e : roundEnv.getElementsAnnotatedWith( UseBuilderGenerator.class ) )
		{
			if( e.getKind() != ElementKind.CONSTRUCTOR && e.getKind() != ElementKind.METHOD )
				continue;
			processExecutableElement( (ExecutableElement) e );
		}

		roundEnv.errorRaised();

		return true;
	}

	private void processExecutableElement( ExecutableElement element )
	{
		// prepare lists of mandatory and optional parameters
		List<ParameterInformation> mandatoryParameters = new ArrayList<>();
		List<ParameterInformation> optionalParameters = new ArrayList<>();

		// split the constructor parameters into mandatory and optional
		analyzeParametersAndFeedLists( element, mandatoryParameters, optionalParameters );

		String returnTypeFqn;
		String finalCallText;
		if( element.getKind() == ElementKind.CONSTRUCTOR )
		{
			returnTypeFqn = getEnclosingTypeElement( element ).getQualifiedName().toString();
			finalCallText = "new " + getEnclosingTypeElement( element ).getQualifiedName().toString();
		}
		else if( element.getKind() == ElementKind.METHOD )
		{
			if( !element.getModifiers().contains( Modifier.STATIC ) )
			{
				processingEnv.getMessager().printMessage( Kind.ERROR, "This method should be static to be called by builder generator !", element );
				return;
			}

			returnTypeFqn = element.getReturnType().toString();
			finalCallText = getEnclosingTypeElement( element ).getQualifiedName() + "." + element.getSimpleName();
		}
		else
		{
			processingEnv.getMessager().printMessage( Kind.ERROR, "This element is not supported by builder generator !", element );
			return;
		}

		// prepare and do code generation
		UseBuilderGenerator useBuilderGeneratorAnnotation = element.getAnnotation( UseBuilderGenerator.class );
		String finalMethodName = useBuilderGeneratorAnnotation.finalMethodName();
		String packageName = getPackageName( element );
		if( !useBuilderGeneratorAnnotation.builderPackage().isEmpty() )
			packageName = useBuilderGeneratorAnnotation.builderPackage();
		String builderClassName = getEnclosingTypeElement( element ).getSimpleName().toString() + "Builder";
		if( !useBuilderGeneratorAnnotation.builderName().isEmpty() )
			builderClassName = useBuilderGeneratorAnnotation.builderName();
		String builderClassFqn = packageName + "." + builderClassName;

		GeneratorContext ctx = new GeneratorContext( element, packageName, builderClassName, finalMethodName, returnTypeFqn, finalCallText, mandatoryParameters, optionalParameters, builderClassFqn );
		StringBuilder sb = new StringBuilder();

		generateBuilderClassCode( ctx, sb );

		saveBuilderClass( ctx, sb );
	}

	private static class GeneratorContext
	{
		final ExecutableElement element;
		final String packageName;
		final String builderClassName;
		final String finalMethodName;
		final String returnTypeFqn;
		final String finalCallText;
		final List<ParameterInformation> mandatoryParameters;
		final List<ParameterInformation> optionalParameters;
		final String builderClassFqn;

		public GeneratorContext( ExecutableElement element, String packageName, String builderClassName, String finalMethodName, String returnTypeFqn, String finalCallText, List<ParameterInformation> mandatoryParameters, List<ParameterInformation> optionalParameters,
				String builderClassFqn )
		{
			this.element = element;
			this.packageName = packageName;
			this.builderClassName = builderClassName;
			this.finalMethodName = finalMethodName;
			this.returnTypeFqn = returnTypeFqn;
			this.finalCallText = finalCallText;
			this.mandatoryParameters = mandatoryParameters;
			this.optionalParameters = optionalParameters;
			this.builderClassFqn = builderClassFqn;
		}
	}

	private void analyzeParametersAndFeedLists( ExecutableElement element, List<ParameterInformation> mandatoryParameters, List<ParameterInformation> optionalParameters )
	{
		for( VariableElement parameter : element.getParameters() )
		{
			String parameterName = parameter.getSimpleName().toString();
			TypeMirror parameterType = parameter.asType();
			Mandatory mandatoryAnnotation = parameter.getAnnotation( Mandatory.class );
			Parameter parameterAnnotation = parameter.getAnnotation( Parameter.class );

			String setterName = "with" + capitalize( parameterName );
			if( parameterAnnotation != null && !parameterAnnotation.name().isEmpty() )
				setterName = parameterAnnotation.name();

			ParameterInformation paramInfo = new ParameterInformation( parameterName, parameterType, setterName );

			List<ParameterInformation> list = optionalParameters;
			if( mandatoryAnnotation != null || (parameterAnnotation != null && parameterAnnotation.mandatory()) )
				list = mandatoryParameters;

			list.add( paramInfo );
		}
	}

	private void generateBuilderClassCode( GeneratorContext ctx, StringBuilder sb )
	{
		sb.append( "package " + ctx.packageName + ";\r\n" );
		sb.append( "\r\n" );
		sb.append( "public class " + ctx.builderClassName + " {\r\n" );

		generateMandatoryParametersInterfaces( ctx, sb );
		generateOptionalParametersInterface( ctx, sb );
		generateBuilderImplementation( ctx, sb );
		generateBootstrapMethod( ctx, sb );

		sb.append( "}\r\n" );
	}

	private void generateMandatoryParametersInterfaces( GeneratorContext ctx, StringBuilder sb )
	{
		for( int i = 0; i < ctx.mandatoryParameters.size(); i++ )
		{
			ParameterInformation paramInfo = ctx.mandatoryParameters.get( i );
			String nextInterfaceName = i < ctx.mandatoryParameters.size() - 1 ? ctx.mandatoryParameters.get( i + 1 ).interfaceName : "OptionalParameters";

			sb.append( tab + "public interface " + paramInfo.interfaceName + " {\r\n" );
			sb.append( tab + tab + nextInterfaceName + " " + paramInfo.setterName + "(" + paramInfo.parameterType + " " + paramInfo.parameterName + ");\r\n" );
			sb.append( tab + "}\r\n" );
			sb.append( "\r\n" );
		}
	}

	private void generateOptionalParametersInterface( GeneratorContext ctx, StringBuilder sb )
	{
		sb.append( tab + "public interface OptionalParameters {\r\n" );
		sb.append( tab + tab + ctx.returnTypeFqn + " " + ctx.finalMethodName + "();\r\n" );
		for( ParameterInformation info : ctx.optionalParameters )
		{
			sb.append( tab + tab + "OptionalParameters " + info.setterName + "(" + info.parameterType + " " + info.parameterName + ");\r\n" );
		}
		sb.append( tab + "}\r\n" );
		sb.append( "\r\n" );
	}

	private void generateBuilderImplementation( GeneratorContext ctx, StringBuilder sb )
	{
		sb.append( tab + "private static class BuilderInternal implements OptionalParameters" );
		for( ParameterInformation info : ctx.mandatoryParameters )
			sb.append( ", " + info.interfaceName );
		sb.append( " {\r\n" );

		generatePrivateFields( ctx, sb );
		generateBuildMethod( ctx, sb );
		generateMandatorySetters( ctx, sb );
		generateOptionalSetters( ctx, sb );

		sb.append( tab + "}\r\n" );
		sb.append( "\r\n" );
	}

	private void generatePrivateFields( GeneratorContext ctx, StringBuilder sb )
	{
		for( VariableElement parameter : ctx.element.getParameters() )
		{
			sb.append( tab + tab + "private " + parameter.asType() + " " + parameter.getSimpleName().toString() + ";\r\n" );
		}
		sb.append( "\r\n" );
	}

	private void generateBuildMethod( GeneratorContext ctx, StringBuilder sb )
	{
		sb.append( tab + tab + "@Override public " + ctx.returnTypeFqn + " " + ctx.finalMethodName + "() {\r\n" );
		sb.append( tab + tab + tab );
		if( !"void".equals( ctx.returnTypeFqn ) )
			sb.append( "return " );
		sb.append( ctx.finalCallText + "(" );
		boolean first = true;
		for( VariableElement parameter : ctx.element.getParameters() )
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

	private void generateMandatorySetters( GeneratorContext ctx, StringBuilder sb )
	{
		for( int i = 0; i < ctx.mandatoryParameters.size(); i++ )
		{
			ParameterInformation paramInfo = ctx.mandatoryParameters.get( i );
			String nextInterfaceName = i < ctx.mandatoryParameters.size() - 1 ? ctx.mandatoryParameters.get( i + 1 ).interfaceName : "OptionalParameters";

			sb.append( tab + tab + "@Override public " + nextInterfaceName + " " + paramInfo.setterName + "(" + paramInfo.parameterType + " " + paramInfo.parameterName + ") {\r\n" );
			sb.append( tab + tab + tab + "this." + paramInfo.parameterName + " = " + paramInfo.parameterName + ";\r\n" );
			sb.append( tab + tab + tab + "return this;\r\n" );
			sb.append( tab + tab + "}\r\n" );
			sb.append( "\r\n" );
		}
	}

	private void generateOptionalSetters( GeneratorContext ctx, StringBuilder sb )
	{
		for( ParameterInformation info : ctx.optionalParameters )
		{
			sb.append( tab + tab + "@Override public OptionalParameters " + info.setterName + "(" + info.parameterType + " " + info.parameterName + ") {\r\n" );
			sb.append( tab + tab + tab + "this." + info.parameterName + " = " + info.parameterName + ";\r\n" );
			sb.append( tab + tab + tab + "return this;\r\n" );
			sb.append( tab + tab + "}\r\n" );
			sb.append( "\r\n" );
		}
	}

	private void generateBootstrapMethod( GeneratorContext ctx, StringBuilder sb )
	{
		if( !ctx.mandatoryParameters.isEmpty() )
		{
			ParameterInformation info = ctx.mandatoryParameters.get( 0 );
			String nextInterfaceName = ctx.mandatoryParameters.size() > 1 ? ctx.mandatoryParameters.get( 1 ).interfaceName : "OptionalParameters";

			sb.append( tab + "public static " + nextInterfaceName + " " + info.setterName + "(" + info.parameterType + " " + info.parameterName + ") {\r\n" );
			sb.append( tab + tab + "return new BuilderInternal()." + info.setterName + "(" + info.parameterName + ");\r\n" );
			sb.append( tab + "}\r\n" );

			sb.append( tab + "public static " + info.interfaceName + " prepare() {\r\n" );
			sb.append( tab + tab + "return new BuilderInternal();\r\n" );
			sb.append( tab + "}\r\n" );
		}
		else
		{
			sb.append( tab + "public static OptionalParameters prepare() {\r\n" );
			sb.append( tab + tab + "return new BuilderInternal();\r\n" );
			sb.append( tab + "}\r\n" );
		}
	}

	private void saveBuilderClass( GeneratorContext ctx, StringBuilder sb )
	{
		try
		{
			JavaFileObject jfo = processingEnv.getFiler().createSourceFile( ctx.builderClassFqn, ctx.element );

			OutputStream os = jfo.openOutputStream();
			PrintWriter pw = new PrintWriter( os );
			pw.print( sb.toString() );
			pw.close();
			os.close();

			processingEnv.getMessager().printMessage( Kind.NOTE, "Builder generated for this constructor: " + ctx.builderClassFqn, ctx.element );
		}
		catch( IOException e )
		{
			e.printStackTrace();
			processingEnv.getMessager().printMessage( Kind.ERROR, "Error generating builder, a builder may already exist (" + ctx.builderClassFqn + ") !" + e, ctx.element );
		}
	}

	private static class ParameterInformation
	{
		String parameterName;
		TypeMirror parameterType;
		String interfaceName;
		String setterName;

		public ParameterInformation( String parameterName, TypeMirror parameterType, String setterName )
		{
			this.parameterName = parameterName;
			this.parameterType = parameterType;
			this.interfaceName = "MandatoryParameter" + capitalize( parameterName );
			this.setterName = setterName;
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

	private static TypeElement getEnclosingTypeElement( Element element )
	{
		while( element != null && !(element instanceof TypeElement) )
			element = element.getEnclosingElement();
		if( element == null )
			return null;
		return (TypeElement) element;
	}

	private static String capitalize( String value )
	{
		return value.substring( 0, 1 ).toUpperCase() + value.substring( 1 );
	}
}
