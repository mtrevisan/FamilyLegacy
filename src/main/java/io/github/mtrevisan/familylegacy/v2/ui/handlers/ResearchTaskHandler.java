package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.ResearchTaskRecordDialog;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;


/**
 * Handler for ResearchTaskRecord.
 */
public class ResearchTaskHandler implements RecordTypeHandler<ResearchTaskRecordDialog>{

	public static final String TYPE = "RESEARCH_TASK";
	public static final String ID_PREFIX = "RT";


	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIdPrefix(){
		return ID_PREFIX;
	}

	@Override
	public String getLabel(){
		return "Research Task";
	}

	@Override
	public String getDisplayText(FLEFRecord record, FLEFModel model){
		if(record == null) return "--";
		String description = FLEFRecordHelper.getChildValue(record, "DESCRIPTION");
		String status = FLEFRecordHelper.getChildValue(record, "STATUS");
		if(StringUtils.isNotEmpty(description)){
			String display = description.length() > 40? description.substring(0, 37) + "...": description;
			if(StringUtils.isNotEmpty(status)){
				display += " [" + status + "]";
			}
			return display;
		}
		return record.getId() != null? record.getId(): "(unnamed)";
	}

	@Override
	public ResearchTaskRecordDialog createNewDialog(Dialog parent, FLEFModel model){
		return ResearchTaskRecordDialog.createNew(parent, model);
	}

	@Override
	public ResearchTaskRecordDialog createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		return ResearchTaskRecordDialog.createEdit(parent, model, record);
	}

}
