/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.io.merger;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.merger.manual.RecordMerger;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.quality.RecordReferences;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


/** A MediaCitation in each of the five kinds of record that can have it: deleting or merging the media follows it. */
class MediaCitationTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";
	private static final List<String> OWNERS = List.of("I1", "G1", "P1", "E1", "R1");

	private static FLEFGrammar grammar;


	@BeforeAll
	static void loadGrammar() throws IOException{
		try(final InputStream is = MediaCitationTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			grammar = FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	private static String citation(final String mediaId, final String caption){
		return "\tmedia {\n\t\tmedia " + mediaId + "\n\t\tcaption " + caption + "\n\t\tpage 2\n"
			+ "\t\tcrop {\n\t\t\tx 1\n\t\t\ty 2\n\t\t\twidth 30\n\t\t\theight 40\n\t\t}\n\t}\n";
	}

	private static FLEFModel model(){
		return new FLEFParser().parse("media M1 {\n\turi a.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "media M2 {\n\turi a.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "individual I1 {\n" + citation("M1", "One") + citation("M2", "Two") + AUDIT + "}\n"
			+ "group G1 {\n" + citation("M1", "Group") + AUDIT + "}\n"
			+ "place P1 {\n\tname.value Town\n" + citation("M1", "Place") + AUDIT + "}\n"
			+ "event E1 {\n\ttype birth\n" + citation("M1", "Event") + AUDIT + "}\n"
			+ "repository R1 {\n\tname.value Archive\n" + citation("M1", "Building") + AUDIT + "}\n");
	}

	private static List<String> mediaOf(final FLEFModel model, final String ownerId){
		return MediaCitationReader.extractAll(model.getRecordById(ownerId), model).stream()
			.map(MediaCitationReader.MediaUse::mediaId)
			.toList();
	}


	@Test
	void theCitationsAreReadWithCaptionCropAndPage(){
		final FLEFModel model = model();

		final List<MediaCitationReader.MediaUse> uses = MediaCitationReader.extractAll(model.getRecordById("I1"), model);

		Assertions.assertEquals(2, uses.size());
		Assertions.assertEquals("One", uses.getFirst().caption());
		Assertions.assertEquals(new Rectangle(1, 2, 30, 40), uses.getFirst().crop());
		Assertions.assertEquals(2, uses.getFirst().page());
		Assertions.assertEquals("a.jpg", uses.getFirst().uri());
		//only the preferred image is the one shown for the record
		Assertions.assertNull(IndividualReader.extractPreferredImageUri(model.getRecordById("I1"), model));
	}

	@Test
	void everyKindOfRecordIsFoundAsAUserOfTheMedia(){
		final FLEFModel model = model();

		final List<String> users = RecordReferences.to(model, "M1").stream()
			.map(reference -> reference.owner().getId())
			.toList();

		Assertions.assertTrue(users.containsAll(OWNERS), users.toString());
	}

	@Test
	void deletingTheMediaRemovesItsCitationsFromEveryKindOfRecord(){
		final FLEFModel model = model();

		new RecordDeleter(grammar).delete(model, "M1");

		Assertions.assertNull(model.getRecordById("M1"));
		for(final String owner : OWNERS){
			Assertions.assertNotNull(model.getRecordById(owner), owner);
			Assertions.assertFalse(mediaOf(model, owner).contains("M1"), owner);
			//no empty citation is left behind
			Assertions.assertTrue(FLEFRecordHelper.findChildren(model.getRecordById(owner), MediaCitationReader.TAG).stream()
				.allMatch(citation -> FLEFRecordHelper.getChildValue(citation, MediaCitationReader.TAG_MEDIA) != null), owner);
		}
		Assertions.assertEquals(List.of("M2"), mediaOf(model, "I1"));
	}

	@Test
	void theMediaOfAPersonalNameFollowDeleteAndMerge(){
		final String named = "media M1 {\n\turi a.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "media M2 {\n\turi b.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "individual I1 {\n\tname {\n\t\ttype birth\n\t\tpart {\n\t\t\ttype given\n\t\t\tvalue Anna\n\t\t}\n"
			+ "\t\tmedia {\n\t\t\tmedia M1\n\t\t\tcaption Signature\n\t\t}\n\t}\n" + AUDIT + "}\n";

		final FLEFModel deleted = new FLEFParser().parse(named);
		new RecordDeleter(grammar).delete(deleted, "M1");
		Assertions.assertTrue(FLEFRecordHelper.findChildren(deleted.getRecordById("I1"), "name.media").isEmpty());

		final FLEFModel merged = new FLEFParser().parse(named);
		final RecordMerger merger = new RecordMerger(merged, grammar);
		merger.apply(merger.plan("M1", "M2"));
		Assertions.assertEquals(List.of("M2"), FLEFRecordHelper.findChildren(merged.getRecordById("I1"), "name.media.media").stream()
			.map(FLEFRecord::getValue)
			.toList());
	}

	@Test
	void mergingTwoMediaRedirectsTheCitationsOfEveryKindOfRecord(){
		final FLEFModel model = model();
		final RecordMerger merger = new RecordMerger(model, grammar);

		merger.apply(merger.plan("M1", "M2"));

		Assertions.assertNull(model.getRecordById("M1"));
		for(final String owner : OWNERS){
			Assertions.assertTrue(mediaOf(model, owner).contains("M2"), owner);
			Assertions.assertFalse(mediaOf(model, owner).contains("M1"), owner);
		}
		//the captions, being of the use, are kept
		final List<FLEFRecord> citations = FLEFRecordHelper.findChildren(model.getRecordById("P1"), MediaCitationReader.TAG);
		Assertions.assertEquals("Place", FLEFRecordHelper.getChildValue(citations.getFirst(), MediaCitationReader.TAG_CAPTION));
	}

}
