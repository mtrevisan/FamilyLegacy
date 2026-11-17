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


/**
 * {@code require every var in field: left operator right}: the comparison holds for every occurrence of a repeated field.
 * <p>
 * Inside the comparison, {@code var} is the occurrence and {@code var.path} a field of it; when the occurrence is a reference to a
 * record (an {@code Xref}), the path is read in the referenced record. Any other field is read in the structure that holds the
 * constraint. Example (in a SourceCitation): {@code require every e in extract: e.source == source}, every extract cited belongs to
 * the source cited.
 * <p>
 * An occurrence whose reference does not resolve, or a comparison with a missing operand, is not checked here: the integrity of
 * the references and the cardinality of the fields are checked by their own rules.
 */
public final class EveryConstraint extends Constraint{

	private final String variable;
	private final String field;
	private final String left;
	private final String operator;
	private final String right;
	//what depends on the tokens only, decided once
	private final String variablePrefix;
	private final boolean ordering;


	public EveryConstraint(final String variable, final String field, final String left, final String operator, final String right){
		this.variable = variable;
		this.field = field;
		this.left = left;
		this.operator = operator;
		this.right = right;

		variablePrefix = variable + '.';
		ordering = ComparisonConstraint.isOrdering(operator);
	}


	public String getVariable(){
		return variable;
	}

	public String getField(){
		return field;
	}

	public String getLeft(){
		return left;
	}

	public String getRight(){
		return right;
	}

	/**
	 * @param operand	One side of the comparison.
	 * @return	Whether the operand is the variable, or a field of it.
	 */
	public boolean isOnVariable(final String operand){
		return (variable.equals(operand) || operand.startsWith(variablePrefix));
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final RecordLookup model,
			final List<String> errors){
		final List<FLEFRecord> occurrences = FLEFRecordHelper.findChildren(record, field);
		for(final FLEFRecord occurrence : occurrences){
			final FLEFRecord element = resolve(occurrence, model);
			if(element == null)
				continue;

			final String leftValue = resolveValue(left, occurrence, element, record);
			final String rightValue = resolveValue(right, occurrence, element, record);
			if(leftValue == null || rightValue == null)
				continue;

			if(ordering && !(ComparisonConstraint.isInteger(leftValue) && ComparisonConstraint.isInteger(rightValue))){
				errors.add(String.format("Constraint violation at '%s': '%s' failed for %s %s (values: %s, %s): the values are not comparable numerically",
					contextPath, this, field, occurrence.getValue(), leftValue, rightValue));

				continue;
			}

			if(!ComparisonConstraint.compare(leftValue, rightValue, operator))
				errors.add(String.format("Constraint violation at '%s': '%s' failed for %s %s (values: %s, %s), record %s",
					contextPath, this, field, StringUtils.defaultString(occurrence.getValue()), leftValue, rightValue, record));
		}
	}

	/** The record an occurrence references, or the occurrence itself if it is a structure; {@code null} for a dangling reference. */
	private static FLEFRecord resolve(final FLEFRecord occurrence, final RecordLookup model){
		final String value = occurrence.getValue();
		if(StringUtils.isEmpty(value))
			return occurrence;

		return (model != null? model.getRecordById(value): null);
	}

	private String resolveValue(final String operand, final FLEFRecord occurrence, final FLEFRecord element, final FLEFRecord record){
		if(variable.equals(operand))
			return (StringUtils.isNotEmpty(occurrence.getValue())? occurrence.getValue(): element.getId());
		if(operand.startsWith(variablePrefix))
			return FLEFRecordHelper.getChildValue(element, operand.substring(variablePrefix.length()));
		if(!ComparisonConstraint.isField(operand))
			return operand;

		return FLEFRecordHelper.getChildValue(record, operand);
	}

	@Override
	public String toString(){
		return "require every " + variable + " in " + field + ": " + left + StringUtils.SPACE + operator + StringUtils.SPACE + right;
	}

}
