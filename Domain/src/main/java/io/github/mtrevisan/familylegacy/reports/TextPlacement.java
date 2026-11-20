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
 * Where a text that belongs to a part of the life of a person (a note, the text of a source) is written: right after the part, or in a
 * section of its own at the end of the chapter of the person, where the part only refers to it.
 */
public enum TextPlacement{

	/** Right after what it belongs to. */
	INLINE,
	/** Inline if short, in the section if longer than the number of lines the report allows ({@link ReportConfig#longTextLines()}). */
	LONG_IN_SECTION,
	/** Always in the section. */
	SECTION;


	/**
	 * @param lines	The length of the text, in lines.
	 * @param longTextLines	What is longer than this is long.
	 * @return	Whether the text goes to the section.
	 */
	public boolean inSection(final int lines, final int longTextLines){
		return switch(this){
			case INLINE -> false;
			case LONG_IN_SECTION -> (lines > longTextLines);
			case SECTION -> true;
		};
	}

}
