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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
/**
 * Panel for editing a {@code DATE_STRUCTURE} according to FLEF 0.1.1.
 * <p>
 * The actual structure (real tags):
 * <pre>
 * struct DateStructure {
 *   value: DateValue
 *   original_text?: Text
 *   citation*: SourceCitation
 *   evidence?: EvidenceQualifiers
 * }
 * </pre>
 */
public class DatePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 7489525613734145165L;


	private static final String TAG_ORIGINAL_TEXT = "ORIGINAL_TEXT";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_CERTAINTY = "CERTAINTY";
	private static final String TAG_CREDIBILITY = "CREDIBILITY";
	private static final String TAG_POINT = "POINT";
	private static final String TAG_BOUNDED = "BOUNDED";
	private static final String TAG_SPANNING = "SPANNING";


	private final BindingManager bindingManager = new BindingManager();

	private final JTabbedPane tabbedPane = new JTabbedPane();

	private final SingleDatePanel pointDatePanel;
	private final BoundedDatePanel boundedDatePanel;
	private final SpanningDatePanel spanningDatePanel;

	private final BoundTextField originalTextField;
	private final SourceCitationListPanel sourceCitationPanel;
	private final BoundComboBox<String> certaintyCombo;
	private final BoundComboBox<String> credibilityCombo;


	public DatePanel(final Dialog parent, final FLEFModel model){
		pointDatePanel = new SingleDatePanel(parent, model);
		boundedDatePanel = new BoundedDatePanel(parent, model);
		spanningDatePanel = new SpanningDatePanel(parent, model);

		originalTextField = new BoundTextField(TAG_ORIGINAL_TEXT, 15);
		sourceCitationPanel = new SourceCitationListPanel(TAG_SOURCE, parent, model);
		certaintyCombo = new BoundComboBox<>(TAG_CERTAINTY,
			new String[]{StringUtils.EMPTY, "proven", "challenged", "disproven"});
		credibilityCombo = new BoundComboBox<>(TAG_CREDIBILITY,
			new String[]{StringUtils.EMPTY, "0", "1", "2", "3"});

		initComponents();
	}


	private void initComponents(){
		bindingManager.bind(originalTextField);
		bindingManager.bind(certaintyCombo);
		bindingManager.bind(credibilityCombo);


		setLayout(new MigLayout("ins 0, fillx, wrap 1", "[grow]", "[]5[]5[]"));

		// Tabbed pane for date types
		final JPanel pointOuter = new JPanel(new MigLayout("ins 0, fillx"));
		pointOuter.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		final JPanel valueWrapper = new JPanel(new MigLayout("ins 7, fillx", "[right]rel[grow]"));
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

		add(new JLabel("Origianl Text:"), "align label");
		add(originalTextField, "growx");

		// Source Citations
		add(sourceCitationPanel, "growx");

		// Qualifiers
		add(createEvidencePanel(), "growx");
	}

	private JPanel createEvidencePanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx", "[right]rel[grow]", "[]5[]"));
		panel.setBorder(new TitledBorder("Qualifiers"));

		panel.add(new JLabel("Certainty:"), "align label");
		panel.add(certaintyCombo, "growx,wrap");

		panel.add(new JLabel("Credibility:"), "align label");
		panel.add(credibilityCombo, "growx,wrap");

		return panel;
	}


	/**
	 * Loads data from a DATE wrapper record.
	 *
	 * @param dateWrapper the DATE record (wrapper), or null
	 */
	public void load(final FLEFRecord dateWrapper){
		clear();

		if(dateWrapper == null)
			return;

		// Load the date value: POINT, BOUNDED, or SPANNING
		final FLEFRecord point = FLEFRecordHelper.findChild(dateWrapper, TAG_POINT);
		if(point != null){
			tabbedPane.setSelectedIndex(0);
			pointDatePanel.load(point);
		}
		else{
			final FLEFRecord bounded = FLEFRecordHelper.findChild(dateWrapper, TAG_BOUNDED);
			if(bounded != null){
				tabbedPane.setSelectedIndex(1);
				boundedDatePanel.load(bounded);
			}
			else{
				final FLEFRecord spanning = FLEFRecordHelper.findChild(dateWrapper, TAG_SPANNING);
				if(spanning != null){
					tabbedPane.setSelectedIndex(2);
					spanningDatePanel.load(spanning);
				}
			}
		}

		// SOURCE_CITATION
		sourceCitationPanel.load(dateWrapper);

		// EVIDENCE_QUALIFIERS
		bindingManager.load(dateWrapper);
	}

	/**
	 * Saves the current data into a DATE wrapper record.
	 *
	 * @return the DATE record, or null if no data
	 */
	public FLEFRecord save(){
		if(!hasData())
			return null;

		final FLEFRecord target = FLEFRecord.createEmpty();

		// Save the date value: POINT, BOUNDED, or SPANNING
		switch(tabbedPane.getSelectedIndex()){
			case 0 -> savePoint(target);
			case 1 -> saveBounded(target);
			case 2 -> saveSpanning(target);
			default -> { /* do nothing */ }
		}

		// Save SOURCE_CITATION
		sourceCitationPanel.save(target);

		// Save EVIDENCE_QUALIFIERS
		bindingManager.save(target);

		return (target.hasData()? target: null);
	}

	public void clear(){
		pointDatePanel.clear();
		boundedDatePanel.clear();
		spanningDatePanel.clear();
		sourceCitationPanel.clear();
		certaintyCombo.setSelectedIndex(0);
		credibilityCombo.setSelectedIndex(0);
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
		if(pointNode.hasData())
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
		if(boundedNode.hasData())
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
		if(spanningNode.hasData())
			parent.addChild(spanningNode);
	}


	public static void main(String[] args){
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
				if(record != null){
					System.out.println("=== Saved DATE ===");
				}
			});
			frame.add(printBtn, BorderLayout.SOUTH);

			frame.setSize(900, 800);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
