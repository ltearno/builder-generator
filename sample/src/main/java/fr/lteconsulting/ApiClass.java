package fr.lteconsulting;

/**
 * Construction of an expression tree
 */
public class ApiClass
{
	static abstract class Node
	{
	}

	public static class Operation extends Node
	{
		Node left;
		Node right;
		String operation;

		@UseBuilderGenerator
		public Operation( @Mandatory Node left, @Mandatory String operation, @Mandatory Node right )
		{
			this.left = left;
			this.right = right;
			this.operation = operation;
		}

		@Override
		public String toString()
		{
			return "(" + left + " " + operation + " " + right + ")";
		}
	}

	public static class Value extends Node
	{
		int value;

		@UseBuilderGenerator
		public Value( @Mandatory int value )
		{
			this.value = value;
		}

		@Override
		public String toString()
		{
			return String.valueOf( value );
		}
	}
}
