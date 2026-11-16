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
package io.github.mtrevisan.reports.pdf;

import java.net.URISyntaxException;
import java.nio.file.Path;


/** Where the fonts the program carries are (shared by {@link PdfFonts} and {@link IndicShaper}, so that they do not depend on each other). */
final class FontsDirectory{

	private static final String FONTS_DIR_PROPERTY = "familylegacy.fonts.dir";
	private static final String FONTS_DIR = "fonts";


	private FontsDirectory(){}


	/**
	 * @return	The directory of the fonts the program carries ({@code fonts}, beside the {@code plugins} directory this plugin is in, or the
	 * 	one the {@code familylegacy.fonts.dir} system property names); {@code null} if it cannot be told. It may not exist.
	 */
	static Path get(){
		final String explicit = System.getProperty(FONTS_DIR_PROPERTY);
		if(explicit != null && !explicit.isBlank())
			return Path.of(explicit);

		try{
			final Path plugins = Path.of(FontsDirectory.class.getProtectionDomain().getCodeSource().getLocation().toURI())
				.getParent();
			final Path parent = (plugins != null? plugins.getParent(): null);
			return (parent != null? parent.resolve(FONTS_DIR): null);
		}
		catch(final URISyntaxException | RuntimeException ignored){
			return null;
		}
	}

}
