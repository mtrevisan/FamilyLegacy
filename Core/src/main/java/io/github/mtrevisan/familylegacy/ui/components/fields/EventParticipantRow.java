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
package io.github.mtrevisan.familylegacy.ui.components.fields;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quickentry.ParticipationCreator;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import net.miginfocom.swing.MigLayout;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * The quick way to say who took part in an event: a person, the role in plain words (witness, godparent, officiant...) and <em>Add</em>,
 * with no nested editor. The participations are made in the model at once, and the dialog that shows them undoes them if it is cancelled.
 */
public final class EventParticipantRow extends JPanel{

	/** The roles of a person at an event that are asked first, in the order a family event needs them. */
	private static final List<String> FIRST_ROLES = List.of(EventParticipationReader.ENUM_ROLE_WITNESS,
		EventParticipationReader.ENUM_ROLE_SPONSOR, EventParticipationReader.ENUM_ROLE_OFFICIANT,
		EventParticipationReader.ENUM_ROLE_INFORMANT, EventParticipationReader.ENUM_ROLE_MIDWIFE,
		EventParticipationReader.ENUM_ROLE_PRINCIPAL);


	private final Window owner;
	private final FLEFModel model;
	private final Supplier<String> eventId;
	private final Consumer<Runnable> undoRegistrar;
	private final Runnable onAdded;

	final JComboBox<String> roleCombo = new JComboBox<>(roles().toArray(String[]::new));
	final JButton addButton = new JButton(I18N.t("dialog.event.add_person.add"));
	private final EntityField personField;


	/**
	 * @param owner	The dialog the row is in.
	 * @param model	The model.
	 * @param eventId	The id of the event, read when a person is added (the event may be new, and not in the file yet).
	 * @param undoRegistrar	Told what undoes an addition, for the dialog to do it if it is cancelled.
	 * @param onAdded	What to do after a person is added (the list of the participations is read again).
	 */
	public EventParticipantRow(final Window owner, final FLEFModel model, final Supplier<String> eventId,
			final Consumer<Runnable> undoRegistrar, final Runnable onAdded){
		super(new MigLayout("ins 0", "[grow,fill]10[]10[]", "[]"));

		this.owner = owner;
		this.model = model;
		this.eventId = eventId;
		this.undoRegistrar = undoRegistrar;
		this.onAdded = onAdded;

		//the person is chosen with the right button of the mouse, as every reference
		personField = EntityField.createForRecordFromReference(null, owner, model, IndividualHandler.class);
		personField.setToolTipText(I18N.t("dialog.event.add_person.tooltip"));
		personField.addPropertyChangeListener(EntityField.PROPERTY_ENTITY_CHANGED, e -> addButton.setEnabled(personField.hasData()));
		roleCombo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				return super.getListCellRendererComponent(list, label(String.valueOf(value)), index, isSelected, cellHasFocus);
			}
		});
		roleCombo.setToolTipText(I18N.t("dialog.event.add_person.role.tooltip"));
		addButton.addActionListener(e -> add());
		addButton.setEnabled(false);

		add(personField);
		add(roleCombo);
		add(addButton);
	}


	/** @return	The roles offered, the ones of a family event first, then the other roles a person can have. */
	static List<String> roles(){
		final List<String> roles = new ArrayList<>(FIRST_ROLES);
		for(final String role : EventParticipationReader.INDIVIDUAL_OR_GROUP_ROLES)
			if(!roles.contains(role))
				roles.add(role);
		return roles;
	}

	/** @return	The role in plain words: godparent for the sponsor, and so on (the usual name of the role if there is no other). */
	static String label(final String role){
		return I18N.t("dialog.event.add_person.role." + role, I18N.t("enum.event_participation.role." + role, role));
	}

	/** Takes the person to add. */
	void select(final FLEFRecord person){
		personField.setEntity(person);
		roleCombo.requestFocusInWindow();
	}

	private void add(){
		if(!personField.hasData())
			return;

		final FLEFRecord participation = ParticipationCreator.addIndividual(model, eventId.get(), personField.getEntity().getId(),
			(String)roleCombo.getSelectedItem());
		if(participation != null){
			final String id = participation.getId();
			//if the event is not saved, the participation goes with it
			undoRegistrar.accept(() -> model.removeRecord(id));
			onAdded.run();
		}

		//the next person: the role stays, as the witnesses of an event are added one after the other
		personField.clear();
	}

}
