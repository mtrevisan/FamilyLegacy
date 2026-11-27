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
package io.github.mtrevisan.familylegacy.ui.components.dates;

import io.github.mtrevisan.calendars.assistant.DateSuggestion;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateShorthand;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateShorthandReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.quickentry.DateShorthandDescriber;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingManager;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.util.List;
import java.util.function.Supplier;


/**
 * Panel for editing a {@code DATE_STRUCTURE} according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * struct DateStructure {
 *   value: DateValue
 *   original_text?: Text
 *   source*: SourceCitation
 *   confidence?: Confidence
 * }
 *
 * DateValue = oneof {
 *   point: SingleDate
 *   bounded: BoundedDate
 *   spanning: SpanningDate
 * }
 * </pre>
 */
public class DateStructurePanel extends JPanel implements HistoricalDateAssistantHost{

	private final FLEFModel model;
	private Supplier<String> placeSupplier = () -> null;
	private final JTabbedPane tabbedPane = new JTabbedPane();

	private final BindingManager bindingManager = new BindingManager();

	private final SingleDatePanel pointDateValuePanel;
	private final BoundedDatePanel boundedDateValuePanel;
	private final SpanningDatePanel spanningDateValuePanel;

	private final JTextField quickEntryField = new JTextField();
	private final JLabel quickEntryPreview = new JLabel(StringUtils.SPACE);

	private final BoundTextField originalTextField;
	private final EntityListPanel sourcePanel;
	private final BoundComboBox<String> confidenceCombo;


	public DateStructurePanel(final Window parent, final FLEFModel model){
		this.model = model;

		pointDateValuePanel = new SingleDatePanel(parent, model);
		boundedDateValuePanel = new BoundedDatePanel(parent, model);
		spanningDateValuePanel = new SpanningDatePanel(parent, model);

		originalTextField = new BoundTextField(DateReader.TAG_ASSISTANT_FINDING_IDAL_TEXT);
		sourcePanel = EntityListPanel.createForCitationWrapper(DateReader.TAG_SOURCE, parent, I18N.t("dialog.component.sources_with_citations"),
			model, SourceCitationHandler.class);
		confidenceCombo = new BoundComboBox<>(DateReader.TAG_CONFIDENCE, GUIHelper.fillCombo(ConfidenceReader.CONFIDENCES, null));
		confidenceCombo.setI18NPrefix("enum.confidence");


		initComponents();
	}


	private void initComponents(){
		bindingManager.bind(originalTextField);
		bindingManager.bind(confidenceCombo);


		setLayout(new MigLayout("ins 0,fillx,wrap 2", "[][grow]", "[]5[]5[]5[]"));

		// Tabbed pane for date types
		final JPanel pointPanel = new JPanel(new MigLayout("ins 0,fillx"));
		pointPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		final JPanel valueWrapper = GUIHelper.createLabelFieldPanel(7, "[]");
		valueWrapper.setBorder(BorderFactory.createTitledBorder(I18N.t("border.date")));
		GUIHelper.addComponent(valueWrapper, pointDateValuePanel);
		pointPanel.add(valueWrapper, "growx");
		GUIHelper.addTab(tabbedPane, I18N.t("dialog.date.point"), "date_point", pointPanel);
		GUIHelper.addTab(tabbedPane, I18N.t("dialog.date.bounded"), "date_bounded", boundedDateValuePanel);
		GUIHelper.addTab(tabbedPane, I18N.t("dialog.date.spanning"), "date_spanning", spanningDateValuePanel);

		// When switching tabs, clear the other panels
		tabbedPane.addChangeListener(e -> {
			switch(tabbedPane.getSelectedIndex()){
				case 0 -> {
					boundedDateValuePanel.clear();
					spanningDateValuePanel.clear();
				}
				case 1 -> {
					pointDateValuePanel.clear();
					spanningDateValuePanel.clear();
				}
				case 2 -> {
					pointDateValuePanel.clear();
					boundedDateValuePanel.clear();
				}
			}
		});
		// quick entry: the date typed in the short notation fills the tabs
		final JLabel quickEntryLabel = new JLabel(I18N.l("dialog.date.quick_entry"));
		quickEntryLabel.setLabelFor(quickEntryField);
		quickEntryField.setToolTipText(I18N.t("dialog.date.quick_entry.tooltip"));
		quickEntryField.addActionListener(e -> applyQuickEntry());
		quickEntryField.getDocument()
			.addDocumentListener(new DocumentListener(){
				@Override
				public void insertUpdate(final DocumentEvent e){
					updateQuickEntryPreview();
				}

				@Override
				public void removeUpdate(final DocumentEvent e){
					updateQuickEntryPreview();
				}

				@Override
				public void changedUpdate(final DocumentEvent e){
					updateQuickEntryPreview();
				}
			});
		add(quickEntryLabel, "align label");
		add(quickEntryField, "growx");
		add(quickEntryPreview, "skip 1,growx");

		add(tabbedPane, "span 2,growx");

		final JLabel originalTextLabel = new JLabel(I18N.l("dialog.date.original_text"));
		originalTextLabel.setLabelFor(originalTextField);
		add(originalTextLabel, "align label");
		add(originalTextField, "growx");

		// source
		add(sourcePanel, "span 2,growx");

		// confidence
		final JLabel confidenceLabel = new JLabel(I18N.l("dialog.component.confidence"));
		confidenceLabel.setLabelFor(confidenceCombo);
		add(confidenceLabel, "align label");
		add(confidenceCombo, "growx");
	}


	/**
	 * Loads data from a DATE wrapper record.
	 *
	 * @param record	the DATE record (wrapper), or {@code null}
	 */
	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		// Load the date value: POINT, BOUNDED, or SPANNING
		final List<FLEFRecord> values = FLEFRecordHelper.extractStructures(record, DateReader.TAG_VALUE);
		final FLEFRecord value = (values.isEmpty()? null: values.getFirst());
		final FLEFRecord point = (value != null? FLEFRecordHelper.findChild(value, DateReader.TAG_POINT): null);
		if(point != null){
			tabbedPane.setSelectedIndex(0);

			pointDateValuePanel.load(point);
		}
		else{
			final FLEFRecord bounded = (value != null? FLEFRecordHelper.findChild(value, DateReader.TAG_BOUNDED): null);
			if(bounded != null){
				tabbedPane.setSelectedIndex(1);

				boundedDateValuePanel.load(bounded);
			}
			else{
				final FLEFRecord spanning = (value != null? FLEFRecordHelper.findChild(value, DateReader.TAG_SPANNING): null);
				if(spanning != null){
					tabbedPane.setSelectedIndex(2);

					spanningDateValuePanel.load(spanning);
				}
			}
		}

		bindingManager.load(record);

		// source
		sourcePanel.load(record);
	}

	/**
	 * Saves the current data into a DATE wrapper record.
	 *
	 * @return the DATE record, or {@code null} if no data
	 */
	public FLEFRecord save(){
		if(!hasData())
			return null;

		final FLEFRecord record = FLEFRecord.createEmpty();

		// date: value.(point|bounded|spanning), the same shape that load(FLEFRecord) reads
		final FLEFRecord value = FLEFRecord.createEmpty();
		switch(tabbedPane.getSelectedIndex()){
			case 0 -> savePoint(value);
			case 1 -> saveBounded(value);
			case 2 -> saveSpanning(value);
			default -> { /* do nothing */ }
		}
		record.addChildWithTag(DateReader.TAG_VALUE, value);

		bindingManager.save(record);

		// source
		sourcePanel.save(record);

		return (record.hasData()? record: null);
	}

	/**
	 * Reads what is typed in the quick entry and, if it is understood, fills the tabs with it.
	 * <p>
	 * The expression is kept as the original text of the date, if there is none yet and the expression says more than a plain date.
	 * </p>
	 *
	 * @return	Whether the quick entry is empty or has been applied (otherwise the user has been told what is wrong).
	 */
	boolean applyQuickEntry(){
		final String text = quickEntryField.getText();
		if(StringUtils.isBlank(text))
			return true;

		final DateShorthand shorthand;
		try{
			shorthand = DateShorthandReader.parse(text);
		}
		catch(final DateShorthandReader.SyntaxException se){
			MessageDialogs.show(this, describeProblem(se), I18N.t("validation.title"), JOptionPane.ERROR_MESSAGE);

			quickEntryField.requestFocusInWindow();

			return false;
		}

		//the calendar of the date that is being edited is the one of the new date
		final String calendar = currentCalendar();
		switch(shorthand.kind()){
			case POINT -> {
				tabbedPane.setSelectedIndex(0);
				pointDateValuePanel.setShorthand(shorthand.first(), calendar);
			}
			case BOUNDED -> {
				tabbedPane.setSelectedIndex(1);
				boundedDateValuePanel.setShorthand(shorthand.first(), shorthand.second(), calendar);
			}
			case SPANNING -> {
				tabbedPane.setSelectedIndex(2);
				spanningDateValuePanel.setShorthand(shorthand.first(), shorthand.second(), calendar);
			}
		}

		final boolean plainDate = (shorthand.kind() == DateShorthand.Kind.POINT && shorthand.first().fullDate() != null
			&& !shorthand.first().approximate());
		if(!plainDate && originalTextField.isEmpty())
			originalTextField.setText(text.strip());

		quickEntryField.setText(StringUtils.EMPTY);
		return true;
	}

	private String currentCalendar(){
		final String calendar = switch(tabbedPane.getSelectedIndex()){
			case 0 -> pointDateValuePanel.getCalendar();
			case 1 -> boundedDateValuePanel.getCalendar();
			case 2 -> spanningDateValuePanel.getCalendar();
			default -> null;
		};
		return (StringUtils.isNotBlank(calendar)? calendar: null);
	}

	/** Tells what the quick entry understood, or what is wrong with it, under the field. */
	private void updateQuickEntryPreview(){
		final String text = quickEntryField.getText();
		if(StringUtils.isBlank(text)){
			quickEntryPreview.setText(StringUtils.SPACE);
			quickEntryPreview.setForeground(UIManager.getColor("Label.foreground"));
			return;
		}

		try{
			quickEntryPreview.setText(describe(DateShorthandReader.parse(text)));
			quickEntryPreview.setForeground(UIManager.getColor("Label.disabledForeground"));
		}
		catch(final DateShorthandReader.SyntaxException se){
			quickEntryPreview.setText(describeProblem(se));
			final Color error = UIManager.getColor("Component.error.focusedBorderColor");
			quickEntryPreview.setForeground(error != null? error: Color.RED);
		}
	}

	/** @return	What is wrong with the expression, in words (see {@link DateShorthandDescriber}). */
	public static String describeProblem(final DateShorthandReader.SyntaxException exception){
		return DateShorthandDescriber.describeProblem(exception);
	}

	/** @return	What the expression means, e.g. {@code Bounded: not before 1850 (approx., stated), not after 1860}. */
	public static String describe(final DateShorthand shorthand){
		return DateShorthandDescriber.describe(shorthand);
	}

	public void clear(){
		bindingManager.clear();
		quickEntryField.setText(StringUtils.EMPTY);

		pointDateValuePanel.clear();
		boundedDateValuePanel.clear();
		spanningDateValuePanel.clear();
		sourcePanel.clear();
	}

	public boolean hasData(){
		return switch(tabbedPane.getSelectedIndex()){
			case 0 -> pointDateValuePanel.hasData();
			case 1 -> boundedDateValuePanel.hasData();
			case 2 -> spanningDateValuePanel.hasData();
			default -> false;
		};
	}

	public boolean validateData(){
		//what is typed in the quick entry and not yet applied is not lost
		if(!applyQuickEntry())
			return false;

		return switch(tabbedPane.getSelectedIndex()){
			case 0 -> pointDateValuePanel.validateData();
			case 1 -> boundedDateValuePanel.validateData();
			case 2 -> spanningDateValuePanel.validateData();
			default -> true;
		};
	}


	@Override
	public void openHistoricalDateAssistant(final HistoricalDateAssistantDialog.Tool tool){
		final boolean point = (tabbedPane.getSelectedIndex() == 0);
		new HistoricalDateAssistantDialog(SwingUtilities.getWindowAncestor(this), tool,
			(point? pointDateValuePanel.getCalendar(): null), (point? pointDateValuePanel.getFullDateText(): null),
			model, this::applySuggestion, placeSupplier.get())
			.setVisible(true);
	}

	/**
	 * Sets where the place of the event the date belongs to is found: the assistant preselects the reform of the
	 * jurisdiction of that place (the user can still choose another one).
	 *
	 * @param placeSupplier	gives the id of the place, or {@code null} if there is none (it is asked each time the assistant opens)
	 */
	public void setPlaceSupplier(final Supplier<String> placeSupplier){
		this.placeSupplier = (placeSupplier != null? placeSupplier: () -> null);
	}

	/**
	 * Applies a suggestion of the assistant, as a point date, after the user asked for it.
	 * <p>
	 * If a date is already present the user has to confirm the replacement.
	 * </p>
	 */
	private void applySuggestion(final DateSuggestion suggestion){
		if(!suggestion.isApplicable())
			return;

		if(hasData() && MessageDialogs.confirm(this, I18N.t("dialog.date_assistant.replace_confirm"),
				I18N.t("dialog.date_assistant.title"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
			return;

		if(suggestion.isBounded()){
			tabbedPane.setSelectedIndex(1);
			boundedDateValuePanel.setRange(suggestion.calendarCode(), suggestion.notBefore(), suggestion.notAfter());
		}
		else{
			tabbedPane.setSelectedIndex(0);
			pointDateValuePanel.setFullDate(suggestion.calendarCode(), suggestion.normalizedDate());
		}
	}


	/**
	 * Saves a VALUE date.
	 * Creates a VALUE node with children ISO/CENTURY/DECADE and APPROXIMATE.
	 */
	private void savePoint(final FLEFRecord parent){
		if(!pointDateValuePanel.hasData())
			return;

		final FLEFRecord pointNode = pointDateValuePanel.save();
		if(pointNode.hasData())
			parent.addChildWithTag(DateReader.TAG_POINT, pointNode);
	}

	/**
	 * Saves a BOUNDED date.
	 * The BoundedDatePanel returns a node with NOT_BEFORE and NOT_AFTER (each containing date tags).
	 */
	private void saveBounded(final FLEFRecord parent){
		if(!boundedDateValuePanel.hasData())
			return;

		final FLEFRecord boundedNode = boundedDateValuePanel.save();
		parent.addChild(boundedNode);
	}

	/**
	 * Saves a SPANNING date.
	 * The SpanningDatePanel returns a node with FROM and TO (each containing date tags).
	 */
	private void saveSpanning(final FLEFRecord parent){
		if(!spanningDateValuePanel.hasData())
			return;

		final FLEFRecord spanningNode = spanningDateValuePanel.saveToRecord();
		parent.addChild(spanningNode);
	}


	static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("DatePanel Test");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new BorderLayout());

			DateStructurePanel panel = new DateStructurePanel(null, model);
			frame.add(panel, BorderLayout.CENTER);

			JButton printBtn = new JButton(I18N.t("button.print_record"));
			printBtn.addActionListener(e -> {
				FLEFRecord record = panel.save();
				if(record != null)
					System.out.println("=== Saved DATE ===");
			});
			frame.add(printBtn, BorderLayout.SOUTH);

			frame.setSize(900, 800);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
