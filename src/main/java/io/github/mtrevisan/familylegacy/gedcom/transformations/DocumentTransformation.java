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
			final GedcomNode format = transformerTo.traverse(file, "FORM");
			final String fileValue = file.getValue();
			destinationDocument.addChild(transformerTo.create("FILE")
				.withValue(fileValue != null? fileValue: transformerTo.traverse(file, "TITLE")
					.getValue())
				.addChildValue("FORMAT", format.getValue())
				.addChildValue("MEDIA", transformerTo.traverse(format, "TYPE")
					.getValue())
			);
		}
		transformerTo.noteTo(document, destinationDocument, destination);

		destination.addSource(destinationDocument);
	}

	@Override
	public void from(final Flef origin, final Gedcom destination){}

}
