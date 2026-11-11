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

import org.apache.commons.lang3.StringUtils;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.FontSelector;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


/**
 * The fonts of a PDF: the ones of the system (embedded, so the file reads the same everywhere), the one the user chose, and the ones that
 * supply the letters the chosen one lacks.
 * <p>
 * Without a choice the font is the sans-serif that OpenPDF carries (Latin, Greek and Cyrillic); for the text it cannot write (Arabic,
 * Hebrew, Chinese, Japanese, Korean, Indic, Thai…) the system fonts are searched, and a system font is added to the chain only if it
 * has some of the missing characters. Scanning the system fonts takes a moment, so it is done once, and only when it is needed.
 */
final class PdfFonts{

	/** The names OpenPDF always knows (the standard PDF fonts), that are not worth a choice. */
	private static final Set<String> STANDARD = Set.of("helvetica", "courier", "times", "times-roman", "symbol", "zapfdingbats");
	/** The family of the monospaced font when the system has none of {@link #MONOSPACED}: the standard Courier. */
	private static final String COURIER = "\0courier";
	/** The monospaced families, the first one the system has is used (the descendants tree is written with it). */
	private static final List<String> MONOSPACED = List.of("consolas", "dejavu sans mono", "liberation mono", "cascadia mono",
		"lucida console", "menlo", "monaco", "courier new", "noto sans mono");
	/** The families that supply the letters the chosen font lacks, in order. */
	private static final List<String> FALLBACKS = List.of("segoe ui", "arial unicode ms", "dejavu sans", "noto sans", "noto sans arabic",
		"noto sans hebrew", "noto sans cjk sc", "noto sans cjk jp", "microsoft yahei", "yu gothic", "ms gothic", "malgun gothic", "simsun",
		"nirmala ui", "leelawadee ui", "segoe ui historic", "segoe ui symbol", "noto sans symbols", "symbola", "arial", "tahoma");

	private static boolean registered;


	private final List<String> bodyChain;
	private final List<String> monoChain;
	private final Map<Key, FontSelector> selectors = new HashMap<>();


	/** @param bold	{@code true} for a bold run.
	 * @param italic	{@code true} for an italic run.
	 * @param mono	{@code true} for a monospaced run. */
	private record Key(float size, boolean bold, boolean italic, boolean mono, Color color){}


	/** A selector whose last font, the default one of OpenPDF, has the style that was asked. */
	private static final class StyledSelector extends FontSelector{

		StyledSelector(final float size, final int style, final Color color){
			super();

			final Font last = fonts.getLast();
			last.setSize(size);
			last.setStyle(style);
			last.setColor(color);
		}

	}


	/**
	 * @param requested	The family the user chose ({@code null} or blank for the default one).
	 * @param text	All the text of the document, to know which characters the fonts must have.
	 * @param needsMonospaced	Whether the document has a preformatted block.
	 */
	PdfFonts(final String requested, final String text, final boolean needsMonospaced){
		final String family = (requested != null && !requested.isBlank()? requested.strip().toLowerCase(Locale.ROOT): null);
		String chosen = null;
		if(family != null){
			registerSystemFonts();
			if(FontFactory.isRegistered(family) && load(family, 10, false, false, null) != null)
				chosen = family;
		}
		bodyChain = chain(chosen, text);

		if(needsMonospaced){
			registerSystemFonts();
			String mono = COURIER;
			for(final String candidate : MONOSPACED)
				if(FontFactory.isRegistered(candidate) && load(candidate, 10, false, false, null) != null){
					mono = candidate;
					break;
				}
			monoChain = chain(mono, text);
		}
		else
			monoChain = List.of(COURIER);
	}


	/** @return	The families of the system, in alphabetical order, as they are shown to the user. */
	static List<String> families(){
		registerSystemFonts();

		return FontFactory.getRegisteredFamilies().stream()
			.filter(name -> !STANDARD.contains(name))
			.map(PdfFonts::display)
			.sorted(String.CASE_INSENSITIVE_ORDER)
			.toList();
	}

	/** Makes OpenPDF know the fonts of the system (once: it reads every font file). */
	private static synchronized void registerSystemFonts(){
		if(registered)
			return;
		registered = true;

		FontFactory.registerDirectories();
		//the fonts that a Windows user installed only for themselves are not in the fonts directory of the system
		final String local = System.getenv("LOCALAPPDATA");
		if(local != null)
			try{
				final Path directory = Path.of(local, "Microsoft", "Windows", "Fonts");
				if(Files.isDirectory(directory))
					FontFactory.registerDirectory(directory.toString());
			}
			catch(final RuntimeException ignored){
				//a path that cannot be read is not a reason to stop: the other fonts are enough
			}
	}

	private static String display(final String name){
		final StringBuilder sb = new StringBuilder(name.length());
		boolean start = true;
		for(final char c : name.toCharArray()){
			sb.append(start? Character.toUpperCase(c): c);
			start = (c == ' ' || c == '-');
		}
		return sb.toString();
	}


	/**
	 * @param primary	The first family ({@code null} for the default font of OpenPDF).
	 * @param text	The text to write.
	 * @return	The families to try in order: the primary, then the ones that supply the characters it lacks.
	 */
	private static List<String> chain(final String primary, final String text){
		final List<String> chain = new ArrayList<>();
		if(primary != null)
			chain.add(primary);

		final BaseFont base = (primary != null? baseOf(primary): defaultFont());
		final Set<Integer> missing = new TreeSet<>();
		text.codePoints()
			.filter(cp -> !Character.isWhitespace(cp) && !Character.isISOControl(cp))
			.filter(cp -> base == null || !base.charExists(cp))
			.forEach(missing::add);
		if(missing.isEmpty())
			return chain;

		//the default font of OpenPDF is the last one of the selector: what it has is not missing
		final BaseFont fallback = defaultFont();
		if(fallback != null)
			missing.removeIf(fallback::charExists);
		if(missing.isEmpty())
			return chain;

		registerSystemFonts();
		for(final String candidate : FALLBACKS){
			if(missing.isEmpty())
				break;
			if(chain.contains(candidate) || !FontFactory.isRegistered(candidate))
				continue;

			final BaseFont candidateBase = baseOf(candidate);
			if(candidateBase != null && missing.removeIf(candidateBase::charExists))
				chain.add(candidate);
		}
		return chain;
	}

	/** @return	The font OpenPDF carries (a Liberation Sans). */
	private static BaseFont defaultFont(){
		//the selector registers it when it is loaded
		new FontSelector();
		final Font font = FontFactory.getFont("sans", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10);
		return font.getBaseFont();
	}

	private static BaseFont baseOf(final String family){
		if(COURIER.equals(family))
			return new Font(Font.COURIER, 10).getCalculatedBaseFont(false);

		final Font font = load(family, 10, false, false, null);
		return (font != null? font.getBaseFont(): null);
	}

	/** @return	The font, or {@code null} if it cannot be loaded or embedded (a font may forbid its embedding). */
	private static Font load(final String family, final float size, final boolean bold, final boolean italic,
			final Color color){
		if(COURIER.equals(family))
			return new Font(Font.COURIER, size, style(bold, italic), color);

		final Font font = FontFactory.getFont(family, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, size, style(bold, italic), color);
		return (font.getBaseFont() != null? font: null);
	}

	private static int style(final boolean bold, final boolean italic){
		return (bold? Font.BOLD: Font.NORMAL) | (italic? Font.ITALIC: Font.NORMAL);
	}


	/**
	 * @param text	The text.
	 * @param size	The size, in points.
	 * @param bold	Whether it is bold.
	 * @param italic	Whether it is italic.
	 * @param mono	Whether it is monospaced.
	 * @param color	The color ({@code null} for black).
	 * @return	The text as chunks, each with the first font of the chain that has its characters.
	 */
	Phrase phrase(final String text, final float size, final boolean bold, final boolean italic, final boolean mono, final Color color){
		final FontSelector selector = selectors.computeIfAbsent(new Key(size, bold, italic, mono, color), this::createSelector);
		return selector.process(text);
	}

	private FontSelector createSelector(final Key key){
		final FontSelector selector = new StyledSelector(key.size(), style(key.bold(), key.italic()), key.color());
		for(final String family : (key.mono()? monoChain: bodyChain)){
			final Font font = load(family, key.size(), key.bold(), key.italic(), key.color());
			if(font != null)
				selector.addFont(font);
		}
		return selector;
	}

	/**
	 * @param text	The text.
	 * @param size	The size, in points.
	 * @param mono	Whether it is monospaced.
	 * @return	The width of the text (of the longest line), in points, in the first font of the chain.
	 */
	float width(final String text, final float size, final boolean mono){
		final List<String> chain = (mono? monoChain: bodyChain);
		final BaseFont base = (chain.isEmpty()? null: baseOf(chain.getFirst()));
		final BaseFont font = (base != null? base: defaultFont());

		float width = 0;
		for(final String line : StringUtils.splitPreserveAllTokens(text, '\n'))
			width = Math.max(width, font.getWidthPoint(line, size));
		return width;
	}

	/**
	 * @param text	The text.
	 * @param size	The size, in points.
	 * @param maxWidth	The width available, in points.
	 * @return	The text, cut with an ellipsis if it is wider than the width available.
	 */
	String truncate(final String text, final float size, final float maxWidth){
		if(width(text, size, false) <= maxWidth)
			return text;

		final String ellipsis = "...";
		int end = text.length();
		while(end > 0 && width(text.substring(0, end) + ellipsis, size, false) > maxWidth)
			end = text.offsetByCodePoints(end, -1);
		return text.substring(0, end) + ellipsis;
	}

}
