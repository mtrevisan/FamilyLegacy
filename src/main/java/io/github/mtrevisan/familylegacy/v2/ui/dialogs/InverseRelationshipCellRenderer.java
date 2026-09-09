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


public class InverseRelationshipCellRenderer extends DefaultListCellRenderer{

	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_ROLE = "role";


	private final FLEFModel model;


	public InverseRelationshipCellRenderer(final FLEFModel model){
		this.model = model;
	}


	@Override
	public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus){
		final JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		if(value instanceof FLEFRecord record){
			final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
			final FLEFRecord subject = FLEFRecordHelper.extractRecordsFromOneOfReference(record, TAG_SUBJECT, model)
				.getFirst();
			String subjectDisplayText = "--";
			if(subject != null){
				final RecordTypeHandler<?> subjectHandler = HandlerRegistry.getHandler(subject.getTag());
				subjectDisplayText = subjectHandler.getDisplayText(subject, model);
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
			sb.append(type).append(" &larr; ").append(subjectDisplayText);
			if(StringUtils.isNotEmpty(role))
				sb.append(" <i>(").append(role).append(")</i>");
			sb.append("</html>");

			label.setText(sb.toString());
		}

		return label;
	}

}
