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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchStatusHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

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

	private final RecordTypeHandler<?> researchStatusHandler = HandlerRegistry.getHandler(ResearchStatusHandler.TYPE);


	public ResearchStatusListPanel(Dialog parent, FLEFModel model){
		super(parent, "Research References", model);
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
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return researchStatusHandler.getDisplayText(rec, model);
		}
		return id;
	}

	@Override
	protected String showAddDialog(){
		final String[] result = {null};
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, researchStatusHandler, selectedItem -> {
			if(selectedItem != null){
				result[0] = selectedItem.getValue();
			}
		});
		dialog.setVisible(true);

		return result[0];
	}

	/**
	 * Creates a new research status and adds it to the list.
	 */
	@Override
	protected String showCreateNewDialog(){
		//TODO
		return null;
	}

	@Override
	protected String showEditDialog(String existing){
		FLEFRecord rec = model.getRecordById(existing);
		if(rec == null){
			JOptionPane.showMessageDialog(parent, "Research record not found: " + existing,
				"Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		JDialog dialog = researchStatusHandler.createEditDialog(parent, model, rec);
		dialog.setVisible(true);

		// Return the same ID (the record was edited in place)
		return existing;
	}

	@Override
	protected boolean validateItem(String item){
		if(items.contains(item)){
			JOptionPane.showMessageDialog(parent,
				"This research reference is already in the list.", "Duplicate", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		return true;
	}

}
