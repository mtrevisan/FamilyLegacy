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
package io.github.mtrevisan.familylegacy.ui.handlers;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Window;
import java.util.List;


public class RelationshipHandler extends AbstractRecordTypeHandler<RecordDialog>{

	public static final String TYPE = RelationshipReader.TYPE;
	public static final String ID_PREFIX = RelationshipReader.ID_PREFIX;


	private static final class SingletonHelper{
		private static final RelationshipHandler INSTANCE = new RelationshipHandler();
	}


	public static RelationshipHandler getInstance(){
		return SingletonHelper.INSTANCE;
	}


	@Override
	public String getLabel(){
		return I18N.t("confirmation.exist_record.relationship");
	}

	@Override
	public String getType(){
		return TYPE;
	}

	public static boolean isOfType(final FLEFRecord record){
		return RelationshipReader.isOfType(record);
	}

	@Override
	public String getIdPrefix(){
		return ID_PREFIX;
	}

	@Override
	public List<FLEFRecord> findReferences(final FLEFModel model, final String recordId,
			final String parentEntityType){
		if(recordId == null)
			return List.of();

		//the relationships that have the given record as subject or target
		return model.getRecordsByType(TYPE).stream()
			.filter(relationship -> referencesRecord(relationship, RelationshipReader.TAG_SUBJECT, recordId, model)
				|| referencesRecord(relationship, RelationshipReader.TAG_TARGET, recordId, model))
			.toList();
	}

	private static boolean referencesRecord(final FLEFRecord relationship, final String tag, final String recordId,
			final FLEFModel model){
		for(final FLEFRecord referenced : FLEFRecordHelper.extractRecordsFromOneOfReference(relationship, tag, model))
			if(recordId.equals(referenced.getId()))
				return true;
		return false;
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final List<FLEFRecord> subjects = FLEFRecordHelper.extractRecordsFromOneOfReference(record, RelationshipReader.TAG_SUBJECT, model);
		final FLEFRecord subject = (subjects.isEmpty()? null: subjects.getFirst());
		String subjectDisplayText = "--";
		if(subject != null){
			final RecordTypeHandler<?> subjectHandler = HandlerRegistry.getHandler(subject.getTag());
			subjectDisplayText = subjectHandler.getDisplayText(subject, model);
		}

		final List<FLEFRecord> targets = FLEFRecordHelper.extractRecordsFromOneOfReference(record, RelationshipReader.TAG_TARGET, model);
		final FLEFRecord target = (targets.isEmpty()? null: targets.getFirst());
		String objectDisplayText = "--";
		if(target != null){
			final RecordTypeHandler<?> objectHandler = HandlerRegistry.getHandler(target.getTag());
			objectDisplayText = objectHandler.getDisplayText(target, model);
		}

		final String type = RelationshipReader.extractType(record);
		final String role = RelationshipReader.extractRole(record);
		final String id = record.getId();

		String display;
		if(StringUtils.isNotEmpty(role))
			display = I18N.tf("dialog.relationship.display_with_role", subjectDisplayText, role, objectDisplayText,
				type);
		else
			display = I18N.tf("dialog.relationship.display_without_role", subjectDisplayText, objectDisplayText, type);
		return display + " [" + id + "]";
	}

	@Override
	public RecordDialog createNewDialog(final Window parent, final FLEFModel model){
		return RecordDialogs.createNew(TYPE, parent, model);
	}

	@Override
	public RecordDialog createEditDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		return RecordDialogs.createEdit(TYPE, parent, model, record);
	}


}
