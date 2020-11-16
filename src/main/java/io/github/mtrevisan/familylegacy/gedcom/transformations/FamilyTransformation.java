package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.Protocol;

import java.util.List;


public class FamilyTransformation implements Transformation<Gedcom, Flef>{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> families = origin.getFamilies();
		for(final GedcomNode family : families)
			familyRecordTo(family, destination);
	}

	private void familyRecordTo(final GedcomNode family, final Flef destination){
		final GedcomNode destinationFamily = transformerTo.create("FAMILY")
			.withID(family.getID())
			.addChildReference("SPOUSE1", transformerTo.extractSubStructure(family, "HUSB")
				.getID())
			.addChildReference("SPOUSE2", transformerTo.extractSubStructure(family, "WIFE")
				.getID());
		final List<GedcomNode> children = family.getChildrenWithTag("CHIL");
		for(final GedcomNode child : children)
			destinationFamily.addChildReference("CHILD", child.getID());
		transformerTo.noteTo(family, destinationFamily, destination);
		transformerTo.sourceCitationTo(family, destinationFamily, destination);
		transformerTo.documentTo(family, destinationFamily, destination);
		transformerTo.eventTo(family, destinationFamily, destination, "ANUL", "ANNULMENT");
		transformerTo.eventTo(family, destinationFamily, destination, "CENS", "CENSUS");
		transformerTo.eventTo(family, destinationFamily, destination, "DIV", "DIVORCE");
		transformerTo.eventTo(family, destinationFamily, destination, "DIVF", "DIVORCE_FILED");
		transformerTo.eventTo(family, destinationFamily, destination, "ENGA", "ENGAGEMENT");
		transformerTo.eventTo(family, destinationFamily, destination, "MARB", "MARRIAGE_BANN");
		transformerTo.eventTo(family, destinationFamily, destination, "MARC", "MARRIAGE_CONTRACT");
		transformerTo.eventTo(family, destinationFamily, destination, "MARR", "MARRIAGE");
		transformerTo.eventTo(family, destinationFamily, destination, "MARL", "MARRIAGE_LICENCE");
		transformerTo.eventTo(family, destinationFamily, destination, "MARS", "MARRIAGE_SETTLEMENT");
		transformerTo.eventTo(family, destinationFamily, destination, "RESI", "RESIDENCE");
		transformerTo.eventTo(family, destinationFamily, destination, "EVEN", "EVENT");
		destinationFamily.addChildValue("RESTRICTION", transformerTo.extractSubStructure(family, "RESN")
			.getValue());

		destination.addFamily(destinationFamily);
	}


	@Override
	public void from(final Flef origin, final Gedcom destination) throws GedcomGrammarParseException{
		final List<GedcomNode> families = origin.getFamilies();
		for(final GedcomNode family : families)
			familyRecordFrom(family, origin, destination);
	}

	private void familyRecordFrom(final GedcomNode family, final Flef origin, final Gedcom destination) throws GedcomGrammarParseException{
		final GedcomNode destinationFamily = transformerFrom.create("FAM")
			.withID(family.getID())
			.addChildValue("RESN", transformerFrom.extractSubStructure(family, "RESTRICTION")
				.getValue());
		final List<GedcomNode> events = family.getChildrenWithTag("EVENT");
		transformerFrom.eventFrom(events, destinationFamily, origin, "ANNULMENT", "ANUL");
		transformerFrom.eventFrom(events, destinationFamily, origin, "CENSUS", "CENS");
		transformerFrom.eventFrom(events, destinationFamily, origin, "DIVORCE", "DIV");
		transformerFrom.eventFrom(events, destinationFamily, origin, "DIVORCE_FILED", "DIVF");
		transformerFrom.eventFrom(events, destinationFamily, origin, "ENGAGEMENT", "ENGA");
		transformerFrom.eventFrom(events, destinationFamily, origin, "MARRIAGE_BANN", "MARB");
		transformerFrom.eventFrom(events, destinationFamily, origin, "MARRIAGE_CONTRACT", "MARC");
		transformerFrom.eventFrom(events, destinationFamily, origin, "MARRIAGE", "MARR");
		transformerFrom.eventFrom(events, destinationFamily, origin, "MARRIAGE_LICENCE", "MARL");
		transformerFrom.eventFrom(events, destinationFamily, origin, "MARRIAGE_SETTLEMENT", "MARS");
		transformerFrom.eventFrom(events, destinationFamily, origin, "RESIDENCE", "RESI");
		transformerFrom.eventFrom(events, destinationFamily, origin, "@EVENT@", "EVEN");
		destinationFamily
			.addChildValue("HUSB", transformerFrom.extractSubStructure(family, "SPOUSE1")
				.getValue())
			.addChildValue("WIFE", transformerFrom.extractSubStructure(family, "SPOUSE2")
				.getValue());
		final List<GedcomNode> children = family.getChildrenWithTag("CHILD");
		for(final GedcomNode child : children)
			destinationFamily.addChildReference("CHIL", child.getID());
		transformerFrom.noteFrom(family, destinationFamily);
		transformerFrom.sourceCitationFrom(family, destinationFamily);

		destination.addFamily(destinationFamily);
	}

}
