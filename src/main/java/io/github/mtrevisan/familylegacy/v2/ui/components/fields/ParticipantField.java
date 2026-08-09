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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.MultiTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


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

	private final List<String> handlerTypes;

	private final JTextField displayField;
	private String participantType;
	private FLEFRecord participantRecord;


	/**
	 * Constructs a ParticipantField with custom participant types.
	 *
	 * @param parent the parent dialog
	 * @param model  the FLEF model
	 * @param handlerTypes  the list of supported participant types
	 */
	public static ParticipantField create(final String path, final Dialog parent, final FLEFModel model,
			final List<String> handlerTypes){
		return new ParticipantField(path, parent, model, handlerTypes);
	}

	/**
	 * Constructs a ParticipantField with custom participant types.
	 *
	 * @param parent the parent dialog
	 * @param model  the FLEF model
	 * @param handlerType  the supported participant type
	 */
	public static ParticipantField create(final String path, final Dialog parent, final FLEFModel model,
			final String handlerType){
		return new ParticipantField(path, parent, model, List.of(handlerType));
	}


	private ParticipantField(final String path, final Dialog parent, final FLEFModel model,
			final List<String> handlerTypes){
		super(new MigLayout("ins 0,fillx", "[grow]"));

		this.parent = parent;

		this.path = path;
		this.model = model;
		this.handlerTypes = handlerTypes;
		this.displayField = new JTextField(20);


		initComponents();
	}


	private void initComponents(){
		displayField.setEditable(false);

		GUIHelper.installBehavior(displayField,
			this::edit,
			null,
			null,
			builder -> {
				builder.item("Add Existing...", this::add);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::edit);
				builder.selectionSensitiveItem("Clear", this::clear);
			});

		add(displayField, "growx");

		updateDisplay();
	}

	/**
	 * Sets the current participant.
	 *
	 * @param type   the participant type (e.g., "individual")
	 * @param record the participant record (must not be null if type is not null)
	 */
	public void setParticipant(final String type, final FLEFRecord record){
		this.participantType = type;
		this.participantRecord = record;

		updateDisplay();

		firePropertyChange(PROPERTY_PARTICIPANT, null, null);
	}

	/**
	 * Clears the current selection.
	 */
	public void clear(){
		setParticipant(null, null);
	}

	/**
	 * Returns whether a participant is selected.
	 */
	public boolean hasData(){
		return (participantRecord != null && participantType != null);
	}

	/**
	 * Returns the selected participant type.
	 */
	public String getParticipantType(){
		return participantType;
	}

	/**
	 * Returns the selected participant record.
	 */
	public FLEFRecord getParticipantRecord(){
		return participantRecord;
	}

	/**
	 * Loads the participant from the given record.
	 *
	 * @param targetRecord the record to load from
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
		final String type = participant.getTag();
		final String ref = participant.getValue();
		if(StringUtils.isEmpty(type) || StringUtils.isEmpty(ref))
			return;

		final FLEFRecord rec = model.getRecordById(ref);
		if(rec != null){
			// Optionally verify that the record's tag matches the expected type
			participantType = type;
			participantRecord = rec;
		}

		updateDisplay();
	}

	/**
	 * Saves the current participant.
	 * Removes any existing child with the given path and creates a new one.
	 *
	 * @param targetRecord the record to save into
	 */
	public void save(final FLEFRecord targetRecord){
		FLEFRecordHelper.removeChildren(targetRecord, path);

		if(hasData()){
			final FLEFRecord parentNode = FLEFRecord.createChild(path);
			final FLEFRecord child = FLEFRecord.createChildWithValue(participantType, participantRecord.getFormattedId());
			parentNode.addChild(child);
			targetRecord.addChild(parentNode);
		}
	}

	private void add(){
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model,
			handlerTypes,
			this::setParticipant
		);
		dialog.setVisible(true);
	}

	private void edit(){
		if(!hasData()){
			add();

			return;
		}

		final RecordTypeHandler<?> handler = findHandler(participantType);
		if(handler == null)
			return;

		final BaseRecordDialog editDialog = handler.createEditDialog(parent, model, participantRecord);
		editDialog.setVisible(true);

		if(editDialog.isSaved())
			// Only needed here because the reference to 'record' doesn't change, but the internal data does.
			updateDisplay();
	}

	private void updateDisplay(){
		final RecordTypeHandler<?> handler = findHandler(participantType);
		GUIHelper.updateDisplay(displayField,
			this::hasData,
			() -> (handler != null? handler.getDisplayText(participantRecord, model): null));
	}

	private RecordTypeHandler<?> findHandler(String type){
		for(final String desc : handlerTypes){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(desc);
			if(handler.getType().equalsIgnoreCase(type))
				return handler;
		}
		return null;
	}

}
