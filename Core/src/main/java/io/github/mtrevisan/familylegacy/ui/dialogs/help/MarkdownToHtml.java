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
package io.github.mtrevisan.familylegacy.ui.dialogs.help;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * A small Markdown to HTML converter for the help pages, producing the HTML 3.2 (plus a little CSS) that the Swing editor pane
 * understands.
 * <p>
 * Supported: headings ({@code #} to {@code ####}), paragraphs, bullet and numbered lists (nested by indentation), fenced code
 * blocks, block quotes, horizontal rules, tables, and inline {@code code}, <b>bold</b>, <i>italic</i> and links. Anything else is
 * shown as text. Every heading gets an anchor named as its {@linkplain #slug(String) slug}, so that a link can point to it.
 */
public final class MarkdownToHtml{

	private static final String STYLE = """
		body { font-family: SansSerif; font-size: 13px; margin: 14px; }
		h1 { font-size: 22px; margin-top: 4px; margin-bottom: 8px; }
		h2 { font-size: 17px; margin-top: 18px; margin-bottom: 6px; }
		h3 { font-size: 14px; margin-top: 14px; margin-bottom: 4px; }
		h4 { font-size: 13px; margin-top: 10px; margin-bottom: 4px; }
		p, li { margin-top: 3px; margin-bottom: 3px; }
		code { font-family: Monospaced; font-size: 13px; background-color: #eeeeee; }
		pre { font-family: Monospaced; font-size: 13px; background-color: #eeeeee; margin: 6px; }
		blockquote { margin-left: 18px; color: #555555; }
		th { background-color: #e4e4e4; }
		""";


	private MarkdownToHtml(){}


	/** @return	the HTML document (with its style) of the page. */
	public static String document(final String markdown){
		return "<html><head><style>" + STYLE + "</style></head><body>" + convert(markdown) + "</body></html>";
	}

	/** @return	the HTML of the body of the page. */
	public static String convert(final String markdown){
		final StringBuilder html = new StringBuilder();
		blocks(markdown.lines().toList(), html);
		return html.toString();
	}

	/** @return	the text of the first level 1 heading, or {@code null} if there is none. */
	public static String title(final String markdown){
		for(final String line : (Iterable<String>)markdown.lines()::iterator){
			final int level = headingLevel(line);
			if(level == 1)
				return line.substring(2).trim();
		}
		return null;
	}

	/** @return	the slugs of every heading of the page, in order. */
	public static List<String> anchors(final String markdown){
		final List<String> anchors = new ArrayList<>();
		for(final String line : (Iterable<String>)markdown.lines()::iterator){
			final int level = headingLevel(line);
			if(level > 0)
				anchors.add(slug(line.substring(level + 1).trim()));
		}
		return anchors;
	}

	/** @return	the name of the anchor of a heading: lower case letters and digits, the spaces and hyphens become hyphens. */
	public static String slug(final String heading){
		final StringBuilder slug = new StringBuilder();
		for(final char c : plainText(heading).toLowerCase(Locale.ROOT).toCharArray())
			if(Character.isLetterOrDigit(c))
				slug.append(c);
			else if(c == ' ' || c == '-')
				slug.append('-');
		return slug.toString();
	}

	/** Every {@code [text](target)} of the page, in order (the images are not links). */
	public static List<String> links(final String markdown){
		final List<String> links = new ArrayList<>();
		boolean fenced = false;
		for(final String line : (Iterable<String>)markdown.lines()::iterator){
			if(line.stripLeading().startsWith("```")){
				fenced = !fenced;
				continue;
			}
			if(fenced)
				continue;

			final String text = withoutInlineCode(line);
			int from = 0;
			while(true){
				final int close = text.indexOf("](", from);
				if(close < 0)
					break;
				final int end = text.indexOf(')', close + 2);
				if(end < 0)
					break;
				if(text.lastIndexOf('[', close) >= 0)
					links.add(text.substring(close + 2, end).trim());
				from = end + 1;
			}
		}
		return links;
	}


	/** The line without the text between backticks, where a {@code [x](y)} is an example and not a link. */
	private static String withoutInlineCode(final String line){
		final StringBuilder text = new StringBuilder();
		boolean code = false;
		for(final char c : line.toCharArray()){
			if(c == '`')
				code = !code;
			else if(!code)
				text.append(c);
		}
		return text.toString();
	}


	/* ======================================================================
	 *                          Blocks
	 * ====================================================================== */

	private static void blocks(final List<String> lines, final StringBuilder html){
		int i = 0;
		while(i < lines.size()){
			final String line = lines.get(i);
			final String trimmed = line.strip();

			if(trimmed.isEmpty()){
				i ++;
			}
			else if(trimmed.startsWith("```")){
				i = codeBlock(lines, i, html);
			}
			else if(headingLevel(line) > 0){
				final int level = headingLevel(line);
				final String text = line.substring(level + 1).trim();
				html.append("<h").append(level).append("><a name=\"").append(slug(text)).append("\"></a>")
					.append(inline(text)).append("</h").append(level).append(">\n");
				i ++;
			}
			else if(isRule(trimmed)){
				html.append("<hr>\n");
				i ++;
			}
			else if(trimmed.startsWith(">")){
				i = quote(lines, i, html);
			}
			else if(isTableStart(lines, i)){
				i = table(lines, i, html);
			}
			else if(listItem(line) != null){
				i = list(lines, i, html);
			}
			else{
				i = paragraph(lines, i, html);
			}
		}
	}

	private static int codeBlock(final List<String> lines, final int start, final StringBuilder html){
		html.append("<pre>");
		int i = start + 1;
		while(i < lines.size() && !lines.get(i).strip().startsWith("```")){
			html.append(escape(lines.get(i))).append('\n');
			i ++;
		}
		html.append("</pre>\n");
		return i + 1;
	}

	private static int quote(final List<String> lines, final int start, final StringBuilder html){
		final List<String> inner = new ArrayList<>();
		int i = start;
		while(i < lines.size() && lines.get(i).strip().startsWith(">")){
			final String text = lines.get(i).strip().substring(1);
			inner.add(text.startsWith(StringUtils.SPACE)? text.substring(1): text);
			i ++;
		}
		html.append("<blockquote>");
		blocks(inner, html);
		html.append("</blockquote>\n");
		return i;
	}

	private static int paragraph(final List<String> lines, final int start, final StringBuilder html){
		final StringBuilder text = new StringBuilder();
		int i = start;
		while(i < lines.size()){
			final String line = lines.get(i);
			final String trimmed = line.strip();
			if(trimmed.isEmpty() || (i > start && startsBlock(lines, i)))
				break;

			if(!text.isEmpty())
				text.append(' ');
			text.append(trimmed);
			i ++;
		}
		html.append("<p>").append(inline(text.toString())).append("</p>\n");
		return i;
	}

	private static boolean startsBlock(final List<String> lines, final int index){
		final String line = lines.get(index);
		final String trimmed = line.strip();
		return (trimmed.startsWith("```") || headingLevel(line) > 0 || isRule(trimmed) || trimmed.startsWith(">")
			|| listItem(line) != null || isTableStart(lines, index));
	}

	private static int headingLevel(final String line){
		int level = 0;
		while(level < line.length() && line.charAt(level) == '#')
			level ++;
		return (level >= 1 && level <= 4 && line.length() > level && line.charAt(level) == ' '? level: 0);
	}

	private static boolean isRule(final String trimmed){
		return (trimmed.length() >= 3 && (StringUtils.containsOnly(trimmed, '-') || StringUtils.containsOnly(trimmed, '*')));
	}


	/* ======================================================================
	 *                          Lists
	 * ====================================================================== */

	/** A list line: its indentation, whether it is numbered and its text. */
	private record ListItem(int indent, boolean ordered, String text){}

	private static ListItem listItem(final String line){
		final String stripped = line.stripLeading();
		final int indent = line.length() - stripped.length();
		if((stripped.startsWith("- ") || stripped.startsWith("* ")) && !isRule(stripped.strip()))
			return new ListItem(indent, false, stripped.substring(2).trim());

		int digits = 0;
		while(digits < stripped.length() && Character.isDigit(stripped.charAt(digits)))
			digits ++;
		if(digits > 0 && stripped.startsWith(". ", digits))
			return new ListItem(indent, true, stripped.substring(digits + 2).trim());
		return null;
	}

	private static int list(final List<String> lines, final int start, final StringBuilder html){
		//the open lists, each with the indentation of its items
		final List<Integer> indents = new ArrayList<>();
		final List<Boolean> ordered = new ArrayList<>();
		int i = start;
		while(i < lines.size()){
			final ListItem item = listItem(lines.get(i));
			if(item == null){
				//a continuation of the last item (indented text)
				final String line = lines.get(i);
				if(!line.isBlank() && line.startsWith("  ") && !indents.isEmpty() && !startsBlock(lines, i)){
					html.append(' ').append(inline(line.strip()));
					i ++;
					continue;
				}
				break;
			}

			if(indents.isEmpty()){
				open(item, indents, ordered, html);
			}
			else{
				while(!indents.isEmpty() && item.indent() < indents.getLast()){
					html.append("</li>");
					close(indents, ordered, html);
				}
				if(indents.isEmpty())
					open(item, indents, ordered, html);
				else if(item.indent() > indents.getLast())
					open(item, indents, ordered, html);
				else
					html.append("</li>\n");
			}
			html.append("<li>").append(inline(item.text()));
			i ++;
		}
		while(!indents.isEmpty()){
			html.append("</li>");
			close(indents, ordered, html);
		}
		return i;
	}

	private static void open(final ListItem item, final List<Integer> indents, final List<Boolean> ordered, final StringBuilder html){
		indents.add(item.indent());
		ordered.add(item.ordered());
		html.append(item.ordered()? "<ol>\n": "<ul>\n");
	}

	private static void close(final List<Integer> indents, final List<Boolean> ordered, final StringBuilder html){
		indents.removeLast();
		html.append(ordered.removeLast()? "</ol>\n": "</ul>\n");
	}


	/* ======================================================================
	 *                          Tables
	 * ====================================================================== */

	private static boolean isTableStart(final List<String> lines, final int index){
		if(index + 1 >= lines.size() || !lines.get(index).strip().startsWith("|"))
			return false;

		final String separator = lines.get(index + 1).strip();
		return (separator.startsWith("|") && separator.contains("-") && StringUtils.containsOnly(separator, "|-: "));
	}

	private static int table(final List<String> lines, final int start, final StringBuilder html){
		html.append("<table border=\"1\" cellpadding=\"4\" cellspacing=\"0\">\n");
		row(lines.get(start), "th", html);
		int i = start + 2;
		while(i < lines.size() && lines.get(i).strip().startsWith("|")){
			row(lines.get(i), "td", html);
			i ++;
		}
		html.append("</table>\n");
		return i;
	}

	private static void row(final String line, final String cellTag, final StringBuilder html){
		String content = line.strip();
		content = content.substring(1);
		if(content.endsWith("|"))
			content = content.substring(0, content.length() - 1);

		html.append("<tr>");
		//the cells are separated by '|', unless escaped
		final StringBuilder cell = new StringBuilder();
		for(int i = 0; i <= content.length(); i ++){
			final char c = (i < content.length()? content.charAt(i): '|');
			if(c == '\\' && i + 1 < content.length() && content.charAt(i + 1) == '|'){
				cell.append('|');
				i ++;
			}
			else if(c == '|'){
				html.append('<').append(cellTag).append('>').append(inline(cell.toString().trim()))
					.append("</").append(cellTag).append('>');
				cell.setLength(0);
			}
			else
				cell.append(c);
		}
		html.append("</tr>\n");
	}


	/* ======================================================================
	 *                          Inline
	 * ====================================================================== */

	private static String inline(final String text){
		final StringBuilder out = new StringBuilder();
		final int length = text.length();
		int i = 0;
		while(i < length){
			final char c = text.charAt(i);

			if(c == '\\' && i + 1 < length && "\\`*[]()#|_-.!<>".indexOf(text.charAt(i + 1)) >= 0){
				out.append(escape(String.valueOf(text.charAt(i + 1))));
				i += 2;
				continue;
			}
			if(c == '`'){
				final int end = text.indexOf('`', i + 1);
				if(end > i){
					out.append("<code>").append(escape(text.substring(i + 1, end))).append("</code>");
					i = end + 1;
					continue;
				}
			}
			if(c == '*'){
				final boolean bold = text.startsWith("**", i);
				final String mark = (bold? "**": "*");
				final int end = closingMark(text, mark, i + mark.length());
				if(end > i + mark.length()){
					final String tag = (bold? "b": "i");
					out.append('<').append(tag).append('>').append(inline(text.substring(i + mark.length(), end)))
						.append("</").append(tag).append('>');
					i = end + mark.length();
					continue;
				}
			}
			if(c == '['){
				final int close = text.indexOf("](", i + 1);
				final int end = (close > 0? text.indexOf(')', close + 2): -1);
				if(end > 0){
					out.append("<a href=\"").append(escape(text.substring(close + 2, end).trim()).replace("\"", "&quot;")).append("\">")
						.append(inline(text.substring(i + 1, close))).append("</a>");
					i = end + 1;
					continue;
				}
			}

			out.append(escape(String.valueOf(c)));
			i ++;
		}
		return out.toString();
	}

	/**
	 * @return	where the emphasis opened just before {@code from} ends, or -1: the text just after the opening mark and just before
	 * 	the closing one must not be a space (so that {@code 2 * 3 * 4} is not emphasis).
	 */
	private static int closingMark(final String text, final String mark, final int from){
		if(from >= text.length() || Character.isWhitespace(text.charAt(from)))
			return -1;

		int end = text.indexOf(mark, from);
		while(end > 0 && Character.isWhitespace(text.charAt(end - 1)))
			end = text.indexOf(mark, end + mark.length());
		return end;
	}

	/** The text without the Markdown marks of the inline elements. */
	private static String plainText(final String text){
		return StringUtils.replaceChars(text, "`*", null);
	}

	private static String escape(final String text){
		return StringUtils.replaceEach(text, new String[]{"&", "<", ">"}, new String[]{"&amp;", "&lt;", "&gt;"});
	}

}
