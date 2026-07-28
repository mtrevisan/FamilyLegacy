package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.DateDialog;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.Dialog;
import java.util.function.Supplier;


public class DateField extends JTextField{

	private final Dialog parentDialog;
	private final String dialogTitle;
	private final FLEFModel model;
	private final String wrapperTag;

	private FLEFRecord record;


	public static DateField create(final Dialog parentDialog, final String dialogTitle, final FLEFModel model){
		return new DateField(parentDialog, dialogTitle, model, null);
	}

	public static DateField createWithWrapperTag(final Dialog parentDialog, final String dialogTitle,
			final FLEFModel model, final String wrapperTag){
		return new DateField(parentDialog, dialogTitle, model, wrapperTag);
	}


	private DateField(final Dialog parentDialog, final String dialogTitle, final FLEFModel model,
			final String wrapperTag){
		super(20);

		this.parentDialog = parentDialog;
		this.dialogTitle = dialogTitle;

		this.model = model;
		this.wrapperTag = wrapperTag;

		setEditable(false);
		setBackground(UIManager.getColor("TextField.background"));

		setupField(
			this,
			() -> record != null,
			this::createNew,
			this::edit,
			this::clear
		);
	}

	public static void setupField(final JTextField field,
			final Supplier<Boolean> hasSelection,
			final Runnable newAction, final Runnable editAction, final Runnable clearAction){
		field.setEditable(false);
		field.setBackground(UIManager.getColor("TextField.background"));
		GUIHelper.installBehavior(field,
			hasSelection,
			editAction,
			newAction,
			clearAction,
			builder -> {
				builder.item("Set Date...", newAction);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", editAction);
				builder.selectionSensitiveItem("Clear", clearAction);
			});
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
		final FLEFRecord wrapper = (wrapperTag != null
			? FLEFRecordUtils.findChild(parentRecord, wrapperTag)
			: parentRecord);
		if(wrapper != null)
			setRecord(FLEFRecordUtils.findChild(wrapper, "DATE"));
		else
			clear();
	}

	public void save(final FLEFRecord parentRecord){
		if(wrapperTag != null)
			FLEFRecordUtils.removeChildren(parentRecord, wrapperTag);
		else
			FLEFRecordUtils.removeChildren(parentRecord, "DATE");

		if(record != null){
			final FLEFRecord wrapper = (wrapperTag != null
				? FLEFRecord.createChild(wrapperTag)
				: parentRecord);
			wrapper.addChild(record);
			if(wrapperTag != null)
				parentRecord.addChild(wrapper);
		}
	}

	private void createNew(){
		final DateDialog dialog = DateDialog.createNew(parentDialog, model, dialogTitle);
		dialog.setVisible(true);

		if(dialog.isSaved())
			setRecord(dialog.getRecord());
	}

	private void edit(){
		if(record == null)
			return;

		final DateDialog dialog = DateDialog.createEdit(parentDialog, model, dialogTitle, record);
		dialog.setVisible(true);

		if(dialog.isSaved())
			updateDisplay();
	}

	private void updateDisplay(){
		setText(record != null
			? DatePanel.getDisplayText(record)
			: StringUtils.EMPTY);
	}

}
