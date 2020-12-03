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
import io.github.mtrevisan.familylegacy.services.JavaHelper;

import java.util.List;
import java.util.StringJoiner;


public class SourceTransformation extends Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> sources = origin.getSources();
		for(final GedcomNode source : sources)
			sourceRecordTo(source, destination);
	}

	private void sourceRecordTo(final GedcomNode source, final Flef destination){
		final GedcomNode title = transformerTo.traverse(source, "TITL");
		final GedcomNode destinationSource = transformerTo.create("SOURCE")
			.withID(source.getID())
			.addChildValue("TITLE", title.getValue());
		String date = null;
		final List<GedcomNode> events = transformerTo.traverse(source, "DATA")
			.getChildrenWithTag("EVEN");
		for(final GedcomNode event : events){
			if(date == null)
				date = transformerTo.traverse(event, "DATE")
					.getValue();

			destinationSource.addChildValue("EVENT", event.getValue());
		}
		destinationSource.addChildValue("DATE", date);
		destinationSource.addChildValue("EXTRACT", transformerTo.traverse(source, "TEXT")
			.getValue());
		final String author = transformerTo.traverse(source, "AUTH")
			.getValue();
		final String publication = transformerTo.traverse(source, "PUBL")
			.getValue();
		final String noteAuthorPublication = joinIfNotNull(", ", author, publication);
		if(noteAuthorPublication != null){
			final String noteID = destination.addNote(transformerTo.create("NOTE")
				.withValue(noteAuthorPublication));
			destinationSource.addChildReference("NOTE", noteID);
		}
		transformerTo.documentTo(source, destinationSource, destination);
		transformerTo.noteCitationTo(source, destinationSource, destination);
		sourceRepositoryCitationTo(source, destinationSource, destination);

		destination.addSource(destinationSource);
	}

	private String joinIfNotNull(final String separator, final String... components){
		final StringJoiner sj = new StringJoiner(separator);
		for(final String component : components)
			JavaHelper.addValueIfNotNull(sj, component);
		return (sj.length() > 0? sj.toString(): null);
	}

	private void sourceRepositoryCitationTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> repositories = parent.getChildrenWithTag("REPO");
		for(final GedcomNode repository : repositories){
			final GedcomNode destinationRepository = transformerTo.create("REPOSITORY");
			transformerTo.noteCitationTo(repository, destinationRepository, destination);
			if(repository.getXRef() == null)
				destination.addRepository(destinationRepository);

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
		final String date = transformerFrom.traverse(source, "DATE")
			.getValue();
		final GedcomNode destinationData = transformerFrom.create("DATA");
		final List<GedcomNode> events = source.getChildrenWithTag("EVENT");
		for(final GedcomNode event : events)
			destinationData.addChild(transformerFrom.create("EVEN")
				.withValue(event.getValue())
				.addChildValue("DATE", date));
		destinationSource.addChild(destinationData);
		destinationSource.addChildValue("TITL", transformerFrom.traverse(source, "TITLE")
			.getValue());
		destinationSource.addChildValue("TEXT", transformerFrom.traverse(source, "EXTRACT")
			.getValue());
		sourceRepositoryCitationFrom(source, destinationSource);
		transformerFrom.noteCitationFrom(source, destinationSource);
		transformerFrom.documentFrom(source, destinationSource);

		destination.addSource(destinationSource);
	}

	private void sourceRepositoryCitationFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> repositories = parent.getChildrenWithTag("REPOSITORY");
		for(final GedcomNode repository : repositories){
			final GedcomNode destinationRepository = transformerFrom.create("REPO")
				.withXRef(repository.getXRef());
			transformerFrom.noteCitationFrom(repository, destinationRepository);

			destinationNode.addChild(destinationRepository);
		}
	}

}
