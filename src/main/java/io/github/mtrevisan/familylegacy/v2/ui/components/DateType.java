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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;


/* DONE */
public enum DateType{

	FULL_DATE("Full Date", Constants.TAG_FULL_DATE, "Date is required for FULL DATE type."),
	DECADE("Decade", Constants.TAG_DECADE, "Decade is required for DECADE type."),
	CENTURY("Century", Constants.TAG_CENTURY, "Century is required for CENTURY type.");


	private final String label;
	private final String tagName;
	private final String errorMessage;


	DateType(final String label, final String tagName, final String errorMessage){
		this.label = label;
		this.tagName = tagName;
		this.errorMessage = errorMessage;
	}


	public String getLabel(){
		return label;
	}

	public String getTagName(){
		return tagName;
	}

	public String getErrorMessage(){
		return errorMessage;
	}


	public static DateType fromNode(final FLEFRecord record){
		for(final DateType type : values())
			if(record.findChild(type.tagName) != null)
				return type;
		return FULL_DATE;
	}


	@Override
	public String toString(){
		return label;
	}


	private static class Constants{
		private static final String TAG_FULL_DATE = "FULL_DATE";
		private static final String TAG_DECADE = "DECADE";
		private static final String TAG_CENTURY = "CENTURY";
	}

}

