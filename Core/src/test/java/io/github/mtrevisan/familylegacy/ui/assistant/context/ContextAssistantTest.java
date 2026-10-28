package io.github.mtrevisan.familylegacy.ui.assistant.context;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
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
			.addChild(FLEFRecord.createChildWithTag("value")
				.addChild(FLEFRecord.createChildWithTag("point")
					.addChild(FLEFRecord.createChildWithTag("value")
						.addChild(FLEFRecord.createChildWithTagAndValue("full_date", fullDate)))
					.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian"))));
	}

	private static FLEFRecord bounded(final String tag, final String from, final String to){
		return FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag("value")
				.addChild(FLEFRecord.createChildWithTag("bounded")
					.addChild(FLEFRecord.createChildWithTag("not_before")
						.addChild(FLEFRecord.createChildWithTag("value")
							.addChild(FLEFRecord.createChildWithTagAndValue("full_date", from)))
						.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian")))
					.addChild(FLEFRecord.createChildWithTag("not_after")
						.addChild(FLEFRecord.createChildWithTag("value")
							.addChild(FLEFRecord.createChildWithTagAndValue("full_date", to)))
						.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian")))));
	}

	private static FLEFRecord war(final String id, final String placeId){
		final FLEFRecord war = FLEFRecord.createMainRecord(id, HistoricEventHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("type", "war"))
			.addChild(FLEFRecord.createChildWithTagAndValue("title", "First World War"))
			.addChild(bounded("date", "28 JUL 1914", "11 NOV 1918"));
		if(placeId != null)
			war.addChild(FLEFRecord.createChildWithTag("place")
				.addChild(FLEFRecord.createChildWithTagAndValue("place", placeId)));
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
			.addChild(FLEFRecord.createChildWithTag("name").addChild(FLEFRecord.createChildWithTagAndValue("value", "Lombardia"))));
		model.addRecord(war("H1", "P99"));
		Assertions.assertTrue(diagnose(model).stream().noneMatch(d -> d.key().startsWith("context.candidate|")));

		final FLEFModel model2 = convert();
		model2.addRecord(war("H1", placeId(model2, "Veneto")));
		final String death = diagnose(model2).stream().filter(d -> d.key().startsWith("context.candidate|")).findFirst().orElseThrow()
			.recordIds().get(1);
		model2.addRecord(FLEFRecord.createMainRecord("CI1", ContextImpactHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTag("context").addChild(FLEFRecord.createChildWithTagAndValue("historic_event", "H1")))
			.addChild(FLEFRecord.createChildWithTag("target").addChild(FLEFRecord.createChildWithTagAndValue("event", death))));

		Assertions.assertTrue(diagnose(model2).stream().noneMatch(d -> d.key().startsWith("context.candidate|")));
	}

	@Test
	void aNormWithAValidityIsReadFromTheModel() throws IOException{
		final FLEFModel model = convert();
		model.addRecord(FLEFRecord.createMainRecord("N1", CulturalNormHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("title", "Burial in the churchyard"))
			.addChild(FLEFRecord.createChildWithTagAndValue("type", "burial_practice"))
			.addChild(point("valid_from", "1 JAN 1900"))
			.addChild(point("valid_to", "31 DEC 1950")));

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
				.withDraft(context, target, "influences", "To be checked.");
			final HistoricEventRecordDialog historic = HistoricEventRecordDialog.createNew(null, model)
				.withDraft("epidemic", "Crisis in Treviso, 1855");
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
