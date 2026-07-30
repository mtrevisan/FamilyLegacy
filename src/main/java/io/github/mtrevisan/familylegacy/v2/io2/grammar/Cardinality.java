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
package io.github.mtrevisan.familylegacy.v2.io.grammar;


/**
 * Represents the cardinality of a tag or structure occurrence.
 * Format: {min:max} where min and max are integers, or "M" for unlimited.
 * Examples: {1:1}, {0:M}, {1:M}, {0:1}
 */
public final class Cardinality{

	private final int min;
	// Integer.MAX_VALUE for unlimited
	private final int max;


	/**
	 * Creates a Cardinality from a string like "{1:1}", "{0:M}", etc.
	 */
	public static Cardinality parse(final String s){
		if(s == null || s.isEmpty())
			throw new IllegalArgumentException("Cardinality string cannot be null or empty");

		final String trimmed = s.trim();
		if(!trimmed.startsWith("{") || !trimmed.endsWith("}"))
			throw new IllegalArgumentException("Invalid cardinality format: " + s);

		final String inner = trimmed.substring(1, trimmed.length() - 1);
		final String[] parts = inner.split(":");
		if(parts.length != 2)
			throw new IllegalArgumentException("Invalid cardinality format: " + s);

		final int min = Integer.parseInt(parts[0]);
		final int max = ("M".equals(parts[1])? Integer.MAX_VALUE: Integer.parseInt(parts[1]));
		return new Cardinality(min, max);
	}


	private Cardinality(final int min, final int max){
		this.min = min;
		this.max = max;
	}

	public boolean isValidCount(final int count){
		return (count >= min && count <= max);
	}

	@Override
	public String toString(){
		return "{" + min + ":" + (max == Integer.MAX_VALUE? "M": max) + "}";
	}

}
