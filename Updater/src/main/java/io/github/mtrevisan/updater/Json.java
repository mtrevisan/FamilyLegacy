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
package io.github.mtrevisan.updater;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * A minimal JSON reader, enough for the answer of the GitHub API (so that the module needs no library).
 * Objects are {@code Map<String, Object>}, arrays {@code List<Object>}, numbers {@code Double}, plus {@code String}, {@code Boolean}
 * and {@code null}.
 */
final class Json{

	/** The answer of GitHub is nested a few levels; more is not what is expected (and would exhaust the stack). */
	private static final int MAX_DEPTH = 64;

	private final String text;
	private int position;
	private int depth;


	private Json(final String text){
		this.text = text;
	}

	/**
	 * @param text	The JSON document.
	 * @return	The value it holds.
	 * @throws IllegalArgumentException	If the text is not valid JSON.
	 */
	static Object parse(final String text){
		final Json reader = new Json(text);
		final Object value = reader.readValue();
		reader.skipWhitespace();
		if(reader.position != text.length())
			throw reader.error("unexpected content after the value");
		return value;
	}

	@SuppressWarnings("unchecked")
	static Map<String, Object> asObject(final Object value){
		return (value instanceof Map<?, ?>? (Map<String, Object>)value: Map.of());
	}

	@SuppressWarnings("unchecked")
	static List<Object> asArray(final Object value){
		return (value instanceof List<?>? (List<Object>)value: List.of());
	}

	static String asString(final Object value){
		return (value instanceof final String s? s: null);
	}


	private Object readValue(){
		skipWhitespace();
		if(position >= text.length())
			throw error("unexpected end");

		final char c = text.charAt(position);
		return switch(c){
			case '{' -> readObject();
			case '[' -> readArray();
			case '"' -> readString();
			case 't' -> readLiteral("true", Boolean.TRUE);
			case 'f' -> readLiteral("false", Boolean.FALSE);
			case 'n' -> readLiteral("null", null);
			default -> readNumber();
		};
	}

	private Map<String, Object> readObject(){
		enter();
		final Map<String, Object> result = new LinkedHashMap<>();
		position ++;
		skipWhitespace();
		if(peek() == '}'){
			position ++;
			depth --;
			return result;
		}
		while(true){
			skipWhitespace();
			if(peek() != '"')
				throw error("a key was expected");
			final String key = readString();
			skipWhitespace();
			expect(':');
			result.put(key, readValue());
			skipWhitespace();
			if(peek() == ','){
				position ++;
				continue;
			}
			expect('}');
			depth --;
			return result;
		}
	}

	private List<Object> readArray(){
		enter();
		final List<Object> result = new ArrayList<>();
		position ++;
		skipWhitespace();
		if(peek() == ']'){
			position ++;
			depth --;
			return result;
		}
		while(true){
			result.add(readValue());
			skipWhitespace();
			if(peek() == ','){
				position ++;
				continue;
			}
			expect(']');
			depth --;
			return result;
		}
	}

	private void enter(){
		if(++ depth > MAX_DEPTH)
			throw error("nested too deeply");
	}

	private String readString(){
		final StringBuilder sb = new StringBuilder();
		position ++;
		while(true){
			if(position >= text.length())
				throw error("unterminated string");
			final char c = text.charAt(position ++);
			if(c == '"')
				return sb.toString();
			if(c < ' ')
				throw error("control character in a string");
			if(c != '\\'){
				sb.append(c);
				continue;
			}

			if(position >= text.length())
				throw error("unterminated escape");
			final char escaped = text.charAt(position ++);
			switch(escaped){
				case '"', '\\', '/' -> sb.append(escaped);
				case 'b' -> sb.append('\b');
				case 'f' -> sb.append('\f');
				case 'n' -> sb.append('\n');
				case 'r' -> sb.append('\r');
				case 't' -> sb.append('\t');
				case 'u' -> {
					if(position + 4 > text.length())
						throw error("bad unicode escape");
					try{
						sb.append((char)Integer.parseInt(text.substring(position, position + 4), 16));
					}
					catch(final NumberFormatException e){
						throw error("bad unicode escape");
					}
					position += 4;
				}
				default -> throw error("bad escape");
			}
		}
	}

	private Object readLiteral(final String literal, final Object value){
		if(!text.startsWith(literal, position))
			throw error("unknown value");
		position += literal.length();
		return value;
	}

	private Double readNumber(){
		final int start = position;
		while(position < text.length() && "+-0123456789.eE".indexOf(text.charAt(position)) >= 0)
			position ++;
		if(start == position)
			throw error("unknown value");
		try{
			return Double.valueOf(text.substring(start, position));
		}
		catch(final NumberFormatException e){
			throw error("bad number");
		}
	}

	private void skipWhitespace(){
		while(position < text.length() && Character.isWhitespace(text.charAt(position)))
			position ++;
	}

	private char peek(){
		return (position < text.length()? text.charAt(position): '\0');
	}

	private void expect(final char c){
		if(peek() != c)
			throw error("'" + c + "' was expected");
		position ++;
	}

	private IllegalArgumentException error(final String message){
		return new IllegalArgumentException("Invalid JSON at " + position + ": " + message);
	}

}
