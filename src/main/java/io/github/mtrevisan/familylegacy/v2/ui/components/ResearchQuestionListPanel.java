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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Panel for managing a list of {@code RESEARCH_STATUS} references according to FLEF 0.1.1.
 */
public class ResearchQuestionListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 8660802930133158028L;


	static{
		HandlerRegistry.register(new ResearchQuestionHandler());
	}


	private final String path;

	private final RecordTypeHandler<?> researchQuestionHandler = HandlerRegistry.getHandler(ResearchQuestionHandler.TYPE);


	public ResearchQuestionListPanel(final String path, final Dialog parent, final FLEFModel model){
		this(path, parent, "Researches", model);
	}

	public ResearchQuestionListPanel(final String path, final Dialog parent, final String borderTitle, final FLEFModel model){
		super(parent, borderTitle, model);

		this.path = path;
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
				builder.item("Add Existing...", this::addItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			}
		);
	}

	@Override
	protected String getDisplay(final FLEFRecord researchQuestion){
		if(researchQuestion != null)
			return researchQuestionHandler.getDisplayText(researchQuestion, model);

		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model,
			ResearchQuestionHandler.TYPE,
			(handlerType, selectedRecord) -> result[0] = selectedRecord
		);
		dialog.setVisible(true);

		return result[0];
	}

	/**
	 * Creates a new research status and adds it to the list.
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
//		final ResearchLogRecordDialog dialog = (ResearchLogRecordDialog)researchQuestionHandler.createNewDialog(parent, model);
//		dialog.setVisible(true);
//
//		return (dialog.isSaved()? dialog.getRecord(): null);
		return null;
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Research record not found: " + existing,
				"Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		JDialog dialog = researchQuestionHandler.createEditDialog(parent, model, existing);
		dialog.setVisible(true);

		// Return the same ID (the record was edited in place)
		return existing;
	}

	@Override
	protected boolean validateItem(FLEFRecord item){
		if(items.contains(item)){
			JOptionPane.showMessageDialog(parent,
				"This research reference is already in the list.", "Duplicate", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		return true;
	}

}
