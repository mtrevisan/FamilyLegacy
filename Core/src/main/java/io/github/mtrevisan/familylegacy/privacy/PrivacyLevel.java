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
package io.github.mtrevisan.familylegacy.privacy;

import io.github.mtrevisan.familylegacy.io.model.readers.PrivacyReader;


/**
 * The privacy levels of the FLEF protocol, in increasing order of restriction.
 */
public enum PrivacyLevel{

	/** Information may be freely displayed, exported, and distributed. */
	PUBLIC,
	/** Information should be treated with caution and may be omitted from public reports, exports, or distributions. */
	RESTRICTED,
	/** Information should not normally be displayed, exported, or included in public distributions. */
	CONFIDENTIAL;


	/**
	 * Converts the protocol value to a level.
	 *
	 * @param level	The {@code privacy.level} value; anything that is not {@code restricted} or {@code confidential} is public.
	 * @return	The level.
	 */
	public static PrivacyLevel of(final String level){
		if(level != null){
			if(PrivacyReader.isConfidential(level))
				return CONFIDENTIAL;
			if(PrivacyReader.isRestricted(level))
				return RESTRICTED;
		}
		return PUBLIC;
	}

	/** The most restrictive of the two levels. */
	public static PrivacyLevel mostRestrictive(final PrivacyLevel a, final PrivacyLevel b){
		return (a.compareTo(b) >= 0? a: b);
	}

}
