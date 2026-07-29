package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Panel for managing a list of RESOLVES references (XREF IDs of conflicting events/associations).
 */
public class ResolvesListPanel extends AbstractListPanel<String>{

	@Serial
	private static final long serialVersionUID = 4468567692587615182L;


	public ResolvesListPanel(FLEFModel model, Dialog parentDialog){
		super(parentDialog, "Resolves (Conflicting Events/Associations)", model);
	}


	@Override
	protected String getDisplay(String id){
		return id;
	}

	@Override
	protected String showAddDialog(){
		String input = JOptionPane.showInputDialog(parentDialog,
			"Enter the XREF ID of the conflicting event or association (e.g., @E123@):",
			"Add Resolves", JOptionPane.PLAIN_MESSAGE);
		if(!StringUtils.isEmpty(input)){
			return input.trim();
		}
		return null;
	}

	@Override
	protected String showEditDialog(String existing){
		String input = JOptionPane.showInputDialog(parentDialog,
			"Edit XREF ID:", "Edit Resolves", JOptionPane.PLAIN_MESSAGE);
		if(!StringUtils.isEmpty(input)){
			return input.trim();
		}
		return null;
	}

	@Override
	protected boolean validateItem(String item){
		// Check for duplicates
		if(items.contains(item)){
			JOptionPane.showMessageDialog(parentDialog,
				"This ID is already in the list.", "Duplicate", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		return true;
	}

}
