package io.github.mtrevisan.familylegacy.flef.gedcom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GedcomFileParser{

	private GedcomGrammar grammar;
	private GedcomNode root;

	private final Map<String, List<GedcomNode>> tables = new HashMap<>();


	public final void load(final String grammarFile, final String dataFile) throws GedcomGrammarParseException, GedcomParseException{
		parse(grammarFile);

		populate(dataFile);
	}

	public void parse(final String grammarFile) throws GedcomGrammarParseException{
		grammar = GedcomGrammar.create(grammarFile);
	}


	public void populate(final String dataFile) throws GedcomParseException{
		this.root = GedcomParser.parse(dataFile, grammar);

		final List<GedcomNode> children = root.children;
		for(int i = 0, length = children.size(); i < length; i++){
			final GedcomNode element = children.get(i);

			final String tag = element.getTag();
			final List<GedcomNode> tagTables = tables.computeIfAbsent(tag, k -> new ArrayList<>());
			tagTables.add(element);
		}
	}


	public static void main(final String[] args) throws IOException, GedcomGrammarParseException, GedcomParseException{
		final GedcomFileParser parser = new GedcomFileParser();
		parser.parse("/gedg/gedcom_5.5.1.tcgb.gedg");
		parser.populate("src/main/resources/ged/large.ged");

		parser.root.children
			.forEach(child -> System.out.println(child));
	}

}
