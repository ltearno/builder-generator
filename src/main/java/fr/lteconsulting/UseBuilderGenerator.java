package fr.lteconsulting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ask for automatic generation of a builder for a constructor
 */
@Retention( RetentionPolicy.SOURCE )
@Target( ElementType.CONSTRUCTOR )
public @interface UseBuilderGenerator
{
	String builderName() default "";
}
