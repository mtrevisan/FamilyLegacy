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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
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


/* DONE */
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
 * </pre>
 */
public class DatePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 7489525613734145165L;


	private static final String TAG_ORIGINAL_TEXT = "ORIGINAL_TEXT";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_POINT = "POINT";
	private static final String TAG_BOUNDED = "BOUNDED";
	private static final String TAG_SPANNING = "SPANNING";


	static{
		HandlerRegistry.register(new SourceHandler());
	}


	private final JTabbedPane tabbedPane = new JTabbedPane();

	private final BindingManager bindingManager = new BindingManager();

	private final SingleDatePanel pointDatePanel;
	private final BoundedDatePanel boundedDatePanel;
	private final SpanningDatePanel spanningDatePanel;

	private final BoundTextField originalTextField;
	private final EntityCitationListPanel sourcePanel;
	private final EvidenceQualifiersPanel qualifiers;


	public DatePanel(final Dialog parent, final FLEFModel model){
		pointDatePanel = new SingleDatePanel(parent, model);
		boundedDatePanel = new BoundedDatePanel(parent, model);
		spanningDatePanel = new SpanningDatePanel(parent, model);

		originalTextField = new BoundTextField(TAG_ORIGINAL_TEXT);
		sourcePanel = new EntityCitationListPanel(TAG_SOURCE, parent, "Sources", model, SourceHandler.TYPE);
		qualifiers = new EvidenceQualifiersPanel(null, "Evidence");


		initComponents();
	}


	private void initComponents(){
		bindingManager.bind(originalTextField);


		setLayout(new MigLayout("ins 0,fillx,wrap 1", "[grow]", "[]5[]5[]"));

		// Tabbed pane for date types
		final JPanel pointOuter = new JPanel(new MigLayout("ins 0,fillx"));
		pointOuter.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		final JPanel valueWrapper = new JPanel(new MigLayout("ins 7,fillx", "[right]rel[grow]"));
		valueWrapper.setBorder(BorderFactory.createTitledBorder("Point Date"));
		valueWrapper.add(pointDatePanel, "growx");
		pointOuter.add(valueWrapper, "growx");
		tabbedPane.addTab("Point", pointOuter);
		tabbedPane.addTab("Bounded", boundedDatePanel);
		tabbedPane.addTab("Spanning", spanningDatePanel);

		// When switching tabs, clear the other panels
		tabbedPane.addChangeListener(e -> {
			switch(tabbedPane.getSelectedIndex()){
				case 0 -> {
					boundedDatePanel.clear();
					spanningDatePanel.clear();
				}
				case 1 -> {
					pointDatePanel.clear();
					spanningDatePanel.clear();
				}
				case 2 -> {
					pointDatePanel.clear();
					boundedDatePanel.clear();
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

		if(record == null)
			return;

		// Load the date value: POINT, BOUNDED, or SPANNING
		final FLEFRecord point = FLEFRecordHelper.findChild(record, TAG_POINT);
		if(point != null){
			tabbedPane.setSelectedIndex(0);
			pointDatePanel.load(point);
		}
		else{
			final FLEFRecord bounded = FLEFRecordHelper.findChild(record, TAG_BOUNDED);
			if(bounded != null){
				tabbedPane.setSelectedIndex(1);
				boundedDatePanel.load(bounded);
			}
			else{
				final FLEFRecord spanning = FLEFRecordHelper.findChild(record, TAG_SPANNING);
				if(spanning != null){
					tabbedPane.setSelectedIndex(2);
					spanningDatePanel.load(spanning);
				}
			}
		}

		bindingManager.load(record);

		// source
		sourcePanel.load(record);

		// evidence
		qualifiers.load(record);
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
		pointDatePanel.clear();
		boundedDatePanel.clear();
		spanningDatePanel.clear();
		sourcePanel.clear();
		qualifiers.clear();
	}

	public boolean hasData(){
		return switch(tabbedPane.getSelectedIndex()){
			case 0 -> pointDatePanel.hasData();
			case 1 -> boundedDatePanel.hasData();
			case 2 -> spanningDatePanel.hasData();
			default -> false;
		};
	}

	public boolean validateData(){
		return switch(tabbedPane.getSelectedIndex()){
			case 0 -> pointDatePanel.validateData();
			case 1 -> boundedDatePanel.validateData();
			case 2 -> spanningDatePanel.validateData();
			default -> true;
		};
	}


	/**
	 * Saves a VALUE date.
	 * Creates a VALUE node with children ISO/CENTURY/DECADE and APPROXIMATE.
	 */
	private void savePoint(final FLEFRecord parent){
		if(!pointDatePanel.hasData())
			return;

		final FLEFRecord pointNode = pointDatePanel.save();
		parent.addChild(pointNode);
	}

	/**
	 * Saves a BOUNDED date.
	 * The BoundedDatePanel returns a node with NOT_BEFORE and NOT_AFTER (each containing date tags).
	 */
	private void saveBounded(final FLEFRecord parent){
		if(!boundedDatePanel.hasData())
			return;

		final FLEFRecord boundedNode = boundedDatePanel.save();
		parent.addChild(boundedNode);
	}

	/**
	 * Saves a SPANNING date.
	 * The SpanningDatePanel returns a node with FROM and TO (each containing date tags).
	 */
	private void saveSpanning(final FLEFRecord parent){
		if(!spanningDatePanel.hasData())
			return;

		final FLEFRecord spanningNode = spanningDatePanel.saveToRecord();
		parent.addChild(spanningNode);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("DatePanel Test");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new BorderLayout());

			DatePanel panel = new DatePanel(null, model);
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
