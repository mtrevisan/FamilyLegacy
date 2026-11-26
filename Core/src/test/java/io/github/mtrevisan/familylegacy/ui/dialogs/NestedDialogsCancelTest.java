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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.ui.api.LinkRole;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.EventParticipationRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.IndividualAttributeRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.RelationshipRecordDialog;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.IndividualRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.PartStructureDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.PersonalNameStructureDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** What a nested dialog saves is kept only if the dialogs below it are saved too. */
class NestedDialogsCancelTest{

	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 SEX F
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static BoundTextField findValueField(final Container container){
		for(final Component component : container.getComponents()){
			if(component instanceof BoundTextField field)
				return field;
			if(component instanceof Container child){
				final BoundTextField found = findValueField(child);
				if(found != null)
					return found;
			}
		}
		return null;
	}

	private static String primaryName(final FLEFRecord individual){
		return IndividualReader.extractPrimaryFullname(individual);
	}

	/** Edits the first part of the first name of the individual through the chain individual -> name -> part, saving the inner two. */
	private static IndividualRecordDialog editFirstPart(final FLEFModel model, final FLEFRecord individual, final String newValue){
		final IndividualRecordDialog individualDialog = IndividualRecordDialog.createEdit(null, model, individual);
		final FLEFRecord name = FLEFRecordHelper.findChildren(individual, IndividualReader.TAG_NAME).getFirst();
		final PersonalNameStructureDialog nameDialog = PersonalNameStructureDialog.createEdit(individualDialog, model, name);
		final FLEFRecord part = FLEFRecordHelper.findChildren(name, NameReader.TAG_PART).getFirst();
		final PartStructureDialog partDialog = PartStructureDialog.createEdit(nameDialog, model, part);

		final BoundTextField value = findValueField(partDialog.getContentPane());
		Assertions.assertNotNull(value);
		value.setText(newValue);
		partDialog.save();
		Assertions.assertTrue(partDialog.isSaved());
		nameDialog.save();
		Assertions.assertTrue(nameDialog.isSaved());

		return individualDialog;
	}


	@Test
	void cancellingTheIndividualUndoesTheNameSavedByTheNestedDialogs() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final FLEFRecord individual = model.getRecordsByType(IndividualReader.TYPE).getFirst();
		final String before = primaryName(individual);

		final IndividualRecordDialog dialog = editFirstPart(model, individual, "Changed");
		Assertions.assertNotEquals(before, primaryName(individual), "the nested dialogs save into the record as they go");
		dialog.dispose();

		Assertions.assertEquals(before, primaryName(individual));
	}

	@Test
	void savingTheIndividualKeepsTheNameSavedByTheNestedDialogs() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final FLEFRecord individual = model.getRecordsByType(IndividualReader.TYPE).getFirst();

		final IndividualRecordDialog dialog = editFirstPart(model, individual, "Changed");
		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		Assertions.assertTrue(primaryName(individual).contains("Changed"), primaryName(individual));
	}

	@Test
	void cancellingANewIndividualRemovesTheAttributesSavedByTheNestedDialogs() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final int before = model.getRecordsByType(IndividualAttributeReader.TYPE).size();

		final IndividualRecordDialog individualDialog = IndividualRecordDialog.createNew(null, model);
		final IndividualAttributeRecordDialog attributeDialog = IndividualAttributeRecordDialog.createNew(individualDialog, model);
		attributeDialog.link(LinkRole.OWNER, individualDialog.getRecord());
		final BoundComboBox<?> type = findComponent(attributeDialog.getContentPane(), BoundComboBox.class);
		Assertions.assertNotNull(type);
		type.setText("occupation");
		attributeDialog.save();
		Assertions.assertTrue(attributeDialog.isSaved());
		Assertions.assertEquals(before + 1, model.getRecordsByType(IndividualAttributeReader.TYPE).size(),
			"the nested dialog saves into the model at once");

		individualDialog.dispose();

		Assertions.assertEquals(before, model.getRecordsByType(IndividualAttributeReader.TYPE).size(),
			"the attribute of an individual that was never saved must not stay in the file");
	}

	@Test
	void cancellingANewIndividualRemovesTheRelationshipsSavedByTheNestedDialogs() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final FLEFRecord anna = model.getRecordsByType(IndividualReader.TYPE).getFirst();
		final int before = model.getRecordsByType(RelationshipReader.TYPE).size();

		final IndividualRecordDialog individualDialog = IndividualRecordDialog.createNew(null, model);
		final RelationshipRecordDialog nested = RelationshipRecordDialog.createNew(individualDialog, model);
		nested.link(LinkRole.SUBJECT, individualDialog.getRecord());
		nested.link(LinkRole.TARGET, anna);
		final BoundComboBox<?> type = findComponent(nested.getContentPane(), BoundComboBox.class);
		Assertions.assertNotNull(type);
		type.setText(RelationshipReader.ENUM_TYPE_CHILD);
		nested.save();
		Assertions.assertTrue(nested.isSaved());
		Assertions.assertEquals(before + 1, model.getRecordsByType(RelationshipReader.TYPE).size());

		individualDialog.dispose();

		Assertions.assertEquals(before, model.getRecordsByType(RelationshipReader.TYPE).size());
	}

	@Test
	void cancellingANewIndividualRemovesTheParticipationsSavedByTheNestedDialogs() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final FLEFRecord event = FLEFRecord.createMainRecord(EventReader.TYPE, EventReader.ID_PREFIX, model)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH));
		model.addRecord(event);
		final int before = model.getRecordsByType(EventParticipationReader.TYPE).size();

		final IndividualRecordDialog individualDialog = IndividualRecordDialog.createNew(null, model);
		final EventParticipationRecordDialog nested = EventParticipationRecordDialog.createNew(individualDialog, model);
		nested.link(LinkRole.PARTICIPANT, individualDialog.getRecord());
		nested.link(LinkRole.EVENT, event);
		nested.save();
		Assertions.assertTrue(nested.isSaved());
		Assertions.assertEquals(before + 1, model.getRecordsByType(EventParticipationReader.TYPE).size());

		individualDialog.dispose();

		Assertions.assertEquals(before, model.getRecordsByType(EventParticipationReader.TYPE).size());
	}

	@Test
	void savingANewIndividualKeepsTheAttributesSavedByTheNestedDialogs() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final int before = model.getRecordsByType(IndividualAttributeReader.TYPE).size();

		final IndividualRecordDialog individualDialog = IndividualRecordDialog.createNew(null, model);
		final IndividualAttributeRecordDialog nested = IndividualAttributeRecordDialog.createNew(individualDialog, model);
		nested.link(LinkRole.OWNER, individualDialog.getRecord());
		findComponent(nested.getContentPane(), BoundComboBox.class).setText("occupation");
		nested.save();
		individualDialog.save();

		Assertions.assertTrue(individualDialog.isSaved());
		Assertions.assertEquals(before + 1, model.getRecordsByType(IndividualAttributeReader.TYPE).size());
	}

	@Test
	void cancellingAnExistingIndividualRemovesTheAttributesSavedByTheNestedDialogs() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final FLEFRecord anna = model.getRecordsByType(IndividualReader.TYPE).getFirst();
		final int before = model.getRecordsByType(IndividualAttributeReader.TYPE).size();

		final IndividualRecordDialog individualDialog = IndividualRecordDialog.createEdit(null, model, anna);
		final IndividualAttributeRecordDialog nested = IndividualAttributeRecordDialog.createNew(individualDialog, model);
		nested.link(LinkRole.OWNER, anna);
		findComponent(nested.getContentPane(), BoundComboBox.class).setText("occupation");
		nested.save();
		Assertions.assertEquals(before + 1, model.getRecordsByType(IndividualAttributeReader.TYPE).size());

		individualDialog.dispose();

		Assertions.assertEquals(before, model.getRecordsByType(IndividualAttributeReader.TYPE).size());
	}

	@Test
	void cancellingAnExistingIndividualRestoresTheAttributeEditedByTheNestedDialogs() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final FLEFRecord anna = model.getRecordsByType(IndividualReader.TYPE).getFirst();
		final FLEFRecord sex = firstAttribute(model, anna);
		final String before = FLEFRecordHelper.getChildValue(sex, IndividualAttributeReader.TAG_VALUE);

		final IndividualRecordDialog individualDialog = IndividualRecordDialog.createEdit(null, model, anna);
		final IndividualAttributeRecordDialog nested = IndividualAttributeRecordDialog.createEdit(individualDialog, model, sex);
		final String changed = (before.equals("male")? "female": "male");
		FLEFRecordHelper.updateChildValue(sex, IndividualAttributeReader.TAG_VALUE, changed);
		nested.save();
		Assertions.assertTrue(nested.isSaved());

		individualDialog.dispose();

		Assertions.assertEquals(before, FLEFRecordHelper.getChildValue(sex, IndividualAttributeReader.TAG_VALUE));
	}

	private static FLEFRecord firstAttribute(final FLEFModel model, final FLEFRecord individual){
		return model.getRecordsByType(IndividualAttributeReader.TYPE).getFirst();
	}

	private static <T extends Component> T findComponent(final Container container, final Class<T> type){
		for(final Component component : container.getComponents()){
			if(type.isInstance(component))
				return type.cast(component);
			if(component instanceof Container child){
				final T found = findComponent(child, type);
				if(found != null)
					return found;
			}
		}
		return null;
	}

}
