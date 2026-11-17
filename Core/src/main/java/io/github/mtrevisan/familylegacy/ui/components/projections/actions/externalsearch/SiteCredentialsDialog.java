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

import io.github.mtrevisan.familylegacy.externalsearch.CredentialStore;
import io.github.mtrevisan.familylegacy.externalsearch.ExternalSearchSettings;
import io.github.mtrevisan.familylegacy.externalsearch.SearchSite;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Window;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


/**
 * The credentials of the user for a site: the ones the site declares or its template uses, the ones already given, and any other the
 * user adds (an API key for a later use). The values are hidden unless the user asks to show them; an empty value removes the
 * credential. They are written in a file of the account of the user, not in a FLEF file (see {@link CredentialStore}).
 */
final class SiteCredentialsDialog extends JDialog{

	private static final char HIDDEN = '•';


	private final CredentialStore store;
	private final SearchSite site;
	private final Map<String, JPasswordField> fields = new LinkedHashMap<>();
	private final JPanel rows = new JPanel(new MigLayout("ins 0,wrap 2,hidemode 3", "[right]rel[grow,fill]"));
	private final JCheckBox show = new JCheckBox(I18N.t("external_sites.credentials.show"));


	/**
	 * @param owner	The window the dialog belongs to.
	 * @param settings	The sites and the credentials of the user.
	 * @param site	The site.
	 */
	static void edit(final Window owner, final ExternalSearchSettings settings, final SearchSite site){
		new SiteCredentialsDialog(owner, settings.credentials(), site)
			.setVisible(true);
	}


	private SiteCredentialsDialog(final Window owner, final CredentialStore store, final SearchSite site){
		super(owner, I18N.tf("external_sites.credentials.title", site.name()), ModalityType.APPLICATION_MODAL);

		this.store = store;
		this.site = site;

		final Set<String> names = new LinkedHashSet<>(site.allCredentials());
		names.addAll(store.names(site.id()));
		for(final String name : names)
			addRow(name);

		final JTextField newName = new JTextField(14);
		newName.setToolTipText(I18N.t("external_sites.credentials.new.tooltip"));
		final JButton add = new JButton(I18N.t("external_sites.credentials.add"));
		add.addActionListener(e -> {
			final String name = StringUtils.strip(newName.getText());
			if(!SearchSite.isValidName(name)){
				JOptionPane.showMessageDialog(this, I18N.tf("external_sites.editor.error.credential", StringUtils.defaultString(name)),
					getTitle(), JOptionPane.WARNING_MESSAGE);
				return;
			}
			if(!fields.containsKey(name))
				addRow(name);
			newName.setText(StringUtils.EMPTY);
			fields.get(name).requestFocusInWindow();
			pack();
		});
		show.addActionListener(e -> {
			for(final JPasswordField field : fields.values())
				field.setEchoChar(show.isSelected()? (char)0: HIDDEN);
		});

		final JPanel content = new JPanel(new MigLayout("ins " + GUIHelper.DIALOG_MARGIN + ",wrap 1", "[grow,fill]"));
		final JLabel note = new JLabel(I18N.t("external_sites.credentials.note"));
		note.setToolTipText(store.file() != null? store.file().toString(): null);
		content.add(note);
		if(fields.isEmpty())
			content.add(new JLabel(I18N.t("external_sites.credentials.none")));
		content.add(rows);
		final JPanel addPanel = new JPanel(new MigLayout("ins 0", "[][][]"));
		addPanel.add(new JLabel(I18N.l("external_sites.credentials.new")));
		addPanel.add(newName);
		addPanel.add(add);
		addPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		content.add(addPanel);
		content.add(show);

		setLayout(new BorderLayout());
		add(content, BorderLayout.CENTER);
		add(GUIHelper.createButtonPanel(this, I18N.t("button.save"), this::save, this::dispose), BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(owner);
	}

	private void addRow(final String name){
		final JPasswordField field = new JPasswordField(30);
		field.setEchoChar(show.isSelected()? (char)0: HIDDEN);
		store.get(site.id(), name)
			.ifPresent(field::setText);
		fields.put(name, field);
		rows.add(new JLabel(I18N.al(SearchSite.credentialLabel(name))));
		rows.add(field);
	}

	private void save(){
		try{
			for(final Map.Entry<String, JPasswordField> entry : fields.entrySet())
				store.put(site.id(), entry.getKey(), new String(entry.getValue().getPassword()));
		}
		catch(final IOException e){
			ExternalSitesDialog.showWriteError(this, e);
		}
		dispose();
	}

}
