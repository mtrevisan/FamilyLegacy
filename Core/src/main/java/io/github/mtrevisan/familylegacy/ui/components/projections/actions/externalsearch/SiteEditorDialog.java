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

package io.github.mtrevisan.familylegacy.ui.components.projections.actions.externalsearch;

import io.github.mtrevisan.familylegacy.externalsearch.ExternalSearchSettings;
import io.github.mtrevisan.familylegacy.externalsearch.Placeholder;
import io.github.mtrevisan.familylegacy.externalsearch.SearchSite;
import io.github.mtrevisan.familylegacy.externalsearch.SearchSubject;
import io.github.mtrevisan.familylegacy.externalsearch.SiteCoverage;
import io.github.mtrevisan.familylegacy.externalsearch.SiteKind;
import io.github.mtrevisan.familylegacy.externalsearch.UrlTemplate;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Edits a site: its name, the template of the address of a search (with a button that inserts a placeholder), what it holds, where
 * and when it has records, the credentials the user may give for it, a description. The template is checked while it is written,
 * and the address of a sample search is shown.
 */
final class SiteEditorDialog extends JDialog{

	/** The person the preview of the address is made with. */
	private static final SearchSubject SAMPLE = sample();


	private final ExternalSearchSettings settings;
	private final SearchSite original;

	private final JTextField name = new JTextField(30);
	private final JTextArea template = new JTextArea(3, 50);
	private final JComboBox<String> insert = new JComboBox<>();
	private final JComboBox<SiteKind> kind = new JComboBox<>(SiteKind.values());
	private final JTextField countries = new JTextField(20);
	private final JTextField fromYear = new JTextField(6);
	private final JTextField toYear = new JTextField(6);
	private final JTextField credentials = new JTextField(20);
	private final JTextArea description = new JTextArea(2, 50);
	private final JCheckBox loginRequired = new JCheckBox(I18N.t("external_sites.editor.login"));
	private final JLabel check = new JLabel(" ");
	private final JTextField preview = new JTextField(50);

	private SearchSite saved;


	/**
	 * @param owner	The window the dialog belongs to.
	 * @param settings	The sites of the user.
	 * @param site	The site to change, {@code null} for a new site of the user.
	 * @return	The site as saved, empty if the user cancelled.
	 */
	static Optional<SearchSite> edit(final Window owner, final ExternalSearchSettings settings, final SearchSite site){
		final SiteEditorDialog dialog = new SiteEditorDialog(owner, settings, site);
		dialog.setVisible(true);
		return Optional.ofNullable(dialog.saved);
	}


	private SiteEditorDialog(final Window owner, final ExternalSearchSettings settings, final SearchSite site){
		super(owner, I18N.t(site == null? "external_sites.editor.title.new": "external_sites.editor.title"), ModalityType.APPLICATION_MODAL);

		this.settings = settings;
		original = site;

		template.setLineWrap(true);
		template.setWrapStyleWord(false);
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
		preview.setEditable(false);
		kind.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected,
					final boolean cellHasFocus){
				return super.getListCellRendererComponent(list, (value instanceof final SiteKind k? k.label(): value), index, isSelected,
					cellHasFocus);
			}
		});
		for(final Placeholder placeholder : Placeholder.values())
			insert.addItem(placeholder.written());
		insert.addItem("{" + UrlTemplate.CREDENTIAL_PREFIX + "api_key}");
		insert.setToolTipText(I18N.t("external_sites.editor.insert.tooltip"));
		final JButton insertButton = new JButton(I18N.t("external_sites.editor.insert"));
		insertButton.addActionListener(e -> {
			template.replaceSelection((String)insert.getSelectedItem());
			template.requestFocusInWindow();
		});
		countries.setToolTipText(I18N.t("external_sites.editor.countries.tooltip"));
		credentials.setToolTipText(I18N.t("external_sites.editor.credentials.tooltip"));
		template.setToolTipText(I18N.t("external_sites.editor.template.tooltip"));

		if(site != null){
			name.setText(site.name());
			template.setText(site.urlTemplate());
			kind.setSelectedItem(site.kind());
			countries.setText(site.coverage().countriesText());
			fromYear.setText(site.coverage().fromYear() != null? site.coverage().fromYear().toString(): StringUtils.EMPTY);
			toYear.setText(site.coverage().toYear() != null? site.coverage().toYear().toString(): StringUtils.EMPTY);
			credentials.setText(String.join(", ", site.credentials()));
			description.setText(site.description());
			loginRequired.setSelected(site.loginRequired());
		}
		else
			kind.setSelectedItem(SiteKind.RECORDS);

		final JPanel form = GUIHelper.createLabelFieldPanel(GUIHelper.DIALOG_MARGIN, StringUtils.EMPTY);
		GUIHelper.addLabeledComponent(form, GUIHelper.requiredLabel("external_sites.editor.name"), name);
		GUIHelper.addLabeledComponent(form, GUIHelper.requiredLabel("external_sites.editor.template"), template);
		final JPanel insertPanel = new JPanel(new MigLayout("ins 0", "[][]"));
		insertPanel.add(insert);
		insertPanel.add(insertButton);
		GUIHelper.addLabeledComponent(form, StringUtils.EMPTY, insertPanel);
		GUIHelper.addLabeledComponent(form, StringUtils.EMPTY, check);
		GUIHelper.addLabeledComponent(form, I18N.l("external_sites.editor.preview"), preview);
		GUIHelper.addLabeledComponent(form, I18N.l("external_sites.editor.kind"), kind);
		GUIHelper.addLabeledComponent(form, I18N.l("external_sites.editor.countries"), countries);
		final JPanel years = new JPanel(new MigLayout("ins 0", "[][]10[][]"));
		years.add(new JLabel(I18N.l("external_sites.editor.from")));
		years.add(fromYear);
		years.add(new JLabel(I18N.l("external_sites.editor.to")));
		years.add(toYear);
		GUIHelper.addLabeledComponent(form, I18N.l("external_sites.editor.years"), years);
		GUIHelper.addLabeledComponent(form, StringUtils.EMPTY, loginRequired);
		GUIHelper.addLabeledComponent(form, I18N.l("external_sites.editor.credentials"), credentials);
		GUIHelper.addLabeledComponent(form, I18N.l("external_sites.editor.description"), description);

		template.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				checkTemplate();
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				checkTemplate();
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				checkTemplate();
			}
		});
		checkTemplate();

		setLayout(new BorderLayout());
		add(form, BorderLayout.CENTER);
		add(GUIHelper.createButtonPanel(this, I18N.t("button.save"), this::save, this::dispose), BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(owner);
	}

	/** Says whether the template is valid, and shows the address of the sample search. */
	private void checkTemplate(){
		try{
			final UrlTemplate parsed = UrlTemplate.parse(template.getText());
			check.setText(I18N.t("external_sites.editor.valid"));
			check.setForeground(ToolDialogs.OK_COLOR);
			preview.setText(parsed.expandForDisplay(SAMPLE));
			preview.setCaretPosition(0);
		}
		catch(final UrlTemplate.TemplateException e){
			check.setText(e.describe());
			check.setForeground(ToolDialogs.ERROR_COLOR);
			preview.setText(StringUtils.EMPTY);
		}
	}

	private void save(){
		final String siteName = StringUtils.strip(name.getText());
		if(StringUtils.isEmpty(siteName)){
			GUIHelper.showValidationErrorAndFocus(this, I18N.t("external_sites.editor.error.name"), null, null, name);
			return;
		}
		try{
			UrlTemplate.parse(template.getText());
		}
		catch(final UrlTemplate.TemplateException e){
			GUIHelper.showValidationErrorAndFocus(this, e.describe(), null, null, template);
			return;
		}
		final SiteCoverage coverage;
		try{
			coverage = SiteCoverage.parse(countries.getText(), fromYear.getText(), toYear.getText());
		}
		catch(final IllegalArgumentException e){
			GUIHelper.showValidationErrorAndFocus(this, I18N.t("external_sites.editor.error.coverage"), null, null, countries);
			return;
		}
		final List<String> credentialNames = new ArrayList<>();
		for(final String credential : StringUtils.split(StringUtils.replaceChars(credentials.getText(), ',', ' '))){
			if(!SearchSite.isValidName(credential)){
				GUIHelper.showValidationErrorAndFocus(this, I18N.tf("external_sites.editor.error.credential", credential), null, null, credentials);
				return;
			}
			credentialNames.add(credential);
		}

		final String id = (original != null? original.id(): settings.newSiteId(siteName));
		final SearchSite site = new SearchSite(id, siteName, template.getText(), (SiteKind)kind.getSelectedItem(), coverage, credentialNames,
			description.getText(), loginRequired.isSelected(), original != null && original.builtIn());
		try{
			settings.save(site);
		}
		catch(final IOException e){
			ExternalSitesDialog.showWriteError(this, e);
		}
		saved = site;
		dispose();
	}

	private static SearchSubject sample(){
		final Map<Placeholder, String> values = new EnumMap<>(Placeholder.class);
		values.put(Placeholder.NAME_GIVEN, "Giovanni Battista");
		values.put(Placeholder.SURNAME, "Rossi");
		values.put(Placeholder.NAME_FULL, "Giovanni Battista Rossi");
		values.put(Placeholder.SEX, "male");
		values.put(Placeholder.BIRTH_YEAR, "1805");
		values.put(Placeholder.BIRTH_YEAR_FROM, "1803");
		values.put(Placeholder.BIRTH_YEAR_TO, "1807");
		values.put(Placeholder.BIRTH_PLACE, "Treviso");
		values.put(Placeholder.BIRTH_PLACE_FULL, "Treviso, Veneto, Italia");
		values.put(Placeholder.BIRTH_COUNTRY, "IT");
		values.put(Placeholder.DEATH_YEAR, "1870");
		values.put(Placeholder.DEATH_YEAR_FROM, "1868");
		values.put(Placeholder.DEATH_YEAR_TO, "1872");
		values.put(Placeholder.DEATH_PLACE, "Venezia");
		values.put(Placeholder.DEATH_PLACE_FULL, "Venezia, Veneto, Italia");
		values.put(Placeholder.DEATH_COUNTRY, "IT");
		values.put(Placeholder.FATHER_GIVEN, "Antonio");
		values.put(Placeholder.FATHER_SURNAME, "Rossi");
		values.put(Placeholder.MOTHER_GIVEN, "Maria");
		values.put(Placeholder.MOTHER_SURNAME, "Bianchi");
		values.put(Placeholder.SPOUSE_GIVEN, "Anna");
		values.put(Placeholder.SPOUSE_SURNAME, "Verdi");
		return SearchSubject.of(values);
	}

}
