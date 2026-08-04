package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.PartDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


public class PartHandler implements RecordTypeHandler<PartDialog>{

	public static final String TYPE = "PART";

	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_PHONETIC = "PHONETIC";
	private static final String TAG_TRANSCRIPTION = "TRANSCRIPTION";


	@Override
	public boolean isTopLevelEntity(){
		return false;
	}

	@Override
	public String getLabel(){
		return "Part";
	}

	@Override
	public String getType(){
		return TYPE	;
	}

	@Override
	public String getIDPrefix(){
		return null;
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		final String type = FLEFRecordHelper.getChildValue(record, "TYPE");
		final String value = FLEFRecordHelper.getChildValue(record, TAG_VALUE);

		final StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotEmpty(type))
			sb.append("[")
				.append(type)
				.append("] ");
		if(StringUtils.isNotEmpty(value)){
			String val = value;
			if(val.length() > 50)
				val = val.substring(0, 50) + "...";
			sb.append(val);
		}
		else
			sb.append("--");

		// If it is a low-level element with no meaningful ID, the variant count is shown
		int variantCount = 0;
		for(final FLEFRecord child : record.getChildren())
			if(TAG_PHONETIC.equals(child.getTag()) || TAG_TRANSCRIPTION.equals(child.getTag()))
				variantCount++;

		if(variantCount > 0)
			sb.append(" (")
				.append(variantCount)
				.append(" variant")
				.append(variantCount > 1? "s": StringUtils.EMPTY)
				.append(")");
		else if(StringUtils.isNotEmpty(record.getId())){
			sb.append(" (")
				.append(record.getId())
				.append(")");
		}

		return sb.toString();
	}

	@Override
	public PartDialog createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		return PartDialog.createEdit(parent, model, record);
	}

	@Override
	public PartDialog createNewDialog(Dialog parent, FLEFModel model){
		return PartDialog.createNew(parent, model);
	}

}
