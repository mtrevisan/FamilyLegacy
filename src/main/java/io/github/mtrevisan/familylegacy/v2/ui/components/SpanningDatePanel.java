package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Dialog;


/**
 * Panel for SPANNING date (duration).
 * <p>
 * Structure (real tags):
 * SPANNING
 * +1 FROM
 * (ISO | CENTURY | DECADE)
 * APPROXIMATE (optional)
 * +1 TO
 * (ISO | CENTURY | DECADE)
 * APPROXIMATE (optional)
 * <p>
 */
public class SpanningDatePanel extends JPanel{

	private final SingleDatePanel fromPanel;
	private final SingleDatePanel toPanel;


	public SpanningDatePanel(final Dialog parent, final FLEFModel model){
		this.fromPanel = new SingleDatePanel(parent, model);
		this.toPanel = new SingleDatePanel(parent, model);

		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 0,fillx,top", "[grow, fill][grow, fill]"));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		final JPanel fromPanelBorder = new JPanel(new MigLayout("fillx", "[right]rel[grow]"));
		fromPanelBorder.setBorder(new TitledBorder("From"));
		fromPanelBorder.add(fromPanel, "growx");
		add(fromPanelBorder, "growx");

		final JPanel toPanelBorder = new JPanel(new MigLayout("fillx", "[right]rel[grow]"));
		toPanelBorder.setBorder(new TitledBorder("To"));
		toPanelBorder.add(toPanel, "growx");
		add(toPanelBorder, "growx");
	}

	public void loadFromRecord(final FLEFRecord spanningRecord){
		clear();

		if(spanningRecord == null)
			return;

		final FLEFRecord from = FLEFRecordUtils.findChild(spanningRecord, "FROM");
		if(from != null)
			fromPanel.loadFromRecord(from);

		final FLEFRecord to = FLEFRecordUtils.findChild(spanningRecord, "TO");
		if(to != null)
			toPanel.loadFromRecord(to);
	}

	public FLEFRecord saveToRecord(final FLEFRecord target){
		final FLEFRecord parent = (target != null? target: FLEFRecord.createEmpty());

		final FLEFRecord record = FLEFRecord.createChild("SPANNING");
		if(fromPanel.hasData()){
			final FLEFRecord from = FLEFRecord.createChild("FROM");
			final FLEFRecord dateNode = fromPanel.saveToRecord(null);
			if(dateNode != null && dateNode.hasChildren()){
				for(final FLEFRecord child : dateNode.getChildren())
					from.addChild(child);
				record.addChild(from);
			}
		}

		if(toPanel.hasData()){
			final FLEFRecord to = FLEFRecord.createChild("TO");
			final FLEFRecord dateNode = toPanel.saveToRecord(null);
			if(dateNode != null && dateNode.hasChildren()){
				for(final FLEFRecord child : dateNode.getChildren())
					to.addChild(child);
				record.addChild(to);
			}
		}

		return (record.hasChildren()? parent.addChild(record): null);
	}

	public void clear(){
		fromPanel.clear();
		toPanel.clear();
	}

	public boolean hasData(){
		return (fromPanel.hasData() || toPanel.hasData());
	}

	public boolean validateRequiredFields(){
		if(!hasData()){
			JOptionPane.showMessageDialog(this,
				"At least one of From or To is required for SPANNING date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		if(fromPanel.hasData() && !fromPanel.validateRequiredFields())
			return false;

		return (!toPanel.hasData() || toPanel.validateRequiredFields());
	}

}
