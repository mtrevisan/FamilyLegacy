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
			final String noteID = destination.getNextNoteID();
			destinationSource.addChildReference("NOTE", noteID);

			destination.addNote(transformerTo.create("NOTE", noteID, noteAuthorPublication));
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
				documentID = destination.getNextSourceID();

				final GedcomNode destinationDocument = transformerTo.create("SOURCE")
					.withID(documentID);
				final String documentFormat = transformerTo.extractSubStructure(document, "FORM")
					.getValue();
				final String documentMedia = transformerTo.extractSubStructure(document, "FORM", "MEDI")
					.getValue();

				destinationDocument.addChildValue("TITLE", transformerTo.extractSubStructure(document, "TITL")
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
			String repositoryID = repository.getID();

			final GedcomNode destinationRepository = transformerTo.create("REPOSITORY");
			if(repositoryID == null){
				repositoryID = destination.getNextRepositoryID();

				destination.addRepository(destinationRepository);

				transformerTo.noteTo(repository, destinationRepository, destination);
			}
			destinationNode.addChild(destinationRepository
				.withID(repositoryID));
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
