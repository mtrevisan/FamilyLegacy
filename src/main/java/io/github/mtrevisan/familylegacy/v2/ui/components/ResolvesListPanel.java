/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
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


	public ResolvesListPanel(FLEFModel model, Dialog parent){
		super(parent, "Resolves (Conflicting Events/Associations)", model);
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		GUIHelper.installBehavior(list,
			this::editItem,
			this::createNewItem,
			this::removeItem,
			builder -> {
				builder.item("Create New...", this::createNewItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			});
	}

	@Override
	protected String getDisplay(String id){
		return id;
	}

	@Override
	protected String showAddDialog(){
		String input = JOptionPane.showInputDialog(parent,
			"Enter the XREF ID of the conflicting event or association (e.g., @E123@):",
			"Add Resolves", JOptionPane.PLAIN_MESSAGE);
		if(StringUtils.isNotEmpty(input))
			return input.trim();
		return null;
	}

	/**
	 * Creates a new item and adds it to the list.
	 */
	@Override
	protected String showCreateNewDialog(){
		//TODO
		return null;
	}

	@Override
	protected String showEditDialog(String existing){
		String input = JOptionPane.showInputDialog(parent,
			"Edit XREF ID:", "Edit Resolves", JOptionPane.PLAIN_MESSAGE);
		if(StringUtils.isNotEmpty(input))
			return input.trim();
		return null;
	}

	@Override
	protected boolean validateItem(String item){
		// Check for duplicates
		if(items.contains(item)){
			JOptionPane.showMessageDialog(parent,
				"This ID is already in the list.", "Duplicate", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		return true;
	}

}
