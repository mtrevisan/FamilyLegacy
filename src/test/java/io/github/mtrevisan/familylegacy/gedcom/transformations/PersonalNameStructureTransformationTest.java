package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


class PersonalNameStructureTransformationTest{

	@Test
	void to(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("NAME")
					.withValue("NAME_PERSONAL")
					.addChild(GedcomNode.create("TYPE")
						.withValue("NAME_TYPE"))
					.addChild(GedcomNode.create("NPFX")
						.withValue("NAME_PIECE_PREFIX"))
					.addChild(GedcomNode.create("GIVN")
						.withValue("NAME_PIECE_GIVEN"))
					.addChild(GedcomNode.create("NICK")
						.withValue("NAME_PIECE_NICKNAME"))
					.addChild(GedcomNode.create("SPFX")
						.withValue("NAME_PIECE_SURNAME_PREFIX"))
					.addChild(GedcomNode.create("SURN")
						.withValue("NAME_PIECE_SURNAME"))
					.addChild(GedcomNode.create("NSFX")
						.withValue("NAME_PIECE_SUFFIX"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("SOUR")
						.withID("S1"))
					.addChild(GedcomNode.create("FONE")
						.withValue("NAME_PHONETIC_VARIATION")
						.addChild(GedcomNode.create("TYPE")
							.withValue("PHONETIC_TYPE"))
						.addChild(GedcomNode.create("NPFX")
							.withValue("NAME_PIECE_PREFIX"))
						.addChild(GedcomNode.create("GIVN")
							.withValue("NAME_PIECE_GIVEN"))
						.addChild(GedcomNode.create("NICK")
							.withValue("NAME_PIECE_NICKNAME"))
						.addChild(GedcomNode.create("SPFX")
							.withValue("NAME_PIECE_SURNAME_PREFIX"))
						.addChild(GedcomNode.create("SURN")
							.withValue("NAME_PIECE_SURNAME"))
						.addChild(GedcomNode.create("NSFX")
							.withValue("NAME_PIECE_SUFFIX"))
						.addChild(GedcomNode.create("NOTE")
							.withID("N2"))
						.addChild(GedcomNode.create("SOUR")
							.withID("S2"))
					)
					.addChild(GedcomNode.create("ROMN")
						.withValue("NAME_ROMANIZED_VARIATION")
						.addChild(GedcomNode.create("TYPE")
							.withValue("ROMANIZED_TYPE"))
						.addChild(GedcomNode.create("NPFX")
							.withValue("NAME_PIECE_PREFIX"))
						.addChild(GedcomNode.create("GIVN")
							.withValue("NAME_PIECE_GIVEN"))
						.addChild(GedcomNode.create("NICK")
							.withValue("NAME_PIECE_NICKNAME"))
						.addChild(GedcomNode.create("SPFX")
							.withValue("NAME_PIECE_SURNAME_PREFIX"))
						.addChild(GedcomNode.create("SURN")
							.withValue("NAME_PIECE_SURNAME"))
						.addChild(GedcomNode.create("NSFX")
							.withValue("NAME_PIECE_SUFFIX"))
						.addChild(GedcomNode.create("NOTE")
							.withID("N3"))
						.addChild(GedcomNode.create("SOUR")
							.withID("S3"))
					)
				));

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: NAME, value: NAME_PERSONAL, children: [{tag: TYPE, value: NAME_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}, {tag: FONE, value: NAME_PHONETIC_VARIATION, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N2, tag: NOTE}, {id: S2, tag: SOUR}]}, {tag: ROMN, value: NAME_ROMANIZED_VARIATION, children: [{tag: TYPE, value: ROMANIZED_TYPE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: GIVN, value: NAME_PIECE_GIVEN}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SPFX, value: NAME_PIECE_SURNAME_PREFIX}, {tag: SURN, value: NAME_PIECE_SURNAME}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {id: N3, tag: NOTE}, {id: S3, tag: SOUR}]}]}]}]", root.toString());

		final Transformation t = new PersonalNameStructureTransformation();
		t.to(extractSubStructure(root, "PARENT", "NAME"), root);
		GedcomNode node = extractSubStructure(root, "PARENT", "NAME", "FONE");
		deleteTag(root, "PARENT", "NAME", "FONE");
		t.to(node, root);
		root.addChild(node);
		node = extractSubStructure(root, "PARENT", "NAME", "ROMN");
		deleteTag(root, "PARENT", "NAME", "ROMN");
		t.to(node, root);
		root.addChild(node);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: NAME, children: [{tag: TYPE, value: NAME_TYPE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME, value: NAME_PIECE_GIVEN}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: NAME_PIECE_SURNAME_PREFIX NAME_PIECE_SURNAME}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {id: N1, tag: NOTE}, {id: S1, tag: SOURCE}]}]}, {tag: NAME, children: [{tag: TYPE, value: PHONETIC_TYPE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME, value: NAME_PIECE_GIVEN}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: NAME_PIECE_SURNAME_PREFIX NAME_PIECE_SURNAME}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {id: N2, tag: NOTE}, {id: S2, tag: SOURCE}]}, {tag: NAME, children: [{tag: TYPE, value: ROMANIZED_TYPE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME, value: NAME_PIECE_GIVEN}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: NAME_PIECE_SURNAME_PREFIX NAME_PIECE_SURNAME}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {id: N3, tag: NOTE}, {id: S3, tag: SOURCE}]}]", root.toString());
	}


	@Test
	void from(){
		final GedcomNode root = GedcomNode.createEmpty()
			.addChild(GedcomNode.create("PARENT")
				.addChild(GedcomNode.create("NAME")
					.withValue("NAME_PERSONAL")
					.addChild(GedcomNode.create("TYPE")
						.withValue("NAME_TYPE"))
					.addChild(GedcomNode.create("LOCALE")
						.withValue("en_US"))
					.addChild(GedcomNode.create("NAME")
						.withValue("NAME_PIECE"))
					.addChild(GedcomNode.create("NAME_PREFIX")
						.withValue("NAME_PIECE_PREFIX"))
					.addChild(GedcomNode.create("NAME_SUFFIX")
						.withValue("NAME_PIECE_SUFFIX"))
					.addChild(GedcomNode.create("NICKNAME")
						.withValue("NAME_PIECE_NICKNAME"))
					.addChild(GedcomNode.create("SURNAME")
						.withValue("SURNAME_PIECE"))
					.addChild(GedcomNode.create("FAMILY_NICKNAME")
						.withValue("SURNAME_PIECE_NICKNAME"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N1"))
					.addChild(GedcomNode.create("SOUR")
						.withID("S1")))
				.addChild(GedcomNode.create("NAME")
					.withValue("NAME_PERSONAL")
					.addChild(GedcomNode.create("TYPE")
						.withValue("NAME_PHONETIC_VARIATION"))
					.addChild(GedcomNode.create("LOCALE")
						.withValue("en_US"))
					.addChild(GedcomNode.create("NAME")
						.withValue("NAME_PIECE"))
					.addChild(GedcomNode.create("NAME_PREFIX")
						.withValue("NAME_PIECE_PREFIX"))
					.addChild(GedcomNode.create("NAME_SUFFIX")
						.withValue("NAME_PIECE_SUFFIX"))
					.addChild(GedcomNode.create("NICKNAME")
						.withValue("NAME_PIECE_NICKNAME"))
					.addChild(GedcomNode.create("SURNAME")
						.withValue("SURNAME_PIECE"))
					.addChild(GedcomNode.create("FAMILY_NICKNAME")
						.withValue("SURNAME_PIECE_NICKNAME"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N2"))
					.addChild(GedcomNode.create("SOUR")
						.withID("S2")))
				.addChild(GedcomNode.create("NAME")
					.withValue("NAME_PERSONAL")
					.addChild(GedcomNode.create("TYPE")
						.withValue("NAME_ROMANIZED_VARIATION"))
					.addChild(GedcomNode.create("LOCALE")
						.withValue("en_US"))
					.addChild(GedcomNode.create("NAME")
						.withValue("NAME_PIECE"))
					.addChild(GedcomNode.create("NAME_PREFIX")
						.withValue("NAME_PIECE_PREFIX"))
					.addChild(GedcomNode.create("NAME_SUFFIX")
						.withValue("NAME_PIECE_SUFFIX"))
					.addChild(GedcomNode.create("NICKNAME")
						.withValue("NAME_PIECE_NICKNAME"))
					.addChild(GedcomNode.create("SURNAME")
						.withValue("SURNAME_PIECE"))
					.addChild(GedcomNode.create("FAMILY_NICKNAME")
						.withValue("SURNAME_PIECE_NICKNAME"))
					.addChild(GedcomNode.create("NOTE")
						.withID("N3"))
					.addChild(GedcomNode.create("SOUR")
						.withID("S3")))
			);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: NAME, value: NAME_PERSONAL, children: [{tag: TYPE, value: NAME_TYPE}, {tag: LOCALE, value: en_US}, {tag: NAME, value: NAME_PIECE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: SURNAME_PIECE}, {tag: FAMILY_NICKNAME, value: SURNAME_PIECE_NICKNAME}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}]}, {tag: NAME, value: NAME_PERSONAL, children: [{tag: TYPE, value: NAME_PHONETIC_VARIATION}, {tag: LOCALE, value: en_US}, {tag: NAME, value: NAME_PIECE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: SURNAME_PIECE}, {tag: FAMILY_NICKNAME, value: SURNAME_PIECE_NICKNAME}, {id: N2, tag: NOTE}, {id: S2, tag: SOUR}]}, {tag: NAME, value: NAME_PERSONAL, children: [{tag: TYPE, value: NAME_ROMANIZED_VARIATION}, {tag: LOCALE, value: en_US}, {tag: NAME, value: NAME_PIECE}, {tag: NAME_PREFIX, value: NAME_PIECE_PREFIX}, {tag: NAME_SUFFIX, value: NAME_PIECE_SUFFIX}, {tag: NICKNAME, value: NAME_PIECE_NICKNAME}, {tag: SURNAME, value: SURNAME_PIECE}, {tag: FAMILY_NICKNAME, value: SURNAME_PIECE_NICKNAME}, {id: N3, tag: NOTE}, {id: S3, tag: SOUR}]}]}]", root.toString());

		final Transformation t = new PersonalNameStructureTransformation();
		final List<GedcomNode> names = extractSubStructure(root, "PARENT")
			.getChildrenWithTag("NAME");
		for(final GedcomNode name : names)
			t.from(name, root);

		Assertions.assertEquals("children: [{tag: PARENT, children: [{tag: NAME, value: NAME_PIECE_PREFIX NAME_PIECE /SURNAME_PIECE/, children: [{tag: TYPE, value: NAME_TYPE}, {tag: _LOCALE, value: en_US}, {tag: GIVN, value: NAME_PIECE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SURN, value: SURNAME_PIECE}, {tag: FAMILY_NICKNAME, value: SURNAME_PIECE_NICKNAME}, {id: N1, tag: NOTE}, {id: S1, tag: SOUR}]}, {tag: FONE, value: NAME_PIECE_PREFIX NAME_PIECE /SURNAME_PIECE/, children: [{tag: TYPE, value: NAME_PHONETIC_VARIATION}, {tag: _LOCALE, value: en_US}, {tag: GIVN, value: NAME_PIECE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SURN, value: SURNAME_PIECE}, {tag: FAMILY_NICKNAME, value: SURNAME_PIECE_NICKNAME}, {id: N2, tag: NOTE}, {id: S2, tag: SOUR}]}, {tag: ROMN, value: NAME_PIECE_PREFIX NAME_PIECE /SURNAME_PIECE/, children: [{tag: TYPE, value: NAME_ROMANIZED_VARIATION}, {tag: _LOCALE, value: en_US}, {tag: GIVN, value: NAME_PIECE}, {tag: NPFX, value: NAME_PIECE_PREFIX}, {tag: NSFX, value: NAME_PIECE_SUFFIX}, {tag: NICK, value: NAME_PIECE_NICKNAME}, {tag: SURN, value: SURNAME_PIECE}, {tag: FAMILY_NICKNAME, value: SURNAME_PIECE_NICKNAME}, {id: N3, tag: NOTE}, {id: S3, tag: SOUR}]}]}]", root.toString());
	}

}