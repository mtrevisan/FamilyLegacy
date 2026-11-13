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
