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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;


/**
 * Dialog that displays all relationships of an individual, grouped by type,
 * with checkboxes to select which ones to remove.
 * The layout is fully resizable: parents and partner sections hold a fixed height,
 * while the children section takes all remaining space inside a scrollable viewport.
 * Each relationship row shows a checkbox and the display name of the related entity.
 * Double-clicking the display name opens the edit dialog for that entity.
 */
public class UnlinkRelationshipsDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -6987495276841379033L;


	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";

	private static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);


	/**
	 * Stores information about a relationship: the relationship record ID,
	 * the ID of the referenced entity, and a human-readable description.
	 */
	private record RelationshipInfo(String relationshipId, String entityId, String description){}

	private record RelationshipCheckbox(JCheckBox checkbox, String relationshipId, String entityId){}


	private final FLEFModel model;
	private final Predicate<String> relationshipTypeFilter;
	private final String individualId;

	private final List<RelationshipCheckbox> checkboxes = new ArrayList<>();
	private boolean confirmed;


	public static UnlinkRelationshipsDialog create(final Window parent, final FLEFModel model,
			final String individualId){
		return new UnlinkRelationshipsDialog(parent, model, type -> true, individualId);
	}

	public static UnlinkRelationshipsDialog create(final Window parent, final FLEFModel model,
			final Predicate<String> relationshipTypeFilter, final String individualId){
		return new UnlinkRelationshipsDialog(parent, model, relationshipTypeFilter, individualId);
	}


	private UnlinkRelationshipsDialog(final Window parent, final FLEFModel model,
			final Predicate<String> relationshipTypeFilter, final String individualId){
		super(parent, "Unlink Relationships", ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.relationshipTypeFilter = relationshipTypeFilter;
		this.individualId = individualId;

		initComponents();

		pack();
		setMinimumSize(new Dimension(380, 150));

		final int maxHeight = (int)(getGraphicsConfiguration().getBounds().getHeight() * 0.45);
		if(getHeight() > maxHeight)
			setSize(getWidth(), maxHeight);

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		setLayout(new BorderLayout());

		// Main container:
		// Rows: Parents (fixed), Partner (fixed), Children (fills remaining vertical space)
		final JPanel contentPanel = new JPanel(new MigLayout(
			"ins 5,gapy 5,fill",
			"[grow,fill]",
			"[grow 0,fill][grow 0,fill][grow 100,fill]"));

		final List<RelationshipInfo> parents = new ArrayList<>();
		final List<RelationshipInfo> children = new ArrayList<>();
		extractRelationships(parents, children);

		// --- Parents section (fixed height) ---
		if(!parents.isEmpty()){
			final JPanel parentGroup = createGroupPanel("Parents", parents);
			contentPanel.add(parentGroup, "wrap");
		}

		// --- Children section (takes all remaining available vertical space) ---
		if(!children.isEmpty()){
			final JPanel childrenGroup = createScrollableGroupPanel("Children", children);
			contentPanel.add(childrenGroup, "grow,push");
		}

		add(contentPanel, BorderLayout.CENTER);

		// Button panel (south)
		final JPanel buttonPanel = GUIHelper.createButtonPanel(this,
			"Confirm", () -> {
				confirmed = true;

				dispose();
			},
			"Cancel", this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	/**
	 * Extracts relationships from the model and groups them into parents, partners, and children.
	 */
	private void extractRelationships(final List<RelationshipInfo> parents, final List<RelationshipInfo> children){
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(type == null)
				continue;

			final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
			final String targetId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
			final boolean involves = (subjectId != null && targetId != null
				&& (individualId.equals(subjectId) || individualId.equals(targetId)));
			if(!involves)
				continue;

			final String otherId = (individualId.equals(subjectId)? targetId: subjectId);
			final FLEFRecord other = model.getRecordById(otherId);
			final String otherName = (other != null
				? IndividualHandler.getInstance().getDisplayText(other, model)
				: otherId);
			final String relationshipId = relationship.getId();

			if(relationshipTypeFilter.test(type)){
				if(individualId.equals(subjectId))
					// individual is the child -> other is a parent
					parents.add(new RelationshipInfo(relationshipId, otherId, otherName));
				else
					// individual is the parent -> other is a child
					children.add(new RelationshipInfo(relationshipId, otherId, otherName));
			}
			// Ignore other relationship types
		}
	}

	/**
	 * Creates a non-scrollable panel for small sets (Parents / Partner).
	 */
	private JPanel createGroupPanel(final String title, final List<RelationshipInfo> infos){
		final JPanel outer = new JPanel(new MigLayout("ins 5,wrap 1,fillx,gapy 2", "[grow,fill]", "[]"));
		outer.setBorder(BorderFactory.createTitledBorder(title));

		for(final RelationshipInfo info : infos){
			final JPanel row = createWrappedCheckbox(info.relationshipId, info.entityId, info.description);
			outer.add(row, "growx");
		}
		return outer;
	}

	/**
	 * Creates a scrollable group panel designed to occupy remaining space gracefully.
	 */
	private JPanel createScrollableGroupPanel(final String title, final List<RelationshipInfo> infos){
		final JPanel outer = new JPanel(new BorderLayout());
		outer.setBorder(BorderFactory.createTitledBorder(title));

		final JPanel inner = new JPanel(new MigLayout("ins 5,wrap 1,fillx,top,gapy 2", "[grow,fill]", "[]"));
		for(final RelationshipInfo info : infos){
			final JPanel row = createWrappedCheckbox(info.relationshipId, info.entityId, info.description);
			inner.add(row, "growx");
		}

		final JScrollPane scrollPane = new JScrollPane(inner);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());

		final Font font = UIManager.getFont("CheckBox.font");
		final int unitIncrement = (font != null
			? outer.getFontMetrics(font).getHeight()
			: 16);
		scrollPane.getVerticalScrollBar()
			.setUnitIncrement(unitIncrement);

		SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));

		outer.add(scrollPane, BorderLayout.CENTER);
		return outer;
	}

	/**
	 * Creates a row consisting of a JCheckBox (for selection) and a JTextArea (for display).
	 * The JTextArea shows the entity description and supports double-click to open the edit dialog.
	 */
	private JPanel createWrappedCheckbox(final String relationshipId, final String entityId, final String description){
		final JPanel panel = new JPanel(new MigLayout("ins 2 0 2 0", "[]0[grow,fill,shrink]", "[]"));

		final JCheckBox cb = new JCheckBox();

		final JTextArea textArea = createTextArea(entityId, description, cb);

		// Double-click on the text area opens the edit dialog for the entity
		textArea.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				if(SwingUtilities.isLeftMouseButton(e)){
					if(e.getClickCount() == 1)
						// Toggle the checkbox on single click
						cb.setSelected(!cb.isSelected());
					else if(e.getClickCount() == 2 && entityId != null)
						editEntity(entityId);
				}
			}
		});

		panel.add(cb);
		panel.add(textArea);

		checkboxes.add(new RelationshipCheckbox(cb, relationshipId, entityId));
		return panel;
	}

	private static JTextArea createTextArea(final String entityId, final String description, final JCheckBox cb){
		final JTextArea textArea = new JTextArea(description);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setOpaque(false);
		textArea.setEditable(false);
		textArea.setFont(cb.getFont());
		textArea.setBorder(null);
		textArea.setFocusable(false);

		textArea.setCaretPosition(0);

		// ---- Indicate that the text is clickable ----
		textArea.setCursor(HAND_CURSOR);

		// Provide a tooltip to explain the interaction
		if(entityId != null)
			textArea.setToolTipText("Double-click to view");

		return textArea;
	}

	/**
	 * Opens the appropriate edit dialog for the entity with the given ID.
	 */
	private void editEntity(final String entityId){
		final FLEFRecord entity = model.getRecordById(entityId);
		if(entity == null)
			return;

		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(entity.getTag());
		if(handler == null)
			return;

		final BaseRecordDialog dialog = handler.createEditDialog(this, model, entity);
		dialog.setVisible(true);
	}

	/**
	 * Returns the list of relationship IDs that the user selected, or an empty list if cancelled.
	 */
	public List<String> getSelectedRelationshipIds(){
		if(!confirmed)
			return Collections.emptyList();

		final List<String> ids = new ArrayList<>();
		for(final RelationshipCheckbox rc : checkboxes)
			if(rc.checkbox.isSelected())
				ids.add(rc.relationshipId);
		return ids;
	}

}
