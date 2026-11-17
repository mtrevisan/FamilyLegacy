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
import io.github.mtrevisan.familylegacy.externalsearch.Relevance;
import io.github.mtrevisan.familylegacy.externalsearch.SearchLink;
import io.github.mtrevisan.familylegacy.externalsearch.SearchSite;
import io.github.mtrevisan.familylegacy.externalsearch.SearchSubject;
import io.github.mtrevisan.familylegacy.externalsearch.flef.NameCandidate;
import io.github.mtrevisan.familylegacy.externalsearch.flef.SearchProfile;
import io.github.mtrevisan.familylegacy.externalsearch.flef.SearchProfileExtractor;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.preferences.AppPreferences;
import io.github.mtrevisan.familylegacy.ui.api.LinkRole;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordDialogs;
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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Searches an individual on external sites: it opens, in the browser, a search already filled with the names and the other values of
 * the individual, on the sites the user chooses.
 * <p>
 * An individual may have several names (birth, married, religious, a transcription): each one is a row, the checked ones are searched,
 * one search for each name on each site; a name the file does not have can be added. The other values (years, places, relatives) can
 * be changed before searching, and never change the file. The sites most likely to have the person (country and years) come first.
 * The search can be recorded as a research activity, which the user completes and saves (nothing is saved without the user).
 */
public final class ExternalSearchDialog extends JDialog{

	/** More searches than this at once are confirmed: each one is a tab of the browser. */
	static final int MANY_SEARCHES = 8;

	private static final String PREFERENCE_SELECTED = "external_search.selected";
	private static final String LIST_SEPARATOR = ",";
	private static final String LINE = "\n";


	private final FLEFModel model;
	private final FLEFRecord individual;
	private final ExternalSearchSettings settings;
	private final SearchProfile profile;

	private final NamesModel namesModel = new NamesModel();
	private final FactsModel factsModel = new FactsModel();
	private final SitesModel sitesModel = new SitesModel();
	private final JCheckBox consent = new JCheckBox(I18N.t("external_search.consent"));
	private final JCheckBox recordActivity = new JCheckBox(I18N.t("external_search.record_activity"));
	private final JLabel summary = new JLabel();


	/**
	 * @param owner	The window the dialog belongs to.
	 * @param model	The model.
	 * @param individual	The individual to search (anything else gives an empty dialog that says so).
	 * @param settings	The sites and the credentials of the user.
	 */
	public ExternalSearchDialog(final Window owner, final FLEFModel model, final FLEFRecord individual, final ExternalSearchSettings settings){
		super(owner, I18N.t("external_search.title"), ModalityType.APPLICATION_MODAL);

		ToolDialogs.installEscapeToClose(this);

		this.model = model;
		this.individual = (IndividualReader.isOfType(individual)? individual: null);
		this.settings = settings;
		profile = (this.individual != null? SearchProfileExtractor.extract(model, this.individual): null);

		setLayout(new BorderLayout());
		if(profile == null)
			add(ToolDialogs.createHeaderPanel(I18N.t("external_search.no_individual"), ToolDialogs.WARN_COLOR), BorderLayout.CENTER);
		else{
			setTitle(I18N.tf("external_search.title.individual", RecordDisplay.describe(this.individual, model)));
			add(createContent(), BorderLayout.CENTER);
			add(createFooter(), BorderLayout.SOUTH);
		}

		pack();
		setMinimumSize(new Dimension(640, 480));
		setLocationRelativeTo(owner);
	}


	private JPanel createContent(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fill,wrap 2", "[grow,fill]10[grow,fill]", "[][grow,fill][grow 2,fill][]"));

		final JPanel consentPanel = new JPanel(new MigLayout("ins 4,wrap 1", "[grow,fill]"));
		if(profile.needsConsent()){
			final JLabel warning = new JLabel(I18N.t(profile.mayBeLiving()? "external_search.warning.living": "external_search.warning.private"));
			warning.setForeground(ToolDialogs.WARN_COLOR);
			consentPanel.add(warning);
			consentPanel.add(consent);
		}
		else
			consentPanel.setVisible(false);
		panel.add(consentPanel, "span 2,growx");

		for(final NameCandidate name : profile.names())
			namesModel.add(name, namesModel.rows.isEmpty());
		final JTable names = new JTable(namesModel);
		names.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		names.getColumnModel().getColumn(0).setMaxWidth(60);
		names.setPreferredScrollableViewportSize(new Dimension(380, 110));
		final JButton addName = new JButton(I18N.t("external_search.name.add"));
		addName.addActionListener(e -> {
			namesModel.add(new NameCandidate(I18N.t("external_search.name.yours"), null, null, null), true);
			final int row = namesModel.getRowCount() - 1;
			names.setRowSelectionInterval(row, row);
			names.editCellAt(row, NamesModel.COLUMN_GIVEN);
		});
		final JButton removeName = new JButton(I18N.t("external_search.name.remove"));
		removeName.addActionListener(e -> {
			final int row = names.getSelectedRow();
			if(row >= 0){
				if(names.isEditing())
					names.getCellEditor().cancelCellEditing();
				namesModel.remove(names.convertRowIndexToModel(row));
			}
		});
		final JPanel namesPanel = new JPanel(new BorderLayout(0, 4));
		namesPanel.setBorder(BorderFactory.createTitledBorder(I18N.t("external_search.names")));
		namesPanel.add(new JScrollPane(names), BorderLayout.CENTER);
		final JPanel nameButtons = new JPanel(new MigLayout("ins 0", "[]push[]"));
		nameButtons.add(addName);
		nameButtons.add(removeName);
		namesPanel.add(nameButtons, BorderLayout.SOUTH);
		panel.add(namesPanel);

		factsModel.load(profile.facts());
		final JTable facts = new JTable(factsModel);
		facts.setPreferredScrollableViewportSize(new Dimension(380, 140));
		final JPanel factsPanel = new JPanel(new BorderLayout());
		factsPanel.setBorder(BorderFactory.createTitledBorder(I18N.t("external_search.values")));
		factsPanel.add(new JScrollPane(facts), BorderLayout.CENTER);
		panel.add(factsPanel);

		final JTable sites = new JTable(sitesModel);
		sites.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sites.getColumnModel().getColumn(0).setMaxWidth(40);
		sites.getColumnModel().getColumn(1).setPreferredWidth(260);
		sites.setDefaultRenderer(Object.class, new SiteRenderer());
		sites.setPreferredScrollableViewportSize(new Dimension(780, 220));
		sites.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				final int row = sites.rowAtPoint(e.getPoint());
				if(e.getClickCount() == 2 && row >= 0 && sites.columnAtPoint(e.getPoint()) != 0)
					open(List.of(sitesModel.rows.get(sites.convertRowIndexToModel(row)).link().site()));
			}
		});
		final JPanel sitesPanel = new JPanel(new BorderLayout());
		sitesPanel.setBorder(BorderFactory.createTitledBorder(I18N.t("external_search.sites")));
		sitesPanel.add(new JScrollPane(sites), BorderLayout.CENTER);
		panel.add(sitesPanel, "span 2,grow");

		panel.add(recordActivity, "span 2");

		namesModel.addTableModelListener(e -> refreshSites());
		factsModel.addTableModelListener(e -> refreshSites());
		sitesModel.addTableModelListener(e -> updateSummary());
		refreshSites();
		sitesModel.select(initialSelection());
		return panel;
	}

	private JPanel createFooter(){
		final JButton open = new JButton(I18N.t("external_search.open"));
		open.addActionListener(e -> open(sitesModel.checkedSites()));
		final JButton copy = new JButton(I18N.t("external_search.copy_links"));
		copy.addActionListener(e -> copyLinks());
		final JButton manage = new JButton(I18N.t("external_search.manage"));
		manage.addActionListener(e -> {
			new ExternalSitesDialog(this, settings)
				.setVisible(true);
			final Set<String> selected = sitesModel.checkedIds();
			refreshSites();
			sitesModel.select(selected);
		});
		getRootPane().setDefaultButton(open);

		final JPanel footer = new JPanel(new BorderLayout(8, 0));
		footer.setBorder(BorderFactory.createEmptyBorder(2, 10, 8, 8));
		footer.add(summary, BorderLayout.WEST);
		final JPanel buttons = new JPanel(new MigLayout("ins 0", "[][][]"));
		buttons.add(manage);
		buttons.add(copy);
		buttons.add(open);
		footer.add(buttons, BorderLayout.EAST);
		return footer;
	}


	/** @return	The values of the person, as changed by the user, without the name. */
	private SearchSubject facts(){
		return factsModel.subject();
	}

	private SearchSubject subjectFor(final NameCandidate name){
		return facts().withAll(name.asSubject());
	}

	private void refreshSites(){
		final List<NameCandidate> checked = namesModel.checkedNames();
		final SearchSubject subject = (checked.isEmpty()? facts(): subjectFor(checked.getFirst()));
		sitesModel.load(settings.links(subject));
	}

	private void updateSummary(){
		final List<SearchSite> sites = sitesModel.checkedSites();
		final List<NameCandidate> names = namesModel.checkedNames();
		summary.setText(I18N.tf("external_search.summary", sites.size(), names.size(), distinctSearches(sites, names, new LinkedHashMap<>()).size()));
	}

	/** @return	The sites checked the last time, or else the likely ones that can be opened. */
	private Set<String> initialSelection(){
		final String saved = AppPreferences.getInstance()
			.stringValue(PREFERENCE_SELECTED, null);
		final Set<String> selected = new LinkedHashSet<>();
		if(saved != null){
			for(final String id : StringUtils.split(saved, LIST_SEPARATOR))
				selected.add(id.strip());
			return selected;
		}

		for(final SiteRow row : sitesModel.rows)
			if(row.link().relevance() == Relevance.LIKELY && row.link().isReady())
				selected.add(row.link().site().id());
		return selected;
	}


	/** Opens the search of every checked name on every given site. */
	private void open(final List<SearchSite> sites){
		final List<Search> searches = searches(sites);
		if(searches.isEmpty())
			return;

		if(searches.size() > MANY_SEARCHES && JOptionPane.showConfirmDialog(this, I18N.tf("external_search.confirm.many", searches.size()),
				getTitle(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION)
			return;

		final List<Search> opened = new ArrayList<>();
		for(final Search search : searches)
			if(WebBrowser.open(this, search.link().url()))
				opened.add(search);
		AppPreferences.getInstance()
			.setStringValue(PREFERENCE_SELECTED, String.join(LIST_SEPARATOR, sitesModel.checkedIds()));

		if(!opened.isEmpty() && recordActivity.isSelected())
			openActivityDraft(opened);
	}

	/** @return	The searches to open (the checked names on the sites), after the checks; empty if there is nothing to open. */
	private List<Search> searches(final List<SearchSite> sites){
		if(profile.needsConsent() && !consent.isSelected()){
			JOptionPane.showMessageDialog(this, I18N.t("external_search.consent.needed"), getTitle(), JOptionPane.WARNING_MESSAGE);
			return List.of();
		}
		final List<NameCandidate> names = namesModel.checkedNames();
		if(names.isEmpty()){
			JOptionPane.showMessageDialog(this, I18N.t("external_search.no_name"), getTitle(), JOptionPane.WARNING_MESSAGE);
			return List.of();
		}
		if(sites.isEmpty()){
			JOptionPane.showMessageDialog(this, I18N.t("external_search.no_site"), getTitle(), JOptionPane.WARNING_MESSAGE);
			return List.of();
		}

		final Map<String, String> notReady = new LinkedHashMap<>();
		final List<Search> searches = distinctSearches(sites, names, notReady);
		if(!notReady.isEmpty()){
			final StringBuilder message = new StringBuilder(I18N.t("external_search.not_ready"));
			for(final Map.Entry<String, String> entry : notReady.entrySet())
				message.append(LINE)
					.append(entry.getKey())
					.append(": ")
					.append(entry.getValue());
			JOptionPane.showMessageDialog(this, message.toString(), getTitle(), JOptionPane.WARNING_MESSAGE);
		}
		return searches;
	}

	/**
	 * @param sites	The sites.
	 * @param names	The names.
	 * @param notReady	Filled with the sites whose search cannot be opened, and why.
	 * @return	The searches that can be opened, each address once: two names with the same surname give the same search on a site that
	 * 	takes only the surname.
	 */
	private List<Search> distinctSearches(final List<SearchSite> sites, final List<NameCandidate> names, final Map<String, String> notReady){
		final List<Search> searches = new ArrayList<>();
		final Set<String> urls = new HashSet<>();
		for(final SearchSite site : sites)
			for(final NameCandidate name : names){
				final SearchLink link = SearchLink.forSites(List.of(site), subjectFor(name), settings.credentials())
					.getFirst();
				if(!link.isReady())
					notReady.putIfAbsent(site.name(), statusText(link));
				else if(urls.add(link.url()))
					searches.add(new Search(site, name, link));
			}
		return searches;
	}

	private void copyLinks(){
		final List<Search> searches = searches(sitesModel.checkedSites());
		if(searches.isEmpty())
			return;

		final StringBuilder text = new StringBuilder();
		for(final Search search : searches)
			text.append(search.link().url())
				.append(LINE);
		final StringSelection selection = new StringSelection(text.toString());
		Toolkit.getDefaultToolkit()
			.getSystemClipboard()
			.setContents(selection, selection);
	}

	/**
	 * Opens a new research activity about the search (a search, in progress, of the individual, with the sites and the addresses),
	 * for the user to complete with the result and to save, or to discard.
	 */
	private void openActivityDraft(final List<Search> searches){
		final Set<String> siteNames = new LinkedHashSet<>();
		final Set<String> nameTexts = new LinkedHashSet<>();
		for(final Search search : searches){
			siteNames.add(search.site().name());
			nameTexts.add(search.name().full());
		}
		final StringBuilder action = new StringBuilder(I18N.tf("external_search.activity.action", String.join(", ", siteNames),
			String.join(" / ", nameTexts)));
		for(final Search search : searches)
			action.append(LINE)
				.append(search.link().displayUrl());

		final RecordDialog dialog = RecordDialogs.createNew(ResearchActivityReader.TYPE, this, model);
		if(dialog == null)
			return;

		dialog.link(LinkRole.TARGET, individual);
		final Map<String, String> values = new LinkedHashMap<>();
		values.put(ResearchActivityReader.TAG_ACTIVITY_TYPE, ResearchActivityReader.ENUM_ACTIVITY_TYPE_SEARCH);
		values.put(ResearchActivityReader.TAG_STATUS, ResearchActivityReader.ENUM_STATUS_IN_PROGRESS);
		values.put(ResearchActivityReader.TAG_ACTION, action.toString());
		dialog.presetValues(values);
		dialog.setVisible(true);
	}

	private static String statusText(final SearchLink link){
		return switch(link.status()){
			case READY -> (link.site().loginRequired()? I18N.t("external_search.status.ready_login"): link.status().label());
			case MISSING_CREDENTIALS -> link.status().label() + ": " + String.join(", ",
				link.missingCredentials().stream().map(SearchSite::credentialLabel).toList());
			case NO_NAME -> link.status().label();
			case INVALID_TEMPLATE -> link.status().label() + ": " + link.problem();
		};
	}


	private record Search(SearchSite site, NameCandidate name, SearchLink link){}


	/* ======================================================================
	 *                          Names
	 * ====================================================================== */

	private static final class NameRow{
		private boolean checked;
		private final String label;
		private String given;
		private String surname;
		/** The whole name as written in the file, {@code null} once the user changes a part of it. */
		private String full;

		private NameRow(final boolean checked, final NameCandidate name){
			this.checked = checked;
			label = name.label();
			given = name.given();
			surname = name.surname();
			full = name.full();
		}
	}

	/** The names of the person: checked (searched or not), what they are, the given names and the surname (both can be changed). */
	private static final class NamesModel extends AbstractTableModel{

		static final int COLUMN_CHECK = 0;
		static final int COLUMN_LABEL = 1;
		static final int COLUMN_GIVEN = 2;
		static final int COLUMN_SURNAME = 3;

		private final List<NameRow> rows = new ArrayList<>();


		void add(final NameCandidate name, final boolean checked){
			rows.add(new NameRow(checked, name));
			fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
		}

		void remove(final int row){
			rows.remove(row);
			fireTableRowsDeleted(row, row);
		}

		/** @return	The names checked, with a given name or a surname. */
		List<NameCandidate> checkedNames(){
			final List<NameCandidate> names = new ArrayList<>();
			for(final NameRow row : rows)
				if(row.checked){
					final NameCandidate name = new NameCandidate(row.label, row.given, row.surname, row.full);
					if(!name.isEmpty())
						names.add(name);
				}
			return names;
		}

		@Override
		public int getRowCount(){
			return rows.size();
		}

		@Override
		public int getColumnCount(){
			return 4;
		}

		@Override
		public String getColumnName(final int column){
			return switch(column){
				case COLUMN_CHECK -> I18N.t("external_search.column.search");
				case COLUMN_LABEL -> I18N.t("external_search.column.name");
				case COLUMN_GIVEN -> Placeholder.NAME_GIVEN.label();
				default -> Placeholder.SURNAME.label();
			};
		}

		@Override
		public Class<?> getColumnClass(final int column){
			return (column == COLUMN_CHECK? Boolean.class: String.class);
		}

		@Override
		public boolean isCellEditable(final int row, final int column){
			return (column != COLUMN_LABEL);
		}

		@Override
		public Object getValueAt(final int row, final int column){
			final NameRow name = rows.get(row);
			return switch(column){
				case COLUMN_CHECK -> name.checked;
				case COLUMN_LABEL -> name.label;
				case COLUMN_GIVEN -> name.given;
				default -> name.surname;
			};
		}

		@Override
		public void setValueAt(final Object value, final int row, final int column){
			final NameRow name = rows.get(row);
			switch(column){
				case COLUMN_CHECK -> name.checked = Boolean.TRUE.equals(value);
				case COLUMN_GIVEN -> {
					name.given = StringUtils.normalizeSpace(value != null? value.toString(): null);
					name.full = null;
				}
				case COLUMN_SURNAME -> {
					name.surname = StringUtils.normalizeSpace(value != null? value.toString(): null);
					name.full = null;
				}
				default -> {
					return;
				}
			}
			fireTableRowsUpdated(row, row);
		}

	}


	/* ======================================================================
	 *                          Values
	 * ====================================================================== */

	/** The values of the person that are not the name, one per row, all of them (an empty one can be filled). */
	private static final class FactsModel extends AbstractTableModel{

		private final List<Placeholder> placeholders = new ArrayList<>();
		private final Map<Placeholder, String> values = new EnumMap<>(Placeholder.class);


		FactsModel(){
			for(final Placeholder placeholder : Placeholder.values())
				if(!placeholder.isName())
					placeholders.add(placeholder);
		}

		void load(final SearchSubject subject){
			values.clear();
			values.putAll(subject.values());
			fireTableDataChanged();
		}

		SearchSubject subject(){
			return SearchSubject.of(values);
		}

		@Override
		public int getRowCount(){
			return placeholders.size();
		}

		@Override
		public int getColumnCount(){
			return 2;
		}

		@Override
		public String getColumnName(final int column){
			return I18N.t(column == 0? "external_search.column.value": "external_search.column.text");
		}

		@Override
		public boolean isCellEditable(final int row, final int column){
			return (column == 1);
		}

		@Override
		public Object getValueAt(final int row, final int column){
			final Placeholder placeholder = placeholders.get(row);
			return (column == 0? placeholder.label(): values.getOrDefault(placeholder, StringUtils.EMPTY));
		}

		@Override
		public void setValueAt(final Object value, final int row, final int column){
			final Placeholder placeholder = placeholders.get(row);
			final String text = StringUtils.normalizeSpace(value != null? value.toString(): null);
			if(StringUtils.isEmpty(text))
				values.remove(placeholder);
			else
				values.put(placeholder, text);
			fireTableRowsUpdated(row, row);
		}

	}


	/* ======================================================================
	 *                          Sites
	 * ====================================================================== */

	private static final class SiteRow{
		private boolean checked;
		private final SearchLink link;

		private SiteRow(final boolean checked, final SearchLink link){
			this.checked = checked;
			this.link = link;
		}

		SearchLink link(){
			return link;
		}
	}

	/** The enabled sites, the most relevant first: checked, name, kind, relevance, whether the search can be opened. */
	private static final class SitesModel extends AbstractTableModel{

		private final List<SiteRow> rows = new ArrayList<>();


		void load(final List<SearchLink> links){
			final Set<String> checked = checkedIds();
			rows.clear();
			for(final SearchLink link : links)
				rows.add(new SiteRow(checked.contains(link.site().id()), link));
			fireTableDataChanged();
		}

		void select(final Set<String> ids){
			for(final SiteRow row : rows)
				row.checked = ids.contains(row.link.site().id());
			fireTableDataChanged();
		}

		Set<String> checkedIds(){
			final Set<String> ids = new LinkedHashSet<>();
			for(final SiteRow row : rows)
				if(row.checked)
					ids.add(row.link.site().id());
			return ids;
		}

		List<SearchSite> checkedSites(){
			final List<SearchSite> sites = new ArrayList<>();
			for(final SiteRow row : rows)
				if(row.checked)
					sites.add(row.link.site());
			return sites;
		}

		@Override
		public int getRowCount(){
			return rows.size();
		}

		@Override
		public int getColumnCount(){
			return 5;
		}

		@Override
		public String getColumnName(final int column){
			return switch(column){
				case 0 -> StringUtils.EMPTY;
				case 1 -> I18N.t("external_search.column.site");
				case 2 -> I18N.t("external_search.column.kind");
				case 3 -> I18N.t("external_search.column.relevance");
				default -> I18N.t("external_search.column.status");
			};
		}

		@Override
		public Class<?> getColumnClass(final int column){
			return (column == 0? Boolean.class: String.class);
		}

		@Override
		public boolean isCellEditable(final int row, final int column){
			return (column == 0);
		}

		@Override
		public Object getValueAt(final int row, final int column){
			final SiteRow site = rows.get(row);
			return switch(column){
				case 0 -> site.checked;
				case 1 -> site.link.site().name();
				case 2 -> site.link.site().kind().label();
				case 3 -> site.link.relevance().label();
				default -> statusText(site.link);
			};
		}

		@Override
		public void setValueAt(final Object value, final int row, final int column){
			if(column == 0){
				rows.get(row).checked = Boolean.TRUE.equals(value);
				fireTableRowsUpdated(row, row);
			}
		}

	}

	/** Greys the sites whose search cannot be opened and the unlikely ones; the tooltip has the description and the address. */
	private final class SiteRenderer extends DefaultTableCellRenderer{

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column){
			final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			final SearchLink link = sitesModel.rows.get(table.convertRowIndexToModel(row)).link();
			if(!isSelected)
				component.setForeground(!link.isReady()
					? ToolDialogs.ERROR_COLOR
					: (link.relevance() == Relevance.UNLIKELY? Color.GRAY: table.getForeground()));
			final String description = link.site().description();
			setToolTipText(GUIHelper.tooltipHtml(description,
				link.displayUrl() != null? link.displayUrl(): link.problem()));
			return component;
		}

	}

}
