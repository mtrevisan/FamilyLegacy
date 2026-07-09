package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;

import javax.swing.*;
import java.awt.*;

public class RepositoryDialog extends BaseRecordDialog {

	public RepositoryDialog(Frame parent, FLEFModel model, FLEFRecord record) {
		super(parent, model, record, "Edit Repository");
		initComponents();
		loadData();
		setMinimumSize(new Dimension(400, 200));
		pack();
		setLocationRelativeTo(parent);
	}

	public RepositoryDialog(Frame parent, FLEFModel model) {
		super(parent, model, "New Repository");
		initComponents();
		loadData();
		setMinimumSize(new Dimension(400, 200));
		pack();
		setLocationRelativeTo(parent);
	}

	@Override protected void initComponents() {
		setLayout(new BorderLayout());
		JLabel label = new JLabel("Repository dialog not yet implemented", SwingConstants.CENTER);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
		add(label, BorderLayout.CENTER);
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(e -> dispose());
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(closeButton);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	@Override protected void loadData() {}
	@Override protected void saveRecord() { JOptionPane.showMessageDialog(this, "Save not implemented", "Info", JOptionPane.INFORMATION_MESSAGE); dispose(); }
	@Override protected FLEFRecord createNewRecord() { FLEFRecord r = new FLEFRecord(); r.setType("REPOSITORY"); r.setId(generateNewId()); return r; }
	@Override protected String generateNewId() { return FLEFRecordUtils.generateNewId(model, "REPOSITORY", "R"); }
}