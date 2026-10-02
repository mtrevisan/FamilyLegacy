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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ContextImpactRecordDialog;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.awt.Window;
import java.util.List;


public class ContextImpactHandler extends AbstractRecordTypeHandler<ContextImpactRecordDialog>{

	public static final String TYPE = "CONTEXT_IMPACT";
	public static final String ID_PREFIX = "CI";


	private static final class SingletonHelper{
		private static final ContextImpactHandler INSTANCE = new ContextImpactHandler();
	}


	public static ContextImpactHandler getInstance(){
		return SingletonHelper.INSTANCE;
	}


	@Override
	public String getLabel(){
		return I18N.t("confirmation.exist.record.context.impact");
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

		// Extract the context reference (oneof: CulturalNorm or HistoricEvent)
		final FLEFRecord context = FLEFRecordHelper.findChild(record, ContextImpactReader.TAG_CONTEXT);
		String contextDisplay = extractReferenceDisplay(record, model, context);
		if(StringUtils.isEmpty(contextDisplay))
			contextDisplay = "Unknown Context";
		sb.append(contextDisplay);

		// Extract the target reference (oneof: many possible types)
		final FLEFRecord target = FLEFRecordHelper.findChild(record, ContextImpactReader.TAG_TARGET);
		String targetDisplay = extractReferenceDisplay(record, model, target);
		if(StringUtils.isEmpty(targetDisplay))
			targetDisplay = "Unknown Target";
		sb.append(" → ")
			.append(targetDisplay);

		// Add an impact type if present
		final String impactType = ContextImpactReader.extractImpactType(record);
		if(StringUtils.isNotEmpty(impactType))
			sb.append(" (")
				.append(impactType)
				.append(')');

		// Optionally add rationale (commented out to keep display concise)
		// final String rationale = FLEFRecordHelper.getChildValue(record, TAG_RATIONALE);
		// if(StringUtils.isNotEmpty(rationale))
		//     sb.append(" - ").append(rationale);

		// Append the record ID if present
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
	 * @param reference the child that contains the reference (e.g., "CONTEXT", "TARGET")
	 * @return the display text of the referenced record, or {@code null} if not found
	 */
	private String extractReferenceDisplay(final FLEFRecord record, final FLEFModel model, final FLEFRecord reference){
		if(reference == null || reference.isEmpty())
			return null;

		final List<FLEFRecord> children = reference.getChildren();
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
	public ContextImpactRecordDialog createNewDialog(final Window parent, final FLEFModel model){
		return ContextImpactRecordDialog.createNew(parent, model);
	}

	@Override
	public ContextImpactRecordDialog createEditDialog(final Window parent, final FLEFModel model,
		final FLEFRecord record){
		return ContextImpactRecordDialog.createEdit(parent, model, record);
	}

}
