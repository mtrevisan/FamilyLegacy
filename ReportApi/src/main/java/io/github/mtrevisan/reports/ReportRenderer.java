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
import java.util.List;


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

	/**
	 * @return	Whether the output is a directory (a website) and not a single file: by default, when {@link #extension()} does not start
	 * 	with a dot.
	 */
	default boolean isDirectory(){
		return !extension().startsWith(".");
	}

	/**
	 * Writes the document.
	 * <p>
	 * The target is the path of the output: the parent directory exists, the target itself may exist. A renderer that writes a file replaces
	 * it; one that writes a directory ({@link #isDirectory()}) creates it if needed and overwrites the files it writes, without removing
	 * the others. On failure it may leave a partial output, and throws.
	 *
	 * @param doc	The document.
	 * @param target	The file, or the directory, to write.
	 * @throws IOException	If the output cannot be written.
	 */
	void render(ReportDocument doc, Path target) throws IOException;

	/** @return	Whether the format lets the user choose the font ({@link #fonts()}); it is cheap, unlike {@link #fonts()}. By default it does not. */
	default boolean choosesFont(){
		return false;
	}

	/**
	 * @return	The names of the fonts the format can be written with, for a format that lets the user choose one (the PDF); empty by default.
	 * 	It can take a while (the fonts of the system are read), so it must not be called on the thread of the user interface.
	 */
	default List<String> fonts(){
		return List.of();
	}

	/**
	 * Writes the document with a font.
	 *
	 * @param doc	The document.
	 * @param target	The file, or the directory, to write.
	 * @param font	The name of one of the {@link #fonts()}, or {@code null} or blank for the one the format chooses; ignored by a format
	 * 	without a choice of fonts (the default).
	 * @throws IOException	If the output cannot be written.
	 */
	default void render(final ReportDocument doc, final Path target, final String font) throws IOException{
		render(doc, target);
	}

}
