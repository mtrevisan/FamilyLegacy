package io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Dialog that displays all relationships of an individual, grouped by type,
 * with checkboxes to select which ones to remove.
 */
public class UnlinkRelationshipsDialog extends JDialog{

	private final FLEFModel model;
	private final String individualId;
	private final List<RelationshipCheckbox> checkboxes = new ArrayList<>();
	private boolean confirmed = false;


	public UnlinkRelationshipsDialog(final Window owner, final FLEFModel model, final String individualId){
		super(owner, "Unlink Relationships", ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.individualId = individualId;

		initUI();

		pack();

		setLocationRelativeTo(owner);
	}


	private void initUI(){
		setLayout(new BorderLayout());

		final JPanel mainPanel = new JPanel(new MigLayout("wrap 1, ins 10", "[grow,fill]", "[]"));

		// Gather relationships and group them
		final List<RelationshipInfo> parents = new ArrayList<>();
		final List<RelationshipInfo> partners = new ArrayList<>();
		final List<RelationshipInfo> children = new ArrayList<>();

		final List<FLEFRecord> allRelations = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord rel : allRelations){
			final String type = FLEFRecordHelper.getChildValue(rel, "type");
			if(type == null) continue;

			final String subjectId = rel.extractReferencedId("subject", IndividualHandler.TYPE);
			final String targetId = rel.extractReferencedId("target", IndividualHandler.TYPE);

			final boolean involves = individualId.equals(subjectId) || individualId.equals(targetId);
			if(!involves) continue;

			final String otherId = individualId.equals(subjectId)? targetId: subjectId;
			final FLEFRecord other = (otherId != null)? model.getRecordById(otherId): null;
			final String otherName = (other != null)
				? IndividualHandler.getInstance().getDisplayText(other, model)
				: otherId;

			final boolean isChildRel = type.endsWith("child");
			final boolean isPartnerRel = type.endsWith("partner");
			if(isChildRel){
				if(individualId.equals(subjectId))
					// individual is the child → other is a parent
					parents.add(new RelationshipInfo(rel.getId(), otherName));
				else
					// individual is the parent → other is a child
					children.add(new RelationshipInfo(rel.getId(), otherName));
			}
			else if(isPartnerRel)
				partners.add(new RelationshipInfo(rel.getId(), otherName));
		}

		// Build UI groups
		addGroup(mainPanel, "Parents", parents);
		addGroup(mainPanel, "Partner", partners);
		addGroup(mainPanel, "Children", children);

		final JScrollPane scrollPane = new JScrollPane(mainPanel);
		scrollPane.setPreferredSize(new Dimension(400, 300));
		add(scrollPane, BorderLayout.CENTER);

		// Buttons
		final JPanel buttonPanel = new JPanel();
		final JButton okButton = new JButton("OK");
		final JButton cancelButton = new JButton("Cancel");
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		okButton.addActionListener(e -> {
			final boolean anySelected = checkboxes.stream().anyMatch(RelationshipCheckbox::isSelected);
			if(!anySelected){
				JOptionPane.showMessageDialog(
					UnlinkRelationshipsDialog.this,
					"No relationships selected.",
					"Selection Empty",
					JOptionPane.WARNING_MESSAGE);
				return;
			}
			confirmed = true;
			dispose();
		});

		cancelButton.addActionListener(e -> dispose());
	}

	private void addGroup(final JPanel parent, final String title, final List<RelationshipInfo> infos){
		if(infos.isEmpty()) return;

		final JPanel group = new JPanel(new MigLayout("wrap 1, ins 0", "[grow,fill]", "[]"));
		group.setBorder(BorderFactory.createTitledBorder(title));

		for(final RelationshipInfo info : infos){
			final JCheckBox cb = new JCheckBox(info.description);
			group.add(cb);
			checkboxes.add(new RelationshipCheckbox(cb, info.id));
		}

		parent.add(group);
	}

	/**
	 * Returns the list of relationship IDs that the user selected, or an empty list if the dialog was cancelled.
	 */
	public List<String> getSelectedRelationshipIds(){
		if(!confirmed) return Collections.emptyList();

		final List<String> ids = new ArrayList<>();
		for(final RelationshipCheckbox rc : checkboxes){
			if(rc.checkbox.isSelected()){
				ids.add(rc.id);
			}
		}
		return ids;
	}

	// ---- Helper records ----
	private static final class RelationshipInfo{
		final String id;
		final String description;

		RelationshipInfo(final String id, final String description){
			this.id = id;
			this.description = description;
		}
	}

	private static final class RelationshipCheckbox{
		final JCheckBox checkbox;
		final String id;

		RelationshipCheckbox(final JCheckBox checkbox, final String id){
			this.checkbox = checkbox;
			this.id = id;
		}

		boolean isSelected(){
			return checkbox.isSelected();
		}
	}

}
