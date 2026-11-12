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
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditStamp;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Migrates the files written before the sex became an individual attribute: the {@code sex} field of an IndividualRecord
 * <pre>
 * individual I1 {
 *   sex male
 * }
 * </pre>
 * becomes an IndividualAttributeRecord of type {@code sex}
 * <pre>
 * individual_attribute IA7 {
 *   individual I1
 *   type sex
 *   value male
 *   audit { creation { date ... comment "Migrated from the sex of the individual" } }
 * }
 * </pre>
 * so that the sex takes part in the preferred assertion rules like the other single-valued assertions.
 * <p>
 * The attribute is dated as the individual (the sex was stated when the individual was recorded), and the citations of the
 * individual are left where they are. Migrating a model that has already been migrated does nothing.
 */
public final class SexMigration{

	/** The comment of the creation of the attributes made by the migration. */
	public static final String MIGRATION_COMMENT = "Migrated from the sex of the individual";

	/** The tag the sex had in the IndividualRecord. */
	private static final String LEGACY_TAG_SEX = "sex";


	private SexMigration(){}


	/**
	 * @param model	The model to migrate (it is changed in place).
	 * @return	The number of the individuals whose sex was migrated.
	 */
	public static int migrate(final FLEFModel model){
		final List<FLEFRecord> individuals = new ArrayList<>(model.getRecordsByType(IndividualReader.TYPE));
		int migrated = 0;
		IdSequence sequence = null;
		for(final FLEFRecord individual : individuals){
			final List<FLEFRecord> legacy = FLEFRecordHelper.findChildren(individual, LEGACY_TAG_SEX);
			if(legacy.isEmpty())
				continue;

			if(sequence == null)
				sequence = new IdSequence(model);
			for(final FLEFRecord sex : new ArrayList<>(legacy)){
				individual.removeChild(sex);

				final String value = StringUtils.trimToNull(sex.getValue());
				if(value != null)
					model.addRecord(createAttribute(sequence.next(), individual, value.toLowerCase(Locale.ROOT)));
			}
			migrated ++;
		}
		return migrated;
	}

	private static FLEFRecord createAttribute(final String id, final FLEFRecord individual, final String value){
		return FLEFRecord.createMainRecord(id, IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, individual.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, value))
			.addChild(auditOf(individual));
	}

	private static FLEFRecord auditOf(final FLEFRecord individual){
		final String date = FLEFRecordHelper.getChildValue(individual, AuditReader.TAG_AUDIT + "." + AuditReader.TAG_CREATION_DATE);
		if(date == null){
			final FLEFRecord audit = AuditStamp.now();
			FLEFRecordHelper.findChild(audit, AuditReader.TAG_CREATION)
				.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_COMMENT, MIGRATION_COMMENT));
			return audit;
		}

		return FLEFRecord.createChildWithTag(AuditReader.TAG_AUDIT)
			.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_CREATION)
				.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_DATE, date))
				.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_COMMENT, MIGRATION_COMMENT)));
	}


	/** Hands out the ids of the attributes that are free in the model (the ids are unique ignoring the case). */
	private static final class IdSequence{

		private final FLEFModel model;
		private long last;


		private IdSequence(final FLEFModel model){
			this.model = model;

			final String prefix = IndividualAttributeReader.ID_PREFIX;
			final List<FLEFRecord> attributes = model.getRecordsByType(IndividualAttributeReader.TYPE);
			for(final FLEFRecord attribute : attributes){
				final String id = attribute.getId();
				if(id != null && Strings.CI.startsWith(id, prefix) && StringUtils.isNumeric(id.substring(prefix.length()))
						&& id.length() - prefix.length() < 18)
					last = Math.max(last, Long.parseLong(id.substring(prefix.length())));
			}
		}

		private String next(){
			String id;
			do{
				last ++;
				id = IndividualAttributeReader.ID_PREFIX + last;
			}while(model.hasRecord(id));
			return id;
		}

	}

}
