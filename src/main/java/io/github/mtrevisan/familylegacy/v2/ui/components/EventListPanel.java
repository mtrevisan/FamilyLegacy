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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.MultiTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Panel for managing a list of EVENT references (XREF IDs).
 */
public class EventListPanel extends AbstractListPanel<String>{

	@Serial
	private static final long serialVersionUID = 4727208227799748736L;


	static{
		HandlerRegistry.register(new EventHandler());
	}


	private static final RecordTypeHandler<?> eventHandler = HandlerRegistry.getHandler(EventHandler.TYPE);


	public EventListPanel(Dialog parent, FLEFModel model){
		super(parent, "Events", model);
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
		if(rec != null)
			return eventHandler.getDisplayText(rec, model);
		return id;
	}

	@Override
	protected String showAddDialog(){
		final String[] result = {null};
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model,
			EventHandler.TYPE,
			(handlerType, selectedRecord) -> result[0] = selectedRecord.getValue()
		);
		dialog.setVisible(true);

		return result[0];
	}

	/**
	 * Creates a new event and adds it to the list.
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
			JOptionPane.showMessageDialog(parent, "Event record not found: " + existing,
				"Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		JDialog dialog = eventHandler.createEditDialog(null, model, rec);
		dialog.setVisible(true);

		return existing;
	}

}
