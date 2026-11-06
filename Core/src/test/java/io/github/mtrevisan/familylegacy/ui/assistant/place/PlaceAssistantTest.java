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
package io.github.mtrevisan.familylegacy.ui.assistant.place;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticProvider;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.PlaceRelationshipRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.List;


class PlaceAssistantTest{

	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 BIRT
		2 PLAC Treviso, Veneto, Italia
		0 @I2@ INDI
		1 NAME Bruno /Rossi/
		1 BIRT
		2 PLAC Treviso, Veneto, Italia
		1 DEAT
		2 PLAC Venezia, Veneto, Italia
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static List<Diagnostic> diagnose(final FLEFModel model){
		final DiagnosticProvider provider = new PlaceAssistantContributor().diagnosticProviders().getFirst();
		return provider.diagnose(new AssistantContext(null, null, model));
	}


	@Test
	void thePlacesAndTheirRelationshipsAreReadFromTheModel() throws IOException{
		final PlaceData data = PlaceDataLoader.load(convert());

		Assertions.assertEquals(4, data.places().size());
		//Treviso, Veneto, Italia: two relationships; Venezia: one more, to Veneto
		Assertions.assertEquals(3, data.relations().size());
		Assertions.assertTrue(data.relations().stream().allMatch(r -> r.type().equals(PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF)));
		final PlaceData.Place treviso = data.places().stream().filter(p -> p.label().equals("Treviso")).findFirst().orElseThrow();
		Assertions.assertEquals(2, treviso.usage());
	}

	@Test
	void aTreeWithItsHierarchyHasNoPlaceFinding() throws IOException{
		Assertions.assertTrue(diagnose(convert()).isEmpty(), diagnose(convert()).toString());
	}

	@Test
	void aPlaceAddedByHandIsReported() throws IOException{
		final FLEFModel model = convert();
		model.addRecord(FLEFRecord.createMainRecord("P99", PlaceHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Veneto")))
			.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_TYPE, PlaceReader.ENUM_TYPE_REGION)));

		final List<Diagnostic> found = diagnose(model);

		//the region Veneto exists twice: the one of the import has no type, so it is not the same place
		Assertions.assertTrue(found.stream().anyMatch(d -> d.key().startsWith("place.possible_duplicate|")), found.toString());
		final Diagnostic duplicate = found.stream().filter(d -> d.key().startsWith("place.possible_duplicate|")).findFirst().orElseThrow();
		Assertions.assertTrue(duplicate.recordIds().contains("P99"));
		//the proposal is to compare, and nothing is merged
		Assertions.assertNotNull(duplicate.action());
		Assertions.assertEquals(2, model.getRecordsByType(PlaceHandler.TYPE).stream()
			.filter(r -> "Veneto".equals(io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader.extractPrimaryName(r))).count());
	}

	@Test
	void aMissingRelationshipComesWithADraft() throws IOException{
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("P1", PlaceHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Treviso, Veneto")))
			.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_TYPE, PlaceReader.ENUM_TYPE_CITY)));
		model.addRecord(FLEFRecord.createMainRecord("P2", PlaceHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Veneto")))
			.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_TYPE, PlaceReader.ENUM_TYPE_REGION)));

		final List<Diagnostic> found = diagnose(model);

		Assertions.assertEquals(1, found.size(), found.toString());
		Assertions.assertEquals(List.of("P1", "P2"), found.getFirst().recordIds());
		Assertions.assertNotNull(found.getFirst().action());
		//reporting does not touch the model
		Assertions.assertEquals(2, model.getRecordCount());
	}

	@Test
	void theDraftEditorCanBePreparedWithoutSavingAnything() throws IOException, InterruptedException, InvocationTargetException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final FLEFRecord subject = model.getRecordsByType(PlaceHandler.TYPE).get(0);
		final FLEFRecord target = model.getRecordsByType(PlaceHandler.TYPE).get(1);
		final int count = model.getRecordCount();

		SwingUtilities.invokeAndWait(() -> {
			final PlaceRelationshipRecordDialog dialog = PlaceRelationshipRecordDialog.createNew(null, model)
				.withDraft(subject, target, PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF);
			Assertions.assertNotNull(dialog);
			dialog.dispose();
		});

		Assertions.assertEquals(count, model.getRecordCount());
	}

	@Test
	void aModelWithoutPlacesHasNothing(){
		Assertions.assertTrue(diagnose(new FLEFModel()).isEmpty());
		Assertions.assertTrue(new PlaceAssistantContributor().diagnosticProviders().getFirst()
			.diagnose(new AssistantContext(null, null, null)).isEmpty());
	}

}
