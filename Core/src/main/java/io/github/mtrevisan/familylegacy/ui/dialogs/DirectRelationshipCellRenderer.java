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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Component;


public class DirectRelationshipCellRenderer extends DefaultListCellRenderer{

	private final FLEFModel model;


	public DirectRelationshipCellRenderer(final FLEFModel model){
		this.model = model;
	}


	@Override
	public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus){
		final JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		if(value instanceof FLEFRecord record){
			final String type = RelationshipReader.extractType(record);
			final String categoryTag = switch(StringUtils.defaultString(type)){
				case RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD -> I18N.t("text.relationship.category.biological");
				case RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD -> I18N.t("text.relationship.category.adoptive");
				case RelationshipReader.ENUM_TYPE_FOSTER_CHILD -> I18N.t("text.relationship.category.custody");
				case RelationshipReader.ENUM_TYPE_GUARDED_CHILD -> I18N.t("text.relationship.category.legal_protection");
				case RelationshipReader.ENUM_TYPE_STEP_CHILD -> I18N.t("text.relationship.category.step_child");
				case RelationshipReader.ENUM_TYPE_GROUP_MEMBER -> I18N.t("text.relationship.category.group");
				default -> I18N.t("text.relationship.category.other");
			};

			final StringBuilder sb = new StringBuilder();
			sb.append("<html><b>").append(categoryTag).append("</b> ")
				.append(StringUtils.replaceEach(RelationshipHandler.getSentence(record, model),
					new String[]{"&", "<", ">"}, new String[]{"&amp;", "&lt;", "&gt;"}))
				.append("</html>");

			label.setText(sb.toString());
		}

		return label;
	}

}
