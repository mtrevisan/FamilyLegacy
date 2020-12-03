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
import java.util.StringJoiner;


public class SubmitterTransformation extends Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> submitters = origin.getSubmitters();
		for(final GedcomNode submitter : submitters)
			submitterRecordTo(submitter, destination);
	}

	private void submitterRecordTo(final GedcomNode submitter, final Flef destination){
		final GedcomNode name = transformerTo.traverse(submitter, "NAME");
		final GedcomNode destinationSource = transformerTo.create("SOURCE")
			.withID(submitter.getID())
			.addChildValue("TITLE", name.getValue());
		transformerTo.placeAddressStructureTo(submitter, destinationSource, destination);
		transformerTo.documentTo(submitter, destinationSource, destination);
		final List<GedcomNode> preferredLanguages = submitter.getChildrenWithTag("LANG");
		final StringJoiner sj = new StringJoiner(", ");
		for(final GedcomNode preferredLanguage : preferredLanguages)
			sj.add(preferredLanguage.getValue());
		if(sj.length() > 0){
			final String noteID = destination.addNote(transformerTo.create("NOTE")
				.withValue("Preferred contact language(s): " + sj));
			destinationSource.addChildReference("NOTE", noteID);
		}
		transformerTo.noteCitationTo(submitter, destinationSource, destination);

		destination.addSource(destinationSource);
	}


	@Override
	public void from(final Flef origin, final Gedcom destination){}

}
