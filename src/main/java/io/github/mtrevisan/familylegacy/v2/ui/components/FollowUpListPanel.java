package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchLogHandler;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Panel for managing a list of follow_up references to ResearchLogRecord.
 */
public class FollowUpListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 4628106926068555455L;

	private static final String TAG_FOLLOW_UP = "FOLLOW_UP";

	private final Dialog parentDialog;
	private final ResearchLogHandler researchLogHandler;

	public FollowUpListPanel(Dialog parentDialog, FLEFModel model){
		super(parentDialog, "Follow Up", model);
		this.parentDialog = parentDialog;
		this.researchLogHandler = new ResearchLogHandler();
	}

//	@Override
//	protected void buildMenu(GUIHelper.MenuBuilder builder){
//		builder.item("Add Follow Up...", this::createNewItem);
//		builder.separator();
//		builder.selectionSensitiveItem("Edit...", this::editItem);
//		builder.selectionSensitiveItem("Remove", this::removeItem);
//	}

	@Override
	protected String getDisplay(FLEFRecord followUp){
		if(followUp == null) return "--";
		String ref = followUp.getValue();
		if(StringUtils.isEmpty(ref)) return "(empty)";
		FLEFRecord rec = model.getRecordById(ref);
		if(rec != null){
			return researchLogHandler.getDisplayText(rec, model);
		}
		return ref;
	}

	@Override
	protected FLEFRecord showAddDialog(){
		return showCreateNewDialog();
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		final FLEFRecord[] result = {null};
//		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
//			parentDialog, model, researchLogHandler, selectedRecord -> {
//			if(selectedRecord != null){
//				FLEFRecord ref = FLEFRecord.createChildWithValue(
//					TAG_FOLLOW_UP, XRefHelper.formatXRef(selectedRecord.getId()));
//				result[0] = ref;
//			}
//		});
//		dialog.setVisible(true);
		return result[0];
	}

	@Override
	protected FLEFRecord showEditDialog(FLEFRecord existing){
		if(existing == null) return null;
		final FLEFRecord[] result = {null};
//		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
//			parentDialog, model, researchLogHandler, selectedRecord -> {
//			if(selectedRecord != null){
//				existing.setValue(XRefHelper.formatXRef(selectedRecord.getId()));
//				result[0] = existing;
//			}
//		});
//		dialog.setVisible(true);
		return result[0];
	}

	public void load(FLEFRecord record){
		List<FLEFRecord> followUps = new ArrayList<>();
		for(FLEFRecord child : record.getChildren()){
			if(TAG_FOLLOW_UP.equals(child.getTag())){
				followUps.add(child);
			}
		}
		setItems(followUps);
	}

	public void save(FLEFRecord record){
		FLEFRecordHelper.removeChildren(record, TAG_FOLLOW_UP);
		for(FLEFRecord followUp : getItems()){
			followUp.setTag(TAG_FOLLOW_UP);
			record.addChild(followUp);
		}
	}

	public List<String> getFollowUpIds(){
		List<String> ids = new ArrayList<>();
		for(FLEFRecord item : items){
			String ref = item.getValue();
			if(StringUtils.isNotEmpty(ref)){
				ids.add(XRefHelper.extractXRef(ref));
			}
		}
		return ids;
	}

	public boolean hasData(){
		return !items.isEmpty();
	}

}
