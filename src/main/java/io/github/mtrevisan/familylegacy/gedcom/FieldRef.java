/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.gedcom;

import java.util.Map;


class FieldRef{

	private final Map<String, Object> target;
	private final String fieldName;


	FieldRef(final Map<String, Object> target, final String fieldName){
		this.target = target;
		this.fieldName = fieldName;
	}

	public Object getTarget(){
		return target;
	}

	public String getFieldName(){
		return fieldName;
	}

	public void setValue(final String value){
		target.put(fieldName, value);
	}

	public Object getValue(){
		return target.get(fieldName);
	}

	public void appendValue(final String value) throws NoSuchMethodException{
		final Object currentValue = getValue();
		if(currentValue != null && !(currentValue instanceof String))
			throw new NoSuchMethodException("Field '" + fieldName + "' is not a string, cannot append value " + value);

		setValue((currentValue == null? "": currentValue) + value);
	}

}
