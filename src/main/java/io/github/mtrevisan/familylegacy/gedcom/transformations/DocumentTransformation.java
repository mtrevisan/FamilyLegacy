package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;


public class DocumentTransformation extends Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> documents = origin.getDocuments();
		for(final GedcomNode document : documents)
			documentRecordTo(document, destination);
	}

	private void documentRecordTo(final GedcomNode document, final Flef destination){
		final GedcomNode destinationDocument = transformerTo.create("SOURCE")
			.withID(document.getID());
		final List<GedcomNode> files = document.getChildrenWithTag("FILE");
		for(final GedcomNode file : files){
			final GedcomNode format = transformerTo.extractSubStructure(file, "FORM");
			final String fileValue = file.getValue();
			destinationDocument.addChild(transformerTo.create("FILE")
				.withValue(fileValue != null? fileValue: transformerTo.extractSubStructure(file, "TITLE")
					.getValue())
				.addChildValue("FORMAT", format.getValue())
				.addChildValue("MEDIA", transformerTo.extractSubStructure(format, "TYPE")
					.getValue())
				.addChildValue("CUT", transformerTo.extractSubStructure(file, "_CUTD")
					.getValue())
			);
		}
		transformerTo.noteTo(document, destinationDocument, destination);

		destination.addSource(destinationDocument);
	}

	@Override
	public void from(final Flef origin, final Gedcom destination){}

}
