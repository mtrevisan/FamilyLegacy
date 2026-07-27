package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;

import javax.swing.*;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Panel for managing a list of CULTURAL_NORM references (XREF IDs).
 */
public class CulturalNormListPanel extends AbstractListPanel<String>{

	@Serial
	private static final long serialVersionUID = -4182038208327584807L;


	private final CulturalNormHandler culturalNormHandler = new CulturalNormHandler();


	public CulturalNormListPanel(FLEFModel model, Dialog parentDialog){
		super(model, parentDialog, "Cultural Norms");
	}

	@Override
	protected String getDisplay(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return culturalNormHandler.getDisplayName(rec);
		}
		return id;
	}

	@Override
	protected String showAddDialog(){
		final String[] result = {null};
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(CulturalNormHandler.TYPE);
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
			JOptionPane.showMessageDialog(parentDialog, "Cultural norm record not found: " + existing,
				"Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(CulturalNormHandler.TYPE);
		JDialog dialog = handler.createEditDialog(null, model, rec);
		dialog.setVisible(true);
		return existing;
	}

}
