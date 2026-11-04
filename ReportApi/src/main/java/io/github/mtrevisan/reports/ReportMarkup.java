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

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The text of a report as the renderers that write markup (XHTML, HTML) need it: the escaping, the inline Markdown, the protocol
 * links.
 */
public final class ReportMarkup{

	private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+?)`");
	private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*(?!\\*)([^*]+?)(?<!\\*)\\*(?!\\*)");
	private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
	/** A record reference such as {@code [I12]} inside the text of a heading. */
	private static final Pattern RECORD_ID = Pattern.compile("\\[([A-Za-z]+\\d+)\\]");
	/** The protocol's internal link {@code [text](@id@)}. */
	private static final Pattern INTERNAL_LINK = Pattern.compile("\\[([^\\]]+?)\\]\\(@([A-Za-z]+\\d+)@\\)");


	private ReportMarkup(){}


	/**
	 * Escapes the text for XML and HTML, dropping the characters that XML cannot carry.
	 *
	 * @param text	The text (can be {@code null}).
	 * @return	The escaped text, empty for {@code null}.
	 */
	public static String escape(final String text){
		if(text == null)
			return "";

		final StringBuilder sb = new StringBuilder(text.length() + 16);
		text.codePoints()
			.filter(cp -> cp == 0x9 || cp == 0xA || cp == 0xD || cp >= 0x20 && cp <= 0xD7FF || cp >= 0xE000 && cp <= 0xFFFD
				|| cp >= 0x10000 && cp <= 0x10FFFF)
			.forEach(cp -> {
				switch(cp){
					case '&' -> sb.append("&amp;");
					case '<' -> sb.append("&lt;");
					case '>' -> sb.append("&gt;");
					case '"' -> sb.append("&quot;");
					default -> sb.appendCodePoint(cp);
				}
			});
		return sb.toString();
	}

	/**
	 * Escapes the text, then turns the internal links and the inline Markdown ({@code **bold**}, {@code *italic*}, {@code `code`}) into
	 * markup.
	 *
	 * @param text	The text (can be {@code null}).
	 * @param linkTarget	Gives the address of the page that has the record whose identifier is passed ({@code "chapter-002.html#I1"});
	 * 	{@code null} if there is none, and then the link is only its text.
	 * @return	The markup.
	 */
	public static String inline(final String text, final UnaryOperator<String> linkTarget){
		final Matcher matcher = INTERNAL_LINK.matcher(escape(text));
		final StringBuilder sb = new StringBuilder();
		while(matcher.find()){
			final String target = linkTarget.apply(matcher.group(2));
			matcher.appendReplacement(sb, Matcher.quoteReplacement(target != null
				? "<a href=\"" + target + "\">" + matcher.group(1) + "</a>"
				: matcher.group(1)));
		}
		matcher.appendTail(sb);

		String result = BOLD.matcher(sb).replaceAll("<b>$1</b>");
		result = ITALIC.matcher(result).replaceAll("<i>$1</i>");
		return INLINE_CODE.matcher(result).replaceAll("<code>$1</code>");
	}

	/**
	 * @param text	The text (can be {@code null}).
	 * @return	The text without the Markdown and link syntax, for the titles.
	 */
	public static String plain(final String text){
		if(text == null)
			return "";

		return INTERNAL_LINK.matcher(text).replaceAll("$1")
			.replace("**", "")
			.replace("*", "")
			.replace("`", "")
			.strip();
	}

	/**
	 * @param text	The text of a heading (can be {@code null}).
	 * @return	The record references ({@code [I12]}) it contains, in order and without duplicates.
	 */
	public static List<String> recordIds(final String text){
		final List<String> ids = new ArrayList<>();
		final Matcher matcher = RECORD_ID.matcher(text == null? "": text);
		while(matcher.find())
			if(!ids.contains(matcher.group(1)))
				ids.add(matcher.group(1));
		return ids;
	}

}
