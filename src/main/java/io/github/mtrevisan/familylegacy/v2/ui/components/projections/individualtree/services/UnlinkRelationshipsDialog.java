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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;


/**
 * Dialog that displays all relationships of an ego entity, grouped by categories
 * (Parents, Partners, Associates, Groups, Children), with checkboxes to select which ones to remove.
 */
public class UnlinkRelationshipsDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -6987495276841379033L;


	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";

	private static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);


	/**
	 * Represents an aggregated relationship entry toward a specific target entity.
	 * Holds multiple relationship IDs (e.g. direct and inverse) to delete both at once.
	 */
	private static class RelationshipInfo{
		final String entityId;
		final String baseDescription;
		final List<String> relationshipIds = new ArrayList<>();
		boolean isDirect;
		boolean isInverse;

		RelationshipInfo(final String entityId, final String baseDescription){
			this.entityId = entityId;
			this.baseDescription = baseDescription;
		}

		void addRelationship(final String relId, final boolean direct){
			relationshipIds.add(relId);
			if(direct)
				isDirect = true;
			else
				isInverse = true;
		}

		String getFormattedDescription(){
			if(isDirect && isInverse)
				return baseDescription + " [Bidirectional]";
			if(isDirect)
				return baseDescription + " [Direct]";
			if(isInverse)
				return baseDescription + " [Inverse]";
			return baseDescription;
		}
	}

	private record RelationshipCheckbox(JCheckBox checkbox, List<String> relationshipIds, String entityId){}


	private final FLEFModel model;
	private final Predicate<String> relationshipTypeFilter;
	private final String individualId;

	private final Set<String> parentIds;
	private final Set<String> associateIds;
	private final Set<String> groupIds;
	private final Set<String> childIds;

	private final List<RelationshipCheckbox> checkboxes = new ArrayList<>();
	private boolean confirmed;


	public UnlinkRelationshipsDialog(final Window parent, final FLEFModel model, final String individualId,
			final Set<String> parentsIds, final Set<String> associatesIds, final Set<String> groupsIds,
			final Set<String> childrenIds){
		this(parent, model, type -> true, individualId, parentsIds, associatesIds, groupsIds, childrenIds);
	}

	public UnlinkRelationshipsDialog(final Window parent, final FLEFModel model,
			final Predicate<String> relationshipTypeFilter, final String individualId, final Set<String> parentsIds,
			final Set<String> associatesIds, final Set<String> groupsIds, final Set<String> childrenIds){
		super(parent, "Unlink Relationships", ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.relationshipTypeFilter = relationshipTypeFilter;
		this.individualId = individualId;

		this.parentIds = parentsIds;
		this.associateIds = associatesIds;
		this.groupIds = groupsIds;
		this.childIds = childrenIds;

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

		final JPanel contentPanel = new JPanel(new MigLayout(
			"ins 5,gapy 5,fill",
			"[grow,fill]",
			"[]"));

		final List<RelationshipInfo> parentList = new ArrayList<>();
		final List<RelationshipInfo> associateList = new ArrayList<>();
		final List<RelationshipInfo> groupList = new ArrayList<>();
		final List<RelationshipInfo> childList = new ArrayList<>();
		extractRelationships(parentList, associateList, groupList, childList);

		if(!parentList.isEmpty())
			contentPanel.add(createGroupPanel("Parents", parentList), "wrap");
		if(!groupList.isEmpty())
			contentPanel.add(createGroupPanel("Groups", groupList), "wrap");
		if(!associateList.isEmpty())
			contentPanel.add(createGroupPanel("Associates", associateList), "wrap");
		if(!childList.isEmpty())
			contentPanel.add(createScrollableGroupPanel("Children", childList), "grow,push");

		final JScrollPane mainScroll = new JScrollPane(contentPanel);
		mainScroll.setBorder(BorderFactory.createEmptyBorder());
		add(mainScroll, BorderLayout.CENTER);

		final JPanel buttonPanel = GUIHelper.createButtonPanel(this,
			"Confirm", () -> {
				confirmed = true;

				dispose();
			},
			"Cancel", this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private void extractRelationships(final List<RelationshipInfo> parentList,
			final List<RelationshipInfo> associateList, final List<RelationshipInfo> groupList,
			final List<RelationshipInfo> childList){
		final Map<String, RelationshipInfo> parentMap = new HashMap<>();
		final Map<String, RelationshipInfo> associateMap = new HashMap<>();
		final Map<String, RelationshipInfo> groupMap = new HashMap<>();
		final Map<String, RelationshipInfo> childMap = new HashMap<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(type == null || !relationshipTypeFilter.test(type))
				continue;

			final String subjectId = extractAnyReferencedId(relationship, TAG_SUBJECT);
			final String targetId = extractAnyReferencedId(relationship, TAG_TARGET);
			if(subjectId == null || targetId == null)
				continue;

			final boolean isDirect = individualId.equals(subjectId);
			final boolean isInverse = individualId.equals(targetId);
			if(!isDirect && !isInverse)
				continue;

			final String otherId = isDirect? targetId: subjectId;
			final FLEFRecord other = model.getRecordById(otherId);
			final String baseDescription = getDisplayText(other, otherId);
			final String relId = relationship.getId();

			Map<String, RelationshipInfo> targetCategoryMap = null;
			if(parentIds.contains(otherId))
				targetCategoryMap = parentMap;
			else if(groupIds.contains(otherId))
				targetCategoryMap = groupMap;
			else if(associateIds.contains(otherId))
				targetCategoryMap = associateMap;
			else if(childIds.contains(otherId))
				targetCategoryMap = childMap;

			if(targetCategoryMap != null){
				final RelationshipInfo info = targetCategoryMap.computeIfAbsent(
					otherId, id -> new RelationshipInfo(id, baseDescription)
				);
				info.addRelationship(relId, isDirect);
			}
		}

		parentList.addAll(parentMap.values());
		associateList.addAll(associateMap.values());
		groupList.addAll(groupMap.values());
		childList.addAll(childMap.values());
	}

	private String extractAnyReferencedId(final FLEFRecord relationship, final String tag){
		String refId = relationship.extractReferencedId(tag, IndividualHandler.TYPE);
		if(refId == null)
			refId = relationship.extractReferencedId(tag, GroupHandler.TYPE);
		return refId;
	}

	private String getDisplayText(final FLEFRecord record, final String fallbackId){
		if(record == null)
			return fallbackId;

		if(GroupHandler.TYPE.equalsIgnoreCase(record.getTag()))
			return GroupHandler.getInstance().getDisplayText(record, model);

		return IndividualHandler.getInstance().getDisplayText(record, model);
	}

	private JPanel createGroupPanel(final String title, final List<RelationshipInfo> infos){
		final JPanel outer = new JPanel(new MigLayout("ins 5,wrap 1,fillx,gapy 2", "[grow,fill]", "[]"));
		outer.setBorder(BorderFactory.createTitledBorder(title));

		for(final RelationshipInfo info : infos){
			final JPanel row = createWrappedCheckbox(info.relationshipIds, info.entityId, info.getFormattedDescription());
			outer.add(row, "growx");
		}
		return outer;
	}

	private JPanel createScrollableGroupPanel(final String title, final List<RelationshipInfo> infos){
		final JPanel outer = new JPanel(new BorderLayout());
		outer.setBorder(BorderFactory.createTitledBorder(title));

		final JPanel inner = new JPanel(new MigLayout("ins 5,wrap 1,fillx,top,gapy 2", "[grow,fill]", "[]"));
		for(final RelationshipInfo info : infos){
			final JPanel row = createWrappedCheckbox(info.relationshipIds, info.entityId, info.getFormattedDescription());
			inner.add(row, "growx");
		}

		final JScrollPane scrollPane = new JScrollPane(inner);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());

		final Font font = UIManager.getFont("CheckBox.font");
		final int unitIncrement = (font != null? outer.getFontMetrics(font).getHeight(): 16);
		scrollPane.getVerticalScrollBar().setUnitIncrement(unitIncrement);

		SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));

		outer.add(scrollPane, BorderLayout.CENTER);
		return outer;
	}

	private JPanel createWrappedCheckbox(final List<String> relationshipIds, final String entityId,
			final String description){
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

		checkboxes.add(new RelationshipCheckbox(cb, relationshipIds, entityId));
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
				ids.addAll(rc.relationshipIds);
		return ids;
	}

}
