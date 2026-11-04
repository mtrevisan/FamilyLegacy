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
package io.github.mtrevisan.reports;

import java.io.IOException;
import java.nio.file.Path;


/**
 * Writes a {@link ReportDocument} in a file format.
 * <p>
 * The renderers are found as {@link java.util.ServiceLoader} providers: a provider needs a public no-argument constructor.
 */
public interface ReportRenderer{

	/** @return	The stable identifier of the format ({@code "markdown"}, {@code "pdf"}, …), also the key of its translated name. */
	String id();

	/** @return	The name of the format, in English, as a fallback when the identifier has no translation. */
	String displayName();

	/** @return	The suffix added to the name of the output: the extension of the file, with the leading dot ({@code ".md"}), or the suffix of the
	 * 	name of the directory for a format that is a directory ({@code "_site"}). */
	String extension();

	void render(ReportDocument doc, Path target) throws IOException;

}
