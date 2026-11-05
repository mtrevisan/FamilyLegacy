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
package io.github.mtrevisan.familylegacy.ui.assistant.relationship;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.RelationshipRecordDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The editor of the relationship, prepared as the "Create Relationship Draft" action does (without showing it). */
class RelationshipDraftDialogTest{

	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 SEX F
		0 @I2@ INDI
		1 NAME Carlo /Bianchi/
		1 SEX M
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static List<FLEFRecord> relationships(final FLEFModel model){
		return model.getRecordsByType("relationship");
	}


	@Test
	void theDraftIsSavedOnlyIfTheResearcherSavesIt() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = convert();
		final List<FLEFRecord> individuals = model.getRecordsByType("individual");
		final FLEFRecord anna = individuals.get(0);
		final FLEFRecord carlo = individuals.get(1);

		//what the action does, but for showing the editor
		final RelationshipRecordDialog dialog = RelationshipRecordDialog.createNew(null, model)
			.withParticipants(anna, carlo, "religious_spouse");

		//the draft is in the editor only
		Assertions.assertTrue(relationships(model).isEmpty());
		Assertions.assertFalse(dialog.isSaved());

		//the researcher confirms it as it is
		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		Assertions.assertEquals(1, relationships(model).size());
		final FLEFRecord relationship = relationships(model).getFirst();
		Assertions.assertEquals("religious_spouse", FLEFRecordHelper.getChildValue(relationship, "type"));
		Assertions.assertEquals(anna.getId(), FLEFRecordHelper.getChildValue(relationship, "subject.individual"));
		Assertions.assertEquals(carlo.getId(), FLEFRecordHelper.getChildValue(relationship, "target.individual"));
	}

	@Test
	void theTypeIsProposedOnlyIfItIsValidForTheParticipants() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = convert();
		final List<FLEFRecord> individuals = model.getRecordsByType("individual");

		//a type that does not exist for two individuals is not put in the editor: the researcher chooses
		final RelationshipRecordDialog dialog = RelationshipRecordDialog.createNew(null, model)
			.withParticipants(individuals.get(0), individuals.get(1), "part_of");

		Assertions.assertTrue(relationships(model).isEmpty());
		Assertions.assertFalse(dialog.isSaved());
		dialog.dispose();
	}

	private static FLEFRecord marriageEvent(final FLEFModel model){
		model.addRecord(FLEFRecord.createMainRecord("S1", "source")
			.addChild(FLEFRecord.createChildWithTagAndValue("title", "Parish register")));
		final FLEFRecord event = FLEFRecord.createMainRecord("E1", "event")
			.addChild(FLEFRecord.createChildWithTagAndValue("type", "marriage"))
			.addChild(FLEFRecord.createChildWithTag("date").addChild(FLEFRecord.createChildWithTag("value")
				.addChild(FLEFRecord.createChildWithTag("point").addChild(FLEFRecord.createChildWithTag("value")
					.addChild(FLEFRecord.createChildWithTagAndValue("full_date", "1865-06-01"))))))
			.addChild(FLEFRecord.createChildWithTag("source")
				.addChild(FLEFRecord.createChildWithTagAndValue("source", "S1")));
		model.addRecord(event);
		return event;
	}

	@Test
	void theDraftStartsFromTheDateAndTheSourcesOfTheMarriageThatIsItsEvidence() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = convert();
		final List<FLEFRecord> individuals = model.getRecordsByType("individual");
		final FLEFRecord event = marriageEvent(model);

		final RelationshipRecordDialog dialog = RelationshipRecordDialog.createNew(null, model)
			.withParticipants(individuals.get(0), individuals.get(1), "civil_spouse")
			.withEvidenceOf(event, true);
		dialog.save();

		final FLEFRecord relationship = relationships(model).getFirst();
		Assertions.assertEquals("1865-06-01", FLEFRecordHelper.getChildValue(relationship, "valid_from.value.point.value.full_date"));
		Assertions.assertEquals("S1", FLEFRecordHelper.getChildValue(relationship, "source.source"));
		//the event is untouched, and its own date and source are not the ones of the relationship (they are copies)
		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(event, "source").size());
		Assertions.assertNotSame(FLEFRecordHelper.findChild(event, "source"), FLEFRecordHelper.findChild(relationship, "source"));
		Assertions.assertEquals("1865-06-01", FLEFRecordHelper.getChildValue(event, "date.value.point.value.full_date"));
	}

	@Test
	void theDateOfTheEventIsNotTheDateOfTheRelationshipUnlessTheDraftSaysSo() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = convert();
		final List<FLEFRecord> individuals = model.getRecordsByType("individual");
		final FLEFRecord event = marriageEvent(model);

		final RelationshipRecordDialog dialog = RelationshipRecordDialog.createNew(null, model)
			.withParticipants(individuals.get(0), individuals.get(1), "civil_spouse")
			.withEvidenceOf(event, false);
		dialog.save();

		final FLEFRecord relationship = relationships(model).getFirst();
		Assertions.assertNull(FLEFRecordHelper.findChild(relationship, "valid_from"));
		Assertions.assertEquals("S1", FLEFRecordHelper.getChildValue(relationship, "source.source"));
	}

	@Test
	void theGenericSpouseCanBeSavedFromTheEditor() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = convert();
		final List<FLEFRecord> individuals = model.getRecordsByType("individual");

		//the type an import gives to a marriage that says nothing more is offered by the editor, so it is not lost when saving
		final RelationshipRecordDialog dialog = RelationshipRecordDialog.createNew(null, model)
			.withParticipants(individuals.get(0), individuals.get(1), "spouse");
		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		Assertions.assertEquals("spouse", FLEFRecordHelper.getChildValue(relationships(model).getFirst(), "type"));
	}

	@Test
	void theSuggestedTypeIsChosenInTheEditorOfTheRelationshipButNothingChangesUntilItIsSaved() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = convert();
		final List<FLEFRecord> individuals = model.getRecordsByType("individual");
		final RelationshipRecordDialog creation = RelationshipRecordDialog.createNew(null, model)
			.withParticipants(individuals.get(0), individuals.get(1), "spouse");
		creation.save();
		final FLEFRecord relationship = relationships(model).getFirst();

		//what the "Change type" action does, but for showing the editor
		final RelationshipRecordDialog dialog = RelationshipRecordDialog.createEdit(null, model, relationship)
			.withType("civil_spouse");
		Assertions.assertEquals("spouse", FLEFRecordHelper.getChildValue(relationship, "type"));
		dialog.save();

		Assertions.assertEquals("civil_spouse", FLEFRecordHelper.getChildValue(relationship, "type"));
	}

	@Test
	void aSuggestedTypeThatTheEditorDoesNotOfferForTheParticipantsIsNotChosen() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = convert();
		final List<FLEFRecord> individuals = model.getRecordsByType("individual");
		final RelationshipRecordDialog creation = RelationshipRecordDialog.createNew(null, model)
			.withParticipants(individuals.get(0), individuals.get(1), "spouse");
		creation.save();
		final FLEFRecord relationship = relationships(model).getFirst();

		final RelationshipRecordDialog dialog = RelationshipRecordDialog.createEdit(null, model, relationship)
			.withType("part_of");
		dialog.save();

		Assertions.assertEquals("spouse", FLEFRecordHelper.getChildValue(relationship, "type"));
	}

	@Test
	void cancellingTheDraftLeavesTheModelAsItWas() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The editor needs a display");
		final FLEFModel model = convert();
		final List<FLEFRecord> individuals = model.getRecordsByType("individual");
		final int records = model.getRecords().size();

		final RelationshipRecordDialog dialog = RelationshipRecordDialog.createNew(null, model)
			.withParticipants(individuals.get(0), individuals.get(1), "civil_spouse");
		//ESC, which is Cancel
		final Object binding = dialog.getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
			.get(io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper.ESCAPE_STROKE);
		dialog.getRootPane().getActionMap().get(binding)
			.actionPerformed(new java.awt.event.ActionEvent(dialog, java.awt.event.ActionEvent.ACTION_PERFORMED, "escape"));

		Assertions.assertFalse(dialog.isSaved());
		Assertions.assertFalse(dialog.isDisplayable());
		Assertions.assertEquals(records, model.getRecords().size());
	}

}
