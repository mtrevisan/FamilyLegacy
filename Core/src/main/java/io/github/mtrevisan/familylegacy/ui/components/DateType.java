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
package io.github.mtrevisan.familylegacy.ui.components;

import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;


public enum DateType{

	FULL_DATE("dialog.date.full_date", DateReader.TAG_VALUE_FULL_DATE),
	DECADE("dialog.date.decade", DateReader.TAG_VALUE_DECADE),
	CENTURY("dialog.date.century", DateReader.TAG_VALUE_CENTURY);


	private final String labelKey;
	private final String tagName;


	DateType(final String labelKey, final String tagName){
		this.labelKey = labelKey;
		this.tagName = tagName;
	}


	public String getLabel(){
		return I18N.t(labelKey);
	}

	public String getTagName(){
		return tagName;
	}

	public String getErrorMessage(){
		return I18N.tf("validation.required", getLabel());
	}


	public static DateType fromNode(final FLEFRecord record){
		for(final DateType type : values())
			if(FLEFRecordHelper.findChild(record, type.tagName) != null)
				return type;
		return null;
	}


	@Override
	public String toString(){
		return getLabel();
	}

}

