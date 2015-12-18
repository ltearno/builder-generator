package fr.lteconsulting;

public @interface Parameter
{
	boolean mandatory() default false;
	String name() default "";
}
