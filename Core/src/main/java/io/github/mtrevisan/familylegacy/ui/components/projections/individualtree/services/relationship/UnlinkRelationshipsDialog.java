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
package io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.services.relationship;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;


/**
 * Dialog that displays all relationships of a focus entity, grouped by
 * categories, with checkboxes to select which ones to remove.
 * <p>
 * The dialog is <b>self-contained</b>: it takes only the model and the
 * id of the focus entity, and derives every category directly from the
 * relationship records. The categorization depends on:
 * <ul>
 *   <li>the type of the focus entity — individual or group;</li>
 *   <li>the relationship type — {@code child}-type, {@code partner}-type,
 *       {@code group_member}, {@code part_of}, or generic
 *       {@code associate};</li>
 *   <li>the direction — whether the focus is the subject or the object
 *       of the relationship;</li>
 *   <li>the type of the other entity — individual or group.</li>
 * </ul>
 * Because the categories are computed here, the dialog works identically
 * for individuals and for groups, and callers do not need to pre-compute
 * parent / child / associate / group id sets.
 */
public class UnlinkRelationshipsDialog extends JDialog{

	private static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);


	/**
	 * Represents an aggregated relationship entry toward a specific
	 * target entity. Holds multiple relationship IDs (e.g. direct and
	 * inverse) to delete both at once.
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


	/**
	 * Categories of relationships. The active set depends on the type of
	 * the focus entity:
	 * <ul>
	 *   <li>an <b>individual</b> shows parents, partners, associates,
	 *       groups, and children;</li>
	 *   <li>a <b>group</b> shows members, subgroups, supergroups, and
	 *       associates.</li>
	 * </ul>
	 * The order of the constants is the order in which the categories
	 * are rendered, and it is chosen so that the most relevant group
	 * appears first for each focus type.
	 */
	private enum Category{
		// Individual focus
		PARENT("Parents"),
		PARTNER("Partners"),
		GROUP("Groups"),
		ASSOCIATE("Associates"),
		CHILD("Children"),
		// Group focus
		MEMBER("Members"),
		SUBGROUP("Subgroups"),
		SUPERGROUP("Supergroups");


		private final String label;


		Category(final String label){
			this.label = label;
		}
	}


	private final FLEFModel model;
	private final String entityId;
	private final Predicate<String> relationshipTypeFilter;

	private final List<RelationshipCheckbox> checkboxes = new ArrayList<>();
	private boolean confirmed;


	public UnlinkRelationshipsDialog(final Window parent, final FLEFModel model, final String entityId){
		this(parent, model, type -> true, entityId);
	}

	public UnlinkRelationshipsDialog(final Window parent, final FLEFModel model,
			final Predicate<String> relationshipTypeFilter, final String entityId){
		super(parent, "Unlink Relationships", ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.entityId = entityId;
		this.relationshipTypeFilter = relationshipTypeFilter;

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

		final JPanel contentPanel = new JPanel(new MigLayout("ins 5,gapy 5,fill", "[grow,fill]", "[]"));

		final Map<Category, List<RelationshipInfo>> categorized = categorizeRelationships();

		// Render categories in the order declared by the enum. Empty
		// ones are skipped. The CHILD and MEMBER categories can grow
		// arbitrarily long, so they are made scrollable.
		boolean anyRendered = false;
		for(final Category cat : Category.values()){
			final List<RelationshipInfo> infos = categorized.get(cat);
			if(infos == null || infos.isEmpty())
				continue;

			anyRendered = true;
			final boolean scrollable = (cat == Category.CHILD || cat == Category.MEMBER);
			final JPanel panel = (scrollable
				? createScrollableGroupPanel(cat.label, infos)
				: createGroupPanel(cat.label, infos));
			contentPanel.add(panel, (scrollable? "grow,push,wrap": "wrap"));
		}

		if(!anyRendered)
			contentPanel.add(new JLabel("No relationships to unlink."), "grow,push");

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

	/**
	 * Iterates every relationship involving the focus entity and assigns
	 * it to a category based on the focus entity type, the relationship
	 * type, the direction, and the type of the other entity.
	 * <p>
	 * The method is self-contained: it does not require pre-computed
	 * sets of parent, child, associate, or group ids, so it works
	 * identically for individual and group focus.
	 *
	 * @return a map from category to the list of aggregated
	 *         relationships for that category; every category is
	 *         present, possibly with an empty list
	 */
	private Map<Category, List<RelationshipInfo>> categorizeRelationships(){
		final Map<Category, Map<String, RelationshipInfo>> byCategory = new EnumMap<>(Category.class);
		for(final Category cat : Category.values())
			byCategory.put(cat, new HashMap<>());

		final FLEFRecord focus = model.getRecordById(entityId);
		if(focus == null)
			return flatten(byCategory);

		final boolean focusIsGroup = GroupHandler.isOfType(focus);

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			if(type == null || !relationshipTypeFilter.test(type))
				continue;

			final String subjectId = extractAnyReferencedId(relationship, RelationshipReader.TAG_SUBJECT);
			final String objectId = extractAnyReferencedId(relationship, RelationshipReader.TAG_OBJECT);
			if(subjectId == null || objectId == null)
				continue;

			final boolean isDirect = entityId.equals(subjectId);
			final boolean isInverse = entityId.equals(objectId);
			if(!isDirect && !isInverse)
				continue;

			final String otherId = (isDirect? objectId: subjectId);
			final FLEFRecord other = model.getRecordById(otherId);

			final Category category = classify(type, focusIsGroup, isDirect);
			if(category == null)
				continue;

			final String baseDescription = getDisplayText(other, otherId);
			final RelationshipInfo info = byCategory.get(category)
				.computeIfAbsent(otherId, id -> new RelationshipInfo(id, baseDescription));
			info.addRelationship(relationship.getId(), isDirect);
		}

		return flatten(byCategory);
	}

	/**
	 * Decides the category of a relationship.
	 * <p>
	 * The rules encode the semantics of the relationship types:
	 * <ul>
	 *   <li>{@code group_member(I -> G)}: for an individual focus, the
	 *       other entity is a group the individual belongs to; for a
	 *       group focus, the other entity is a member of the group.</li>
	 *   <li>{@code part_of(A -> B)}: {@code A} is a subgroup of
	 *       {@code B}. For a group focus, the other entity is a
	 *       subgroup when the focus is the parent, a supergroup when
	 *       the focus is the child.</li>
	 *   <li>{@code child-type(C -> P)}: for an individual focus, the
	 *       other entity is a parent when the focus is the child, a
	 *       child when the focus is the parent.</li>
	 *   <li>{@code partner-type}: symmetric, always a partner.</li>
	 *   <li>{@code associate}: symmetric, always an associate.</li>
	 * </ul>
	 *
	 * @param type          the relationship type, not lowercased
	 * @param focusIsGroup  {@code true} when the focus entity is a group
	 * @param focusIsSubject {@code true} when the focus is the subject
	 *                      of the relationship
	 * @return the category, or {@code null} when the relationship should
	 *         be ignored
	 */
	private static Category classify(final String type, final boolean focusIsGroup, final boolean focusIsSubject){
		final String t = type.toLowerCase(Locale.ROOT);

		if(RelationshipReader.isTypeGroupMember(t)){
			// group_member: subject = individual, object = group.
			if(focusIsGroup)
				// Focus is the group; the other is an individual member.
				return (focusIsSubject? null: Category.MEMBER);

			// Focus is the individual; the other is a group.
			return (focusIsSubject? Category.GROUP: null);
		}

		if(RelationshipReader.isTypePartOf(t)){
			// part_of: subject = child group, object = parent group.
			if(!focusIsGroup)
				return null;

			return (focusIsSubject? Category.SUPERGROUP: Category.SUBGROUP);
		}

		if(RelationshipReader.isTypeChild(t)){
			// child-type: subject = child, object = parent.
			if(focusIsGroup)
				return null;

			return (focusIsSubject? Category.PARENT: Category.CHILD);
		}

		if(RelationshipReader.isTypePartner(t)){
			if(focusIsGroup)
				return null;

			return Category.PARTNER;
		}

		if("associate".equals(t))
			return Category.ASSOCIATE;

		return null;
	}

	private static Map<Category, List<RelationshipInfo>> flatten(
		final Map<Category, Map<String, RelationshipInfo>> byCategory){
		final Map<Category, List<RelationshipInfo>> result = new EnumMap<>(Category.class);
		for(final Map.Entry<Category, Map<String, RelationshipInfo>> e : byCategory.entrySet())
			result.put(e.getKey(), new ArrayList<>(e.getValue().values()));
		return result;
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

		if(GroupHandler.isOfType(record))
			return GroupHandler.getInstance()
				.getDisplayText(record, model);

		return IndividualHandler.getInstance()
			.getDisplayText(record, model);
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
		scrollPane.getVerticalScrollBar()
			.setUnitIncrement(unitIncrement);

		SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar()
			.setValue(0));

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
	 * Returns the list of relationship IDs that the user selected, or an
	 * empty list if canceled.
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
