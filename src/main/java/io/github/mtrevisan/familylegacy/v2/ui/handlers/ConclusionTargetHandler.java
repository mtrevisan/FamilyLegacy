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
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;

import java.awt.Dialog;
import java.util.ArrayList;
import java.util.List;


public class ConclusionTargetHandler extends AbstractRecordTypeHandler<BaseRecordDialog>{

	public static final String TYPE = "CONCLUSION_TARGET";

	private static final String TAG_RESOLVES = "RESOLVES";


	@Override
	public boolean isTopLevelEntity(){
		return false;
	}

	@Override
	public String getLabel(){
		return "Conclusion Target";
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIdPrefix(){
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public RecordTypeHandler<?> getRecordHandler(){
		return HandlerRegistry.getHandler(ConclusionHandler.class);
	}

	@Override
	public List<FLEFRecord> extractEntities(final FLEFRecord record, final String path){
		final List<FLEFRecord> resolves = FLEFRecordHelper.findChildren(record, path);
		final List<FLEFRecord> entities = new ArrayList<>(resolves.size());
		for(final FLEFRecord resolve : resolves)
			entities.add(resolve.getChildren().getFirst());
		return entities;
	}

	@Override
	public List<FLEFRecord> findReferences(final FLEFModel model, final String recordId,
			final String parentEntityType){
		return model.getRecordsByType(ConclusionHandler.TYPE).stream()
			.filter(conclusion -> {
				final List<FLEFRecord> resolves = FLEFRecordHelper.findChildren(conclusion, TAG_RESOLVES);
				for(final FLEFRecord resolve : resolves){
					final FLEFRecord resolveCitation = resolve.getTheOnlyChild();
					if(resolveCitation != null && !resolveCitation.isEmpty()){
						final String resolveTag = resolveCitation.getTag();
						final String resolveXRef = XRefHelper.extractXRef(resolveCitation.getValue());
						if(resolveTag.equals(parentEntityType) && resolveXRef.equals(recordId))
							return true;
					}
				}
				return false;
			})
			.toList();
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());

		if(ConclusionHandler.class.equals(handler.getClass()))
			return handler.getDisplayText(record, model);

		final String recordId = record.getValue();
		final FLEFRecord parentRecord = model.getRecordById(recordId);
		if(parentRecord == null)
			return "--";

		return handler.getDisplayText(parentRecord, model);
	}

	@Override
	public BaseRecordDialog createNewDialog(final Dialog parent, final FLEFModel model){
		final RecordTypeHandler<?> recordHandler = getRecordHandler();
		return (recordHandler != null? recordHandler: this).createNewDialog(parent, model);
	}

	@Override
	public BaseRecordDialog createEditDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
		return handler.createEditDialog(parent, model, record);
	}

}
