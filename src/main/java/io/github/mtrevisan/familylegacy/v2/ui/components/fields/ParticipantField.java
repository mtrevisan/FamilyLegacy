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
package io.github.mtrevisan.familylegacy.v2.ui.components.fields;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.MultiTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * A text field that displays and manages a participant reference.
 * It extends {@link BoundTextField} and adds behavior for selecting,
 * editing, and clearing a participant record via a popup menu.
 */
public class ParticipantField extends BoundTextField{

	@Serial
	private static final long serialVersionUID = -8333332073516970045L;


	public static final String PROPERTY_PARTICIPANT_CHANGED = "participant-changed";


	private final Dialog parent;

	private final FLEFModel model;

	private boolean isDirect;
	private List<Class<? extends RecordTypeHandler<?>>> handlerTypes;

	private FLEFRecord participantRecord;


	/**
	 * Constructs a ParticipantField.
	 *
	 * @param path	the path used for binding (may be null if binding is handled externally
	 * @param parent	the parent dialog
	 * @param model	the FLEF model
	 * @return a new ParticipantField instance
	 */
	public static ParticipantField create(final String path, final Dialog parent, final FLEFModel model){
		return new ParticipantField(path, parent, model);
	}

	public static ParticipantField create(final String path, final Dialog parent, final FLEFModel model,
			final Class<? extends RecordTypeHandler<?>> handlerType){
		final ParticipantField field = new ParticipantField(path, parent, model);
		field.isDirect = true;
		field.setHandlerTypes(handlerType);
		return field;
	}


	private ParticipantField(final String path, final Dialog parent, final FLEFModel model){
		super(path);

		this.parent = parent;

		this.model = model;

		handlerTypes = Collections.emptyList();


		initComponents();
	}


	private void initComponents(){
		GUIHelper.installBehavior(this,
			this::editItem, null,
			null, null,
			builder -> {
				builder.item("Add Existing…", this::addItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit…", this::editItem);
				builder.selectionSensitiveItem("Clear", this::clear);
			}
		);

		addPropertyChangeListener(PROPERTY_PARTICIPANT_CHANGED, e -> updateDisplay());
	}

	private void updateDisplay(){
		String value = null;
		if(participantRecord != null && !participantRecord.isEmpty()){
			final FLEFRecord participant = model.getRecordById(participantRecord.getId());
			final RecordTypeHandler<?> handler = findHandler(participantRecord.getTag());
			if(participant != null && handler != null)
				value = handler.getDisplayText(participant, model);
		}
		setText(value);
	}

	/**
	 * Sets the allowed handler types for the participant.
	 *
	 * @param handlerTypes the record types this field can accept
	 */
	@SafeVarargs
	public final void setHandlerTypes(final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		for(final Class<? extends RecordTypeHandler<?>> handlerType : handlerTypes){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerType);
			if(handler == null){
				JOptionPane.showMessageDialog(this, "Handler for " + handlerType
						+ " not loaded.",
					"Error", JOptionPane.ERROR_MESSAGE);

				return;
			}
		}

		this.handlerTypes = List.of(handlerTypes);
	}

	/**
	 * Sets the current participant and updates the display.
	 *
	 * @param record	the participant record (may be null)
	 */
	public void setParticipant(final FLEFRecord record){
		participantRecord = record;

		firePropertyChange(PROPERTY_PARTICIPANT_CHANGED, null, null);
	}

	/**
	 * Clears the current selection.
	 */
	public void clear(){
		setParticipant(null);
	}

	/**
	 * Returns whether a participant is selected.
	 *
	 * @return	Whether a non-empty participant is set.
	 */
	public boolean hasData(){
		return (participantRecord != null && !participantRecord.isEmpty());
	}

	/**
	 * Returns the selected participant record.
	 *
	 * @return the participant record, or {@code null} if none
	 */
	public FLEFRecord getParticipantRecord(){
		return participantRecord;
	}

	/**
	 * Loads the participant from a given target record.
	 *
	 * @param record the record containing the reference
	 */
	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		FLEFRecord participant = null;
		if(isDirect){
			final FLEFRecord participantCitation = FLEFRecordHelper.findChild(record, path);
			if(participantCitation != null)
				participant = model.getRecordById(participantCitation.getValue());
		}
		else
			participant = FLEFRecordHelper.extractRecordFromXRef(record, path, model);
		setParticipant(participant);
	}

	/**
	 * Saves the current participant into the target record.
	 * Removes any existing child with the given path and adds a new one.
	 *
	 * @param targetRecord	the record to save into
	 */
	public void saveReferences(final FLEFRecord targetRecord){
		if(hasData()){
			final FLEFRecord parentNode;
			if(isDirect){
				final String id = participantRecord.getFormattedId();
				parentNode = FLEFRecord.createChildWithTag(path)
					.setValue(id);
			}
			else{
				final String tag = participantRecord.getTag();
				final String id = participantRecord.getFormattedId();
				final FLEFRecord child = FLEFRecord.createChildWithTagAndValue(tag, id);
				parentNode = FLEFRecord.createChildWithTag(path)
					.addChild(child);
			}
			targetRecord.addChild(parentNode);
		}
	}

	private void addItem(){
		if(handlerTypes.isEmpty()){
			JOptionPane.showMessageDialog(this, "Empty handler types.\n"
					+ "Cannot show dialog.",
				"Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		@SuppressWarnings("unchecked")
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model,
			handlerTypes.toArray(Class[]::new));
		dialog.addPropertyChangeListener(MultiTypeSelectionDialog.PROPERTY_TYPE_SELECTED, e -> {
			final FLEFRecord selectedRecord = dialog.getSelectedRecord();
			setParticipant(selectedRecord);
		});
		dialog.setVisible(true);
	}

	private void editItem(){
		if(!hasData()){
			addItem();

			return;
		}

		final RecordTypeHandler<?> handler = findHandler(participantRecord.getTag());
		if(handler == null)
			return;

		final BaseRecordDialog dialog = handler.createEditDialog(parent, model, participantRecord);
		dialog.setVisible(true);

		if(dialog.isSaved())
			// The record may have changed; refresh the display
			firePropertyChange(PROPERTY_PARTICIPANT_CHANGED, null, null);
	}

	private RecordTypeHandler<?> findHandler(final String type){
		for(final Class<? extends RecordTypeHandler<?>> headerType : handlerTypes){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(headerType);
			if(handler != null && handler.getType().equalsIgnoreCase(type))
				return handler;
		}
		return null;
	}


	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final ParticipantField other = (ParticipantField)obj;
		return Objects.equals(participantRecord, other.participantRecord);
	}

	@Override
	public int hashCode(){
		return Objects.hashCode(participantRecord);
	}

	@Override
	public String toString(){
		final StringBuilder sb = new StringBuilder(super.toString());
		if(participantRecord != null &&  !participantRecord.isEmpty())
			sb.append(", participant: ")
				.append(participantRecord);
		return sb.toString();
	}

}
