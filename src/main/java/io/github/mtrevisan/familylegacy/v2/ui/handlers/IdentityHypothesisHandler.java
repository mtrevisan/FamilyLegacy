package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.IdentityHypothesisRecordDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


/**
 * Handler for IdentityHypothesisRecord.
 */
public class IdentityHypothesisHandler implements RecordTypeHandler<IdentityHypothesisRecordDialog>{

	public static final String TYPE = "IDENTITY_HYPOTHESIS";
	public static final String ID_PREFIX = "IH";


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
		return "Identity Hypothesis";
	}

	@Override
	public String getDisplayText(FLEFRecord record, FLEFModel model){
		if(record == null) return "--";
		String subjectId = null;
		String candidateId = null;

		FLEFRecord subjectNode = FLEFRecordHelper.findChild(record, "SUBJECT");
		if(subjectNode != null && !subjectNode.getChildren().isEmpty()){
			FLEFRecord child = subjectNode.getChildren().getFirst();
			String ref = child.getValue();
			if(StringUtils.isNotEmpty(ref)){
				subjectId = XRefHelper.extractXRef(ref);
			}
		}

		FLEFRecord candidateNode = FLEFRecordHelper.findChild(record, "CANDIDATE");
		if(candidateNode != null && !candidateNode.getChildren().isEmpty()){
			FLEFRecord child = candidateNode.getChildren().getFirst();
			String ref = child.getValue();
			if(StringUtils.isNotEmpty(ref)){
				candidateId = XRefHelper.extractXRef(ref);
			}
		}

		if(subjectId != null && candidateId != null){
			return subjectId + " ↔ " + candidateId;
		}
		return record.getId() != null? record.getId(): "(unnamed)";
	}

	@Override
	public IdentityHypothesisRecordDialog createNewDialog(Dialog parent, FLEFModel model){
		return null;
	}

	@Override
	public IdentityHypothesisRecordDialog createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		return null;
	}

}
