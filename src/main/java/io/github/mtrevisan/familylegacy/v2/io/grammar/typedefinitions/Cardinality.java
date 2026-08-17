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
package io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions;

import org.apache.commons.lang3.StringUtils;


public enum Cardinality{
	REQUIRED,		// no suffix
	OPTIONAL,		// ?
	ZERO_OR_MORE,	// *
	ONE_OR_MORE;	// +

	public String symbol(){
		return switch(this){
			case OPTIONAL -> "?";
			case ZERO_OR_MORE -> "*";
			case ONE_OR_MORE -> "+";
			default -> StringUtils.EMPTY;
		};
	}

	public boolean isRequired(){
		return (this == REQUIRED || this == ONE_OR_MORE);
	}

	public boolean isSingle(){
		return false;
	}

	/**
	 * Validates whether a given occurrence count satisfies this cardinality constraint.
	 *
	 * @param count	the number of occurrences found in the record
	 * @return {@code true} if the count is valid according to this cardinality; {@code false} otherwise
	 */
	public boolean isValidCount(final int count){
		return switch(this){
			case REQUIRED -> (count == 1);
			case OPTIONAL -> (count <= 1);
			case ONE_OR_MORE -> (count >= 1);
			case ZERO_OR_MORE -> (count >= 0);
		};
	}

}
