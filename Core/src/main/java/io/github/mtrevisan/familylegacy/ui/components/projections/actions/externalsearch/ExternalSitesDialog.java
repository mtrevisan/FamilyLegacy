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
import io.github.mtrevisan.familylegacy.externalsearch.SearchSite;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * The sites a person can be searched on: which are offered (enabled), their templates and coverage, the credentials of the user for
 * them. A built-in site can be changed and restored; a site of the user can be added, copied from another one, changed, removed.
 */
public final class ExternalSitesDialog extends JDialog{

	private static final int COLUMN_ENABLED = 0;


	private final ExternalSearchSettings settings;
	private final SitesModel sitesModel = new SitesModel();
	private final JTable table = new JTable(sitesModel);
	private final JButton edit = new JButton(I18N.t("external_sites.edit"));
	private final JButton duplicate = new JButton(I18N.t("external_sites.duplicate"));
	private final JButton remove = new JButton(I18N.t("external_sites.remove"));
	private final JButton credentials = new JButton(I18N.t("external_sites.credentials"));


	/**
	 * @param owner	The window the dialog belongs to.
	 * @param settings	The sites and the credentials of the user.
	 */
	public ExternalSitesDialog(final Window owner, final ExternalSearchSettings settings){
		super(owner, I18N.t("external_sites.title"), ModalityType.APPLICATION_MODAL);

		ToolDialogs.installEscapeToClose(this);

		this.settings = settings;

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getColumnModel().getColumn(COLUMN_ENABLED).setMaxWidth(70);
		table.getColumnModel().getColumn(1).setPreferredWidth(260);
		table.setDefaultRenderer(Object.class, new DescriptionRenderer());
		table.setPreferredScrollableViewportSize(new Dimension(820, 360));
		table.getSelectionModel().addListSelectionListener(e -> updateButtons());
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && table.columnAtPoint(e.getPoint()) != COLUMN_ENABLED)
					editSelected();
			}
		});

		final JButton add = new JButton(I18N.t("external_sites.add"));
		add.addActionListener(e -> SiteEditorDialog.edit(this, settings, null)
			.ifPresent(this::reloadAndSelect));
		edit.addActionListener(e -> editSelected());
		duplicate.addActionListener(e -> selected().ifPresent(site -> {
			final SearchSite copy = site.withId(settings.newSiteId(site.name()), false)
				.withName(I18N.tf("external_sites.copy_of", site.name()));
			SiteEditorDialog.edit(this, settings, copy)
				.ifPresent(this::reloadAndSelect);
		}));
		remove.addActionListener(e -> removeSelected());
		credentials.addActionListener(e -> selected().ifPresent(site -> {
			SiteCredentialsDialog.edit(this, settings, site);
			reloadAndSelect(site);
		}));

		final JPanel buttons = new JPanel(new MigLayout("ins 0,wrap 1", "[fill]"));
		buttons.add(add);
		buttons.add(edit);
		buttons.add(duplicate);
		buttons.add(remove);
		buttons.add(credentials, "gaptop 10");

		final JPanel content = new JPanel(new BorderLayout(8, 8));
		content.setBorder(BorderFactory.createEmptyBorder(GUIHelper.DIALOG_MARGIN, GUIHelper.DIALOG_MARGIN, 0, GUIHelper.DIALOG_MARGIN));
		content.add(new JScrollPane(table), BorderLayout.CENTER);
		content.add(buttons, BorderLayout.EAST);
		final JLabel where = new JLabel(I18N.t("external_sites.where"));
		where.setToolTipText(settings.credentials().file() != null? settings.credentials().file().toString(): null);
		content.add(where, BorderLayout.SOUTH);

		setLayout(new BorderLayout());
		add(content, BorderLayout.CENTER);
		final JButton close = new JButton(I18N.t("button.close"));
		close.addActionListener(e -> dispose());
		add(ToolDialogs.createFooter(null, close), BorderLayout.SOUTH);

		reload();
		updateButtons();
		pack();
		setLocationRelativeTo(owner);
	}


	private Optional<SearchSite> selected(){
		final int row = table.getSelectedRow();
		return (row >= 0? Optional.of(sitesModel.sites.get(table.convertRowIndexToModel(row))): Optional.empty());
	}

	private void editSelected(){
		selected().ifPresent(site -> SiteEditorDialog.edit(this, settings, site)
			.ifPresent(this::reloadAndSelect));
	}

	private void removeSelected(){
		final Optional<SearchSite> site = selected();
		if(site.isEmpty())
			return;

		final String question = (site.get().builtIn()
			? I18N.tf("external_sites.restore.confirm", site.get().name())
			: I18N.tf("external_sites.remove.confirm", site.get().name()));
		if(JOptionPane.showConfirmDialog(this, question, getTitle(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)
				!= JOptionPane.OK_OPTION)
			return;

		try{
			settings.remove(site.get().id());
		}
		catch(final IOException e){
			showWriteError(this, e);
		}
		reload();
	}

	private void reload(){
		sitesModel.load(settings.sites());
		updateButtons();
	}

	private void reloadAndSelect(final SearchSite site){
		reload();
		for(int i = 0; i < sitesModel.sites.size(); i ++)
			if(sitesModel.sites.get(i).id().equals(site.id())){
				final int row = table.convertRowIndexToView(i);
				table.setRowSelectionInterval(row, row);
				table.scrollRectToVisible(table.getCellRect(row, 0, true));
				break;
			}
	}

	private void updateButtons(){
		final Optional<SearchSite> site = selected();
		edit.setEnabled(site.isPresent());
		duplicate.setEnabled(site.isPresent());
		credentials.setEnabled(site.isPresent());
		remove.setEnabled(site.isPresent() && (!site.get().builtIn() || settings.isModified(site.get().id())));
		remove.setText(I18N.t(site.isPresent() && site.get().builtIn()? "external_sites.restore": "external_sites.remove"));
	}

	static void showWriteError(final Component parent, final IOException e){
		JOptionPane.showMessageDialog(parent, I18N.tf("external_sites.write_error", e.getMessage()), I18N.t("external_sites.title"),
			JOptionPane.ERROR_MESSAGE);
	}


	/** Enabled, name, kind, countries, years, origin (built-in, changed, of the user), credentials (given, missing). */
	private final class SitesModel extends AbstractTableModel{

		private final List<SearchSite> sites = new ArrayList<>();


		void load(final List<SearchSite> newSites){
			sites.clear();
			sites.addAll(newSites);
			fireTableDataChanged();
		}

		@Override
		public int getRowCount(){
			return sites.size();
		}

		@Override
		public int getColumnCount(){
			return 7;
		}

		@Override
		public String getColumnName(final int column){
			return switch(column){
				case COLUMN_ENABLED -> I18N.t("external_sites.column.enabled");
				case 1 -> I18N.t("external_search.column.site");
				case 2 -> I18N.t("external_search.column.kind");
				case 3 -> I18N.t("external_sites.column.countries");
				case 4 -> I18N.t("external_sites.column.years");
				case 5 -> I18N.t("external_sites.column.origin");
				default -> I18N.t("external_sites.column.credentials");
			};
		}

		@Override
		public Class<?> getColumnClass(final int column){
			return (column == COLUMN_ENABLED? Boolean.class: String.class);
		}

		@Override
		public boolean isCellEditable(final int row, final int column){
			return (column == COLUMN_ENABLED);
		}

		@Override
		public Object getValueAt(final int row, final int column){
			final SearchSite site = sites.get(row);
			return switch(column){
				case COLUMN_ENABLED -> settings.isEnabled(site.id());
				case 1 -> site.name();
				case 2 -> site.kind().label();
				case 3 -> (site.coverage().isWorldwide()? I18N.t("external_sites.worldwide"): site.coverage().countriesText());
				case 4 -> site.coverage().yearsText();
				case 5 -> I18N.t(!site.builtIn()
					? "external_sites.origin.user"
					: (settings.isModified(site.id())? "external_sites.origin.modified": "external_sites.origin.builtin"));
				default -> credentialsText(site);
			};
		}

		private String credentialsText(final SearchSite site){
			if(!settings.credentials().missing(site).isEmpty())
				return I18N.t("external_sites.credentials.missing");
			if(!settings.credentials().names(site.id()).isEmpty())
				return I18N.t("external_sites.credentials.given");
			return (site.allCredentials().isEmpty()? "": I18N.t("external_sites.credentials.optional"));
		}

		@Override
		public void setValueAt(final Object value, final int row, final int column){
			if(column != COLUMN_ENABLED)
				return;

			try{
				settings.setEnabled(sites.get(row).id(), Boolean.TRUE.equals(value));
			}
			catch(final IOException e){
				showWriteError(ExternalSitesDialog.this, e);
			}
			fireTableRowsUpdated(row, row);
		}

	}

	/** The tooltip of a row is the description of the site and its template. */
	private final class DescriptionRenderer extends DefaultTableCellRenderer{

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column){
			final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			final SearchSite site = sitesModel.sites.get(table.convertRowIndexToModel(row));
			setToolTipText(GUIHelper.tooltipHtml(site.description(), site.urlTemplate()));
			return component;
		}

	}

}
