/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.helpers;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;


public class FilterString{

	private static final String FILER_DELIMITER = " | ";

	private final StringJoiner filter = new StringJoiner(FILER_DELIMITER);
	private final Set<String> data = new HashSet<>(0);


	public static FilterString create(){
		return new FilterString();
	}


	private FilterString(){}


	public FilterString add(final Integer value){
		if(value != null)
			data.add(Integer.toString(value));

		return this;
	}

	public FilterString add(final String text){
		if(StringUtils.isNotBlank(text))
			data.add(text);

		return this;
	}

	public FilterString add(final StringJoiner sj){
		if(sj != null)
			add(sj.toString());

		return this;
	}

	@Override
	public String toString(){
		for(final String datum : data)
			filter.add(datum);
		return filter.toString();
	}

}
