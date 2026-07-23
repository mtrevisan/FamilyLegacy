package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;


/**
 * Panel for BOUNDED date (uncertainty interval).
 */
public class BoundedDatePanel extends JPanel{
	private final SingleDatePanel notBeforePanel = new SingleDatePanel();
	private final SingleDatePanel notAfterPanel = new SingleDatePanel();

	BoundedDatePanel(){
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 0, fillx", "[grow]", "[]5[]"));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		JPanel beforePanel = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow]"));
		beforePanel.setBorder(new TitledBorder("Not Before"));
		beforePanel.add(notBeforePanel, "growx,wrap");
		add(beforePanel, "growx,wrap");

		JPanel afterPanel = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow]"));
		afterPanel.setBorder(new TitledBorder("Not After"));
		afterPanel.add(notAfterPanel, "growx,wrap");
		add(afterPanel, "growx,wrap");
	}

	public void loadFromRecord(FLEFRecord boundedRecord){
		clear();
		if(boundedRecord == null) return;

		FLEFRecord notBefore = FLEFRecordUtils.findChild(boundedRecord, "NOT_BEFORE");
		if(notBefore != null){
			FLEFRecord qualified = FLEFRecordUtils.findChild(notBefore, "QUALIFIED_DATE");
			if(qualified != null){
				notBeforePanel.loadFromQualifiedDate(qualified);
			}
		}

		FLEFRecord notAfter = FLEFRecordUtils.findChild(boundedRecord, "NOT_AFTER");
		if(notAfter != null){
			FLEFRecord qualified = FLEFRecordUtils.findChild(notAfter, "QUALIFIED_DATE");
			if(qualified != null){
				notAfterPanel.loadFromQualifiedDate(qualified);
			}
		}
	}

	public FLEFRecord saveToRecord(FLEFRecord target){
		FLEFRecord record = target != null? target: new FLEFRecord();

		if(notBeforePanel.hasData()){
			FLEFRecord notBefore = FLEFRecord.createChild(1, "NOT_BEFORE");
			FLEFRecord qualified = notBeforePanel.saveToQualifiedDate(null);
			if(qualified != null && !qualified.getChildren().isEmpty()){
				notBefore.addChild(qualified);
				record.addChild(notBefore);
			}
		}

		if(notAfterPanel.hasData()){
			FLEFRecord notAfter = FLEFRecord.createChild(1, "NOT_AFTER");
			FLEFRecord qualified = notAfterPanel.saveToQualifiedDate(null);
			if(qualified != null && !qualified.getChildren().isEmpty()){
				notAfter.addChild(qualified);
				record.addChild(notAfter);
			}
		}

		return record.hasChildren()? record: null;
	}

	public void clear(){
		notBeforePanel.clear();
		notAfterPanel.clear();
	}

	public boolean hasData(){
		return notBeforePanel.hasData() || notAfterPanel.hasData();
	}

	public boolean validateRequiredFields(){
		// At least one of NOT_BEFORE or NOT_AFTER is required
		if(!hasData()){
			JOptionPane.showMessageDialog(this,
				"At least one of Not Before or Not After is required for BOUNDED date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if(notBeforePanel.hasData() && !notBeforePanel.validateRequiredFields()){
			return false;
		}
		if(notAfterPanel.hasData() && !notAfterPanel.validateRequiredFields()){
			return false;
		}
		return true;
	}
}
