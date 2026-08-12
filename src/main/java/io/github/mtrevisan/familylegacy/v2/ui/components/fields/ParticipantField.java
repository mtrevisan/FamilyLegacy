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
import net.miginfocom.swing.MigLayout;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.Dialog;
import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/* DONE */
/**
 * Component for selecting and displaying an EntityParticipant.
 */
public class ParticipantField extends JPanel{

	@Serial
	private static final long serialVersionUID = -8333332073516970045L;


	public static final String PROPERTY_PARTICIPANT = "participant";


	private final Dialog parent;
	private final String path;
	private final FLEFModel model;

	private List<String> handlerTypes;

	private final BoundTextField displayField;
	private FLEFRecord participantRecord;

	private final EventListenerList listenerList = new EventListenerList();


	/**
	 * Constructs a ParticipantField.
	 *
	 * @param path	the path in the record structure
	 * @param parent	the parent dialog
	 * @param model	the FLEF model
	 * @return a new ParticipantField instance
	 */
	public static ParticipantField create(final String path, final Dialog parent, final FLEFModel model){
		return new ParticipantField(path, parent, model);
	}


	private ParticipantField(final String path, final Dialog parent, final FLEFModel model){
		super(new MigLayout("ins 0,fillx", "[grow]"));

		this.parent = parent;

		this.path = path;
		this.model = model;
		this.handlerTypes = Collections.emptyList();
		this.displayField = new BoundTextField(null);


		initComponents();
	}


	private void initComponents(){
		displayField.setEditable(false);

		GUIHelper.installBehavior(displayField,
			this::edit,
			null,
			null,
			builder -> {
				builder.item("Set…", this::set);
				builder.separator();
				builder.selectionSensitiveItem("Edit…", this::edit);
				builder.selectionSensitiveItem("Clear", this::clear);
			});

		add(displayField, "growx");

		updateDisplay();
	}

	public void addChangeListener(final ChangeListener listener){
		listenerList.add(ChangeListener.class, listener);
	}

	public void removeChangeListener(final ChangeListener listener){
		listenerList.remove(ChangeListener.class, listener);
	}

	protected void fireStateChanged(){
		final Object[] listeners = listenerList.getListenerList();
		ChangeEvent e = null;
		for(int i = listeners.length - 2; i >= 0; i -= 2)
			if(listeners[i] == ChangeListener.class){
				if(e == null)
					e = new ChangeEvent(this);

				((ChangeListener)listeners[i + 1]).stateChanged(e);
			}
	}

	public void setHandlerType(final String handlerType){
		setHandlerTypes(List.of(handlerType));
	}

	public void setHandlerTypes(final List<String> handlerTypes){
		for(final String handlerType : handlerTypes){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerType);
			if(handler == null){
				JOptionPane.showMessageDialog(this, "Handler for " + handlerType
						+ " not loaded.",
					"Error", JOptionPane.ERROR_MESSAGE);

				return;
			}
		}

		this.handlerTypes = handlerTypes;
	}

	/**
	 * Sets the current participant.
	 *
	 * @param record	the participant record (must not be null if type is not null)
	 */
	public void setParticipant(final FLEFRecord record){
		this.participantRecord = record;

		updateDisplay();

		firePropertyChange(PROPERTY_PARTICIPANT, null, null);
		fireStateChanged();
	}

	/**
	 * Clears the current selection.
	 */
	public void clear(){
		participantRecord = null;
		displayField.setText(null);

		firePropertyChange(PROPERTY_PARTICIPANT, null, null);
		fireStateChanged();
	}

	/**
	 * Returns whether a participant is selected.
	 *
	 * @return	Whether a participant is set.
	 */
	public boolean hasData(){
		return (participantRecord != null && !participantRecord.isEmpty());
	}

	/**
	 * Returns the selected participant record.
	 *
	 * @return the participant record
	 */
	public FLEFRecord getParticipantRecord(){
		return participantRecord;
	}

	/**
	 * Loads the participant from the given record.
	 *
	 * @param targetRecord	the record to load from
	 */
	public void load(final FLEFRecord targetRecord){
		clear();

		if(targetRecord == null)
			return;

		final FLEFRecord node = FLEFRecordHelper.findChild(targetRecord, path);
		if(node == null || node.getChildren().size() != 1)
			return;

		final FLEFRecord participant = node.getChildren()
			.getFirst();
		if(participant.isEmpty())
			return;

		final String ref = participant.getValue();
		final FLEFRecord rec = model.getRecordById(ref);
		if(rec != null)
			// Optionally verify that the record's tag matches the expected type
			setParticipant(rec);

		updateDisplay();
	}

	/**
	 * Saves the current participant.
	 * Removes any existing child with the given path and creates a new one.
	 *
	 * @param targetRecord	the record to save into
	 */
	public void saveReferences(final FLEFRecord targetRecord){
		FLEFRecordHelper.removeChildren(targetRecord, path);

		if(hasData()){
			final FLEFRecord parentNode = FLEFRecord.createChild(path);
			final FLEFRecord child = FLEFRecord.createChildWithValue(participantRecord.getTag(), participantRecord.getFormattedId());
			parentNode.addChild(child);
			targetRecord.addChild(parentNode);
		}
	}

	private void set(){
		if(handlerTypes.isEmpty()){
			JOptionPane.showMessageDialog(this, "Empty handler types.\n"
					+ "Cannot show dialog.",
				"Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model,
			handlerTypes,
			(handlerType, selectedRecord) -> setParticipant(selectedRecord)
		);
		dialog.setVisible(true);
	}

	private void edit(){
		if(!hasData()){
			set();

			return;
		}

		final RecordTypeHandler<?> handler = findHandler(participantRecord.getTag());
		if(handler == null)
			return;

		final BaseRecordDialog editDialog = handler.createEditDialog(parent, model, participantRecord);
		editDialog.setVisible(true);

		if(editDialog.isSaved())
			// Only needed here because the reference to 'record' doesn't change, but the internal data does.
			updateDisplay();
	}

	private void updateDisplay(){
		if(participantRecord != null){
			final RecordTypeHandler<?> handler = findHandler(participantRecord.getTag());
			GUIHelper.updateDisplay(displayField,
				this::hasData,
				() -> (handler != null? handler.getDisplayText(participantRecord, model): null));
		}
	}

	private RecordTypeHandler<?> findHandler(final String type){
		for(final String desc : handlerTypes){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(desc);
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

}
