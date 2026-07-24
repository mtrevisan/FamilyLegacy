package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;


/**
 * Panel for BOUNDED date (uncertainty interval).
 * <p>
 * Structure (real tags):
 * BOUNDED
 * +1 NOT_BEFORE
 * (ISO | CENTURY | DECADE)
 * APPROXIMATE (optional)
 * +1 NOT_AFTER
 * (ISO | CENTURY | DECADE)
 * APPROXIMATE (optional)
 * <p>
 */
public class BoundedDatePanel extends JPanel{

	private final SingleDatePanel notBeforePanel;
	private final SingleDatePanel notAfterPanel;

	public BoundedDatePanel(FLEFModel model){
		this.notBeforePanel = new SingleDatePanel(model);
		this.notAfterPanel = new SingleDatePanel(model);
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 0,fillx,top", "[grow,fill][grow,fill]"));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		JPanel beforePanel = new JPanel(new MigLayout("fillx", "[right]rel[grow]"));
		beforePanel.setBorder(new TitledBorder("Not Before"));
		beforePanel.add(notBeforePanel, "growx");
		add(beforePanel, "growx");

		JPanel afterPanel = new JPanel(new MigLayout("fillx", "[right]rel[grow]"));
		afterPanel.setBorder(new TitledBorder("Not After"));
		afterPanel.add(notAfterPanel, "growx");
		add(afterPanel, "growx");
	}

	public void loadFromRecord(FLEFRecord boundedRecord){
		clear();
		if(boundedRecord == null) return;

		FLEFRecord notBefore = FLEFRecordUtils.findChild(boundedRecord, "NOT_BEFORE");
		if(notBefore != null){
			notBeforePanel.loadFromRecord(notBefore);
		}

		FLEFRecord notAfter = FLEFRecordUtils.findChild(boundedRecord, "NOT_AFTER");
		if(notAfter != null){
			notAfterPanel.loadFromRecord(notAfter);
		}
	}

	public FLEFRecord saveToRecord(FLEFRecord target){
		FLEFRecord record = target != null? target: new FLEFRecord();

		if(notBeforePanel.hasData()){
			FLEFRecord notBefore = FLEFRecord.createChild(1, "NOT_BEFORE");
			FLEFRecord dateNode = notBeforePanel.saveToRecord(null);
			if(dateNode != null && dateNode.hasChildren()){
				// copy children (the actual date tags) into notBefore
				for(FLEFRecord child : dateNode.getChildren()){
					child.setLevel(2);
					notBefore.addChild(child);
				}
				record.addChild(notBefore);
			}
		}

		if(notAfterPanel.hasData()){
			FLEFRecord notAfter = FLEFRecord.createChild(1, "NOT_AFTER");
			FLEFRecord dateNode = notAfterPanel.saveToRecord(null);
			if(dateNode != null && dateNode.hasChildren()){
				for(FLEFRecord child : dateNode.getChildren()){
					child.setLevel(2);
					notAfter.addChild(child);
				}
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
		if(!hasData()){
			JOptionPane.showMessageDialog(this,
				"At least one of Not Before or Not After is required for BOUNDED date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if(notBeforePanel.hasData() && !notBeforePanel.validateRequiredFields()){
			return false;
		}
		return !notAfterPanel.hasData() || notAfterPanel.validateRequiredFields();
	}

}
