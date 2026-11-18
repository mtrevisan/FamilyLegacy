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


/**
 * What a report does with the individuals that may be living (see {@link PrivacyResolver}), and with everything about them: their
 * events, attributes, media, notes, citations and the relationships they take part in.
 * <p>
 * It does not apply when the privacy policy is {@link PrivacyPolicy#SHOW_ALL}, that shows everything.
 * </p>
 */
public enum LivingPrivacy{

	/** Hide them, and everything about them: it is as if they were not in the file. */
	HIDE,

	/** Show only their name, and their role in the family (child of, spouse of, member of a group): nothing else about them. */
	NAME_AND_ROLE,

	/** Show them as any other individual: the privacy policy alone decides. */
	SHOW

}
