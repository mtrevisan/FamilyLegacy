package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.PlaceDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.PlaceStructureDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.Dialog;
import java.util.function.Supplier;


/* DONE */
public class PlaceField extends JTextField{

	private final Dialog parentDialog;
	private final String dialogTitle;

	private final FLEFModel model;

	private FLEFRecord record;


	public static PlaceField create(final Dialog parent, final String dialogTitle, final FLEFModel model){
		return new PlaceField(parent, dialogTitle, model);
	}

	public static PlaceField createWithWrapperTag(final Dialog parent, final String dialogTitle, final FLEFModel model){
		return new PlaceField(parent, dialogTitle, model);
	}


	private PlaceField(final Dialog parent, final String dialogTitle, final FLEFModel model){
		super(20);

		this.parentDialog = parent;
		this.dialogTitle = dialogTitle;

		this.model = model;

		setEditable(false);
		setBackground(UIManager.getColor("TextField.background"));

		initComponents();
	}

	private void initComponents(){
		setupField(this,
			() -> (record != null),
			this::createNew,
			this::add,
			this::edit,
			this::editCitation,
			this::clear
		);
	}

	private void setupField(final JTextField field,
			final Supplier<Boolean> hasSelection,
			final Runnable newAction, final Runnable addAction, final Runnable editAction,
			final Runnable editCitationAction, final Runnable clearAction){
		field.setEditable(false);
		field.setBackground(UIManager.getColor("TextField.background"));
		GUIHelper.installBehavior(field,
			hasSelection,
			editAction,
			newAction,
			clearAction,
			builder -> {
				builder.item("Create New...", newAction);
				builder.item("Add Existing...", addAction);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", editAction);
				builder.selectionSensitiveItem("Edit Citation...", editCitationAction);
				builder.selectionSensitiveItem("Clear", clearAction);
			}
		);
	}

	public void setRecord(final FLEFRecord record){
		this.record = record;

		updateDisplay();
	}

	public void clear(){
		setRecord(null);
	}

	public boolean hasData(){
		return (record != null);
	}

	public void load(final FLEFRecord parentRecord){
		if(parentRecord != null){
			final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler(PlaceHandler.TYPE);
			setText(placeHandler.getDisplayText(parentRecord));
		}
		else
			clear();
	}

	public void save(final FLEFRecord parentRecord){
		FLEFRecordUtils.removeChildren(parentRecord, "PLACE");

		if(record != null){
			final FLEFRecord wrapper = FLEFRecord.createChildWithValue("PLACE", record.getFormattedId());
			parentRecord.addChild(wrapper);
		}
	}

	private void createNew(){
		final PlaceDialog dialog = PlaceDialog.createNew(parentDialog, model);
		dialog.setVisible(true);

		if(dialog.isSaved())
			setRecord(dialog.getRecord());
	}

	private void add(){
		final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler("PLACE");
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parentDialog, model, placeHandler,
			selectedId -> {
				if(selectedId != null){
					final FLEFRecord record = model.getRecordById(selectedId);
					setRecord(record);
				}
			}
		);
		dialog.setVisible(true);
	}

	private void edit(){
		if(record == null)
			return;

		final PlaceDialog dialog = PlaceDialog.createEdit(parentDialog, model, record);
		dialog.setVisible(true);
		if(dialog.isSaved())
			updateDisplay();
	}

	private void editCitation(){
		if(record == null)
			return;

		final PlaceStructureDialog dialog = new PlaceStructureDialog(parentDialog, model, record);
		dialog.setVisible(true);
		if(dialog.isSaved())
			updateDisplay();
	}

	private void updateDisplay(){
		String displayText = StringUtils.EMPTY;
		if(record != null){
			final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler(PlaceHandler.TYPE);
			displayText = placeHandler.getDisplayText(record);
		}
		setText(displayText);
	}

}
