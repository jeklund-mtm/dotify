package org.daisy.dotify.api.obfl;

import java.util.Map;

public interface Expression {

	/**
	 * Evaluate is the method to use when evaluating an expression.
	 * 
	 * @param expr
	 *            the expression to evaluate
	 * @return returns the evaluation result
	 */
	public Object evaluate(String expr);

	/**
	 * Evaluates this expression by first replacing any occurrences of the
	 * supplied variable
	 * names (prefixed by $) with the corresponding values in the map. The
	 * variable names must only
	 * contain word characters.
	 * 
	 * @param expr
	 * @param variables
	 * @return returns the evaluation result
	 */
	public Object evaluate(String expr, Map<String, String> variables);

	public Object evaluate(String expr, String... vars);
}
