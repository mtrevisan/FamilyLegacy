package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;


/**
 * Panel for SPANNING date (duration).
 */
public class SpanningDatePanel extends JPanel{
	private final SingleDatePanel fromPanel = new SingleDatePanel();
	private final SingleDatePanel toPanel = new SingleDatePanel();

	SpanningDatePanel(){
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 0, fillx", "[grow]", "[]5[]"));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		JPanel fromPanelBorder = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow]"));
		fromPanelBorder.setBorder(new TitledBorder("From"));
		fromPanelBorder.add(fromPanel, "growx,wrap");
		add(fromPanelBorder, "growx,wrap");

		JPanel toPanelBorder = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow]"));
		toPanelBorder.setBorder(new TitledBorder("To"));
		toPanelBorder.add(toPanel, "growx,wrap");
		add(toPanelBorder, "growx,wrap");
	}

	public void loadFromRecord(FLEFRecord spanningRecord){
		clear();
		if(spanningRecord == null) return;

		FLEFRecord from = FLEFRecordUtils.findChild(spanningRecord, "FROM");
		if(from != null){
			FLEFRecord qualified = FLEFRecordUtils.findChild(from, "QUALIFIED_DATE");
			if(qualified != null){
				fromPanel.loadFromQualifiedDate(qualified);
			}
		}

		FLEFRecord to = FLEFRecordUtils.findChild(spanningRecord, "TO");
		if(to != null){
			FLEFRecord qualified = FLEFRecordUtils.findChild(to, "QUALIFIED_DATE");
			if(qualified != null){
				toPanel.loadFromQualifiedDate(qualified);
			}
		}
	}

	public FLEFRecord saveToRecord(FLEFRecord target){
		FLEFRecord record = target != null? target: new FLEFRecord();

		if(fromPanel.hasData()){
			FLEFRecord from = FLEFRecord.createChild(1, "FROM");
			FLEFRecord qualified = fromPanel.saveToQualifiedDate(null);
			if(qualified != null && !qualified.getChildren().isEmpty()){
				from.addChild(qualified);
				record.addChild(from);
			}
		}

		if(toPanel.hasData()){
			FLEFRecord to = FLEFRecord.createChild(1, "TO");
			FLEFRecord qualified = toPanel.saveToQualifiedDate(null);
			if(qualified != null && !qualified.getChildren().isEmpty()){
				to.addChild(qualified);
				record.addChild(to);
			}
		}

		return record.hasChildren()? record: null;
	}

	public void clear(){
		fromPanel.clear();
		toPanel.clear();
	}

	public boolean hasData(){
		return fromPanel.hasData() || toPanel.hasData();
	}

	public boolean validateRequiredFields(){
		// At least one of FROM or TO is required
		if(!hasData()){
			JOptionPane.showMessageDialog(this,
				"At least one of From or To is required for SPANNING date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if(fromPanel.hasData() && !fromPanel.validateRequiredFields()){
			return false;
		}
		if(toPanel.hasData() && !toPanel.validateRequiredFields()){
			return false;
		}
		return true;
	}
}
