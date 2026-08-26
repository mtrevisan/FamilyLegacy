package io.github.mtrevisan.familylegacy.v2.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


class RecordDialogTest{

	private String content;
	private FLEFParser parser;
	private FLEFModel originalModel;


	@BeforeEach
	void setUp() throws IOException{
		HandlerRegistry.scanHandlers();


		try(final InputStream is = ConclusionRecordDialog.class.getResourceAsStream("/tests/test.flef")){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);

			parser = new FLEFParser();
			originalModel = parser.parse(content);
		}
	}


	@Test
	void conclusion(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "CC1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = ConclusionRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}


	@Test
	void contextImpact(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "CI1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = ContextImpactRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}


	@Test
	void culturalNorm(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "CN1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = CulturalNormRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}


	@Test
	void document(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "D1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = DocumentRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}


	@Test
	void eventParticipation(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "EP1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = EventParticipationRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}

	@Test
	void event(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "E1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = EventRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}


	@Test
	void groupAttribute(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "GA1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = GroupAttributeRecordDialog.createEdit(null, model, record)
			.withGroup("G1");
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}

	@Test
	void group(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "G1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = GroupRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}


	@Test
	void historicEvent(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "HE1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = HistoricEventRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}


	@Test
	void identityHypothesis(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "IH1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = IdentityHypothesisRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}


	@Test
	void individualAttribute(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "IA1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = IndividualAttributeRecordDialog.createEdit(null, model, record)
			.withIndividual("I1");
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}

	@Test
	void individual(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "I1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = IndividualRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}


	@Test
	void place(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "P1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = PlaceRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}


	@Test
	void placeRelationship(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "PR1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = PlaceRelationshipRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}

	@Test
	void relationship(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "RL1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = RelationshipRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}


	@Test
	void repository(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "R1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = RepositoryRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}


	@Test
	void researchActivity(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "RA1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = ResearchActivityRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}

	@Test
	void researchQuestion(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "RQ1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = ResearchQuestionRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}

	@Test
	void researchTask(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "RT1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = ResearchTaskRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}


	@Test
	void source(){
		final FLEFModel model = parser.parse(content);

		final String recordId = "S1";
		final FLEFRecord record = model.getRecordById(recordId);

		final BaseRecordDialog dialog = SourceRecordDialog.createEdit(null, model, record);
		dialog.save();

		Assertions.assertEquals(originalModel, model);
	}

}
