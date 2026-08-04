package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Panel for managing a list of PLACE references (XREF IDs).
 */
public class PlaceListPanel extends AbstractListPanel<String>{

	@Serial
	private static final long serialVersionUID = -5998352597761066840L;


	static{
		HandlerRegistry.register(new PlaceHandler());
	}

	private final PlaceHandler placeHandler = new PlaceHandler();


	public PlaceListPanel(final Dialog parent, final FLEFModel model){
		super(parent, "Places", model);
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
	protected String getDisplay(final String id){
		final FLEFRecord record = model.getRecordById(id);
		return (record != null? placeHandler.getDisplayText(record, model): id);

	}

	@Override
	protected String showAddDialog(){
		final String[] result = {null};
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(PlaceHandler.TYPE);
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, handler, selectedId -> {
			if(selectedId != null)
				result[0] = selectedId;
		});
		dialog.setVisible(true);

		return result[0];
	}

	/**
	 * Creates a new place and adds it to the list.
	 */
	@Override
	protected String showCreateNewDialog(){
		//TODO
		return null;
	}

	@Override
	protected String showEditDialog(final String existing){
		final FLEFRecord rec = model.getRecordById(existing);
		if(rec == null){
			JOptionPane.showMessageDialog(parent, "Place record not found: " + existing,
				"Error", JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(PlaceHandler.TYPE);
		final JDialog dialog = handler.createEditDialog(null, model, rec);
		dialog.setVisible(true);

		return existing;
	}

}
