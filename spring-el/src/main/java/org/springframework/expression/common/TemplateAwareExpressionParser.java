package org.springframework.expression.common;

import java.util.LinkedList;
import java.util.List;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;

/**
 * An expression parser that understands templates. It can be subclassed by expression parsers that do not offer first
 * class support for templating.
 * 
 * @author Keith Donald
 * @author Andy Clement
 */
public abstract class TemplateAwareExpressionParser implements ExpressionParser {

	public Expression parseExpression(String expressionString) throws ParseException {
		return parseExpression(expressionString, DefaultNonTemplateParserContext.INSTANCE);
	}

	public final Expression parseExpression(String expressionString, ParserContext context) throws ParseException {
		if (context == null) {
			context = DefaultNonTemplateParserContext.INSTANCE;
		}
		if (context.isTemplate()) {
			return parseTemplate(expressionString, context);
		} else {
			return doParseExpression(expressionString, context);
		}
	}

	private Expression parseTemplate(String expressionString, ParserContext context) throws ParseException {
		if (expressionString.length() == 0) {
			// TODO throw exception if there are template prefix/suffixes and it is length 0?
			return new LiteralExpression("");
		}
		Expression[] expressions = parseExpressions(expressionString, context);
		if (expressions.length == 1) {
			return expressions[0];
		} else {
			return new CompositeStringExpression(expressionString, expressions);
		}
	}

	// helper methods

	/**
	 * Helper that parses given expression string using the configured parser. The expression string can contain any
	 * number of expressions all contained in "${...}" markers. For instance: "foo${expr0}bar${expr1}". The static
	 * pieces of text will also be returned as Expressions that just return that static piece of text. As a result,
	 * evaluating all returned expressions and concatenating the results produces the complete evaluated string.
	 * Unwrapping is only done of the outermost delimiters found, so the string 'hello ${foo${abc}}' would break into
	 * the pieces 'hello ' and 'foo${abc}'. This means that expression languages that used ${..} as part of their
	 * functionality are supported without any problem
	 * 
	 * @param expressionString the expression string
	 * @return the parsed expressions
	 * @throws ParseException when the expressions cannot be parsed
	 */
	private final Expression[] parseExpressions(String expressionString, ParserContext context) throws ParseException {
		// TODO this needs to handle nested delimiters for cases where the expression uses the delim chars
		List<Expression> expressions = new LinkedList<Expression>();
		int startIdx = 0;
		String prefix = context.getExpressionPrefix();
		String suffix = context.getExpressionSuffix();
		while (startIdx < expressionString.length()) {
			int prefixIndex = expressionString.indexOf(prefix, startIdx);
			if (prefixIndex >= startIdx) {
				// a inner expression was found - this is a composite
				if (prefixIndex > startIdx) {
					expressions.add(new LiteralExpression(expressionString.substring(startIdx, prefixIndex)));
					startIdx = prefixIndex;
				}
				int nextPrefixIndex = expressionString.indexOf(prefix, prefixIndex + prefix.length());
				int suffixIndex;
				if (nextPrefixIndex == -1) {
					// this is the last expression in the expression string
					suffixIndex = expressionString.lastIndexOf(suffix);

				} else {
					// another expression exists after this one in the expression string
					suffixIndex = expressionString.lastIndexOf(suffix, nextPrefixIndex);
				}
				if (suffixIndex < (prefixIndex + prefix.length())) {
					throw new ParseException(expressionString, "No ending suffix '" + suffix
							+ "' for expression starting at character " + prefixIndex + ": "
							+ expressionString.substring(prefixIndex), null);
				} else if (suffixIndex == prefixIndex + prefix.length()) {
					throw new ParseException(expressionString, "No expression defined within delimiter '" + prefix
							+ suffix + "' at character " + prefixIndex, null);
				} else {
					String expr = expressionString.substring(prefixIndex + prefix.length(), suffixIndex);
					expressions.add(doParseExpression(expr, context));
					startIdx = suffixIndex + suffix.length();
				}
			} else {
				if (startIdx == 0) {
					expressions.add(doParseExpression(expressionString, context));
				} else {
					// no more ${expressions} found in string, add rest as static text
					expressions.add(new LiteralExpression(expressionString.substring(startIdx)));
				}
				startIdx = expressionString.length();
			}
		}
		return expressions.toArray(new Expression[expressions.size()]);
	}

	protected abstract Expression doParseExpression(String expressionString, ParserContext context)
			throws ParseException;

}
