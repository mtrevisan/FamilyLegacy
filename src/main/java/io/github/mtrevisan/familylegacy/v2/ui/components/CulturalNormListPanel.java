package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.CulturalNormDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


/**
 * Panel for managing a list of CULTURAL_NORM references.
 */
public class CulturalNormListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -4182038208327584807L;


	static{
		HandlerRegistry.register(new CulturalNormHandler());
	}


	private final String path;

	private final RecordTypeHandler<?> culturalNormHandler;


	public CulturalNormListPanel(final String path, final Dialog parentDialog, final FLEFModel model){
		super(parentDialog, "Cultural Norms", model);

		this.path = path;

		culturalNormHandler = HandlerRegistry.getHandler(CulturalNormHandler.TYPE);
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		GUIHelper.installBehavior(list,
			() -> (list.getSelectedIndex() >= 0),
			this::editItem,
			this::createNewItem,
			this::removeItem,
			builder -> {
				builder.item("Create New...", this::createNewItem);
				builder.item("Add Existing...", this::addItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			});
	}

	@Override
	protected String getDisplay(final FLEFRecord culturalNorm){
		if(culturalNorm != null)
			return culturalNormHandler.getDisplayText(culturalNorm, model);

		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parentDialog, model, culturalNormHandler, selectedId -> {
			final FLEFRecord culturalNorm = model.getRecordById(selectedId);
			if(culturalNorm != null && !items.contains(culturalNorm))
				result[0] = culturalNorm;
		}
		);
		dialog.setVisible(true);

		return result[0];
	}

	/**
	 * Creates a new cultural norm and adds it to the list.
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
		final CulturalNormDialog dialog = (CulturalNormDialog)culturalNormHandler.createNewDialog(parentDialog, model);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parentDialog, "Cultural norm not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}
		JDialog dialog = culturalNormHandler.createEditDialog(null, model, existing);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return existing;
	}

	public void load(final FLEFRecord record){
		final List<FLEFRecord> culturalNorms = FLEFRecordHelper.findChildren(record, path);
		setItems(culturalNorms);
	}

	public void save(final FLEFRecord record){
		for(final FLEFRecord culturalNorm : getItems())
			FLEFRecordHelper.addChild(record, path, culturalNorm.getFormattedId());
	}

}
