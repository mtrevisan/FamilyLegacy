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
package io.github.mtrevisan.familylegacy.v2.io;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammarValidator;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.Strings;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;


public class FLEFParser{

	private static final char TAG_OPEN_CURLY_BRACE = '{';
	private static final char TAG_CLOSE_CURLY_BRACE = '}';

	private static final String FIELD_HEADER = "header";
	private static final String FIELD_RECORDS = "records";

	private static final String TAG_VALUE_MULTILINE = "\"\"\"";


	private String text;
	private int length;
	private int position;


	public FLEFModel parse(final String text){
		this.text = text;
		this.length = text.length();

		final FLEFModel root = new FLEFModel();

		skipIgnored();

		while(!eof()){
			final FLEFRecord record = parseRecord();

			final String tag = record.getTag();
			if(Strings.CI.equals(FIELD_HEADER, tag))
				root.setHeader(record);
			else if(Strings.CI.equals(FIELD_RECORDS, tag)){
				for(final FLEFRecord child : record.getChildren())
					root.addRecord(child);
			}
			else
				throw new RuntimeException("Unexpected tag: " + tag + " at position " + position);

			skipIgnored();
		}

		return root;
	}

	private FLEFRecord parseRecord(){
		final String tag = readIdentifier()
			.toLowerCase(Locale.ROOT);
		final FLEFRecord record = FLEFRecord.createChildWithTag(tag);

		skipSpaces();

		// Block
		if(!eof()){
			final char chr = peek();
			// Child block with no scalar value
			if(chr == TAG_OPEN_CURLY_BRACE){
				consume(TAG_OPEN_CURLY_BRACE);

				parseBlock(record);

				return record;
			}

			// Multi-line string as value
			if(startsWithTripleQuotes()){
				record.setValue(readMultilineString());

				skipSpaces();

				if(!eof() && peek() == TAG_OPEN_CURLY_BRACE){
					consume(TAG_OPEN_CURLY_BRACE);

					parseBlock(record);
				}
				return record;
			}

			// Inline value until EOL or block '{'
			String value = readToEndOfLine();
			// If there is an inline id followed by a block
			if(!value.isEmpty() && value.charAt(value.length() - 1) == TAG_OPEN_CURLY_BRACE){
				value = value.substring(0, value.length() - 1)
					.trim();
				if(!value.isEmpty())
					record.setId(value);
				parseBlock(record);

				return record;
			}

			// Simple value
			if(!value.isEmpty())
				record.setValue(value);

			skipSpaces();

			if(!eof() && peek() == TAG_OPEN_CURLY_BRACE){
				consume(TAG_OPEN_CURLY_BRACE);

				parseBlock(record);
			}
		}

		return record;
	}

	private void parseBlock(final FLEFRecord parent){
		skipIgnored();

		while(!eof()){
			if(peek() == TAG_CLOSE_CURLY_BRACE){
				consume(TAG_CLOSE_CURLY_BRACE);

				return;
			}

			parent.addChild(parseRecord());

			skipIgnored();
		}

		throw error("Missing closing brace");
	}


	/**
	 * Reads an identifier.
	 */
	private String readIdentifier(){
		final int start = position;
		while(!eof()){
			final char c = peek();
			if(Character.isWhitespace(c) || c == TAG_OPEN_CURLY_BRACE || c == TAG_CLOSE_CURLY_BRACE)
				break;

			advance();
		}
		if(start == position)
			throw error("Expected identifier");

		return text.substring(start, position);
	}

	/**
	 * Reads everything until end-of-line.
	 */
	private String readToEndOfLine(){
		final int start = position;
		while(!eof()){
			final char c = peek();
			if(c == '\n' || c == '\r')
				break;

			advance();
		}
		final String res = text.substring(start, position);

		skipLineBreak();

		return res;
	}

	/**
	 * Reads a string delimited by triple quotes.
	 */
	private String readMultilineString(){
		expectTripleQuotes();
		advanceBy(3);

		// Find the closing triple quotes
		final int end = text.indexOf(TAG_VALUE_MULTILINE, position);
		if(end < 0)
			throw error("Unterminated multiline string");

		// Extract the raw content between the delimiters
		final String raw = text.substring(position, end);
		// Compute the leading whitespace (indent) of the line containing the closing delimiter
		int closeIndent = 0;
		int startOfCloseLine = end;
		// Find the start of the line that contains the closing quotes
		while(startOfCloseLine > 0 && text.charAt(startOfCloseLine - 1) != '\n')
			startOfCloseLine --;
		// The line content from startOfCloseLine to 'end' is the closing line (including the triple quotes)
		final String closeLine = text.substring(startOfCloseLine, end);
		closeIndent = countLeadingWhitespace(closeLine);

		// Move position past the closing delimiter and normalize the trailing newline
		position = end + 3;

		skipLineBreak();

		return normalizeTextBlock(raw, closeIndent);
	}

	/**
	 * Normalizes a raw text block content (enclosed in """) according to Java text block rules.
	 * <p>
	 * This method performs the following transformations:
	 * <ol>
	 *   <li>Normalizes line endings: all {@code \r\n} and {@code \r} are converted to {@code \n}.</li>
	 *   <li>Strips the leading newline if the content starts with one (the opening delimiter
	 *       is typically followed by a newline).</li>
	 *   <li>Determines the minimum indentation (incidental indentation) across all non-blank lines,
	 *       including the indentation of the closing delimiter line.</li>
	 *   <li>Removes this minimum indentation from every line.</li>
	 *   <li>Translates standard escape sequences: {@code \\}, {@code \"}, {@code \n}, {@code \r}, {@code \t}, {@code \s}.</li>
	 * </ol>
	 *
	 * @param raw the raw string read between the triple quotes (including the leading/trailing newlines)
	 * @param closeIndent  the number of leading whitespace characters on the closing delimiter line
	 * @return the normalized content, ready to be stored as the record value
	 */
	private String normalizeTextBlock(final String raw, final int closeIndent){
		// Step 1: Normalize line endings to LF only
		String normalized = raw.replace("\r\n", "\n")
			.replace('\r', '\n');

		// Step 2: Remove the leading newline if present (the opening """ is usually followed by \n)
		if(normalized.startsWith("\n"))
			normalized = normalized.substring(1);

		// Step 3: Split into lines (keep trailing empty lines)
		final String[] lines = normalized.split("\n", -1);

		// Step 4: Calculate the minimum indentation among all non‑blank content lines
		// and also consider the indentation of the closing delimiter line.
		int minIndent = closeIndent;
		for(final String line : lines){
			// Ignore lines that consist solely of whitespace when computing the minimum indent
			if(line.trim().isEmpty())
				continue;

			final int leading = countLeadingWhitespace(line);
			if(leading < minIndent)
				minIndent = leading;
		}
		// If the block consists only of blank lines, set minIndent to 0
		if(minIndent == Integer.MAX_VALUE)
			minIndent = 0;

		// Step 5: Strip the minimum indentation from each line
		final StringBuilder result = new StringBuilder();
		for(int i = 0; i < lines.length; i ++){
			final String line = lines[i];
			if(i > 0)
				result.append('\n');
			// Remove the minIndent only if the line is long enough
			if(!line.isEmpty() && line.length() >= minIndent)
				// Ensure we don't remove partial whitespace; strip exactly minIndent characters
				result.append(line, minIndent, line.length());
			else
				result.append(line);
		}

		// Step 6: Translate escape sequences (handle \", \\, \n, \r, \t, \s)
		return translateEscapes(result.toString());
	}

	/**
	 * Counts the number of leading whitespace characters (spaces and tabs).
	 *
	 * @param str the string to examine
	 * @return the number of leading whitespace characters
	 */
	private int countLeadingWhitespace(final String str){
		int count = 0;
		for(int i = 0; i < str.length(); i ++){
			char c = str.charAt(i);
			if(c == ' ' || c == '\t')
				count ++;
			else
				break;
		}
		return count;
	}

	/**
	 * Translates standard escape sequences found in a text block.
	 * <p>
	 * Supports: {@code \\} -> {@code \}, {@code \"} -> {@code "}, {@code \n} -> newline,
	 * {@code \r} -> carriage return, {@code \t} -> tab, {@code \s} -> space.
	 * Unrecognized escapes are left unchanged (e.g., {@code \x} remains {@code \x}).
	 *
	 * @param input the string potentially containing escape sequences
	 * @return the string with escapes translated
	 */
	private String translateEscapes(final String input){
		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < input.length(); i ++){
			final char c = input.charAt(i);
			if(c == '\\' && i + 1 < input.length()){
				char next = input.charAt(i + 1);
				switch(next){
					case 'n' -> {
						sb.append('\n');
						i ++;
					}
					case 'r' -> {
						sb.append('\r');
						i ++;
					}
					case 't' -> {
						sb.append('\t');
						i ++;
					}
					case 's' -> {
						sb.append(' ');
						i ++;
					}
					case '"' -> {
						sb.append('"');
						i ++;
					}
					case '\\' -> {
						sb.append('\\');
						i ++;
					}
					default ->
						// Unknown escape: leave both characters as-is
						sb.append(c);
				}
			}
			else
				sb.append(c);
		}
		return sb.toString();
	}


	private void skipIgnored(){
		while(!eof()){
			final char c = peek();
			if(Character.isWhitespace(c))
				advance();
			else if(c == '#' || (c == '/' && peek() == '/')){
				// Skip single-line comments (# or //)
				while(!eof() && peek() != '\n' && peek() != '\r')
					advance();
			}
			else
				break;
		}
	}

	/**
	 * Skip only spaces and tabs.
	 * Newlines are preserved because they terminate values.
	 */
	private void skipSpaces(){
		while(!eof()){
			final char c = peek();
			if(c != ' ' && c != '\t')
				break;

			advance();
		}
	}

	private void skipLineBreak(){
		if(!eof() && peek() == '\r')
			advance();
		if(!eof() && peek() == '\n')
			advance();
	}

	private boolean startsWithTripleQuotes(){
		return text.startsWith(TAG_VALUE_MULTILINE, position);
	}

	private void expectTripleQuotes(){
		if(!startsWithTripleQuotes())
			throw error("Expected triple quotes");
	}

	private boolean eof(){
		return (position >= length);
	}

	private char peek(){
		return text.charAt(position);
	}

	private void consume(char expected){
		if(eof() || peek() != expected)
			throw error("Expected '" + expected + "'");

		advance();
	}

	private void advance(){
		position ++;
	}

	private void advanceBy(final int count){
		position += count;
	}

	private IllegalArgumentException error(final String message){
		return new IllegalArgumentException(message + " at position " + position);
	}


	public static void main(final String[] args) throws IOException{
		//with errors
		String text2 = """
			header {
			  date 2026-07-31
			  submitter {
			    name Mario Rossi
			  }
			}
			records {
			  individual {
			    id @I1@
			    sex INVALID_VALUE
			    modification {
			      creation {
			        date 2026-07-31
			      }
			    }
			  }
			  event_participation {
			    id @EP1@
			    event {
			      event @E999@
			    }
			    participant {
			      individual @I1@
			    }
			    role CHILD
			    modification {
			      creation {
			        date 2026-07-31
			      }
			    }
			  }
			}
		""";

		//without errors
		String text = """
			header {
			  protocol {
			    name Family LEgacy Format
			    version 0.1.2
			  }
			  source {
			    system_id MyGenealogySoftware
			    name My Genealogy Software
			    version 1.0.0
			  }
			  date 2026-07-31
			  submitter {
			    name Mario Rossi
			    contact {
			      address mario.rossi@example.com
			      type personal
			      modification {
			        creation {
			          date 2026-07-31
			        }
			      }
			    }
			  }
			  scope Example family
			}
			records {
			  individual {
			    id @I1@
			      name {
			        part {
			          type given
			          value Mario
			        }
			        part {
			          type family
			          value ""\"
			            Rossi
			            Bianchi
			          ""\"
			        }
			      }
			    sex MALE
			    modification {
			      creation {
			        date 2026-07-31
			      }
			    }
			  }
			  note {
			    id @N1@
			    value Individuo presente nel registro di nascita.
			    modification {
			      creation {
			        date 2026-07-31
			      }
			    }
			  }
			  event {
			    id @E1@
			    type BIRTH
			    detail {
			      date {
			        value {
			          point {
			            single_date {
			              full_date {
			                value 1894-03-17
			                calendar gregorian
			              }
			            }
			          }
			        }
			      }
			      modification {
			        creation {
			          date 2026-07-31
			        }
			      }
			    }
			    modification {
			      creation {
			        date 2026-07-31
			      }
			    }
			  }
			  event_participation {
			    id @EP1@
			    event @E1@
			    participant {
			      individual @I1@
			    }
			    role CHILD
			    modification {
			      creation {
			        date 2026-07-31
			      }
			    }
			  }
			}
			""";

		final Path path = Paths.get("src/main/resources/gedg/flef_0.1.2.gedg");
		final FLEFGrammar grammar = FLEFGrammarParser.parse(path);
		for(final String warning : grammar.getParseWarnings())
			System.out.println(warning);


		System.out.println();


		final FLEFGrammarValidator.ValidationResult validationResult = FLEFGrammarValidator.validate(grammar);
		System.out.println("Grammar check (errors):");
		for(final String error : validationResult.errors())
			System.out.println(error);
		System.out.println("Grammar check (warnings):");
		for(final String warning : validationResult.warnings())
			System.out.println(warning);


		System.out.println();


		final FLEFParser parser = new FLEFParser();
		final FLEFModel root = parser.parse(text);

		System.out.println(root);


		System.out.println();


		final FLEFWriter writer = FLEFWriter.create();
		System.out.println(writer.writeToString(root));


		System.out.println();


		final FLEFValidator validator = new FLEFValidator(grammar);
		final List<String> errorsSchema = validator.validateSchema(root);
		System.out.println("Schema check:");
		for(final String error : errorsSchema)
			System.out.println(error);


		System.out.println();


		final List<String> errorsIntegrity = validator.validateIntegrity(root);
		System.out.println("Integrity check:");
		for(final String error : errorsIntegrity)
			System.out.println(error);
	}

}
