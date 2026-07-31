package io.github.mtrevisan.familylegacy.v2.io.grammar;


/**
 * Thrown on any lexical or syntactic error, with the 1-based source line number when known.
 */
public final class FLEFGrammarParseException extends RuntimeException{

	private final int line;

	public FLEFGrammarParseException(final String message, final int line){
		super("Line " + line + ": " + message);

		this.line = line;
	}

	public int getLine(){
		return line;
	}

}
