/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;


public class FamilyTransformation extends Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> families = origin.getFamilies();
		for(final GedcomNode family : families)
			familyRecordTo(family, destination);
	}

	private void familyRecordTo(final GedcomNode family, final Flef destination){
		final GedcomNode destinationFamily = transformerTo.create("FAMILY")
			.withID(family.getID())
			.addChildReference("SPOUSE1", transformerTo.traverse(family, "HUSB")
				.getID())
			.addChildReference("SPOUSE2", transformerTo.traverse(family, "WIFE")
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
		destinationFamily.addChildValue("RESTRICTION", transformerTo.traverse(family, "RESN")
			.getValue());

		destination.addFamily(destinationFamily);
	}


	@Override
	public void from(final Flef origin, final Gedcom destination){
		final List<GedcomNode> families = origin.getFamilies();
		for(final GedcomNode family : families)
			familyRecordFrom(family, origin, destination);
	}

	private void familyRecordFrom(final GedcomNode family, final Flef origin, final Gedcom destination){
		final GedcomNode destinationFamily = transformerFrom.create("FAM")
			.withID(family.getID())
			.addChildValue("RESN", transformerFrom.traverse(family, "RESTRICTION")
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
			.addChildValue("HUSB", transformerFrom.traverse(family, "SPOUSE1")
				.getValue())
			.addChildValue("WIFE", transformerFrom.traverse(family, "SPOUSE2")
				.getValue());
		final List<GedcomNode> children = family.getChildrenWithTag("CHILD");
		for(final GedcomNode child : children)
			destinationFamily.addChildReference("CHIL", child.getID());
		transformerFrom.noteFrom(family, destinationFamily);
		transformerFrom.sourceCitationFrom(family, destinationFamily);

		destination.addFamily(destinationFamily);
	}

}
