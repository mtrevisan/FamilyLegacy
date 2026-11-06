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
package io.github.mtrevisan.familylegacy.ui.assistant.context;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ContextImpactRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.HistoricEventRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
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


class ContextAssistantTest{

	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 DEAT
		2 DATE 10 MAY 1916
		2 PLAC Treviso, Veneto, Italia
		0 @I2@ INDI
		1 NAME Bruno /Rossi/
		1 DEAT
		2 DATE 10 MAY 1930
		2 PLAC Treviso, Veneto, Italia
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static List<Diagnostic> diagnose(final FLEFModel model){
		return new ContextAssistantContributor().diagnosticProviders().getFirst().diagnose(new AssistantContext(null, null, model));
	}

	private static String placeId(final FLEFModel model, final String name){
		return model.getRecordsByType(PlaceHandler.TYPE).stream()
			.filter(place -> name.equals(PlaceReader.extractPrimaryName(place)))
			.findFirst().orElseThrow().getId();
	}

	private static FLEFRecord point(final String tag, final String fullDate){
		return FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_VALUE)
				.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_POINT)
					.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_VALUE)
						.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_FULL_DATE, fullDate)))
					.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_CALENDAR, DateReader.ENUM_CALENDAR_GREGORIAN))));
	}

	private static FLEFRecord bounded(final String tag, final String from, final String to){
		return FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_VALUE)
				.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_BOUNDED)
					.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_NOT_BEFORE)
						.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_VALUE)
							.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_FULL_DATE, from)))
						.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_CALENDAR, DateReader.ENUM_CALENDAR_GREGORIAN)))
					.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_NOT_AFTER)
						.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_VALUE)
							.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_FULL_DATE, to)))
						.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_CALENDAR, DateReader.ENUM_CALENDAR_GREGORIAN)))));
	}

	private static FLEFRecord war(final String id, final String placeId){
		final FLEFRecord war = FLEFRecord.createMainRecord(id, HistoricEventHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(HistoricEventReader.TAG_TYPE, HistoricEventReader.ENUM_TYPE_WAR))
			.addChild(FLEFRecord.createChildWithTagAndValue(HistoricEventReader.TAG_TITLE, "First World War"))
			.addChild(bounded(HistoricEventReader.TAG_DATE, "28 JUL 1914", "11 NOV 1918"));
		if(placeId != null)
			war.addChild(FLEFRecord.createChildWithTag(HistoricEventReader.TAG_PLACE)
				.addChild(FLEFRecord.createChildWithTagAndValue(HistoricEventReader.TAG_PLACE, placeId)));
		return war;
	}

	@Test
	void aWarInARegionMayHaveInfluencedADeathInATownOfTheRegion() throws IOException{
		final FLEFModel model = convert();
		model.addRecord(war("H1", placeId(model, "Veneto")));
		final int count = model.getRecordCount();

		final List<Diagnostic> found = diagnose(model).stream().filter(d -> d.key().startsWith("context.candidate|")).toList();

		//the death of 1916 is in the war and in the region; the one of 1930 is not in the war
		Assertions.assertEquals(1, found.size(), found.toString());
		Assertions.assertEquals("H1", found.getFirst().recordIds().get(0));
		Assertions.assertNotNull(found.getFirst().action());
		Assertions.assertTrue(found.getFirst().title().contains("First World War"), found.getFirst().title());
		Assertions.assertTrue(found.getFirst().title().contains("Anna Rossi"), found.getFirst().title());
		//suggesting does not touch the model
		Assertions.assertEquals(count, model.getRecordCount());
	}

	@Test
	void aWarInAnotherRegionDoesNotMatchAndAnImpactThatExistsIsNotProposed() throws IOException{
		final FLEFModel model = convert();
		model.addRecord(FLEFRecord.createMainRecord("P99", PlaceHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME).addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Lombardia"))));
		model.addRecord(war("H1", "P99"));
		Assertions.assertTrue(diagnose(model).stream().noneMatch(d -> d.key().startsWith("context.candidate|")));

		final FLEFModel model2 = convert();
		model2.addRecord(war("H1", placeId(model2, "Veneto")));
		final String death = diagnose(model2).stream().filter(d -> d.key().startsWith("context.candidate|")).findFirst().orElseThrow()
			.recordIds().get(1);
		model2.addRecord(FLEFRecord.createMainRecord("CI1", ContextImpactHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTag(ContextImpactReader.TAG_CONTEXT).addChild(FLEFRecord.createChildWithTagAndValue(HistoricEventReader.TYPE, "H1")))
			.addChild(FLEFRecord.createChildWithTag(ContextImpactReader.TAG_TARGET).addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, death))));

		Assertions.assertTrue(diagnose(model2).stream().noneMatch(d -> d.key().startsWith("context.candidate|")));
	}

	@Test
	void aNormWithAValidityIsReadFromTheModel() throws IOException{
		final FLEFModel model = convert();
		model.addRecord(FLEFRecord.createMainRecord("N1", CulturalNormHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(HistoricEventReader.TAG_TITLE, "Burial in the churchyard"))
			.addChild(FLEFRecord.createChildWithTagAndValue(CulturalNormReader.TAG_TYPE, CulturalNormReader.ENUM_TYPE_BURIAL_PRACTICE))
			.addChild(point(CulturalNormReader.TAG_VALID_FROM, "1 JAN 1900"))
			.addChild(point(CulturalNormReader.TAG_VALID_TO, "31 DEC 1950")));

		final List<Diagnostic> found = diagnose(model).stream().filter(d -> d.key().startsWith("context.candidate|")).toList();

		//both deaths are in the validity of the norm
		Assertions.assertEquals(2, found.size(), found.toString());
		Assertions.assertTrue(found.stream().allMatch(d -> d.recordIds().get(0).equals("N1")));
	}

	@Test
	void theDraftEditorsCanBePreparedWithoutSavingAnything() throws IOException, InterruptedException, InvocationTargetException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		model.addRecord(war("H1", null));
		final FLEFRecord context = model.getRecordById("H1");
		final FLEFRecord target = model.getRecordsByType(EventHandler.TYPE).getFirst();
		final int count = model.getRecordCount();

		SwingUtilities.invokeAndWait(() -> {
			final ContextImpactRecordDialog impact = ContextImpactRecordDialog.createNew(null, model)
				.withDraft(context, target, ContextImpactReader.ENUM_IMPACT_TYPE_INFLUENCES, "To be checked.");
			final HistoricEventRecordDialog historic = HistoricEventRecordDialog.createNew(null, model)
				.withDraft(HistoricEventReader.ENUM_TYPE_EPIDEMIC, "Crisis in Treviso, 1855");
			Assertions.assertNotNull(impact);
			Assertions.assertNotNull(historic);
			impact.dispose();
			historic.dispose();
		});

		Assertions.assertEquals(count, model.getRecordCount());
	}

	@Test
	void aModelWithoutContextHasNothing() throws IOException{
		Assertions.assertTrue(diagnose(convert()).isEmpty(), diagnose(convert()).toString());
		Assertions.assertTrue(diagnose(new FLEFModel()).isEmpty());
		Assertions.assertTrue(new ContextAssistantContributor().diagnosticProviders().getFirst()
			.diagnose(new AssistantContext(null, null, null)).isEmpty());
	}

}
