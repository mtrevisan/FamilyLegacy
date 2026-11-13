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
package io.github.mtrevisan.familylegacy.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.search.SearchCriteria;
import io.github.mtrevisan.familylegacy.search.strategies.EventParticipationSearchStrategy;
import io.github.mtrevisan.familylegacy.ui.components.searches.RecordFilterPanel;
import io.github.mtrevisan.familylegacy.ui.bindings.ComboBoxSearch;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Filter panel for EventParticipation records: role, type of the event, and type of the participant.
 */
public class EventParticipationFilterPanel extends JPanel implements RecordFilterPanel{

	private static final String[] PARTICIPANT_TYPES = {EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, EventParticipationReader.TAG_PARTICIPANT_GROUP,
		EventParticipationReader.TAG_PARTICIPANT_PLACE};


	private final JComboBox<String> roleCombo = ComboBoxSearch.searchable(new JComboBox<>(GUIHelper.fillCombo(EventParticipationReader.ROLES,
		I18N.t("search.combo.any"))));
	private final JComboBox<String> eventTypeCombo = ComboBoxSearch.searchable(new JComboBox<>(GUIHelper.fillCombo(EventReader.TYPES, I18N.t("search.combo.any"))));
	private final JComboBox<String> participantTypeCombo = ComboBoxSearch.searchable(new JComboBox<>(GUIHelper.fillCombo(PARTICIPANT_TYPES,
		I18N.t("search.combo.any"))));

	private final Consumer<SearchCriteria> onChanged;


	public EventParticipationFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder(I18N.tf("dialog.search_filter.title",
			I18N.t("dialog.component.event.participations"))));

		add(new JLabel(I18N.l("dialog.event_participation.role")));
		add(roleCombo, "growx");
		add(new JLabel(I18N.l("dialog.event.type")));
		add(eventTypeCombo, "growx");
		add(new JLabel(I18N.l("dialog.event_participation.participant")));
		add(participantTypeCombo, "growx");

		roleCombo.addActionListener(e -> fireChanged());
		eventTypeCombo.addActionListener(e -> fireChanged());
		participantTypeCombo.addActionListener(e -> fireChanged());
	}


	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(EventParticipationReader.TAG_ROLE, selected(roleCombo));
		filters.put(EventReader.TAG_TYPE, selected(eventTypeCombo));
		filters.put(EventParticipationSearchStrategy.FILTER_PARTICIPANT_TYPE, selected(participantTypeCombo));
		return filters;
	}

	private static String selected(final JComboBox<String> combo){
		return (combo.getSelectedIndex() == 0? null: (String)combo.getSelectedItem());
	}

}
