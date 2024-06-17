package io.github.mtrevisan.familylegacy.flef.gedcom;

import java.io.IOException;


public class GedcomFileParser{

	protected GedcomNode root;

	private GedcomGrammar grammar;


	public final void load(final String grammarFile, final String dataFile) throws GedcomGrammarParseException, GedcomParseException{
		parse(grammarFile);

		populate(dataFile);
	}

	public void parse(final String grammarFile) throws GedcomGrammarParseException{
		grammar = GedcomGrammar.create(grammarFile);
	}


	public void populate(final String dataFile) throws GedcomParseException{
		this.root = GedcomParser.parse(dataFile, grammar);
	}


	public static void main(final String[] args) throws IOException, GedcomGrammarParseException, GedcomParseException{
		final GedcomFileParser parser = new GedcomFileParser();
		parser.parse("/gedg/gedcom_5.5.1.tcgb.gedg");
		parser.populate("src/main/resources/ged/large.ged");

		parser.root.children
			.forEach(child -> System.out.println(child));
	}

}
