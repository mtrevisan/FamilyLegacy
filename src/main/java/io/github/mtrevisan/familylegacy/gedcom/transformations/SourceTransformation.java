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


public class SourceTransformation extends Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> sources = origin.getSources();
		for(final GedcomNode source : sources)
			sourceRecordTo(source, destination);
	}

	private void sourceRecordTo(final GedcomNode source, final Flef destination){
		final GedcomNode title = transformerTo.extractSubStructure(source, "TITL");
		final GedcomNode destinationSource = transformerTo.create("SOURCE")
			.withID(source.getID())
			.addChildValue("TITLE", title.getValue());
		String date = null;
		final List<GedcomNode> events = transformerTo.extractSubStructure(source, "DATA")
			.getChildrenWithTag("EVEN");
		for(final GedcomNode event : events){
			if(date == null)
				date = transformerTo.extractSubStructure(event, "DATE")
					.getValue();

			destinationSource.addChildValue("EVENT", event.getValue());
		}
		destinationSource.addChildValue("DATE", date);
		destinationSource.addChildValue("EXTRACT", transformerTo.extractSubStructure(source, "TEXT")
			.getValue());
		final String author = transformerTo.extractSubStructure(source, "AUTH")
			.getValue();
		final String publication = transformerTo.extractSubStructure(source, "PUBL")
			.getValue();
		final String noteAuthorPublication = transformerTo.joinIfNotNull(", ", author, publication);
		if(noteAuthorPublication != null){
			final String noteID = destination.addNote(transformerTo.create("NOTE")
				.withValue(noteAuthorPublication));
			destinationSource.addChildReference("NOTE", noteID);
		}
		documentsTo(source, destinationSource, destination);
		transformerTo.noteTo(source, destinationSource, destination);
		sourceRepositoryCitationTo(source, destinationSource, destination);

		destination.addSource(destinationSource);
	}

	private void documentsTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> documents = parent.getChildrenWithTag("OBJE");
		for(final GedcomNode document : documents){
			String documentID = document.getID();
			if(documentID == null){
				final String documentFormat = transformerTo.extractSubStructure(document, "FORM")
					.getValue();
				final String documentMedia = transformerTo.extractSubStructure(document, "FORM", "MEDI")
					.getValue();

				final GedcomNode destinationDocument = transformerTo.create("SOURCE")
					.addChildValue("TITLE", transformerTo.extractSubStructure(document, "TITL")
						.getValue());
				if(documentFormat != null || documentMedia != null)
					destinationDocument.addChild(transformerTo.create("FILE")
						.withValue(transformerTo.extractSubStructure(document, "FILE")
							.getValue())
						.addChildValue("FORMAT", documentFormat)
						.addChildValue("MEDIA", documentMedia)
						.addChildValue("CUT", transformerTo.extractSubStructure(document, "_CUTD")
							.getValue())
					);

				destination.addSource(destinationDocument);
			}
			destinationNode.addChildReference("SOURCE", documentID);
		}
	}

	private void sourceRepositoryCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> repositories = parent.getChildrenWithTag("REPO");
		for(final GedcomNode repository : repositories){
			final GedcomNode destinationRepository = transformerTo.create("REPOSITORY");
			if(repository.getID() == null){
				destination.addRepository(destinationRepository);

				transformerTo.noteTo(repository, destinationRepository, destination);
			}
			destinationNode.addChild(destinationRepository);
		}
	}


	@Override
	public void from(final Flef origin, final Gedcom destination){
		final List<GedcomNode> sources = origin.getSources();
		for(final GedcomNode source : sources)
			sourceRecordFrom(source, destination);
	}

	private void sourceRecordFrom(final GedcomNode source, final Gedcom destination){
		final GedcomNode destinationSource = transformerFrom.create("SOUR")
			.withID(source.getID());
		final String date = transformerFrom.extractSubStructure(source, "DATE")
			.getValue();
		final GedcomNode destinationData = transformerFrom.create("DATA");
		final List<GedcomNode> events = source.getChildrenWithTag("EVENT");
		for(final GedcomNode event : events)
			destinationData.addChild(transformerFrom.create("EVEN")
				.withValue(event.getValue())
				.addChildValue("DATE", date));
		destinationSource.addChild(destinationData);
		destinationSource.addChildValue("TITL", transformerFrom.extractSubStructure(source, "TITLE")
			.getValue());
		destinationSource.addChildValue("TEXT", transformerFrom.extractSubStructure(source, "EXTRACT")
			.getValue());
		sourceRepositoryCitationFrom(source, destinationSource);
		transformerFrom.noteFrom(source, destinationSource);
		transformerFrom.documentFrom(source, destinationSource);

		destination.addSource(destinationSource);
	}

	private void sourceRepositoryCitationFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> repositories = parent.getChildrenWithTag("REPOSITORY");
		for(final GedcomNode repository : repositories)
			destinationNode.addChildReference("REPO", repository.getID());
	}

}
