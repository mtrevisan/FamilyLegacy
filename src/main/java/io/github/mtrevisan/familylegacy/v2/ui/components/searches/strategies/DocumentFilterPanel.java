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
package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Filter panel for Document records based on description, mapping projection, and URI.
 */
public class DocumentFilterPanel extends JPanel implements RecordFilterPanel{

	static final String FILTER_KEY_DESCRIPTION = "description";
	static final String FILTER_KEY_MAPPING = "mapping";
	static final String FILTER_KEY_URI = "uri";


	private final JTextField descriptionField = new JTextField(20);
	private final JComboBox<String> mappingCombo = new JComboBox<>(new String[]{
		"Any",
		"planar",
		"spherical_equirectangular",
		"spherical_uv",
		"cubemap",
		"cylindrical_equirectangular_horizontal",
		"cylindrical_equirectangular_vertical"
	});
	private final JTextField uriField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public DocumentFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Document Filters"));

		add(new JLabel("Description:"));
		add(descriptionField, "growx");
		add(new JLabel("Mapping:"));
		add(mappingCombo, "growx");
		add(new JLabel("URI / Path:"));
		add(uriField, "growx");
	}

	private void setupListeners(){
		mappingCombo.addActionListener(e -> fireChanged());

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

		descriptionField.getDocument()
			.addDocumentListener(docListener);
		uriField.getDocument()
			.addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(FILTER_KEY_DESCRIPTION, getDescription());
		filters.put(FILTER_KEY_MAPPING, getMapping());
		filters.put(FILTER_KEY_URI, getUri());
		return filters;
	}

	public String getDescription(){
		return descriptionField.getText()
			.trim();
	}

	public String getMapping(){
		return (mappingCombo.getSelectedIndex() == 0? null: (String)mappingCombo.getSelectedItem());
	}

	public String getUri(){
		return uriField.getText()
			.trim();
	}

}
