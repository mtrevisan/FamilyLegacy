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
package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.RelationshipRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.awt.Window;
import java.util.List;


public class RelationshipHandler extends AbstractRecordTypeHandler<RelationshipRecordDialog>{

	public static final String TYPE = "RELATIONSHIP";
	public static final String ID_PREFIX = "RL";


	private static final class SingletonHelper{
		private static final RelationshipHandler INSTANCE = new RelationshipHandler();
	}


	public static RelationshipHandler getInstance(){
		return SingletonHelper.INSTANCE;
	}


	@Override
	public String getLabel(){
		return I18N.t("confirmation.exist.record.relationship");
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIdPrefix(){
		return ID_PREFIX;
	}

	@Override
	public List<FLEFRecord> findReferences(final FLEFModel model, final String recordId,
			final String parentEntityType){
		return model.getRecordsByType(TYPE);
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final FLEFRecord subject = FLEFRecordHelper.extractRecordsFromOneOfReference(record, RelationshipReader.TAG_SUBJECT, model)
			.getFirst();
		String subjectDisplayText = "--";
		if(subject != null){
			final RecordTypeHandler<?> subjectHandler = HandlerRegistry.getHandler(subject.getTag());
			subjectDisplayText = subjectHandler.getDisplayText(subject, model);
		}

		final FLEFRecord object = FLEFRecordHelper.extractRecordsFromOneOfReference(record, RelationshipReader.TAG_OBJECT, model)
			.getFirst();
		String objectDisplayText = "--";
		if(object != null){
			final RecordTypeHandler<?> objectHandler = HandlerRegistry.getHandler(object.getTag());
			objectDisplayText = objectHandler.getDisplayText(object, model);
		}

		final String type = RelationshipReader.extractType(record);
		final String role = RelationshipReader.extractRole(record);
		final String id = record.getId();

		String display;
		if(StringUtils.isNotEmpty(role))
			display = I18N.tf("dialog.relationship.display.with.role", subjectDisplayText, role, objectDisplayText,
				type);
		else
			display = I18N.tf("dialog.relationship.display.without.role", subjectDisplayText, objectDisplayText, type);
		return display + " [" + id + "]";
	}

	@Override
	public RelationshipRecordDialog createNewDialog(final Window parent, final FLEFModel model){
		return RelationshipRecordDialog.createNew(parent, model);
	}

	@Override
	public RelationshipRecordDialog createEditDialog(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return RelationshipRecordDialog.createEdit(parent, model, record);
	}

}
