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

import io.github.mtrevisan.familylegacy.imaging.MediaRegions;
import io.github.mtrevisan.familylegacy.imaging.MediaRegions.Region;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.merger.manual.RecordMerger;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * The regions of a media are the crops of the individuals on it, so nothing is left to clean up: deleting or merging media or individuals leaves
 * the regions of the others as they were, and nothing of the ones that are gone.
 */
class MediaRegionsCleanupTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";
	private static final Rectangle FACE_A = new Rectangle(1, 2, 30, 40);
	private static final Rectangle FACE_B = new Rectangle(100, 2, 30, 40);

	private static FLEFGrammar grammar;


	@BeforeAll
	static void loadGrammar() throws IOException{
		try(final InputStream is = MediaRegionsCleanupTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			grammar = FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	private static String crop(final int x){
		return "\t\tcrop {\n\t\t\tx " + x + "\n\t\t\ty 2\n\t\t\twidth 30\n\t\t\theight 40\n\t\t}\n";
	}

	private static String citation(final String mediaId, final int x){
		return "\tmedia {\n\t\tmedia " + mediaId + "\n" + crop(x) + "\t}\n";
	}

	/** Anna is tagged in M1 with the face A (it is her portrait), Bruno with the face B; Carla has the face A in M2. */
	private static FLEFModel model(){
		return new FLEFParser().parse("media M1 {\n\turi a.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "media M2 {\n\turi b.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "individual I1 {\n\tname.part.type given\n\tname.part.value Anna\n" + citation("M1", 1)
			+ "\tpreferred_image {\n\t\tmedia M1\n" + crop(1) + "\t}\n" + AUDIT + "}\n"
			+ "individual I2 {\n\tname.part.type given\n\tname.part.value Bruno\n" + citation("M1", 100) + AUDIT + "}\n"
			+ "individual I3 {\n\tname.part.type given\n\tname.part.value Carla\n" + citation("M2", 1) + AUDIT + "}\n");
	}

	private static Region region(final Rectangle area, final String... individuals){
		return new Region(area, null, List.of(individuals));
	}


	@Test
	void theRegionsAreThere(){
		final FLEFModel model = model();

		Assertions.assertEquals(List.of(region(FACE_A, "I1"), region(FACE_B, "I2")), MediaRegions.of(model, "M1"));
		Assertions.assertEquals(List.of(region(FACE_A, "I3")), MediaRegions.of(model, "M2"));
	}

	@Test
	void deletingTheMediaTakesAwayItsRegionsAndThePreferredImage(){
		final FLEFModel model = model();

		new RecordDeleter(grammar).delete(model, "M1");

		Assertions.assertTrue(MediaRegions.of(model, "M1").isEmpty());
		Assertions.assertNull(IndividualReader.extractPreferredImageMediaId(model.getRecordById("I1")), "the portrait was in the deleted media");
		Assertions.assertTrue(MediaRegions.mediaOf(model.getRecordById("I1")).isEmpty());
		Assertions.assertEquals(List.of(region(FACE_A, "I3")), MediaRegions.of(model, "M2"), "the other media is as it was");
	}

	@Test
	void mergingTwoMediaMovesTheRegionsToTheOneThatSurvives(){
		final FLEFModel model = model();
		final RecordMerger merger = new RecordMerger(model, grammar);

		merger.apply(merger.plan("M1", "M2"));

		Assertions.assertTrue(MediaRegions.of(model, "M1").isEmpty());
		//(Carla and Anna have the same crop of the same image, so now they share a region)
		Assertions.assertEquals(List.of(region(FACE_A, "I1", "I3"), region(FACE_B, "I2")), MediaRegions.of(model, "M2"));
		Assertions.assertEquals("M2", IndividualReader.extractPreferredImageMediaId(model.getRecordById("I1")), "the portrait follows the media");
		Assertions.assertTrue(MediaRegions.isPortraitOf(model.getRecordById("I1"), "M2", MediaRegions.of(model, "M2").getFirst()));
	}

	@Test
	void deletingAnIndividualTakesItOutOfTheRegions(){
		final FLEFModel model = model();

		new RecordDeleter(grammar).delete(model, "I1");

		Assertions.assertEquals(List.of(region(FACE_B, "I2")), MediaRegions.of(model, "M1"));
		Assertions.assertEquals(List.of(region(FACE_A, "I3")), MediaRegions.of(model, "M2"));
	}

	@Test
	void mergingTwoIndividualsPutsTheSurvivorInTheRegionsOfBoth(){
		final FLEFModel model = model();
		final RecordMerger merger = new RecordMerger(model, grammar);

		merger.apply(merger.planAllFields("I2", "I1"));

		Assertions.assertNull(model.getRecordById("I2"));
		Assertions.assertEquals(List.of(region(FACE_A, "I1"), region(FACE_B, "I1")), MediaRegions.of(model, "M1"));
		Assertions.assertEquals(2, MediaRegions.mediaOf(model.getRecordById("I1")).getFirst().regions().size());
		Assertions.assertEquals(2, FLEFRecordHelper.findChildren(model.getRecordById("I1"), "media").size(), "the citations were carried over");
	}

}
