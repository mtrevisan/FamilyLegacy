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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.EventStructurePanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CalendarHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.FamilyEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.FamilyHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.Serial;


/**
 * Dialog for editing a FAMILY_EVENT_RECORD according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * FAMILY_EVENT_RECORD :=
 *   n @<XREF:EVENT>@ EVENT    {1:1}
 *     +1 TYPE [ ENGAGEMENT | MARRIAGE_BANN | MARRIAGE_CONTRACT | MARRIAGE_LICENCE | MARRIAGE_SETTLEMENT | MARRIAGE | CHILDREN_COUNT | ADOPTION | DIVORCE_FILED | DIVORCE_DECREE | DIVORCE | ANNULMENT | RESIDENCE | CENSUS | &lt;EVENT_TYPE&gt; ]    {1:1}
 *     +1 <<EVENT_STRUCTURE>>    {0:1}
 * </pre>
 */
public class FamilyEventDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -2474860174064870507L;


	static{
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new CalendarHandler());
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new FamilyHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new FamilyEventHandler());
	}

	// ========== Basic fields ==========
	private final JTextField idField = new JTextField(10);
	private final JComboBox<String> typeCombo = new JComboBox<>();

	// ========== EVENT_STRUCTURE (0:1) ==========
	private final EventStructurePanel eventStructurePanel;

	// ========== Buttons ==========
	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	// ==================== Constructors ====================
	public FamilyEventDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, model, "Edit Family Event", record);

		this.eventStructurePanel = new EventStructurePanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(900, 750));
		pack();
		setLocationRelativeTo(parent);
	}

	public FamilyEventDialog(Frame parent, FLEFModel model){
		super(parent, model, "New Family Event", null);

		this.eventStructurePanel = new EventStructurePanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(900, 750));
		pack();
		setLocationRelativeTo(parent);
	}

	// ==================== UI Initialization ====================
	@Override
	protected void initComponents(){
		setLayout(new BorderLayout(10, 10));

		// Initialize type combo with predefined family event types
		initTypeCombo();

		JTabbedPane tabbedPane = new JTabbedPane();

		// --- Basic tab ---
		tabbedPane.addTab("Basic", createBasicPanel());

		// --- Event Structure tab ---
		tabbedPane.addTab("Event Structure", eventStructurePanel);

		add(tabbedPane, BorderLayout.CENTER);

		// --- Button panel ---
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}

	// ==================== Panel factories ====================

	private void initTypeCombo(){
		String[] predefinedTypes = {
			"ENGAGEMENT", "MARRIAGE_BANN", "MARRIAGE_CONTRACT", "MARRIAGE_LICENCE",
			"MARRIAGE_SETTLEMENT", "MARRIAGE", "CHILDREN_COUNT", "ADOPTION",
			"DIVORCE_FILED", "DIVORCE_DECREE", "DIVORCE", "ANNULMENT",
			"RESIDENCE", "CENSUS"
		};
		for(String type : predefinedTypes){
			typeCombo.addItem(type);
		}
		typeCombo.setEditable(true);
	}

	private JPanel createBasicPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// ID (read-only)
		idField.setEditable(false);
		idField.setText(record.getId());
		panel.add(new JLabel("ID:"), "align label");
		panel.add(idField, "growx,wrap");

		// TYPE (1:1) - marked with an asterisk
		panel.add(new JLabel("Type*:"), "align label");
		panel.add(typeCombo, "growx");

		return panel;
	}

	// ==================== Load Data ====================

	@Override
	protected void loadData(){
		idField.setText(record.getId());

		// TYPE (1:1)
		String type = FLEFRecordUtils.getChildValue(record, "TYPE");
		if(type != null && !type.isEmpty()){
			typeCombo.setSelectedItem(type);
		}

		// EVENT_STRUCTURE (0:1)
		FLEFRecord eventStruct = FLEFRecordUtils.findChild(record, "EVENT_STRUCTURE");
		eventStructurePanel.loadFromRecord(eventStruct);
	}

	// ==================== Validation ====================

	@Override
	protected boolean validateData(){
		// TYPE (1:1) - required
		String type = (String)typeCombo.getSelectedItem();
		if(type == null || type.trim().isEmpty()){
			JOptionPane.showMessageDialog(this,
				"TYPE is required.\nPlease select or enter an event type.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			typeCombo.requestFocusInWindow();
			return false;
		}

		// EVENT_STRUCTURE (0:1) - validate if present
		return (!eventStructurePanel.hasData() || eventStructurePanel.validateRequiredFields());
	}

	// ==================== Save ====================

	@Override
	protected void saveRecord(){
		// Validation is already done by save() before calling this method
		record.getChildren().clear();

		// TYPE (1:1)
		String type = (String)typeCombo.getSelectedItem();
		if(type != null && !type.trim().isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "TYPE", type.trim());
		}

		// EVENT_STRUCTURE (0:1)
		if(eventStructurePanel.hasData()){
			FLEFRecord eventStruct = eventStructurePanel.saveToRecord(null);
			if(eventStruct != null){
				eventStruct.setLevel(1);
				eventStruct.setTag("EVENT_STRUCTURE");
				record.addChild(eventStruct);
			}
		}

		if(isNew){
			model.addRecord(record);
		}
		dispose();
	}

	// ==================== Overrides ====================

	@Override
	protected FLEFRecord createNewRecord(){
		FLEFRecord newRecord = new FLEFRecord();
		newRecord.setType("EVENT");
		newRecord.setId(generateNewId());
		return newRecord;
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, "EVENT", "E");
	}

	private Frame getParentFrame(){
		Container parent = getParent();
		while(parent != null && !(parent instanceof Frame)){
			parent = parent.getParent();
		}
		return (Frame)parent;
	}

	// ==================== Main per test ====================

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Family Event Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Family Event");
			btn.addActionListener(e -> {
				FamilyEventDialog dialog = new FamilyEventDialog(frame, model);
				dialog.setVisible(true);
				System.out.println("Family Event saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
