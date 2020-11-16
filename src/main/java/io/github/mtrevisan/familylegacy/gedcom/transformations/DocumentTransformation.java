package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.noteTo;


public class DocumentTransformation implements Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> documents = origin.getDocuments();
		for(final GedcomNode document : documents)
			documentRecordTo(document, destination);
	}

	private void documentRecordTo(final GedcomNode document, final Flef destination){
		final GedcomNode destinationDocument = GedcomNode.create("SOURCE")
			.withID(document.getID());
		final List<GedcomNode> files = document.getChildrenWithTag("FILE");
		for(final GedcomNode file : files){
			final GedcomNode format = extractSubStructure(file, "FORM");
			final String fileValue = file.getValue();
			destinationDocument.addChild(GedcomNode.create("FILE")
				.withValue(fileValue != null? fileValue: extractSubStructure(file, "TITLE")
					.getValue())
				.addChildValue("FORMAT", format.getValue())
				.addChildValue("MEDIA", extractSubStructure(format, "TYPE")
					.getValue())
				.addChildValue("CUT", extractSubStructure(file, "_CUTD")
					.getValue())
			);
		}
		noteTo(document, destinationDocument, destination);

		destination.addSource(destinationDocument);
	}

	@Override
	public void from(final Flef origin, final Gedcom destination){}

}
