package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
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
		if(rec != null){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(EventHandler.TYPE);
			return handler.getDisplayText(rec, model);
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
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(EventHandler.TYPE);
		JDialog dialog = handler.createEditDialog(null, model, rec);
		dialog.setVisible(true);

		return existing;
	}

}
