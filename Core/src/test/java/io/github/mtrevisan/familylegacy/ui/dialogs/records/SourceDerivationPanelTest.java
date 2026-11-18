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
package io.github.mtrevisan.familylegacy.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.sources.SourceDerivationTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The tree of the derivations of a source: what it contains, when it is shown, how a node reads. */
class SourceDerivationPanelTest{

	private FLEFModel model;


	/**
	 * S1 is a register, S2 a transcription of it (that disagrees on a page), S3 a copy of the transcription, S4 an index of the register,
	 * S5 is unrelated; S6 and S7 are derived one from the other (a circle in the file).
	 */
	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		model.addRecord(source("S1"));
		model.addRecord(source("S2", derivedFrom("S1", "p. 12", SourceCitationReader.STANCE_CONTRADICTS)));
		model.addRecord(source("S3", derivedFrom("S2", null, SourceCitationReader.STANCE_SUPPORTS)));
		model.addRecord(source("S4", derivedFrom("S1", null, SourceCitationReader.STANCE_SUPPORTS)));
		model.addRecord(source("S5"));
		model.addRecord(source("S6", derivedFrom("S7", null, SourceCitationReader.STANCE_SUPPORTS)));
		model.addRecord(source("S7", derivedFrom("S6", null, SourceCitationReader.STANCE_SUPPORTS)));
	}

	private static FLEFRecord source(final String id, final FLEFRecord... derivations){
		final FLEFRecord source = FLEFRecord.createMainRecord(id, SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Title of " + id)));
		for(final FLEFRecord derivation : derivations)
			source.addChild(derivation);
		return source;
	}

	private static FLEFRecord derivedFrom(final String originalId, final String locator, final String stance){
		final FLEFRecord citation = FLEFRecord.createChildWithTag(SourceReader.TAG_DERIVED_FROM)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, originalId))
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, stance));
		if(locator != null)
			citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_LOCATOR, locator));
		return citation;
	}


	@Test
	void theAncestorsGoUpToTheOriginals(){
		final List<SourceDerivationTree.Node> ancestors = new SourceDerivationTree(model).ancestors("S3");

		Assertions.assertEquals(1, ancestors.size());
		Assertions.assertEquals("S2", ancestors.getFirst().id());
		Assertions.assertEquals("Title of S2", ancestors.getFirst().title());
		Assertions.assertEquals("S1", ancestors.getFirst().children().getFirst().id());
		Assertions.assertTrue(ancestors.getFirst().children().getFirst().children().isEmpty());
	}

	@Test
	void theDescendantsCarryTheLocatorAndTheStanceOfTheirCitation(){
		final List<SourceDerivationTree.Node> descendants = new SourceDerivationTree(model).descendants("S1");

		Assertions.assertEquals(List.of("S2", "S4"), descendants.stream().map(SourceDerivationTree.Node::id).toList());
		final SourceDerivationTree.Node transcription = descendants.getFirst();
		Assertions.assertEquals("p. 12", transcription.locator());
		Assertions.assertTrue(transcription.contradicts());
		Assertions.assertEquals("S3", transcription.children().getFirst().id());
		Assertions.assertFalse(descendants.get(1).contradicts());
	}

	@Test
	void anOriginalWithoutDerivativesHasNoTree(){
		final SourceDerivationTree tree = new SourceDerivationTree(model);

		Assertions.assertTrue(tree.ancestors("S5").isEmpty());
		Assertions.assertTrue(tree.descendants("S5").isEmpty());
		Assertions.assertEquals(0, tree.relatedCount("S5"));
	}

	@Test
	void theRelatedSourcesAreCountedOnceEvenIfTheyAreAboveAndBelow(){
		final SourceDerivationTree tree = new SourceDerivationTree(model);

		//S2: S1 above, S3 below
		Assertions.assertEquals(2, tree.relatedCount("S2"));
		//S1: S2, S4 and S3 below
		Assertions.assertEquals(3, tree.relatedCount("S1"));
	}

	@Test
	void theTreeIsWorthShowingFromThreeRelatedSources(){
		final SourceDerivationTree tree = new SourceDerivationTree(model);

		Assertions.assertTrue(tree.isWorthShowing("S1"));
		Assertions.assertFalse(tree.isWorthShowing("S2"));
		Assertions.assertFalse(tree.isWorthShowing("S5"));
	}

	@Test
	void aCircleInTheFileEndsTheBranchInsteadOfLoopingForever(){
		final List<SourceDerivationTree.Node> ancestors = new SourceDerivationTree(model).ancestors("S6");

		//S6 <- S7 <- S6 (the circle, a leaf)
		Assertions.assertEquals("S7", ancestors.getFirst().id());
		final SourceDerivationTree.Node back = ancestors.getFirst().children().getFirst();
		Assertions.assertEquals("S6", back.id());
		Assertions.assertTrue(back.cycle());
		Assertions.assertTrue(back.children().isEmpty());
	}

	@Test
	void aSourceThatIsNotInTheModelIsShownByItsIdentifier(){
		model.addRecord(source("S8", derivedFrom("S99", null, SourceCitationReader.STANCE_SUPPORTS)));

		final SourceDerivationTree.Node node = new SourceDerivationTree(model).ancestors("S8").getFirst();

		Assertions.assertEquals("S99", node.title());
		Assertions.assertEquals("S99", SourceDerivationPanel.describe(node));
	}

	@Test
	void aNodeReadsTitleIdentifierLocatorAndStance(){
		final SourceDerivationTree.Node node = new SourceDerivationTree(model).descendants("S1").getFirst();

		final String text = SourceDerivationPanel.describe(node);

		Assertions.assertTrue(text.startsWith("Title of S2 [S2]"), text);
		Assertions.assertTrue(text.contains("p. 12"), text);
		Assertions.assertTrue(text.contains("contradicts"), text);
	}

	@Test
	void thePanelIsVisibleOnlyAboveTheThreshold(){
		final SourceDerivationPanel panel = new SourceDerivationPanel(model);

		panel.load("S1");
		Assertions.assertTrue(panel.isVisible());
		//two groups at most: derived sources only (an original has no ancestors)
		Assertions.assertEquals(1, panel.getTree().getModel().getChildCount(panel.getTree().getModel().getRoot()));

		panel.load("S2");
		Assertions.assertFalse(panel.isVisible());

		panel.load(null);
		Assertions.assertFalse(panel.isVisible());
	}

}
