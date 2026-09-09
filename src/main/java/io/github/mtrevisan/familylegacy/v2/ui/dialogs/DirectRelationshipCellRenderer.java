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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Component;


public class DirectRelationshipCellRenderer extends DefaultListCellRenderer{

	private static final String TAG_TYPE = "type";
	private static final String TAG_TARGET = "target";
	private static final String TAG_ROLE = "role";


	private final FLEFModel model;


	public DirectRelationshipCellRenderer(final FLEFModel model){
		this.model = model;
	}


	@Override
	public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus){
		final JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		if(value instanceof FLEFRecord record){
			final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
			final FLEFRecord target = FLEFRecordHelper.extractRecordsFromOneOfReference(record, TAG_TARGET, model)
				.getFirst();
			String targetDisplayText = "--";
			if(target != null){
				final RecordTypeHandler<?> objectHandler = HandlerRegistry.getHandler(target.getTag());
				targetDisplayText = objectHandler.getDisplayText(target, model);
			}
			final String role = FLEFRecordHelper.getChildValue(record, TAG_ROLE);

			final String categoryTag = switch(type){
				case "biological_child" -> "[Biological]";
				case "adoptive_child" -> "[Adoptive]";
				case "foster_child" -> "[Custody]";
				case "guarded_child" -> "[Legal Protection]";
				case "step_child" -> "[Step child]";
				case "group_member" -> "[Group]";
				default -> "[Other]";
			};

			final StringBuilder sb = new StringBuilder();
			sb.append("<html><b>").append(categoryTag).append("</b> ");
			sb.append(type).append(" &rarr; ").append(targetDisplayText);
			if(StringUtils.isNotEmpty(role))
				sb.append(" <i>(").append(role).append(")</i>");
			sb.append("</html>");

			label.setText(sb.toString());
		}

		return label;
	}

}
