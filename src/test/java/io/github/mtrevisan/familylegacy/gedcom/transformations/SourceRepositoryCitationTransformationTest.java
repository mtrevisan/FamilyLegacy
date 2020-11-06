package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class SourceRepositoryCitationTransformationTest{

	@Test
	void to1(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("REPO")
					.withID("R1")
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("CALN")
						.withValue("SOURCE_CALL_NUMBER")
						.addChild(GedcomNode.create("MEDI")
							.withValue("SOURCE_MEDIA_TYPE")))))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: R1, tag: REPO, children: [{id: N1, tag: NOTE}, {tag: CALN, value: SOURCE_CALL_NUMBER, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new SourceRepositoryCitationTransformation();
		t.to(extractSubStructure(root, "PARENT", "REPO"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: R1, tag: REPOSITORY, children: [{id: N1, tag: NOTE}, {tag: _CALN, value: SOURCE_CALL_NUMBER, children: [{tag: _MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("REPOSITORY")
					.withID("R1")
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: R1, tag: REPOSITORY, children: [{id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new SourceRepositoryCitationTransformation();
		t.from(extractSubStructure(root, "REPOSITORY"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: R1, tag: REPOSITORY, children: [{id: N1, tag: NOTE}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());
	}

	@Test
	void to2(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("REPO")
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("CALN")
						.withValue("SOURCE_CALL_NUMBER")
						.addChild(GedcomNode.create("MEDI")
							.withValue("SOURCE_MEDIA_TYPE")))))
			.addChild(GedcomNode.create("NOTE")
				.withID("N1")
				.withValue("SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: REPO, children: [{id: N1, tag: NOTE}, {tag: CALN, value: SOURCE_CALL_NUMBER, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}]", root.toString());

		final Transformation t = new SourceRepositoryCitationTransformation();
		t.to(extractSubStructure(root, "PARENT", "REPO"), root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{id: R1, tag: REPOSITORY, children: [{id: N1, tag: NOTE}, {tag: _CALN, value: SOURCE_CALL_NUMBER, children: [{tag: _MEDI, value: SOURCE_MEDIA_TYPE}]}]}]}, {id: N1, tag: NOTE, value: SUBMITTER_TEXT}, {id: R1, tag: REPOSITORY}]", root.toString());
	}

}