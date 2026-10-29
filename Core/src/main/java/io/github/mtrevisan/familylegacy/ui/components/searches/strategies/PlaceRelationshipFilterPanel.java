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

import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.ui.components.searches.RecordFilterPanel;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Filter panel for {@code PlaceRelationshipRecord}.
 * <p>
 * The only filter exposed here is the relationship type
 * (administrative, ecclesiastical, judicial, cadastral, geographic).
 * The {@code subject} and {@code object} are not exposed as filters:
 * hiding an edge by one of its endpoints would leave a dangling node
 * in the hierarchy tree, which is worse than showing an unfiltered
 * edge. The user navigates to the relevant place through the tree,
 * not by typing a place name into a filter.
 * <p>
 * The panel drives the hierarchy's relation-type filter through
 * {@link #getFilters()}: the dialog reads the map, extracts the
 * {@code type} value, and forwards it to
 * {@code PlaceHierarchyPanel.setRelationTypeFilter}.
 */
public class PlaceRelationshipFilterPanel extends JPanel implements RecordFilterPanel{

	private final JComboBox<String> typeCombo = new JComboBox<>(GUIHelper.fillCombo(PlaceRelationshipReader.TYPES, I18N.t("search.combo.any")));

	private final Consumer<SearchCriteria> onChanged;


	public PlaceRelationshipFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();
		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder(I18N.tf("dialog.search_filter.title", I18N.t("dialog.component.place.relationships"))));

		add(new JLabel(I18N.l("dialog.place_relationship.type")));
		add(typeCombo, "growx");
	}

	private void setupListeners(){
		typeCombo.addActionListener(e -> fireChanged());
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(PlaceRelationshipReader.TAG_TYPE, getType());
		return filters;
	}

	/**
	 * Returns the currently selected relationship type, or {@code null}
	 * when the "any" entry is selected.
	 */
	public String getType(){
		return (typeCombo.getSelectedIndex() == 0? null: (String)typeCombo.getSelectedItem());
	}

}
