package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogstodo._GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchStatusHandler;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Panel for managing a list of blocked_by references to ResearchStatusRecord.
 */
public class BlockedByListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 8489320158068443490L;

	private static final String TAG_BLOCKED_BY = "BLOCKED_BY";

	private final Dialog parentDialog;
	private final ResearchStatusHandler researchHandler;

	public BlockedByListPanel(Dialog parentDialog, FLEFModel model){
		super(parentDialog, "Blocked By", model);
		this.parentDialog = parentDialog;
		this.researchHandler = new ResearchStatusHandler();
	}

//	@Override
//	protected void buildMenu(GUIHelper.MenuBuilder builder){
//		builder.item("Add Blocked By...", this::createNewItem);
//		builder.separator();
//		builder.selectionSensitiveItem("Edit...", this::editItem);
//		builder.selectionSensitiveItem("Remove", this::removeItem);
//	}

	@Override
	protected String getDisplay(FLEFRecord blockedBy){
		if(blockedBy == null) return "--";
		String ref = blockedBy.getValue();
		if(StringUtils.isEmpty(ref)) return "(empty)";
		FLEFRecord rec = model.getRecordById(ref);
		if(rec != null){
			return researchHandler.getDisplayText(rec, model);
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
		_GenericSelectionDialog<?> dialog = new _GenericSelectionDialog<>(
			parentDialog, model, researchHandler, selectedRecord -> {
			if(selectedRecord != null){
				FLEFRecord ref = FLEFRecord.createChildWithValue(
					TAG_BLOCKED_BY, XRefHelper.formatXRef(selectedRecord.getId()));
				result[0] = ref;
			}
		});
		dialog.setVisible(true);
		return result[0];
	}

	@Override
	protected FLEFRecord showEditDialog(FLEFRecord existing){
		if(existing == null) return null;
		// Re-select a new record
		final FLEFRecord[] result = {null};
		_GenericSelectionDialog<?> dialog = new _GenericSelectionDialog<>(
			parentDialog, model, researchHandler, selectedRecord -> {
			if(selectedRecord != null){
				existing.setValue(XRefHelper.formatXRef(selectedRecord.getId()));
				result[0] = existing;
			}
		});
		dialog.setVisible(true);
		return result[0];
	}

	/**
	 * Loads all BLOCKED_BY children from the given record.
	 */
	public void load(FLEFRecord record){
		List<FLEFRecord> blockedByList = new ArrayList<>();
		for(FLEFRecord child : record.getChildren()){
			if(TAG_BLOCKED_BY.equals(child.getTag())){
				blockedByList.add(child);
			}
		}
		setItems(blockedByList);
	}

	/**
	 * Saves the current list of blocked_by as children of the given record.
	 */
	public void save(FLEFRecord record){
		FLEFRecordHelper.removeChildren(record, TAG_BLOCKED_BY);
		for(FLEFRecord blockedBy : getItems()){
			blockedBy.setTag(TAG_BLOCKED_BY);
			record.addChild(blockedBy);
		}
	}

	/**
	 * Returns the list of referenced record IDs.
	 */
	public List<String> getBlockedByIds(){
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
