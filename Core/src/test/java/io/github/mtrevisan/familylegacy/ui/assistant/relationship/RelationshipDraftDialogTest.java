package io.github.mtrevisan.familylegacy.ui.assistant.relationship;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
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
