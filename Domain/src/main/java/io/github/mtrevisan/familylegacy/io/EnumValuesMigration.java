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
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;


/**
 * Converts the enum values that the files written before the alignment to {@code flef_0.2.0.gedg} still hold:
 * <ul>
 *   <li>group type {@code neighbourhood} becomes {@code neighborhood}, {@code organisation} becomes {@code organization};</li>
 *   <li>research question status {@code closed} becomes {@code abandoned};</li>
 *   <li>proof status {@code unresearched} (removed from the grammar) is dropped: a draft leaves the status blank.</li>
 * </ul>
 * The comparison ignores the case; migrating a model that has already been migrated does nothing.
 */
public final class EnumValuesMigration{

	private static final String LEGACY_GROUP_NEIGHBOURHOOD = "neighbourhood";
	private static final String LEGACY_GROUP_ORGANISATION = "organisation";
	private static final String LEGACY_QUESTION_CLOSED = "closed";
	private static final String LEGACY_PROOF_UNRESEARCHED = "unresearched";

	private static final Map<String, String> GROUP_TYPES = Map.of(
		LEGACY_GROUP_NEIGHBOURHOOD, GroupReader.ENUM_TYPE_NEIGHBORHOOD,
		LEGACY_GROUP_ORGANISATION, GroupReader.ENUM_TYPE_ORGANIZATION);
	private static final Map<String, String> QUESTION_STATUSES = Map.of(
		LEGACY_QUESTION_CLOSED, ResearchQuestionReader.ENUM_STATUS_ABANDONED);


	private EnumValuesMigration(){}


	/**
	 * @param model	The model to migrate (it is changed in place).
	 * @return	The number of the values that were converted or dropped.
	 */
	public static int migrate(final FLEFModel model){
		int migrated = 0;
		for(final FLEFRecord group : model.getRecordsByType(GroupReader.TYPE))
			migrated += rename(group, GroupReader.TAG_TYPE, GROUP_TYPES);
		for(final FLEFRecord question : model.getRecordsByType(ResearchQuestionReader.TYPE))
			migrated += rename(question, ResearchQuestionReader.TAG_STATUS, QUESTION_STATUSES);
		for(final FLEFRecord record : model.getRecords())
			migrated += dropUnresearched(record);
		return migrated;
	}

	private static int rename(final FLEFRecord record, final String tag, final Map<String, String> replacements){
		int migrated = 0;
		for(final FLEFRecord child : FLEFRecordHelper.findChildren(record, tag)){
			final String value = StringUtils.trimToEmpty(child.getValue()).toLowerCase(Locale.ROOT);
			final String replacement = replacements.get(value);
			if(replacement != null){
				child.setValue(replacement);
				migrated ++;
			}
		}
		return migrated;
	}

	/** The proof status can sit in any conclusion, at any depth. */
	private static int dropUnresearched(final FLEFRecord record){
		int migrated = 0;
		for(final FLEFRecord child : new ArrayList<>(record.getChildren())){
			if(ConclusionReader.TAG_PROOF_STATUS.equalsIgnoreCase(child.getTag())
					&& LEGACY_PROOF_UNRESEARCHED.equalsIgnoreCase(StringUtils.trimToEmpty(child.getValue()))){
				record.removeChild(child);
				migrated ++;
			}
			else
				migrated += dropUnresearched(child);
		}
		return migrated;
	}

}
