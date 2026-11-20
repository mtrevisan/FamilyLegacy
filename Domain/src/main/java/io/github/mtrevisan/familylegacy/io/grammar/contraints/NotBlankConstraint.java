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
 * {@code require not_blank(field)}: the field, when it is present, MUST NOT be empty or made of white space only.
 * <p>
 * A missing field is the business of the cardinality of the field (a required one is already reported as missing), not of this rule.
 */
public final class NotBlankConstraint extends Constraint{

	private final String field;


	public NotBlankConstraint(final String field){
		this.field = field;
	}


	@Override
	public void validate(final String contextPath, final FLEFRecord record, final RecordLookup model, final List<String> errors){
		if(FLEFRecordHelper.findChild(record, field) != null && StringUtils.isBlank(FLEFRecordHelper.getChildValue(record, field)))
			errors.add(String.format("Constraint violation at '%s': '%s' must not be blank", contextPath, field));
	}

	@Override
	public String toString(){
		return "require not_blank(" + field + ")";
	}

}
