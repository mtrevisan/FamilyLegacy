package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
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
		// 2 colonne con larghezza equivalente distribuita (50% / 50%)
		setLayout(new MigLayout("ins 0, fillx, top", "[grow, fill][grow, fill]", "[]"));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		final JPanel fromPanelBorder = new JPanel(new MigLayout("ins 5, fillx", "[right]rel[grow]"));
		fromPanelBorder.setBorder(new TitledBorder("From"));
		fromPanelBorder.add(fromPanel, "growx");
		add(fromPanelBorder, "growx"); // Niente wrap: il prossimo pannello va a destra

		final JPanel toPanelBorder = new JPanel(new MigLayout("ins 5, fillx", "[right]rel[grow]"));
		toPanelBorder.setBorder(new TitledBorder("To"));
		toPanelBorder.add(toPanel, "growx");
		add(toPanelBorder, "growx");
	}

	public void loadFromRecord(final FLEFRecord spanningRecord){
		clear();
		if(spanningRecord == null){
			return;
		}

		final FLEFRecord from = FLEFRecordUtils.findChild(spanningRecord, "FROM");
		if(from != null){
			final FLEFRecord qualified = FLEFRecordUtils.findChild(from, "QUALIFIED_DATE");
			if(qualified != null){
				fromPanel.loadFromQualifiedDate(qualified);
			}
		}

		final FLEFRecord to = FLEFRecordUtils.findChild(spanningRecord, "TO");
		if(to != null){
			final FLEFRecord qualified = FLEFRecordUtils.findChild(to, "QUALIFIED_DATE");
			if(qualified != null){
				toPanel.loadFromQualifiedDate(qualified);
			}
		}
	}

	public FLEFRecord saveToRecord(final FLEFRecord target){
		final FLEFRecord record = target != null ? target : new FLEFRecord();

		if(fromPanel.hasData()){
			final FLEFRecord from = FLEFRecord.createChild(1, "FROM");
			final FLEFRecord qualified = fromPanel.saveToQualifiedDate(null);
			if(qualified != null && !qualified.getChildren().isEmpty()){
				from.addChild(qualified);
				record.addChild(from);
			}
		}

		if(toPanel.hasData()){
			final FLEFRecord to = FLEFRecord.createChild(1, "TO");
			final FLEFRecord qualified = toPanel.saveToQualifiedDate(null);
			if(qualified != null && !qualified.getChildren().isEmpty()){
				to.addChild(qualified);
				record.addChild(to);
			}
		}

		return record.hasChildren() ? record : null;
	}

	public void clear(){
		fromPanel.clear();
		toPanel.clear();
	}

	public boolean hasData(){
		return fromPanel.hasData() || toPanel.hasData();
	}

	public boolean validateRequiredFields(){
		if(!hasData()){
			JOptionPane.showMessageDialog(this,
				"At least one of From or To is required for SPANNING date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if(fromPanel.hasData() && !fromPanel.validateRequiredFields()){
			return false;
		}
		return !toPanel.hasData() || toPanel.validateRequiredFields();
	}

}
