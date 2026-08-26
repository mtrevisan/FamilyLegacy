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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.ContextImpactRecordDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;
import java.util.List;


public class ContextImpactHandler extends AbstractRecordTypeHandler<ContextImpactRecordDialog>{

	public static final String TYPE = "CONTEXT_IMPACT";
	public static final String ID_PREFIX = "CI";

	private static final String TAG_CONTEXT = "CONTEXT";
	private static final String TAG_TARGET = "TARGET";
	private static final String TAG_IMPACT_TYPE = "IMPACT_TYPE";


	@Override
	public String getLabel(){
		return "Context Impact";
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
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		final StringBuilder sb = new StringBuilder();

		// 1. Extract the context reference (oneof: CulturalNorm or HistoricEvent)
		String contextDisplay = extractReferenceDisplay(record, model, TAG_CONTEXT);
		if(StringUtils.isEmpty(contextDisplay))
			contextDisplay = "Unknown Context";
		sb.append(contextDisplay);

		// 2. Extract the target reference (oneof: many possible types)
		String targetDisplay = extractReferenceDisplay(record, model, TAG_TARGET);
		if(StringUtils.isEmpty(targetDisplay))
			targetDisplay = "Unknown Target";
		sb.append(" → ")
			.append(targetDisplay);

		// 3. Add an impact type if present
		final String impactType = FLEFRecordHelper.getChildValue(record, TAG_IMPACT_TYPE);
		if(StringUtils.isNotEmpty(impactType))
			sb.append(" (")
				.append(impactType)
				.append(')');

		// 4. Optionally add significance (commented out to keep display concise)
		// final String significance = FLEFRecordHelper.getChildValue(record, TAG_SIGNIFICANCE);
		// if (StringUtils.isNotEmpty(significance))
		//     sb.append(" - ").append(significance);

		// 5. Append the record ID if present
		final String id = record.getId();
		if(StringUtils.isNotEmpty(id))
			sb.append(" [")
				.append(id)
				.append(']');

		return sb.toString();
	}

	/**
	 * Helper method to extract the display text of a reference node.
	 * The node with the given tag is expected to have a single child
	 * whose value is a Xref to another record.
	 *
	 * @param record the parent record
	 * @param model  the model to resolve references
	 * @param tag    the tag of the child that contains the reference (e.g., "CONTEXT", "TARGET")
	 * @return the display text of the referenced record, or {@code null} if not found
	 */
	private String extractReferenceDisplay(final FLEFRecord record, final FLEFModel model, final String tag){
		final FLEFRecord refContainer = FLEFRecordHelper.findChild(record, tag);
		if(refContainer == null || refContainer.isEmpty())
			return null;

		final List<FLEFRecord> children = refContainer.getChildren();
		if(children.isEmpty())
			return null;

		// The `oneof` is represented by a single child whose tag indicates the type
		final FLEFRecord refNode = children.getFirst();
		final String refId = refNode.getValue();
		if(StringUtils.isEmpty(refId))
			return null;

		final FLEFRecord targetRecord = model.getRecordById(refId);
		if(targetRecord == null || targetRecord.isEmpty())
			return null;

		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(refNode.getTag());
		if(handler == null)
			return null;

		return handler.getDisplayText(targetRecord, model);
	}

	@Override
	public ContextImpactRecordDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return ContextImpactRecordDialog.createNew(parent, model);
	}

	@Override
	public ContextImpactRecordDialog createEditDialog(final Dialog parent, final FLEFModel model,
		final FLEFRecord record){
		return ContextImpactRecordDialog.createEdit(parent, model, record);
	}

}
