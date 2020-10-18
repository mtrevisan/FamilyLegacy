package io.github.mtrevisan.familylegacy.gedcom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.List;


class GedcomParserTest{

	@Test
	public void parseFile() throws Exception{
		GedcomNode root = GedcomParser.parse("/Case001-AddressStructure.ged");

		Assertions.assertNotNull(root);
		Assertions.assertEquals(4, root.getChildren().size());

		GedcomNode headTag = root.getChildren().get(0);
		Assertions.assertNotNull(headTag);
		Assertions.assertEquals("HEAD", headTag.getTag());

		GedcomNode submissionTag = root.getChildren().get(1);
		Assertions.assertNotNull(submissionTag);
		Assertions.assertEquals("SUBM", submissionTag.getTag());

		GedcomNode individualTag = root.getChildren().get(2);
		Assertions.assertNotNull(individualTag);
		Assertions.assertEquals("INDI", individualTag.getTag());

		GedcomNode repoTag = root.getChildren().get(3);
		Assertions.assertNotNull(repoTag);
		Assertions.assertEquals("REPO", repoTag.getTag());
	}

	@Test
	public void parseLargeFile() throws Exception{
		GedcomNode root = GedcomParser.parse("/complex.ged");

		Assertions.assertNotNull(root);
		Assertions.assertEquals(4310, root.getChildren().size());

		GedcomNode headTag = root.getChildren().get(0);
		Assertions.assertNotNull(headTag);
		Assertions.assertEquals("HEAD", headTag.getTag());

		GedcomNode submissionTag = root.getChildren().get(1);
		Assertions.assertNotNull(submissionTag);
		Assertions.assertEquals("SUBN", submissionTag.getTag());

		GedcomNode individualTag = root.getChildren().get(2);
		Assertions.assertNotNull(individualTag);
		Assertions.assertEquals("INDI", individualTag.getTag());

		GedcomNode repoTag = root.getChildren().get(3);
		Assertions.assertNotNull(repoTag);
		Assertions.assertEquals("INDI", repoTag.getTag());
	}

}