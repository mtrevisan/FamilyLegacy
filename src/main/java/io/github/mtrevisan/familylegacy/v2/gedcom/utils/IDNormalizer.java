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
package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import org.apache.commons.lang3.StringUtils;


/**
 * Utility to normalize GEDCOM local identifiers.
 */
public final class IDNormalizer{

	private IDNormalizer(){
	}

	/**
	 * Removes leading/trailing '@' characters and trims the result.
	 * Example: "@I7@" -> "I7"
	 */
	public static String clean(String id){
		if(id == null) return null;
		return id.replace("@", StringUtils.EMPTY).trim();
	}

	public static boolean isValidIdFormat(String id){
		// Simple check: alphanumeric, no spaces or dashes (but dashes are allowed per spec, but we avoid)
		// We'll accept letters and digits only.
		return id != null && id.matches("^[A-Za-z][A-Za-z0-9]*$");
	}

}
