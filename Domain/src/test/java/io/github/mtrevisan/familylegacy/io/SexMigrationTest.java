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
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


/** The sex that was a field of the individual becomes an attribute of the individual, without losing anything. */
class SexMigrationTest{

	private static final String FILE = """
		individual I1 {
			name {
				part {
					type given
					value Mario
				}
			}
			sex male
			audit.creation.date 2020-01-01T00:00:00Z
		}
		individual I2 {
			sex unknown
			audit.creation.date 2022-01-01T00:00:00Z
		}
		individual I3 {
			audit.creation.date 2023-01-01T00:00:00Z
		}
		individual_attribute IA7 {
			individual I3
			type occupation
			value smith
			audit.creation.date 2021-01-01T00:00:00Z
		}
		""";


	@Test
	void theSexOfTheIndividualBecomesAnAttribute(){
		final FLEFModel model = new FLEFParser().parse(FILE);

		final FLEFRecord individual = model.getRecordById("I1");
		Assertions.assertTrue(FLEFRecordHelper.findChildren(individual, "sex").isEmpty());
		final List<FLEFRecord> sexes = PreferredSex.assertions("I1", model);
		Assertions.assertEquals(1, sexes.size());
		final FLEFRecord attribute = sexes.getFirst();
		Assertions.assertEquals("I1", FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_INDIVIDUAL));
		Assertions.assertEquals("sex", FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_TYPE));
		Assertions.assertEquals("male", FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_VALUE));
		Assertions.assertEquals(SexType.MALE, PreferredSex.of(individual, model));
	}

	@Test
	void theAttributeIsDatedAsTheIndividual(){
		final FLEFModel model = new FLEFParser().parse(FILE);

		final FLEFRecord attribute = PreferredSex.assertions("I1", model).getFirst();
		Assertions.assertEquals("2020-01-01T00:00:00Z", FLEFRecordHelper.getChildValue(attribute, "audit.creation.date"));
		Assertions.assertEquals(SexMigration.MIGRATION_COMMENT, FLEFRecordHelper.getChildValue(attribute, "audit.creation.comment"));
	}

	@Test
	void anUnknownSexIsKeptButSaysNothing(){
		final FLEFModel model = new FLEFParser().parse(FILE);

		final List<FLEFRecord> sexes = PreferredSex.assertions("I2", model);
		Assertions.assertEquals(1, sexes.size());
		Assertions.assertEquals("unknown", FLEFRecordHelper.getChildValue(sexes.getFirst(), IndividualAttributeReader.TAG_VALUE));
		Assertions.assertEquals(SexType.UNKNOWN, PreferredSex.of(model.getRecordById("I2"), model));
		//the individual without a sex gets no attribute
		Assertions.assertTrue(PreferredSex.assertions("I3", model).isEmpty());
	}

	@Test
	void theIdsFollowTheOnesOfTheFile(){
		final FLEFModel model = new FLEFParser().parse(FILE);

		//IA7 was taken: the new ones follow it
		Assertions.assertEquals("IA8", PreferredSex.assertions("I1", model).getFirst().getId());
		Assertions.assertEquals("IA9", PreferredSex.assertions("I2", model).getFirst().getId());
		Assertions.assertNotNull(model.getRecordById("IA7"));
		Assertions.assertEquals(3, model.getRecordsByType(IndividualAttributeReader.TYPE).size());
	}

	@Test
	void migratingTwiceChangesNothing(){
		final FLEFModel model = new FLEFParser().parse(FILE);
		final int records = model.getRecordCount();

		Assertions.assertEquals(0, SexMigration.migrate(model));

		Assertions.assertEquals(records, model.getRecordCount());
	}

	@Test
	void theFileWrittenBackHasNoSexInTheIndividual() throws IOException{
		final FLEFModel model = new FLEFParser().parse(FILE);
		final Path path = Files.createTempFile("sex", ".flef");
		try{
			FLEFWriter.createCompact().write(model, path, false);
			final FLEFModel reread = new FLEFParser().parse(path);

			Assertions.assertEquals(SexType.MALE, PreferredSex.of(reread.getRecordById("I1"), reread));
			Assertions.assertTrue(FLEFRecordHelper.findChildren(reread.getRecordById("I1"), "sex").isEmpty());
		}
		finally{
			Files.deleteIfExists(path);
		}
	}

}
