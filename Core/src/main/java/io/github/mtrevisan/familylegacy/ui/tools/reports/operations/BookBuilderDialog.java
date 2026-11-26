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
package io.github.mtrevisan.familylegacy.ui.tools.reports.operations;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.BookBuilder;
import io.github.mtrevisan.familylegacy.reports.BookDefinition;
import io.github.mtrevisan.familylegacy.reports.BookEntry;
import io.github.mtrevisan.familylegacy.reports.BookIndex;
import io.github.mtrevisan.familylegacy.reports.ReportBuilder;
import io.github.mtrevisan.familylegacy.reports.ReportConfig;
import io.github.mtrevisan.familylegacy.reports.ReportPreferences;
import io.github.mtrevisan.familylegacy.reports.ReportProgressListener;
import io.github.mtrevisan.familylegacy.reports.ReportType;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.familylegacy.ui.bindings.ComboBoxSearch;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import io.github.mtrevisan.familylegacy.ui.tools.reports.ProgressDialog;
import io.github.mtrevisan.familylegacy.ui.tools.reports.ReportLayoutPanel;
import io.github.mtrevisan.familylegacy.ui.tools.reports.ReportOptionsPanel;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportLayout;
import io.github.mtrevisan.reports.ReportRenderer;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;


/**
 * Builds a book out of the reports of the application: the user picks the reports (each about its own record, each with its own options),
 * puts them in order, chooses the pages that come before them (cover, title page, preface) and the indexes that come after, and writes the
 * book in any of the formats of the reports. The reports are made by {@link BookBuilder}, that composes the ones of {@link ReportBuilder}.
 */
public final class BookBuilderDialog extends JDialog{

	/** The longest part of the name of a file that comes from the title of the book. */
	private static final int MAX_FILE_NAME = 60;

	private final ToolContext context;
	/** What the user chose the last time: the language, the format, the folder, the options of every type of report. */
	private final ReportPreferences preferences = ReportPreferences.application();

	private final JTextField title = new JTextField();
	private final JTextField subtitle = new JTextField();
	private final JComboBox<ReportLanguage> language = ComboBoxSearch.searchable(new JComboBox<>(ReportLanguage.available().toArray(ReportLanguage[]::new)));
	private final JCheckBox coverPage = new JCheckBox(I18N.t("checkbox.book.cover_page"), true);
	private final JCheckBox titlePage = new JCheckBox(I18N.t("checkbox.book.title_page"), true);
	private final JTextField prefaceTitle = new JTextField();
	private final JTextArea prefaceText = new JTextArea(5, 30);
	private final Map<BookIndex, JCheckBox> indexes = new EnumMap<>(BookIndex.class);

	private final DefaultListModel<Item> items = new DefaultListModel<>();
	private final JList<Item> list = new JList<>(items);
	private final JButton configure = new JButton(I18N.t("button.book.configure"));
	private final JButton remove = new JButton(I18N.t("button.book.remove"));
	private final JButton up = new JButton(I18N.t("button.book.up"));
	private final JButton down = new JButton(I18N.t("button.book.down"));
	private final ReportLayoutPanel layoutPanel;


	/** A report in the list: what it is and what it is about. */
	private record Item(BookEntry entry, String subject){

		@Override
		public String toString(){
			return entry.type().getTitle() + (subject != null? " — " + subject: StringUtils.EMPTY);
		}

	}


	private BookBuilderDialog(final ToolContext context){
		super(context.owner(), I18N.t("dialog.book.title"), ModalityType.APPLICATION_MODAL);

		ToolDialogs.installEscapeToClose(this);

		this.context = context;
		language.setSelectedItem(ReportLanguage.orEnglish(preferences.language()));
		layoutPanel = new ReportLayoutPanel(preferences.layout());
		for(final BookIndex index : BookIndex.values())
			indexes.put(index, new JCheckBox(index.getTitle(), true));

		initComponents();
	}

	public static void showDialog(final ToolContext context){
		SwingUtilities.invokeLater(() -> {
			final BookBuilderDialog dialog = new BookBuilderDialog(context);

			dialog.pack();
			dialog.setLocationRelativeTo(context.owner());
			dialog.setVisible(true);
		});
	}


	private void initComponents(){
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout(0, 0));

		final JPanel main = new JPanel(new MigLayout("ins 10,wrap 1,fillx", "[grow,fill]", "[]5[]5[]5[]5[]"));
		main.add(createBookPanel());
		main.add(createReportsPanel());
		main.add(createPrefacePanel());
		main.add(createIndexesPanel());
		main.add(layoutPanel);

		final JScrollPane scroll = new JScrollPane(main, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getVerticalScrollBar()
			.setUnitIncrement(GUIHelper.SCROLLBAR_UNIT_INCREMENT);

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
		final JButton generate = new JButton(I18N.t("button.generate"));
		generate.addActionListener(e -> onGenerate());
		buttons.add(generate);

		add(scroll, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		setPreferredSize(new Dimension(620, 720));
	}

	private JPanel createBookPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 4 8 8 8,wrap 2,fillx", "[][grow,fill]"));
		panel.setBorder(BorderFactory.createTitledBorder(I18N.t("border.book.book")));

		title.setToolTipText(I18N.t("tooltip.book.title"));
		subtitle.setToolTipText(I18N.t("tooltip.book.subtitle"));
		language.setToolTipText(I18N.t("tooltip.book.language"));
		coverPage.setToolTipText(I18N.t("tooltip.book.cover_page"));
		titlePage.setToolTipText(I18N.t("tooltip.book.title_page"));

		panel.add(new JLabel(I18N.l("label.book.title")));
		panel.add(title);
		panel.add(new JLabel(I18N.l("label.book.subtitle")));
		panel.add(subtitle);
		panel.add(new JLabel(I18N.l("label.book.language")));
		panel.add(language);
		panel.add(coverPage, "span 2");
		panel.add(titlePage, "span 2");
		return panel;
	}

	private JPanel createReportsPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 4 8 8 8,fillx", "[grow,fill][]", "[]"));
		panel.setBorder(BorderFactory.createTitledBorder(I18N.t("border.book.reports")));
		panel.setToolTipText(I18N.t("tooltip.book.reports"));

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setVisibleRowCount(7);
		list.setCellRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				return super.getListCellRendererComponent(list, (index + 1) + ". " + value, index, isSelected, cellHasFocus);
			}
		});
		list.addListSelectionListener(e -> updateButtons());
		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && list.getSelectedIndex() >= 0)
					configureSelected();
			}
		});

		final JButton add = new JButton(I18N.t("button.book.add"));
		add.addActionListener(e -> addReport());
		configure.addActionListener(e -> configureSelected());
		remove.addActionListener(e -> removeSelected());
		up.addActionListener(e -> moveSelected(-1));
		down.addActionListener(e -> moveSelected(1));
		add.setToolTipText(I18N.t("tooltip.book.add"));
		configure.setToolTipText(I18N.t("tooltip.book.configure"));
		up.setToolTipText(I18N.t("tooltip.book.up"));
		down.setToolTipText(I18N.t("tooltip.book.down"));
		updateButtons();

		panel.add(new JScrollPane(list), "grow,spany 5,h 150::");
		panel.add(add, "growx,wrap");
		panel.add(configure, "growx,wrap");
		panel.add(remove, "growx,wrap");
		panel.add(up, "growx,wrap");
		panel.add(down, "growx");
		return panel;
	}

	private JPanel createPrefacePanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 4 8 8 8,wrap 2,fillx", "[][grow,fill]"));
		panel.setBorder(BorderFactory.createTitledBorder(I18N.t("border.book.preface")));
		panel.setToolTipText(I18N.t("tooltip.book.preface"));

		prefaceTitle.setToolTipText(I18N.t("tooltip.book.preface_title"));
		prefaceText.setToolTipText(I18N.t("tooltip.book.preface_text"));
		prefaceText.setLineWrap(true);
		prefaceText.setWrapStyleWord(true);

		panel.add(new JLabel(I18N.l("label.book.preface_title")));
		panel.add(prefaceTitle);
		panel.add(new JLabel(I18N.l("label.book.preface_text")), "top");
		panel.add(new JScrollPane(prefaceText));
		return panel;
	}

	private JPanel createIndexesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 4 8 8 8,wrap 2,fillx", "[grow,fill][grow,fill]"));
		panel.setBorder(BorderFactory.createTitledBorder(I18N.t("border.book.indexes")));
		panel.setToolTipText(I18N.t("tooltip.book.indexes"));

		for(final BookIndex index : BookIndex.values())
			panel.add(indexes.get(index));
		return panel;
	}

	private void updateButtons(){
		final int selected = list.getSelectedIndex();
		configure.setEnabled(selected >= 0);
		remove.setEnabled(selected >= 0);
		up.setEnabled(selected > 0);
		down.setEnabled(selected >= 0 && selected < items.size() - 1);
	}


	/* ======================================================================
	 *                          The reports of the book
	 * ====================================================================== */

	/** @return	The kinds of record a report of the type can be about; empty for a report of the whole file. */
	private static Class<? extends RecordTypeHandler<?>>[] subjectKinds(final ReportType type){
		return switch(type){
			case ANCESTOR, DESCENDANT, INDIVIDUAL, RELATIONSHIP, FAMILY_GROUP_SHEET, END_OF_LINE, PEDIGREE_COLLAPSE, KINSHIP ->
				new Class[]{IndividualHandler.class};
			case GROUP -> new Class[]{GroupHandler.class};
			case PLACE -> new Class[]{PlaceHandler.class};
			case SOURCE -> new Class[]{SourceHandler.class};
			case RESEARCH_QUESTION, GPS_PROOF -> new Class[]{ResearchQuestionHandler.class};
			//about any record
			case BIBLIOGRAPHY, RESEARCH_PROGRESS -> new Class[]{IndividualHandler.class, GroupHandler.class, SourceHandler.class};
			default -> new Class[0];
		};
	}

	/** Asks which report to add, and what it is about. */
	private void addReport(){
		final JComboBox<ReportType> types = ComboBoxSearch.searchable(new JComboBox<>(ReportType.values()));
		types.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				return super.getListCellRendererComponent(list, (value instanceof ReportType type? type.getTitle(): value), index,
					isSelected, cellHasFocus);
			}
		});
		final JPanel message = new JPanel(new MigLayout("ins 0,wrap 1,fillx", "[grow,fill]"));
		message.add(new JLabel(I18N.l("label.book.report_type")));
		message.add(types);
		if(MessageDialogs.confirm(this, message, I18N.t("dialog.book.add.title"), JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION)
			return;

		final ReportType type = (ReportType)types.getSelectedItem();
		FLEFRecord subject = null;
		final Class<? extends RecordTypeHandler<?>>[] kinds = subjectKinds(type);
		if(kinds.length > 0){
			final RecordSelectionDialog selection = RecordSelectionDialog.create(this, context.model(), (record, handler) -> {}, kinds);
			selection.setVisible(true);
			if(!selection.isConfirmed() || selection.getSelectedRecord() == null)
				return;

			subject = selection.getSelectedRecord();
		}

		//the options start from the ones the user chose the last time for this report
		final BookEntry entry = new BookEntry(type, (subject != null? subject.getId(): null), preferences.options(type, ReportConfig.defaults()));
		items.addElement(new Item(entry, describe(subject)));
		list.setSelectedIndex(items.size() - 1);
	}

	private String describe(final FLEFRecord subject){
		return (subject != null? HandlerRegistry.getHandler(subject.getTag()).getDisplayText(subject, context.model()): null);
	}

	/** Shows the options of the selected report: the ones it uses, as in the dialog of the report. */
	private void configureSelected(){
		final int index = list.getSelectedIndex();
		if(index < 0)
			return;

		final Item item = items.get(index);
		final BookEntry entry = item.entry();
		final FLEFRecord root = (entry.subjectId() != null? context.model().getRecordById(entry.subjectId()): null);
		final ReportOptionsPanel panel = new ReportOptionsPanel(entry.config(), option -> ReportBuilder.optionState(entry.type(), root, option));

		final JDialog dialog = new JDialog(this, item.toString(), ModalityType.APPLICATION_MODAL);
		final JScrollPane scroll = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getVerticalScrollBar()
			.setUnitIncrement(GUIHelper.SCROLLBAR_UNIT_INCREMENT);
		//(ESC cancels)
		final JPanel buttons = GUIHelper.createButtonPanel(dialog, () -> {
			final ReportConfig config = panel.toConfig((ReportLanguage)language.getSelectedItem());
			items.set(index, new Item(new BookEntry(entry.type(), entry.subjectId(), config), item.subject()));
			dialog.dispose();
		}, dialog::dispose);
		dialog.setLayout(new BorderLayout());
		dialog.add(scroll, BorderLayout.CENTER);
		dialog.add(buttons, BorderLayout.SOUTH);
		dialog.setPreferredSize(new Dimension(580, 680));
		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	private void removeSelected(){
		final int index = list.getSelectedIndex();
		if(index < 0)
			return;

		items.remove(index);
		if(!items.isEmpty())
			list.setSelectedIndex(Math.min(index, items.size() - 1));
		updateButtons();
	}

	private void moveSelected(final int direction){
		final int index = list.getSelectedIndex();
		final int target = index + direction;
		if(index < 0 || target < 0 || target >= items.size())
			return;

		final Item item = items.remove(index);
		items.add(target, item);
		list.setSelectedIndex(target);
	}


	/* ======================================================================
	 *                          The book
	 * ====================================================================== */

	/** @return	The book as the user described it; the lines of the cover text of the layout are the author of the title page. */
	private BookDefinition toDefinition(final ReportLayout layout){
		final List<BookEntry> entries = new ArrayList<>(items.size());
		for(int i = 0; i < items.size(); i ++)
			entries.add(items.get(i).entry());

		final EnumSet<BookIndex> chosen = EnumSet.noneOf(BookIndex.class);
		for(final Map.Entry<BookIndex, JCheckBox> entry : indexes.entrySet())
			if(entry.getValue().isSelected())
				chosen.add(entry.getKey());

		return new BookDefinition(title.getText(), subtitle.getText(), layout.coverText(), (ReportLanguage)language.getSelectedItem(),
			coverPage.isSelected(), titlePage.isSelected(), prefaceTitle.getText(), prefaceText.getText(), chosen, entries);
	}

	/** @return	The name the file of the book is proposed with: the title, with what a file name refuses as a single underscore. */
	private String suggestedBaseName(){
		final String text = title.getText();
		final StringBuilder name = new StringBuilder();
		for(int i = 0; i < text.length() && name.length() < MAX_FILE_NAME; i ++){
			final char c = text.charAt(i);
			if(Character.isLetterOrDigit(c))
				name.append(c);
			else if(!name.isEmpty() && name.charAt(name.length() - 1) != '_')
				name.append('_');
		}
		final String clean = StringUtils.strip(name.toString(), "_");
		return (clean.isEmpty()? "book": clean);
	}

	private void onGenerate(){
		if(items.isEmpty()){
			MessageDialogs.show(this, I18N.t("dialog.book.empty.message"), I18N.t("dialog.book.empty.title"), JOptionPane.WARNING_MESSAGE);

			return;
		}

		final Map.Entry<Path, ReportRenderer> target = ReportTargets.ask(this, preferences, suggestedBaseName());
		if(target == null)
			return;

		final Path file = target.getKey();
		final Path outDir = file.getParent();
		if(!ReportTargets.confirmOverwrite(this, outDir, List.of(file.getFileName().toString())))
			return;

		final ReportLayout layout = layoutPanel.toLayout();
		final BookDefinition book = toDefinition(layout);

		preferences.setDirectory(outDir.toString());
		preferences.setLayout(layout);
		preferences.setFormats(List.of(target.getValue().id()));
		preferences.setLanguage(book.language());

		runWithProgress(book, file, target.getValue(), layout);
	}

	private void runWithProgress(final BookDefinition book, final Path file, final ReportRenderer renderer, final ReportLayout layout){
		final ProgressDialog progressDialog = new ProgressDialog(this, I18N.t("dialog.book.generating"));
		final FLEFModel model = context.model();

		final SwingWorker<Path, int[]> worker = new SwingWorker<>(){

			private volatile String message;


			@Override
			protected Path doInBackground() throws Exception{
				try{
					final ReportProgressListener listener = (percent, text) -> {
						if(isCancelled())
							throw new RuntimeException("Report generation cancelled");
						if(text != null)
							message = text;
						progressDialog.update(percent / 2, message);
					};

					progressDialog.update(0, I18N.t("text.report.building"));
					final ReportDocument document = BookBuilder.build(model, book, listener);

					if(isCancelled())
						throw new RuntimeException("Report generation cancelled");
					progressDialog.update(50, I18N.tf("text.report.writing", file.getFileName()));
					//(the font is the automatic one: the one that has what the book needs)
					renderer.render(document, file, null, layout);
					progressDialog.update(100, I18N.t("text.report.done"));
					return file;
				}
				catch(final Exception e){
					if(isCancelled())
						try{
							Files.deleteIfExists(file);
						}
						catch(final Exception ignored){
							//a file that cannot be removed stays: nothing else can be done
						}
					throw e;
				}
			}

			@Override
			protected void done(){
				progressDialog.dispose();

				if(isCancelled()){
					MessageDialogs.show(BookBuilderDialog.this,
						I18N.t("dialog.report.cancelled.message"), I18N.t("dialog.report.cancelled.title"), JOptionPane.INFORMATION_MESSAGE);
					return;
				}

				try{
					final Path written = get();
					ReportTargets.promptOpenFolder(BookBuilderDialog.this, written.getParent(), List.of(written));
					dispose();
				}
				catch(final Exception ex){
					final Throwable cause = (ex.getCause() != null? ex.getCause(): ex);
					MessageDialogs.show(BookBuilderDialog.this,
						I18N.l("dialog.report.error.message") + StringUtils.LF + cause.getMessage(),
						I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
				}
			}

		};

		//the user cancels: the worker is told at once
		progressDialog.onCancel(() -> worker.cancel(true));

		worker.execute();
		progressDialog.setVisible(true);
	}

}
