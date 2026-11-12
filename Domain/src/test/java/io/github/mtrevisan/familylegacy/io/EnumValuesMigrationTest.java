package io.github.mtrevisan.familylegacy.io;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** The enum values renamed or removed by the grammar are converted when the file is opened. */
class EnumValuesMigrationTest{

	private static final String FILE = """
		group G1 {
			type neighbourhood
		}
		group G2 {
			type Organisation
		}
		group G3 {
			type club
		}
		research_question Q1 {
			status closed
		}
		research_question Q2 {
			status open
		}
		""";


	@Test
	void groupTypesAreRenamed(){
		final FLEFModel model = new FLEFParser().parse(FILE);

		Assertions.assertEquals("neighborhood", FLEFRecordHelper.getChildValue(model.getRecordById("G1"), "type"));
		Assertions.assertEquals("organization", FLEFRecordHelper.getChildValue(model.getRecordById("G2"), "type"));
		Assertions.assertEquals("club", FLEFRecordHelper.getChildValue(model.getRecordById("G3"), "type"));
	}

	@Test
	void closedQuestionBecomesAbandoned(){
		final FLEFModel model = new FLEFParser().parse(FILE);

		Assertions.assertEquals("abandoned", FLEFRecordHelper.getChildValue(model.getRecordById("Q1"), "status"));
		Assertions.assertEquals("open", FLEFRecordHelper.getChildValue(model.getRecordById("Q2"), "status"));
	}

	@Test
	void migratingTwiceChangesNothing(){
		final FLEFModel model = new FLEFParser().parse(FILE);

		Assertions.assertEquals(0, EnumValuesMigration.migrate(model));
	}

}
