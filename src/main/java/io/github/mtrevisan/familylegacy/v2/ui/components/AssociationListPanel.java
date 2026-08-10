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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.AssociationStructureDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.AssociationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


/* DONE */
/**
 * Panel for managing a list of association structures.
 * Each association has a target (Xref or VOID) and an optional name.
 * <p>
 * Structure:
 * <pre>
 * association: struct {
 *   target: XrefOrVoid&lt;LocalID&gt;
 *   name?: Text
 * }
 * </pre>
 */
public class AssociationListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 5201318669653810012L;


	static{
		HandlerRegistry.register(new AssociationHandler());
	}

	private final String path;

	private final RecordTypeHandler<?> associationHandler = HandlerRegistry.getHandler(AssociationHandler.TYPE);


	public AssociationListPanel(final String path, final Dialog parent, final FLEFModel model){
		super(parent, "Associations", model);

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
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			}
		);
	}

	@Override
	protected String getDisplay(final FLEFRecord association){
		if(association != null)
			return associationHandler.getDisplayText(association, model);

		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		return null;
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		final AssociationStructureDialog dialog = (AssociationStructureDialog)associationHandler.createNewDialog(parent, model);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Association not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final JDialog dialog = associationHandler.createEditDialog(parent, model, existing);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return existing;
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final List<FLEFRecord> associations = FLEFRecordHelper.findChildren(record, path);
		setItems(associations);
	}

	public void save(final FLEFRecord record){
		super.save(record, path);
	}

	public boolean hasData(){
		return !items.isEmpty();
	}

}
