package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.Serial;


public class TextValueVariantDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -4887775439277994973L;


	private final JRadioButton phoneticRadio = new JRadioButton("Phonetic", true);
	private final JRadioButton transcriptionRadio = new JRadioButton("Transcription");

	private final JTextField systemField = new JTextField(15);
	private final JTextField typeField = new JTextField(15);
	private final JTextField valueField = new JTextField(20);

	private final JLabel systemLabel = new JLabel("System*:");
	private final JLabel typeLabel = new JLabel("Type:");
	private final JLabel valueLabel = new JLabel("Value*:");

	private FLEFRecord variantRecord;
	private boolean saved;

	TextValueVariantDialog(final Window parent, final FLEFRecord existing){
		super(parent, existing == null? "Add Text Value Variant": "Edit Text Value Variant", ModalityType.APPLICATION_MODAL);
		this.variantRecord = existing;
		initComponents();
		loadData();
		pack();
		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		final ButtonGroup group = new ButtonGroup();
		group.add(phoneticRadio);
		group.add(transcriptionRadio);

		final JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		radioPanel.add(phoneticRadio);
		radioPanel.add(transcriptionRadio);

		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx", "[right]rel[grow]", "[]5[]5[]5[]"));
		panel.add(new JLabel("Variant Kind:"), "align label");
		panel.add(radioPanel, "growx, wrap");

		panel.add(systemLabel, "align label");
		panel.add(systemField, "growx, wrap");

		panel.add(typeLabel, "align label");
		panel.add(typeField, "growx, wrap");

		panel.add(valueLabel, "align label");
		panel.add(valueField, "growx, wrap");

		phoneticRadio.addActionListener(e -> updateFieldsState());
		transcriptionRadio.addActionListener(e -> updateFieldsState());

		final JButton okBtn = new JButton("OK");
		final JButton cancelBtn = new JButton("Cancel");
		okBtn.addActionListener(e -> save());
		cancelBtn.addActionListener(e -> dispose());

		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(okBtn);
		buttonPanel.add(cancelBtn);

		setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);

		updateFieldsState();
	}

	private void updateFieldsState(){
		final boolean isTranscription = transcriptionRadio.isSelected();
		typeLabel.setEnabled(isTranscription);
		typeField.setEnabled(isTranscription);
		systemLabel.setText("System*:");
	}

	private void loadData(){
		if(variantRecord == null) return;

		if("TRANSCRIPTION".equals(variantRecord.getTag())){
			transcriptionRadio.setSelected(true);
			systemField.setText(variantRecord.getValue());
			typeField.setText(FLEFRecordUtils.getChildValue(variantRecord, "TYPE"));
			valueField.setText(FLEFRecordUtils.getChildValue(variantRecord, "VALUE"));
		}
		else{
			phoneticRadio.setSelected(true);
			systemField.setText(variantRecord.getValue());
			valueField.setText(FLEFRecordUtils.getChildValue(variantRecord, "VALUE"));
		}
		updateFieldsState();
	}

	private void save(){
		final String system = systemField.getText().trim();
		final String value = valueField.getText().trim();

		if(system.isEmpty() || value.isEmpty()){
			JOptionPane.showMessageDialog(this, "System and Value fields are required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if(phoneticRadio.isSelected()){
			variantRecord = FLEFRecord.createChildWithValue("PHONETIC", system);
			variantRecord.addChild(FLEFRecord.createChildWithValue("VALUE", value));
		}
		else{
			variantRecord = FLEFRecord.createChildWithValue("TRANSCRIPTION", system);
			final String type = typeField.getText().trim();
			if(!type.isEmpty()){
				variantRecord.addChild(FLEFRecord.createChildWithValue("TYPE", type));
			}
			variantRecord.addChild(FLEFRecord.createChildWithValue("VALUE", value));
		}

		saved = true;
		dispose();
	}

	public boolean isSaved(){
		return saved;
	}

	public FLEFRecord getVariantRecord(){
		return variantRecord;
	}

}
