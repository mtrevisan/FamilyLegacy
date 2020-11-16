package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.joinIfNotNull;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.noteTo;


public class SourceTransformation implements Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> sources = origin.getSources();
		for(final GedcomNode source : sources)
			sourceRecordTo(source, destination);
	}

	private void sourceRecordTo(final GedcomNode source, final Flef destination){
		final GedcomNode title = extractSubStructure(source, "TITL");
		final GedcomNode destinationSource = GedcomNode.create("SOURCE")
			.withID(source.getID())
			.addChildValue("TITLE", title.getValue());
		String date = null;
		final List<GedcomNode> events = extractSubStructure(source, "DATA")
			.getChildrenWithTag("EVEN");
		for(final GedcomNode event : events){
			if(date == null)
				date = extractSubStructure(event, "DATE")
					.getValue();

			destinationSource.addChildValue("EVENT", event.getValue());
		}
		destinationSource.addChildValue("DATE", date);
		destinationSource.addChildValue("EXTRACT", extractSubStructure(source, "TEXT")
			.getValue());
		final String author = extractSubStructure(source, "AUTH")
			.getValue();
		final String publication = extractSubStructure(source, "PUBL")
			.getValue();
		final String noteAuthorPublication = joinIfNotNull(", ", author, publication);
		if(noteAuthorPublication != null){
			final String noteID = destination.getNextNoteID();
			destinationSource.addChildReference("NOTE", noteID);

			destination.addNote(GedcomNode.create("NOTE", noteID, noteAuthorPublication));
		}
		documentsTo(source, destinationSource, destination);
		noteTo(source, destinationSource, destination);
		sourceRepositoryCitationTo(source, destinationSource, destination);

		destination.addSource(destinationSource);
	}

	private void documentsTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> documents = parent.getChildrenWithTag("OBJE");
		for(final GedcomNode document : documents){
			String documentID = document.getID();
			if(documentID == null){
				documentID = destination.getNextSourceID();

				final GedcomNode destinationDocument = GedcomNode.create("SOURCE")
					.withID(documentID);
				final String documentFormat = extractSubStructure(document, "FORM")
					.getValue();
				final String documentMedia = extractSubStructure(document, "FORM", "MEDI")
					.getValue();

				destinationDocument.addChildValue("TITLE", extractSubStructure(document, "TITL")
					.getValue());
				if(documentFormat != null || documentMedia != null)
					destinationDocument.addChild(GedcomNode.create("FILE")
						.withValue(extractSubStructure(document, "FILE")
							.getValue())
						.addChildValue("FORMAT", documentFormat)
						.addChildValue("MEDIA", documentMedia)
						.addChildValue("CUT", extractSubStructure(document, "_CUTD")
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

			final GedcomNode destinationRepository = GedcomNode.create("REPOSITORY");
			if(repositoryID == null){
				repositoryID = destination.getNextRepositoryID();

				destination.addRepository(destinationRepository);

				noteTo(repository, destinationRepository, destination);
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
		final GedcomNode destinationSource = GedcomNode.create("SOUR")
			.withID(source.getID());
		final String date = extractSubStructure(source, "DATE")
			.getValue();
		final GedcomNode destinationData = GedcomNode.create("DATA");
		final List<GedcomNode> events = source.getChildrenWithTag("EVENT");
		for(final GedcomNode event : events)
			destinationData.addChild(GedcomNode.create("EVEN")
				.withValue(event.getValue())
				.addChildValue("DATE", date));
		destinationSource.addChild(destinationData);
		destinationSource.addChildValue("TITL", extractSubStructure(source, "TITLE")
			.getValue());
		destinationSource.addChildValue("TEXT", extractSubStructure(source, "EXTRACT")
			.getValue());
		sourceRepositoryCitationFrom(source, destinationSource);
		notesFrom(source, destinationSource);
		documentsFrom(source, destinationSource);

		destination.addSource(destinationSource);
	}

	private void sourceRepositoryCitationFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> repositories = parent.getChildrenWithTag("REPOSITORY");
		for(final GedcomNode repository : repositories)
			destinationNode.addChild(GedcomNode.create("REPO")
				.withID(repository.getID()));
	}

	private void notesFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			destinationNode.addChild(GedcomNode.create("NOTE")
				.withID(note.getID()));
	}

	private void documentsFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> files = parent.getChildrenWithTag("FILE");
		for(final GedcomNode file : files){
			final String format = extractSubStructure(file, "FORMAT")
				.getValue();
			final String media = extractSubStructure(file, "MEDIA")
				.getValue();
			final GedcomNode destinationObject = GedcomNode.create("OBJE")
				.addChild(GedcomNode.create("FORM")
					.withValue(format)
					.addChild(GedcomNode.create("MEDI")
						.withValue(media))
				)
				.addChildValue("FILE", file.getValue());
			final GedcomNode cut = extractSubStructure(file, "CUT");
			if(!cut.isEmpty())
				destinationObject.addChildValue("CUT", "Y")
					.addChildValue("_CUTD", cut.getValue());
			destinationNode.addChild(destinationObject);
		}
	}

}
