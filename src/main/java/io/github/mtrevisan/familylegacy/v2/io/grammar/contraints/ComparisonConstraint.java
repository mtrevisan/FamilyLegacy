package io.github.mtrevisan.familylegacy.v2.io.grammar.contraints;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


public final class ComparisonConstraint extends Constraint{

	private final String left;
	private final String operator;
	private final String right;


	public ComparisonConstraint(final String left, final String operator, final String right){
		this.left = left;
		this.operator = operator;
		this.right = right;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final FLEFModel model,
			final List<String> errors){
		final String leftValue = resolveValue(record, left);
		final String rightValue = resolveValue(record, right);

		// If both fields are optional and not present, the constraint does not apply.
		if(leftValue == null && rightValue == null)
			return;

		// If one of the two is a required field and is missing, error
		if(isField(left) && leftValue == null){
			errors.add(String.format(
				"Constraint violation at '%s': field '%s' not found",
				contextPath, left
			));

			return;
		}
		if(isField(right) && rightValue == null){
			errors.add(String.format(
				"Constraint violation at '%s': field '%s' not found",
				contextPath, right
			));

			return;
		}

		final boolean valid = compare(leftValue, rightValue, operator);
		if(!valid){
			errors.add(String.format(
				"Constraint violation at '%s': '%s %s %s' failed (values: %s, %s)",
				contextPath, left, operator, right, leftValue, rightValue
			));
		}
	}

	private boolean isField(final String token){
		// A field is an identifier (letters, digits, underscores); a literal is a number or string.
		return (!token.matches("^[0-9].*") && !token.startsWith("\""));
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
				case ">" -> l > r;
				case ">=" -> l >= r;
				case "<" -> l < r;
				case "<=" -> l <= r;
				case "==" -> l == r;
				case "!=" -> l != r;
				default -> false;
			};
		}
		catch(final NumberFormatException e){
			return switch(op){
				case "==" -> leftVal.equals(rightVal);
				case "!=" -> !leftVal.equals(rightVal);
				default -> false;
			};
		}
	}

	@Override
	public String toString(){
		return ("require " + left + StringUtils.SPACE + operator + StringUtils.SPACE + right);
	}

}
