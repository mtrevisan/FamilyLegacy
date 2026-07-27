package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;

import javax.swing.*;
import java.awt.Dialog;
import java.awt.Frame;
import java.io.Serial;


/**
 * Panel for managing a list of EVENT references (XREF IDs).
 */
public class EventListPanel extends AbstractListPanel<String>{

	@Serial
	private static final long serialVersionUID = 4727208227799748736L;


	private final EventHandler eventHandler = new EventHandler();


	public EventListPanel(FLEFModel model, Dialog parentDialog){
		super(model, parentDialog, "Events");
	}

	@Override
	protected String getDisplay(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return eventHandler.getDisplayName(rec);
		}
		return id;
	}

	@Override
	protected String showAddDialog(){
		final String[] result = {null};
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(EventHandler.TYPE);
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			null, model, handler, selectedId -> {
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
			JOptionPane.showMessageDialog(parentDialog, "Event record not found: " + existing,
				"Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(EventHandler.TYPE);
		JDialog dialog = handler.createEditDialog(null, model, rec);
		dialog.setVisible(true);
		return existing;
	}

}
