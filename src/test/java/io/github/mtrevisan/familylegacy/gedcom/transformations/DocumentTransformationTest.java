package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.Protocol;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class DocumentTransformationTest{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);


	@Test
	void to(){
		final GedcomNode document = transformerTo.create("OBJE")
			.withID("D1")
			.addChild(transformerTo.create("FILE")
				.withValue("MULTIMEDIA_FILE_REFN")
				.addChild(transformerTo.create("FORM")
					.withValue("MULTIMEDIA_FORMAT")
					.addChildValue("TYPE", "SOURCE_MEDIA_TYPE")
				)
				.addChildValue("TITL", "DESCRIPTIVE_TITLE")
			)
			.addChild(transformerTo.create("REFN")
				.withValue("USER_REFERENCE_NUMBER")
				.addChildValue("TYPE", "USER_REFERENCE_TYPE")
			)
			.addChildValue("RIN", "AUTOMATED_RECORD_ID")
			.addChildReference("NOTE", "N1")
			.addChildReference("SOUR", "S1")
			.addChild(transformerTo.create("CHAN")
				.withValue("USER_REFERENCE_NUMBER")
				.addChildValue("TYPE", "USER_REFERENCE_TYPE")
			)
			.addChildValue("RIN", "AUTOMATED_RECORD_ID")
			.addChild(transformerTo.create("CHAN")
				.addChildValue("DATE", "CHANGE_DATE"));
		final Gedcom origin = new Gedcom();
		origin.addDocument(document);
		final Flef destination = new Flef();

		Assertions.assertEquals("id: D1, tag: OBJE, children: [{tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORM, value: MULTIMEDIA_FORMAT, children: [{tag: TYPE, value: SOURCE_MEDIA_TYPE}]}, {tag: TITL, value: DESCRIPTIVE_TITLE}]}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}, {tag: CHAN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}]", origin.getDocuments().get(0).toString());

		final Transformation<Gedcom, Flef> t = new DocumentTransformation();
		t.to(origin, destination);

		Assertions.assertEquals("id: D1, tag: SOURCE, children: [{tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORMAT, value: MULTIMEDIA_FORMAT}, {tag: MEDIA, value: SOURCE_MEDIA_TYPE}]}, {id: N1, tag: NOTE}]", destination.getSources().get(0).toString());
	}

}