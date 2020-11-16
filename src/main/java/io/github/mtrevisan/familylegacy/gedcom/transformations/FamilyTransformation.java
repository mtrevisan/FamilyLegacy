package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.createEventTo;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.documentTo;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.eventFrom;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.noteTo;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.notesFrom;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.sourceCitationFrom;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.sourceCitationTo;


public class FamilyTransformation implements Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> families = origin.getFamilies();
		for(final GedcomNode family : families)
			familyRecordTo(family, destination);
	}

	private void familyRecordTo(final GedcomNode family, final Flef destination){
		final GedcomNode destinationFamily = GedcomNode.create("FAMILY")
			.withID(family.getID())
			.addChildReference("SPOUSE1", extractSubStructure(family, "HUSB")
				.getID())
			.addChildReference("SPOUSE2", extractSubStructure(family, "WIFE")
				.getID());
		final List<GedcomNode> children = family.getChildrenWithTag("CHIL");
		for(final GedcomNode child : children)
			destinationFamily.addChildReference("CHILD", child.getID());
		noteTo(family, destinationFamily, destination);
		sourceCitationTo(family, destinationFamily, destination);
		documentTo(family, destinationFamily, destination);
		eventTo(family, destinationFamily, destination, "ANUL", "ANNULMENT");
		eventTo(family, destinationFamily, destination, "CENS", "CENSUS");
		eventTo(family, destinationFamily, destination, "DIV", "DIVORCE");
		eventTo(family, destinationFamily, destination, "DIVF", "DIVORCE_FILED");
		eventTo(family, destinationFamily, destination, "ENGA", "ENGAGEMENT");
		eventTo(family, destinationFamily, destination, "MARB", "MARRIAGE_BANN");
		eventTo(family, destinationFamily, destination, "MARC", "MARRIAGE_CONTRACT");
		eventTo(family, destinationFamily, destination, "MARR", "MARRIAGE");
		eventTo(family, destinationFamily, destination, "MARL", "MARRIAGE_LICENCE");
		eventTo(family, destinationFamily, destination, "MARS", "MARRIAGE_SETTLEMENT");
		eventTo(family, destinationFamily, destination, "RESI", "RESIDENCE");
		eventTo(family, destinationFamily, destination, "EVEN", "EVENT");
		destinationFamily.addChildValue("RESTRICTION", extractSubStructure(family, "RESN")
			.getValue());

		destination.addFamily(destinationFamily);
	}

	private void eventTo(final GedcomNode individual, final GedcomNode destinationNode, final Flef destination,
		final String tagFrom, final String valueTo){
		final List<GedcomNode> events = individual.getChildrenWithTag(tagFrom);
		for(final GedcomNode event : events){
			final GedcomNode destinationEvent = createEventTo(valueTo, event, destination);
			destinationNode.addChild(destinationEvent);
		}
	}


	@Override
	public void from(final Flef origin, final Gedcom destination) throws GedcomGrammarParseException{
		final List<GedcomNode> families = origin.getFamilies();
		for(final GedcomNode family : families)
			familyRecordFrom(family, origin, destination);
	}

	private void familyRecordFrom(final GedcomNode family, final Flef origin, final Gedcom destination) throws GedcomGrammarParseException{
		final GedcomNode destinationFamily = GedcomNode.create("FAM")
			.withID(family.getID())
			.addChildValue("RESN", extractSubStructure(family, "RESTRICTION")
				.getValue());
		final List<GedcomNode> events = family.getChildrenWithTag("EVENT");
		eventFrom(events, destinationFamily, origin, "ANNULMENT", "ANUL");
		eventFrom(events, destinationFamily, origin, "CENSUS", "CENS");
		eventFrom(events, destinationFamily, origin, "DIVORCE", "DIV");
		eventFrom(events, destinationFamily, origin, "DIVORCE_FILED", "DIVF");
		eventFrom(events, destinationFamily, origin, "ENGAGEMENT", "ENGA");
		eventFrom(events, destinationFamily, origin, "MARRIAGE_BANN", "MARB");
		eventFrom(events, destinationFamily, origin, "MARRIAGE_CONTRACT", "MARC");
		eventFrom(events, destinationFamily, origin, "MARRIAGE", "MARR");
		eventFrom(events, destinationFamily, origin, "MARRIAGE_LICENCE", "MARL");
		eventFrom(events, destinationFamily, origin, "MARRIAGE_SETTLEMENT", "MARS");
		eventFrom(events, destinationFamily, origin, "RESIDENCE", "RESI");
		eventFrom(events, destinationFamily, origin, "@EVENT@", "EVEN");
		destinationFamily
			.addChildValue("HUSB", extractSubStructure(family, "SPOUSE1")
				.getValue())
			.addChildValue("WIFE", extractSubStructure(family, "SPOUSE2")
				.getValue());
		final List<GedcomNode> children = family.getChildrenWithTag("CHILD");
		for(final GedcomNode child : children)
			destinationFamily.addChildReference("CHIL", child.getID());
		notesFrom(family, destinationFamily);
		sourceCitationFrom(family, destinationFamily);

		destination.addFamily(destinationFamily);
	}

}
