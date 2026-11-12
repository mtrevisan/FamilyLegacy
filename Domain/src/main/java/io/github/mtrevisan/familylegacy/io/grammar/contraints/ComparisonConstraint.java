/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.io.grammar.contraints;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


public final class ComparisonConstraint extends Constraint{

	/** The comparison operators of the grammar, shared with the grammar parser. */
	public static final String NOT_EQUALS = "!=";
	public static final String EQUALS = "==";
	public static final String GREATER_THAN = ">";
	public static final String LESS_THAN = "<";
	public static final String GREATER_THAN_OR_EQUALS = ">=";
	public static final String LESS_THAN_OR_EQUALS = "<=";

	private final String left;
	private final String operator;
	private final String right;


	public ComparisonConstraint(final String left, final String operator, final String right){
		this.left = left;
		this.operator = operator;
		this.right = right;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final RecordLookup model,
			final List<String> errors){
		final String leftValue = resolveValue(record, left);
		final String rightValue = resolveValue(record, right);

		// If both fields are optional and not present, the constraint does not apply.
		if(leftValue == null && rightValue == null)
			return;

		// If one of the two is a required field and is missing, error
		if(isField(left) && leftValue == null){
			errors.add(String.format("Constraint violation at '%s': field '%s' not found", contextPath, left));

			return;
		}
		if(isField(right) && rightValue == null){
			errors.add(String.format("Constraint violation at '%s': field '%s' not found", contextPath, right));

			return;
		}

		if(isOrdering(operator) && !(isInteger(leftValue) && isInteger(rightValue))){
			errors.add(String.format("Constraint violation at '%s': '%s %s %s' failed (values: %s, %s): the values are not comparable numerically",
				contextPath, left, operator, right, leftValue, rightValue));

			return;
		}

		final boolean valid = compare(leftValue, rightValue, operator);
		if(!valid)
			errors.add(String.format("Constraint violation at '%s': '%s %s %s' failed (values: %s, %s)",
				contextPath, left, operator, right, leftValue, rightValue));
	}

	private static boolean isOrdering(final String op){
		return (GREATER_THAN.equals(op) || GREATER_THAN_OR_EQUALS.equals(op) || LESS_THAN.equals(op)
			|| LESS_THAN_OR_EQUALS.equals(op));
	}

	private static boolean isInteger(final String value){
		try{
			Long.parseLong(value);

			return true;
		}
		catch(final NumberFormatException ignored){
			return false;
		}
	}

	private boolean isField(final String token){
		// A field is an identifier (letters, digits, underscores); a literal is a number.
		return ((token.isEmpty() || token.charAt(0) < '0' || token.charAt(0) > '9'));
	}

	private String resolveValue(final FLEFRecord record, final String token){
		if(!isField(token))
			return token;

		final FLEFRecord child = FLEFRecordHelper.findChild(record, token);
		return (child != null? child.getValue(): null);
	}

	private boolean compare(final String leftVal, final String rightVal, final String op){
		try{
			long l = Long.parseLong(leftVal);
			long r = Long.parseLong(rightVal);
			return switch(op){
				case GREATER_THAN -> l > r;
				case GREATER_THAN_OR_EQUALS -> l >= r;
				case LESS_THAN -> l < r;
				case LESS_THAN_OR_EQUALS -> l <= r;
				case EQUALS -> l == r;
				case NOT_EQUALS -> l != r;
				default -> false;
			};
		}
		catch(final NumberFormatException e){
			return switch(op){
				case EQUALS -> leftVal.equals(rightVal);
				case NOT_EQUALS -> !leftVal.equals(rightVal);
				default -> false;
			};
		}
	}

	@Override
	public String toString(){
		return ("require " + left + StringUtils.SPACE + operator + StringUtils.SPACE + right);
	}

}
