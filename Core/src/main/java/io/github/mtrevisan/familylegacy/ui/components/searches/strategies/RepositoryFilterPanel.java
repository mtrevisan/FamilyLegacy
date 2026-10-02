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

import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.ui.components.searches.RecordFilterPanel;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Filter panel for Repository records: repository name, custodian reference, and location.
 */
public class RepositoryFilterPanel extends JPanel implements RecordFilterPanel{

	private final JTextField nameField = new JTextField(20);
	private final JTextField custodianField = new JTextField(20);
	private final JTextField placeField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public RepositoryFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder(I18N.tf("dialog.search.filter.title", I18N.t("dialog.component.repository"))));

		add(new JLabel(I18N.t("dialog.repository.name") + ":"));
		add(nameField, "growx");
		add(new JLabel(I18N.t("dialog.repository.custodian") + ":"));
		add(custodianField, "growx");
		add(new JLabel(I18N.t("dialog.repository.place") + ":"));
		add(placeField, "growx");
	}

	private void setupListeners(){
		final DocumentListener docListener = new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				fireChanged();
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				fireChanged();
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				fireChanged();
			}
		};

		nameField.getDocument()
			.addDocumentListener(docListener);
		custodianField.getDocument()
			.addDocumentListener(docListener);
		placeField.getDocument()
			.addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(RepositoryReader.TAG_NAME, getRepositoryName());
		filters.put(RepositoryReader.TAG_CUSTODIAN, getCustodian());
		filters.put(RepositoryReader.TAG_PLACE, getRepositoryPlace());
		return filters;
	}

	public String getRepositoryName(){
		return nameField.getText()
			.trim();
	}

	public String getCustodian(){
		return custodianField.getText()
			.trim();
	}

	public String getRepositoryPlace(){
		return placeField.getText()
			.trim();
	}

}
