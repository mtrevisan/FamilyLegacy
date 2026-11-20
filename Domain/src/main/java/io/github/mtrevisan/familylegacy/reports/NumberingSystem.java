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
package io.github.mtrevisan.familylegacy.reports;


/**
 * How the persons of an ancestor or descendant report are numbered.
 * <p>
 * The numbers are written before the name of the person, in the index and in the tree, and (if the cross references are on) after the
 * name wherever another person of the report is mentioned.
 * </p>
 */
public enum NumberingSystem{

	/** The most appropriate system for the report: Ahnentafel for the ancestors, d'Aboville for the descendants. */
	AUTO,
	/** No numbers. */
	NONE,
	/** Ahnentafel (Sosa-Stradonitz), for the ancestors: the person is 1, the father of the person n is 2n, the mother is 2n + 1. */
	AHNENTAFEL,
	/** d'Aboville, for the descendants: the person is 1, the children are 1.1, 1.2, ..., the children of 1.2 are 1.2.1, 1.2.2, ... */
	D_ABOVILLE,
	/** Henry, for the descendants: the person is 1, the children are 11, 12, ..., the children of 12 are 121, 122, ... (X for the tenth). */
	HENRY;


	/**
	 * @param type	The type of the report.
	 * @return	The system the report really uses: {@code AUTO} is the natural one of the report, and a system that numbers the other direction
	 * 	(Ahnentafel in a report of the descendants, d'Aboville or Henry in a report of the ancestors) is replaced by it too.
	 */
	public NumberingSystem resolve(final ReportType type){
		final boolean ancestors = (type == ReportType.ANCESTOR);
		if(!ancestors && type != ReportType.DESCENDANT)
			return NONE;

		return switch(this){
			case NONE -> NONE;
			case AHNENTAFEL -> (ancestors? AHNENTAFEL: D_ABOVILLE);
			case D_ABOVILLE, HENRY -> (ancestors? AHNENTAFEL: this);
			case AUTO -> (ancestors? AHNENTAFEL: D_ABOVILLE);
		};
	}

}
