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
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.groups.GroupHelper;
import io.github.mtrevisan.familylegacy.ui.tools.individuals.IndividualHelper;
import io.github.mtrevisan.familylegacy.ui.tools.places.PlaceHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
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
import java.util.TreeMap;
import java.util.function.Predicate;


/**
 * Dialog that displays all dependent records and relationships of a focus entity,
 * organized into categorized sub-panels within dedicated tabs, allowing the user
 * to select which ones to unlink or remove before deleting the record.
 */
public class UnlinkDialog extends JDialog{

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
				return baseDescription + " [" + I18N.t("dialog.unlink.relationship.bidirectional") + "]";
			if(isDirect)
				return baseDescription + " [" + I18N.t("dialog.unlink.relationship.direct") + "]";
			if(isInverse)
				return baseDescription + " [" + I18N.t("dialog.unlink.relationship.inverse") + "]";
			return baseDescription;
		}
	}

	private record RelationshipCheckbox(JCheckBox checkbox, List<String> recordIds, String entityId){}

	private static class ArrowIcon implements Icon{
		private enum State{EMPTY, PARTIAL, FULL}

		private State state = State.EMPTY;

		void setState(final State state){
			this.state = state;
		}

		@Override
		public void paintIcon(final java.awt.Component c, final java.awt.Graphics g, final int x, final int y){
			final java.awt.Graphics2D g2d = (java.awt.Graphics2D)g.create();
			g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

			// Triangle coordinates (pointing right or down depending on your preference, here pointing right '►' or down '▼')
			// Let's draw a right arrow or down arrow. Assuming standard triangle:
			final int[] xPoints = {x + 2, x + 2, x + 10};
			final int[] yPoints = {y + 2, y + 10, y + 6};

			if(state == State.FULL){
				g2d.setColor(java.awt.Color.BLACK);
				g2d.fillPolygon(xPoints, yPoints, 3);
			}
			else if(state == State.PARTIAL){
				g2d.setColor(java.awt.Color.GRAY);
				g2d.fillPolygon(xPoints, yPoints, 3);
			}
			else{
				g2d.setColor(java.awt.Color.BLACK);
				g2d.drawPolygon(xPoints, yPoints, 3);
			}
			g2d.dispose();
		}

		@Override
		public int getIconWidth(){
			return 12;
		}

		@Override
		public int getIconHeight(){
			return 12;
		}

	}


	/**
	 * Categories of relationships. The active set depends on the type of
	 * the focus entity.
	 */
	private enum Category{
		// Individual focus
		PARENT(I18N.t("dialog.unlink.relationship.parents")),
		PARTNER(I18N.t("dialog.unlink.relationship.partners")),
		GROUP(I18N.t("dialog.unlink.relationship.groups")),
		ASSOCIATE(I18N.t("dialog.unlink.relationship.associates")),
		CHILD(I18N.t("dialog.unlink.relationship.children")),
		// Group focus
		MEMBER(I18N.t("dialog.unlink.relationship.members")),
		SUBGROUP(I18N.t("dialog.unlink.relationship.subgroups")),
		SUPERGROUP(I18N.t("dialog.unlink.relationship.supergroups"));


		private final String label;


		Category(final String label){
			this.label = label;
		}
	}


	private final FLEFModel model;
	private final String entityId;
	private final Predicate<String> relationshipTypeFilter;

	private final List<RelationshipCheckbox> checkboxes = new ArrayList<>();
	private int totalLinks;
	private final List<String> linkIdsToRemove = new ArrayList<>();


	public UnlinkDialog(final Window parent, final FLEFModel model, final String entityId){
		this(parent, model, type -> true, entityId);
	}

	public UnlinkDialog(final Window parent, final FLEFModel model,
			final Predicate<String> relationshipTypeFilter, final String entityId){
		super(parent, I18N.t("dialog.unlink.title"), ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.entityId = entityId;
		this.relationshipTypeFilter = relationshipTypeFilter;

		initComponents();

		pack();
		setMinimumSize(new Dimension(400, 250));
		final int maxHeight = (int)(getGraphicsConfiguration().getBounds().getHeight() * 0.45);
		if(getHeight() > maxHeight)
			setSize(getWidth(), maxHeight);

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		setLayout(new BorderLayout());

		// Retrieve all related records grouped by handler class using the appropriate helper
		final Map<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> allRelatedIds = extractRelatedIds();

		final JTabbedPane tabbedPane = new JTabbedPane();
		boolean anyRendered = false;

		// Sort entries: RelationshipHandler always first, then alphabetically by tab label
		final List<Map.Entry<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>>> sortedEntries =
			new ArrayList<>(allRelatedIds.entrySet());
		sortedEntries.sort((e1, e2) -> {
			if(e1.getKey().equals(RelationshipHandler.class))
				return -1;
			if(e2.getKey().equals(RelationshipHandler.class))
				return 1;

			final RecordTypeHandler<?> h1 = HandlerRegistry.getHandler(e1.getKey());
			final RecordTypeHandler<?> h2 = HandlerRegistry.getHandler(e2.getKey());
			return h1.getType().compareToIgnoreCase(h2.getType());
		});

		for(final Map.Entry<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> entry : sortedEntries){
			final Class<? extends RecordTypeHandler<?>> handlerClass = entry.getKey();
			final List<FLEFRecord> records = entry.getValue();
			if(records == null || records.isEmpty())
				continue;

			totalLinks += records.size();

			anyRendered = true;
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerClass);
			final String tabTitle = handler.getLabel();

			final JPanel tabContentPanel;
			if(handlerClass.equals(RelationshipHandler.class))
				tabContentPanel = createCategorizedRelationshipsPanel(records);
			else
				tabContentPanel = createCategorizedGenericRecordsPanel(records);

			tabbedPane.addTab(tabTitle, tabContentPanel);
		}

		if(!anyRendered){
			final JPanel emptyPanel = new JPanel(new MigLayout("ins 10, align center", "[grow]", "[grow]"));
			emptyPanel.add(new JLabel(I18N.t("dialog.unlink.no_links")), "grow");
			add(emptyPanel, BorderLayout.CENTER);
		}
		else
			add(tabbedPane, BorderLayout.CENTER);

		final JPanel buttonPanel = new JPanel(new MigLayout("ins 5 10 5 10, fillx", "[left][grow,right]", "[]"));
		final javax.swing.JButton selectAllButton = new javax.swing.JButton(I18N.t("button.select_all", "Select all"));
		selectAllButton.addActionListener(e -> {
			final boolean select = checkboxes.stream().anyMatch(rc -> !rc.checkbox.isSelected());
			for(final RelationshipCheckbox rc : checkboxes)
				rc.checkbox.setSelected(select);

			selectAllButton.setText(select
				? I18N.t("button.deselect_all", "Deselect all")
				: I18N.t("button.select_all", "Select all"));
		});
		buttonPanel.add(selectAllButton);

		final JPanel confirmCancelPanel = GUIHelper.createButtonPanel(this,
			I18N.t("button.confirm"), () -> {
				for(final RelationshipCheckbox rc : checkboxes)
					if(rc.checkbox.isSelected())
						linkIdsToRemove.addAll(rc.recordIds);

				dispose();
			},
			I18N.t("button.cancel"), this::dispose);
		buttonPanel.add(confirmCancelPanel, "alignx right");

		add(buttonPanel, BorderLayout.SOUTH);
	}

	/**
	 * Extracts dependent records using the correct helper based on the entity type.
	 */
	private Map<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> extractRelatedIds(){
		final FLEFRecord record = model.getRecordById(entityId);

		if(IndividualHandler.isOfType(record))
			return IndividualHelper.relationshipIds(entityId, model);
		if(GroupHandler.isOfType(record))
			return GroupHelper.relationshipIds(entityId, model);
		if(PlaceHandler.isOfType(record))
			return PlaceHelper.relationshipIds(entityId, model);
		// TODO
//		if(SourceHandler.isOfType(record))
//			return SourceHelper.relationshipIds(entityId, model);
//		if(EventHandler.isOfType(record))
//			return EventHelper.relationshipIds(entityId, model);
//		if(ResearchQuestionHandler.isOfType(record))
//			return ResearchQuestionHelper.relationshipIds(entityId, model);
//		if(ResearchActivityHandler.isOfType(record))
//			return ResearchActivityHelper.relationshipIds(entityId, model);
//		if(ResearchTaskHandler.isOfType(record))
//			return ResearchTaskHelper.relationshipIds(entityId, model);

		return Collections.emptyMap();
	}

	/**
	 * Categorizes and builds the panel for relationship records with rich descriptions and directions.
	 */
	private JPanel createCategorizedRelationshipsPanel(final List<FLEFRecord> relationships){
		final Map<Category, Map<String, RelationshipInfo>> byCategory = new EnumMap<>(Category.class);
		for(final Category cat : Category.values())
			byCategory.put(cat, new HashMap<>());

		final FLEFRecord record = model.getRecordById(entityId);
		final boolean focusIsGroup = GroupHandler.isOfType(record);

		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			if(type == null || !relationshipTypeFilter.test(type))
				continue;

			final String subjectId = extractAnyReferencedId(relationship, RelationshipReader.TAG_SUBJECT);
			final String objectId = extractAnyReferencedId(relationship, RelationshipReader.TAG_TARGET);
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

		final JPanel contentPanel = new JPanel(new MigLayout("ins 5,wrap 1,fillx,gapy 5", "[grow,fill]", "[]"));
		for(final Category cat : Category.values()){
			final Map<String, RelationshipInfo> map = byCategory.get(cat);
			if(map == null || map.isEmpty())
				continue;

			final List<RelationshipInfo> infos = new ArrayList<>(map.values());
			final boolean scrollable = (cat == Category.CHILD || cat == Category.MEMBER);
			final JPanel groupPanel = (scrollable
				? createScrollableGroupPanel(cat.label, infos)
				: createGroupPanel(cat.label, infos));
			contentPanel.add(groupPanel, "growx");
		}

		return wrapInScrollPane(contentPanel);
	}

	/**
	 * Categorizes and builds the panel for generic dependent records, grouping them alphabetically by their specific sub-type or tag,
	 * with each group rendered as a separate independent accordion starting collapsed and compacting cleanly at the top.
	 */
	private JPanel createCategorizedGenericRecordsPanel(final List<FLEFRecord> records){
		final Map<String, List<FLEFRecord>> groups = new TreeMap<>();
		for(final FLEFRecord record : records){
			final String groupKey = extractSubGroupKey(record);
			groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(record);
		}

		final JPanel contentPanel = new JPanel(new MigLayout("ins 5,wrap 1,fillx,top,gapy 3", "[grow,fill]", "[]"));

		for(final Map.Entry<String, List<FLEFRecord>> entry : groups.entrySet()){
			final String subCategoryTitle = I18N.t("enum.event.type." + entry.getKey(), entry.getKey());
			final List<FLEFRecord> groupRecords = entry.getValue();

			final JPanel accordion = createAccordionPanel(subCategoryTitle, groupRecords);
			contentPanel.add(accordion, "growx");
		}

		return wrapInScrollPane(contentPanel);
	}

	/**
	 * Extracts a specific sub-group key (e.g. event type like birth, death, or attribute tag) for fine-grained categorization.
	 */
	private String extractSubGroupKey(final FLEFRecord record){
		if(EventHandler.isOfType(record))
			return FLEFRecordHelper.getChildValue(record, EventReader.TAG_TYPE);
		return record.getTag();
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
				return (focusIsSubject? null: Category.MEMBER);
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

		if(RelationshipReader.isTypeAssociate(t))
			return Category.ASSOCIATE;

		return null;
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
		if(IndividualHandler.isOfType(record))
			return IndividualHandler.getInstance()
				.getDisplayText(record, model);
		if(PlaceHandler.isOfType(record))
			return PlaceHelper.displayName(record);
		// TODO
//		if(SourceHandler.isOfType(record))
//			return SourceHelper.displayName(record);
//		if(EventHandler.isOfType(record))
//			return EventHelper.displayName(record);
//		if(ResearchQuestionHandler.isOfType(record))
//			return ResearchQuestionHelper.displayName(record);
//		if(ResearchActivityHandler.isOfType(record))
//			return ResearchActivityHelper.displayName(record);
//		if(ResearchTaskHandler.isOfType(record))
//			return ResearchTaskHelper.displayName(record);

		return record.getTag() + " [" + fallbackId + "]";
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

		final JPanel inner = new JPanel(new MigLayout("ins 5,wrap 1,fillx,top,gapy 2,hidemode 3", "[grow,fill]", "[]"));
		for(final RelationshipInfo info : infos){
			final JPanel row = createWrappedCheckbox(info.relationshipIds, info.entityId, info.getFormattedDescription());
			inner.add(row, "growx");
		}

		final JScrollPane scrollPane = new JScrollPane(inner);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar()
			.setUnitIncrement(GUIHelper.SCROLLBAR_UNIT_INCREMENT);

		// Ensure scrollbar starts at position 0 when rendered
		SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));

		outer.add(scrollPane, BorderLayout.CENTER);
		return outer;
	}

	private JPanel wrapInScrollPane(final JPanel panel){
		// Wrap the content panel in another panel using BorderLayout with NORTH
		// so it stays compact at the top and doesn't stretch vertically
		final JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setOpaque(false);
		wrapper.add(panel, BorderLayout.NORTH);

		final JScrollPane scrollPane = new JScrollPane(wrapper);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar()
			.setUnitIncrement(GUIHelper.SCROLLBAR_UNIT_INCREMENT);

		SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));

		final JPanel outer = new JPanel(new BorderLayout());
		outer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		outer.add(scrollPane, BorderLayout.CENTER);
		return outer;
	}

	private JPanel createWrappedCheckbox(final List<String> recordIds, final String entityId, final String description){
		final JPanel panel = new JPanel(new MigLayout("ins 2 0 2 0", "[]0[grow,fill,shrink]", "[]"));

		final JCheckBox cb = new JCheckBox();
		final JTextArea textArea = createTextArea(entityId, description, cb);

		// Handle single click vs double click using a timer to prevent state collision
		final Timer clickTimer = new Timer(250, e -> cb.setSelected(!cb.isSelected()));
		clickTimer.setRepeats(false);
		textArea.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				if(SwingUtilities.isLeftMouseButton(e)){
					if(e.getClickCount() == 1)
						clickTimer.restart();
					else if(e.getClickCount() == 2){
						clickTimer.stop();

						if(entityId != null)
							editEntity(entityId);
					}
				}
			}
		});

		panel.add(cb);
		panel.add(textArea);

		checkboxes.add(new RelationshipCheckbox(cb, recordIds, entityId));
		return panel;
	}

	private JPanel createAccordionPanel(final String title, final List<FLEFRecord> groupRecords){
		final JPanel container = new JPanel(new MigLayout("ins 0,wrap 1,fillx,top,hidemode 3", "[grow,fill]", "[]0[]"));

		final JPanel contentPanel = new JPanel(new MigLayout("ins 5,wrap 1,fillx,top,gapy 2", "[grow,fill]", "[]"));
		contentPanel.setVisible(false); // Start closed by default

		final List<JCheckBox> accordionCheckboxes = new ArrayList<>();

		for(final FLEFRecord record : groupRecords){
			final String recordId = record.getId();
			final RecordTypeHandler<?> recHandler = HandlerRegistry.getHandler(record.getTag());
			final String description = (recHandler != null? recHandler.getDisplayText(record, model): record.getTag() + " [" + recordId + "]");
			final JPanel row = createWrappedCheckbox(List.of(recordId), recordId, description);

			// Extract the checkbox from the created row (it is the first component added in createWrappedCheckbox)
			if(row.getComponent(0) instanceof JCheckBox cb)
				accordionCheckboxes.add(cb);

			contentPanel.add(row, "growx");
		}

		final ArrowIcon arrowIcon = new ArrowIcon();
		final javax.swing.JButton headerButton = new javax.swing.JButton(title + " (" + groupRecords.size() + ")", arrowIcon);
		headerButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
		headerButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		headerButton.setFocusPainted(false);
		headerButton.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

		// Method to update accordion header state based on child checkboxes
		final Runnable updateHeaderState = () -> {
			final long selectedCount = accordionCheckboxes.stream().filter(JCheckBox::isSelected).count();
			if(selectedCount == 0)
				arrowIcon.setState(ArrowIcon.State.EMPTY);
			else if(selectedCount == accordionCheckboxes.size())
				arrowIcon.setState(ArrowIcon.State.FULL);
			else
				arrowIcon.setState(ArrowIcon.State.PARTIAL);
			headerButton.repaint();
		};

		// Add listener to each checkbox to update header dynamically
		for(final JCheckBox cb : accordionCheckboxes){
			cb.addItemListener(e -> updateHeaderState.run());
		}
		updateHeaderState.run(); // Initial state

		headerButton.addActionListener(e -> {
			final boolean visible = !contentPanel.isVisible();
			contentPanel.setVisible(visible);

			container.revalidate();
			container.repaint();

			final java.awt.Container ancestor = javax.swing.SwingUtilities.getAncestorOfClass(JScrollPane.class, container);
			if(ancestor != null){
				ancestor.revalidate();
				ancestor.repaint();
			}
		});

		container.add(headerButton, "growx");
		container.add(contentPanel, "growx");

		return container;
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
			textArea.setToolTipText(I18N.t("tooltip.double_click_to_view"));

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
		final BaseRecordDialog dialog = handler.createEditDialog(this, model, entity);
		dialog.setVisible(true);
	}

	/**
	 * Returns the list of record IDs selected by the user to be unlinked/removed.
	 */
	public List<String> getSelectedLinkIds(){
		return linkIdsToRemove;
	}

	public boolean removeParentRecord(){
		return (linkIdsToRemove.size() == totalLinks);
	}

}
