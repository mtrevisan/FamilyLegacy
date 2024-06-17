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
package io.github.mtrevisan.familylegacy.flef.gedcom;


/** The gedcom grammar file needs these keywords before the first structure. */
enum GrammarFileHeaderKeywords{

	/** The version of the gedcom grammar. */
	GEDCOM_VERSION("GEDCOM_VERSION"),
	/** The date of the gedcom grammar. */
	GEDCOM_DATE("GEDCOM_DATE"),
	/** The source of the gedcom grammar (The website/file/book/...). */
	GEDCOM_SOURCE("GEDCOM_SOURCE"),
	/**
	 * A description about the gedcom grammar file.
	 * <p>List any modifications of the grammar structures here and give any additional information.<br>
	 * The description can have multiple lines. Everything after the GRAMPS_DESCRIPTION keyword and the next keyword or the first
	 * structure will be taken as description.</p>
	 */
	GEDCOM_DESCRIPTION("GEDCOM_DESCRIPTION");


	final String value;

	GrammarFileHeaderKeywords(final String value){
		this.value = value;
	}

}
