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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Panel for editing a {@code DATE_STRUCTURE} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * struct DateStructure {
 *   value: DateValue
 *   original_text?: Text
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 * }
 *
 * DateValue = oneof {
 *   point: QualifiedDate
 *   bounded: BoundedDate
 *   spanning: SpanningDate
 * }
 * </pre>
 */
public class DateStructurePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 7489525613734145165L;


	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_ORIGINAL_TEXT = "ORIGINAL_TEXT";
	private static final String TAG_SOURCE = "SOURCE";

	private static final String TAG_POINT = "POINT";
	private static final String TAG_BOUNDED = "BOUNDED";
	private static final String TAG_SPANNING = "SPANNING";

	private static final String TAG_EVIDENCE = "EVIDENCE";


	private final JTabbedPane tabbedPane = new JTabbedPane();

	private final BindingManager bindingManager = new BindingManager();

	private final QualifiedDatePanel pointDateValuePanel;
	private final BoundedDatePanel boundedDateValuePanel;
	private final SpanningDatePanel spanningDateValuePanel;

	private final BoundTextField originalTextField;
	private final EntityCitationListPanel sourcePanel;
	private final EvidenceQualifiersPanel qualifiers;


	public DateStructurePanel(final Dialog parent, final FLEFModel model){
		pointDateValuePanel = new QualifiedDatePanel(parent, model);
		boundedDateValuePanel = new BoundedDatePanel(parent, model);
		spanningDateValuePanel = new SpanningDatePanel(parent, model);

		originalTextField = new BoundTextField(TAG_ORIGINAL_TEXT);
		sourcePanel = new EntityCitationListPanel(TAG_SOURCE, parent, "Sources with Citations", model, SourceCitationHandler.class);
		qualifiers = new EvidenceQualifiersPanel(null, parent, "Evidence", model, null);


		initComponents();
	}


	private void initComponents(){
		bindingManager.bind(originalTextField);


		setLayout(new MigLayout("ins 0,fillx,wrap 1", "[grow]", "[]5[]5[]"));

		// Tabbed pane for date types
		final JPanel pointPanel = new JPanel(new MigLayout("ins 0,fillx"));
		pointPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		final JPanel valueWrapper = GUIHelper.createLabelFieldPanel(7, "[]");
		valueWrapper.setBorder(BorderFactory.createTitledBorder("Date"));
		GUIHelper.addComponent(valueWrapper, pointDateValuePanel);
		pointPanel.add(valueWrapper, "growx");
		tabbedPane.addTab("Point", pointPanel);
		tabbedPane.addTab("Bounded", boundedDateValuePanel);
		tabbedPane.addTab("Spanning", spanningDateValuePanel);

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
		add(tabbedPane, "growx,wrap");

		add(new JLabel("Original Text:"), "align label");
		add(originalTextField, "growx");

		// source
		add(sourcePanel, "growx");

		// qualifiers
		add(qualifiers, "span 2,growx");
	}


	/**
	 * Loads data from a DATE wrapper record.
	 *
	 * @param record	the DATE record (wrapper), or null
	 */
	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		// Load the date value: POINT, BOUNDED, or SPANNING
		final FLEFRecord value = FLEFRecordHelper.extractStructures(record, TAG_VALUE)
			.getFirst();
		final FLEFRecord point = FLEFRecordHelper.findChild(value, TAG_POINT);
		if(point != null){
			tabbedPane.setSelectedIndex(0);

			pointDateValuePanel.load(point);
		}
		else{
			final FLEFRecord bounded = FLEFRecordHelper.findChild(value, TAG_BOUNDED);
			if(bounded != null){
				tabbedPane.setSelectedIndex(1);

				boundedDateValuePanel.load(bounded);
			}
			else{
				final FLEFRecord spanning = FLEFRecordHelper.findChild(value, TAG_SPANNING);
				if(spanning != null){
					tabbedPane.setSelectedIndex(2);

					spanningDateValuePanel.load(spanning);
				}
			}
		}

		bindingManager.load(record);

		// source
		sourcePanel.load(record);

		// evidence
		qualifiers.load(record.getTheOnlyChild(TAG_EVIDENCE));
	}

	/**
	 * Saves the current data into a DATE wrapper record.
	 *
	 * @return the DATE record, or null if no data
	 */
	public FLEFRecord save(){
		if(!hasData())
			return null;

		final FLEFRecord record = FLEFRecord.createEmpty();

		// date
		switch(tabbedPane.getSelectedIndex()){
			case 0 -> savePoint(record);
			case 1 -> saveBounded(record);
			case 2 -> saveSpanning(record);
			default -> { /* do nothing */ }
		}

		bindingManager.save(record);

		// source
		sourcePanel.save(record);

		// evidence
		qualifiers.save(record);

		return (record.hasData()? record: null);
	}

	public void clear(){
		pointDateValuePanel.clear();
		boundedDateValuePanel.clear();
		spanningDateValuePanel.clear();
		sourcePanel.clear();
		qualifiers.clear();
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
		return switch(tabbedPane.getSelectedIndex()){
			case 0 -> pointDateValuePanel.validateData();
			case 1 -> boundedDateValuePanel.validateData();
			case 2 -> spanningDateValuePanel.validateData();
			default -> true;
		};
	}


	/**
	 * Saves a VALUE date.
	 * Creates a VALUE node with children ISO/CENTURY/DECADE and APPROXIMATE.
	 */
	private void savePoint(final FLEFRecord parent){
		if(!pointDateValuePanel.hasData())
			return;

		final FLEFRecord pointNode = pointDateValuePanel.save();
		parent.addChild(pointNode);
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


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){}

		HandlerRegistry.scanHandlers();

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("DatePanel Test");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new BorderLayout());

			DateStructurePanel panel = new DateStructurePanel(null, model);
			frame.add(panel, BorderLayout.CENTER);

			JButton printBtn = new JButton("Print Record");
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
