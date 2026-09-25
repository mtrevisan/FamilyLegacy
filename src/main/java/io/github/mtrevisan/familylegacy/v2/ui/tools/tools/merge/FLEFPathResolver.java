package io.github.mtrevisan.familylegacy.v2.ui.tools.tools.merge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;


/**
 * Parses a FLEF fragment up to a given position and returns the context
 * of the caret: the innermost open block, the full path of enclosing
 * blocks, the identifier prefix being typed, and the indentation of the
 * current line.
 * <p>
 * The scanner skips line comments ({@code //} and {@code #}), block
 * comments ({@code /* ... *\/}), and multiline strings
 * ({@code """ ... """}), so braces inside them do not affect the block
 * depth. It is a best-effort reader: it never throws, it just returns a
 * best-guess context.
 */
public final class FLEFPathResolver{


	private FLEFPathResolver(){}


	/**
	 * Result of the scan.
	 *
	 * @param path       the tags of the enclosing blocks, outermost first;
	 *                   empty at the top level
	 * @param parentTag  the innermost enclosing block, or {@code null}
	 * @param prefix     the identifier being typed at the caret, or an
	 *                   empty string
	 * @param indent     the leading whitespace of the current line
	 * @param suppressed {@code true} when the caret is inside a string or
	 *                   a comment, in which case no suggestion is offered
	 */
	public record Context(List<String> path, String parentTag, String prefix, String indent, boolean suppressed){}


	public static Context resolve(final String text, final int caret){
		if(text == null || caret < 0 || caret > text.length())
			return new Context(List.of(), null, "", "", true);

		final Deque<String> stack = new ArrayDeque<>();
		final StringBuilder token = new StringBuilder();
		boolean inLineComment = false;
		boolean inBlockComment = false;
		boolean inMultilineString = false;
		int i = 0;
		while(i < caret){
			final char c = text.charAt(i);

			// Multiline string: everything until the closing """ is opaque.
			if(inMultilineString){
				if(c == '"' && i + 2 < caret && text.charAt(i + 1) == '"' && text.charAt(i + 2) == '"'){
					inMultilineString = false;
					i += 3;

					continue;
				}
				i ++;

				continue;
			}
			if(inBlockComment){
				if(c == '*' && i + 1 < caret && text.charAt(i + 1) == '/'){
					inBlockComment = false;
					i += 2;

					continue;
				}
				i ++;

				continue;
			}
			if(inLineComment){
				if(c == '\n')
					inLineComment = false;
				i ++;

				continue;
			}

			// Start of a comment or a multiline string.
			if(c == '/' && i + 1 < caret){
				final char n = text.charAt(i + 1);
				if(n == '/'){
					inLineComment = true;
					i += 2;

					continue;
				}
				if(n == '*'){
					inBlockComment = true;
					i += 2;

					continue;
				}
			}
			if(c == '#'){
				inLineComment = true;
				i ++;

				continue;
			}
			if(c == '"' && i + 2 < caret && text.charAt(i + 1) == '"' && text.charAt(i + 2) == '"'){
				inMultilineString = true;
				i += 3;

				continue;
			}

			if(c == '{'){
				final String tag = token.toString().trim();
				if(!tag.isEmpty())
					stack.push(lastSegment(tag));
				token.setLength(0);
			}
			else if(c == '}'){
				if(!stack.isEmpty())
					stack.pop();
				token.setLength(0);
			}
			else if(Character.isWhitespace(c))
				token.setLength(0);
			else
				token.append(c);

			i ++;
		}

		// The prefix is the run of identifier characters immediately
		// before the caret, but only if it is not a completed token
		// (a completed token is followed by whitespace or a brace).
		final String prefix = currentPrefix(text, caret);

		// The indent is the leading whitespace of the line containing
		// the caret.
		final String indent = currentIndent(text, caret);

		final List<String> path = new ArrayList<>(stack);
		// Deque iterates from top of stack to bottom; reverse to get
		// outermost first.
		Collections.reverse(path);

		final String parentTag = (path.isEmpty()? null: path.get(path.size() - 1));
		final boolean suppressed = inMultilineString || inBlockComment || inLineComment;

		return new Context(path, parentTag, prefix, indent, suppressed);
	}


	/**
	 * Extracts the trailing segment of a dotted tag path (e.g.
	 * {@code date.value.point.full_date} returns {@code full_date}).
	 */
	private static String lastSegment(final String tag){
		final int dot = tag.lastIndexOf('.');
		return (dot >= 0? tag.substring(dot + 1): tag);
	}

	private static String currentPrefix(final String text, final int caret){
		int i = caret;
		while(i > 0){
			final char c = text.charAt(i - 1);
			if(!Character.isLetterOrDigit(c) && c != '_')
				break;

			i --;
		}
		return text.substring(i, caret);
	}

	private static String currentIndent(final String text, final int caret){
		int lineStart = caret;
		while(lineStart > 0 && text.charAt(lineStart - 1) != '\n')
			lineStart --;

		int i = lineStart;
		while(i < text.length() && i < caret){
			final char c = text.charAt(i);
			if(c != ' ' && c != '\t')
				break;

			i ++;
		}
		return text.substring(lineStart, i);
	}

}
