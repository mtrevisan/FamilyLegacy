package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.FLEFFile;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
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
import java.util.ArrayList;
import java.util.List;


/* DONE */
/**
 * Panel for editing a {@code DATE_STRUCTURE} according to FLEF 0.1.0.
 * <p>
 * The actual structure (real tags):
 * <pre>
 * DATE_STRUCTURE :=
 * n <<DATE_VALUE>>    {1:1}
 * n <<SOURCE_CITATION>>    {0:M}
 * n <<EVIDENCE_QUALIFIERS>>    {0:1}
 * </pre>
 */
public class DatePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 7489525613734145165L;


	private final BindingManager bindingManager = new BindingManager();

	// Tabs for date types
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final SingleDatePanel pointDatePanel;
	private final BoundedDatePanel boundedDatePanel;
	private final SpanningDatePanel spanningDatePanel;

	// Source Citations
	private final SourceCitationListPanel sourceCitationPanel;

	// Qualifiers
	private final BoundComboBox<String> certaintyCombo;
	private final BoundComboBox<String> credibilityCombo;


	public DatePanel(final Dialog parent, final FLEFModel model){
		this.pointDatePanel = new SingleDatePanel(parent, model);
		this.boundedDatePanel = new BoundedDatePanel(parent, model);
		this.spanningDatePanel = new SpanningDatePanel(parent, model);

		this.sourceCitationPanel = new SourceCitationListPanel(parent, model);

		certaintyCombo = new BoundComboBox<>("CERTAINTY",
			new String[]{StringUtils.EMPTY, "challenged", "disproven", "proven"});
		credibilityCombo = new BoundComboBox<>("CREDIBILITY",
			new String[]{StringUtils.EMPTY, "0", "1", "2", "3"});

		initComponents();
	}


	private void initComponents(){
		bindingManager.bind(certaintyCombo);
		bindingManager.bind(credibilityCombo);


		setLayout(new MigLayout("ins 0, fillx, wrap 1", "[grow]", "[]5[]5[]"));
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

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

		// Source Citations
		add(sourceCitationPanel, "growx");

		// Qualifiers
		add(createEvidencePanel(), "growx");
	}

	private JPanel createEvidencePanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 5, fillx", "[right]rel[grow]", "[]5[]"));
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
		final FLEFRecord point = FLEFRecordUtils.findChild(dateWrapper, "POINT");
		if(point != null){
			tabbedPane.setSelectedIndex(0);
			pointDatePanel.loadFromRecord(point);
		}
		else{
			final FLEFRecord bounded = FLEFRecordUtils.findChild(dateWrapper, "BOUNDED");
			if(bounded != null){
				tabbedPane.setSelectedIndex(1);
				boundedDatePanel.loadFromRecord(bounded);
			}
			else{
				final FLEFRecord spanning = FLEFRecordUtils.findChild(dateWrapper, "SPANNING");
				if(spanning != null){
					tabbedPane.setSelectedIndex(2);
					spanningDatePanel.loadFromRecord(spanning);
				}
			}
		}

		// SOURCE_CITATION
		final List<FLEFRecord> citations = new ArrayList<>();
		for(final FLEFRecord child : dateWrapper.getChildren())
			if("SOURCE".equals(child.getTag()))
				citations.add(child);
		sourceCitationPanel.loadFromCitations(citations);

		// EVIDENCE_QUALIFIERS
		bindingManager.loadFromRecord(dateWrapper);
	}

	/**
	 * Saves the current data into a DATE wrapper record.
	 *
	 * @param target an existing DATE record to update, or null to create a new one
	 * @return the DATE record, or null if no data
	 */
	public FLEFRecord save(final FLEFRecord target){
		if(!hasData())
			return null;

		final FLEFRecord record = target != null? target: FLEFRecord.createChild("DATE");
		FLEFRecordUtils.removeAllChildren(record);

		// Save the date value: POINT, BOUNDED, or SPANNING
		switch(tabbedPane.getSelectedIndex()){
			case 0 -> savePoint(record);
			case 1 -> saveBounded(record);
			case 2 -> saveSpanning(record);
			default -> { /* do nothing */ }
		}

		// Save SOURCE_CITATION
		for(final FLEFRecord citation : sourceCitationPanel.getCitations()){
			citation.setTag("SOURCE");
			record.addChild(citation);
		}

		// Save EVIDENCE_QUALIFIERS
		bindingManager.saveToRecord(record);

		return record;
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

	public boolean validateRequiredFields(){
		return switch(tabbedPane.getSelectedIndex()){
			case 0 -> pointDatePanel.validateRequiredFields();
			case 1 -> boundedDatePanel.validateRequiredFields();
			case 2 -> spanningDatePanel.validateRequiredFields();
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

		final FLEFRecord pointNode = FLEFRecord.createChild("POINT");
		pointDatePanel.saveToRecord(pointNode);
		if(pointNode.hasChildren()){
			parent.addChild(pointNode);
		}
	}

	/**
	 * Saves a BOUNDED date.
	 * The BoundedDatePanel returns a node with NOT_BEFORE and NOT_AFTER (each containing date tags).
	 */
	private void saveBounded(final FLEFRecord parent){
		if(!boundedDatePanel.hasData())
			return;

		final FLEFRecord boundedNode = boundedDatePanel.saveToRecord(null);
		if(boundedNode != null && boundedNode.hasChildren())
			for(final FLEFRecord child : boundedNode.getChildren())
				parent.addChild(child);
	}

	/**
	 * Saves a SPANNING date.
	 * The SpanningDatePanel returns a node with FROM and TO (each containing date tags).
	 */
	private void saveSpanning(final FLEFRecord parent){
		if(!spanningDatePanel.hasData())
			return;

		final FLEFRecord spanningNode = spanningDatePanel.saveToRecord(null);
		if(spanningNode != null && spanningNode.hasChildren())
			for(final FLEFRecord child : spanningNode.getChildren())
				parent.addChild(child);
	}


	public static String getDisplayText(FLEFRecord dateRecord){
		if(dateRecord == null)
			return StringUtils.EMPTY;

		// 1. POINT
		final FLEFRecord point = FLEFRecordUtils.findChild(dateRecord, "POINT");
		if(point != null)
			return formatQualifiedDate(point);

		// 2. BOUNDED (NOT_BEFORE / NOT_AFTER)
		final FLEFRecord bounded = FLEFRecordUtils.findChild(dateRecord, "BOUNDED");
		if(bounded != null){
			final FLEFRecord notBefore = FLEFRecordUtils.findChild(bounded, "NOT_BEFORE");
			final FLEFRecord notAfter = FLEFRecordUtils.findChild(bounded, "NOT_AFTER");

			final String nbStr = (notBefore != null? formatQualifiedDate(notBefore): null);
			final String naStr = (notAfter != null? formatQualifiedDate(notAfter): null);

			if(nbStr != null && naStr != null)
				return "Between " + nbStr + " and " + naStr;
			if(nbStr != null)
				return "After " + nbStr;
			if(naStr != null)
				return "Before " + naStr;
		}

		// 3. SPANNING (FROM / TO)
		final FLEFRecord spanning = FLEFRecordUtils.findChild(dateRecord, "SPANNING");
		if(spanning != null){
			final FLEFRecord from = FLEFRecordUtils.findChild(spanning, "FROM");
			final FLEFRecord to = FLEFRecordUtils.findChild(spanning, "TO");

			final String fromStr = (from != null? formatQualifiedDate(from): null);
			final String toStr = (to != null? formatQualifiedDate(to): null);

			if(fromStr != null && toStr != null)
				return "From " + fromStr + " to " + toStr;
			if(fromStr != null)
				return "From " + fromStr;
			if(toStr != null)
				return "To " + toStr;
		}

		return StringUtils.defaultString(dateRecord.getValue());
	}

	private static String formatQualifiedDate(final FLEFRecord parentNode){
		if(parentNode == null)
			return null;

		final String singleDateStr = formatSingleDate(parentNode);
		if(StringUtils.isBlank(singleDateStr))
			return null;

		final boolean isApprox = (FLEFRecordUtils.findChild(parentNode, "APPROXIMATE") != null);
		return (isApprox? "abt. " + singleDateStr: singleDateStr);
	}

	private static String formatSingleDate(final FLEFRecord parentNode){
		final FLEFRecord fullDate = FLEFRecordUtils.findChild(parentNode, "FULL_DATE");
		if(fullDate != null && StringUtils.isNotBlank(fullDate.getValue()))
			return fullDate.getValue();

		final FLEFRecord century = FLEFRecordUtils.findChild(parentNode, "CENTURY");
		if(century != null && StringUtils.isNotBlank(century.getValue())){
			final FLEFRecord part = FLEFRecordUtils.findChild(century, "PART");
			if(part != null && StringUtils.isNotBlank(part.getValue()))
				return part.getValue().replace('_', ' ') + " of " + century.getValue() + " cent.";
			return century.getValue() + " cent.";
		}

		final FLEFRecord decade = FLEFRecordUtils.findChild(parentNode, "DECADE");
		if(decade != null && StringUtils.isNotBlank(decade.getValue()))
			return decade.getValue() + "s";

		return null;
	}


	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("DatePanel Test");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new BorderLayout());

			DatePanel panel = new DatePanel(null, model);
			frame.add(panel, BorderLayout.CENTER);

			JButton printBtn = new JButton("Print Record");
			printBtn.addActionListener(e -> {
				FLEFRecord record = panel.save(null);
				if(record != null){
					System.out.println("=== Saved DATE ===");
					FLEFFile.print(model);
				}
			});
			frame.add(printBtn, BorderLayout.SOUTH);

			frame.setSize(900, 800);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
