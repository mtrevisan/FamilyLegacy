package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchStatusHandler;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Panel for managing a list of RESEARCH references (XREF IDs of RESEARCH_STATUS records).
 */
public class ResearchStatusListPanel extends AbstractListPanel<String>{

	@Serial
	private static final long serialVersionUID = 8660802930133158028L;


	static{
		HandlerRegistry.register(new ResearchStatusHandler());
	}

	private final ResearchStatusHandler researchHandler = new ResearchStatusHandler();


	public ResearchStatusListPanel(FLEFModel model, Dialog parentDialog){
		super(parentDialog, "Research References", model);
	}

	@Override
	protected String getDisplay(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return researchHandler.getDisplayText(rec);
		}
		return id;
	}

	@Override
	protected String showAddDialog(){
		final String[] result = {null};
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(ResearchStatusHandler.TYPE);
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parentDialog, model, handler, selectedId -> {
			if(selectedId != null){
				result[0] = selectedId;
			}
		});
		dialog.setVisible(true);
		return result[0];
	}

	@Override
	protected String showEditDialog(String existing){
		FLEFRecord rec = model.getRecordById(existing);
		if(rec == null){
			JOptionPane.showMessageDialog(parentDialog, "Research record not found: " + existing,
				"Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(ResearchStatusHandler.TYPE);
		JDialog dialog = handler.createEditDialog(parentDialog, model, rec);
		dialog.setVisible(true);
		// Return the same ID (the record was edited in place)
		return existing;
	}

	@Override
	protected boolean validateItem(String item){
		if(items.contains(item)){
			JOptionPane.showMessageDialog(parentDialog,
				"This research reference is already in the list.", "Duplicate", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		return true;
	}

}
