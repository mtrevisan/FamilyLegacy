package io.github.mtrevisan.familylegacy.v2.io;

import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


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
			if(FIELD_HEADER.equalsIgnoreCase(tag))
				root.setHeader(record);
			else if(FIELD_RECORDS.equalsIgnoreCase(tag)){
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
		final String tag = readIdentifier();

		skipSpaces();

		final FLEFRecord record = FLEFRecord.createChild(tag);

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
		final int end = text.indexOf(TAG_VALUE_MULTILINE, position);
		if(end < 0)
			throw error("Unterminated multiline string");

		final String value = text.substring(position, end);
		position = end + 3;

		skipLineBreak();

		return value;
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
			    version 0.1.1
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

		final Path path = Paths.get("src/main/resources/gedg/flef_0.1.1.gedg");
		final FLEFGrammar grammar = FLEFGrammarParser.parse(path);
		for(final String warning : grammar.getParseWarnings())
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
