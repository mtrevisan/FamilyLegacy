/**
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.panels;

import io.github.mtrevisan.familylegacy.flef.ui.helpers.ScrollableContainerHost;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;


public class TreePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 4700955059623460223L;

	private static final Logger LOGGER = LoggerFactory.getLogger(TreePanel.class);

	private static final Color BACKGROUND_COLOR_APPLICATION = new Color(242, 238, 228);

	private static final int GENERATION_SEPARATOR_SIZE = 36;

	private static final Map<Integer, Integer> CHILDREN_SCROLLBAR_POSITION = new HashMap<>(0);

	private static final String TABLE_NAME_GROUP = "group";
	private static final String TABLE_NAME_GROUP_JUNCTION = "group_junction";
	private static final String TABLE_NAME_PERSON = "person";


	private GroupPanel partner1Partner1Panel;
	private GroupPanel partner1Partner2Panel;
	private GroupPanel partner2Partner1Panel;
	private GroupPanel partner2Partner2Panel;
	private GroupPanel partner1PartnersPanel;
	private GroupPanel partner2PartnersPanel;
	private GroupPanel homeGroupPanel;
	private JScrollPane childrenScrollPane;
	private ChildrenPanel childrenPanel;

	private Map<String, Object> partner1;
	private Map<String, Object> partner2;
	private Map<String, Object> homeUnion;
	private final int generations;
	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;

	private Map<String, Object> partner1Parents;
	private Map<String, Object> partner2Parents;
	private Map<String, Object> partner1Partner1;
	private Map<String, Object> partner1Partner2;
	private Map<String, Object> partner1Partner1Parents;
	private Map<String, Object> partner1Partner2Parents;
	private Map<String, Object> partner2Partner1;
	private Map<String, Object> partner2Partner2;
	private Map<String, Object> partner2Partner1Parents;
	private Map<String, Object> partner2Partner2Parents;


	public static TreePanel create(final int generations, final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		return new TreePanel(generations, store);
	}


	private TreePanel(final int generations, final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		this.generations = generations;
		this.store = store;
	}


	void initComponents(){
		if(generations <= 3)
			initComponents3Generations();
		else
			initComponents4Generations();
	}

	private void initComponents3Generations(){
		partner1PartnersPanel = GroupPanel.create(store, BoxPanelType.SECONDARY);
		partner1PartnersPanel.initComponents();
		EventBusService.subscribe(partner1PartnersPanel);
		partner2PartnersPanel = GroupPanel.create(store, BoxPanelType.SECONDARY);
		partner2PartnersPanel.initComponents();
		EventBusService.subscribe(partner2PartnersPanel);
		homeGroupPanel = GroupPanel.create(store, BoxPanelType.PRIMARY);
		homeGroupPanel.initComponents();
		EventBusService.subscribe(homeGroupPanel);
		childrenPanel = ChildrenPanel.create(store);
		childrenPanel.initComponents();

		setBackground(BACKGROUND_COLOR_APPLICATION);

		childrenScrollPane = new JScrollPane(new ScrollableContainerHost(childrenPanel, ScrollableContainerHost.ScrollType.VERTICAL));
		childrenScrollPane.setOpaque(false);
		childrenScrollPane.getViewport().setOpaque(false);
		childrenScrollPane.setBorder(null);
		childrenScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		childrenScrollPane.setAutoscrolls(true);
		//trigger repaint in order to move the connections between children and home union
		childrenScrollPane.getHorizontalScrollBar().addAdjustmentListener(e -> {
			if(!homeUnion.isEmpty()){
				//remember last scroll position, restore it if present
				final Integer homeUnionID = extractRecordID(homeUnion);
				CHILDREN_SCROLLBAR_POSITION.put(homeUnionID, childrenScrollPane.getHorizontalScrollBar().getValue());
			}
		});

		setLayout(new MigLayout("insets 0",
			"[grow,center]" + GroupPanel.GROUP_SEPARATION + "[grow,center]",
			"[]" + GENERATION_SEPARATOR_SIZE + "[]" + GENERATION_SEPARATOR_SIZE + "[]"));
		add(partner1PartnersPanel, "growx 50");
		add(partner2PartnersPanel, "growx 50,wrap");
		add(homeGroupPanel, "span 2,wrap");
		add(childrenScrollPane, "span 2");
	}

	private void initComponents4Generations(){
		partner1Partner1Panel = GroupPanel.create(store, BoxPanelType.SECONDARY);
		partner1Partner1Panel.initComponents();
		EventBusService.subscribe(partner1Partner1Panel);
		partner1Partner2Panel = GroupPanel.create(store, BoxPanelType.SECONDARY);
		partner1Partner2Panel.initComponents();
		EventBusService.subscribe(partner1Partner2Panel);
		partner2Partner1Panel = GroupPanel.create(store, BoxPanelType.SECONDARY);
		partner2Partner1Panel.initComponents();
		EventBusService.subscribe(partner2Partner1Panel);
		partner2Partner2Panel = GroupPanel.create(store, BoxPanelType.SECONDARY);
		partner2Partner2Panel.initComponents();
		EventBusService.subscribe(partner2Partner2Panel);
		partner1PartnersPanel = GroupPanel.create(store, BoxPanelType.SECONDARY);
		partner1PartnersPanel.initComponents();
		EventBusService.subscribe(partner1PartnersPanel);
		partner2PartnersPanel = GroupPanel.create(store, BoxPanelType.SECONDARY);
		partner2PartnersPanel.initComponents();
		EventBusService.subscribe(partner2PartnersPanel);
		homeGroupPanel = GroupPanel.create(store, BoxPanelType.PRIMARY);
		homeGroupPanel.initComponents();
		EventBusService.subscribe(homeGroupPanel);
		childrenPanel = ChildrenPanel.create(store);
		childrenPanel.initComponents();

		setBackground(BACKGROUND_COLOR_APPLICATION);

		childrenScrollPane = new JScrollPane(new ScrollableContainerHost(childrenPanel, ScrollableContainerHost.ScrollType.HORIZONTAL));
		childrenScrollPane.setOpaque(false);
		childrenScrollPane.getViewport().setOpaque(false);
		childrenScrollPane.setBorder(null);
		childrenScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		childrenScrollPane.setAutoscrolls(true);
		//trigger repaint in order to move the connections between children and home union
		childrenScrollPane.getHorizontalScrollBar().addAdjustmentListener(e -> {
			if(!homeUnion.isEmpty()){
				//remember last scroll position, restore it if present
				final Integer homeUnionID = extractRecordID(homeUnion);
				CHILDREN_SCROLLBAR_POSITION.put(homeUnionID, childrenScrollPane.getHorizontalScrollBar().getValue());
			}
		});

		setLayout(new MigLayout("insets 0",
			"[grow,center]" + GroupPanel.GROUP_SEPARATION
				+ "[grow,center]" + GroupPanel.GROUP_SEPARATION
				+ "[grow,center]" + GroupPanel.GROUP_SEPARATION
				+ "[grow,center]",
			"[]" + GENERATION_SEPARATOR_SIZE + "[]" + GENERATION_SEPARATOR_SIZE + "[]" + GENERATION_SEPARATOR_SIZE + "[]"));
		add(partner1Partner1Panel, "growx 25");
		add(partner1Partner2Panel, "growx 25");
		add(partner2Partner1Panel, "growx 25");
		add(partner2Partner2Panel, "growx 25,wrap");
		add(partner1PartnersPanel, "span 2,growx 50");
		add(partner2PartnersPanel, "span 2,growx 50,wrap");
		add(homeGroupPanel, "span 4,wrap");
		add(childrenScrollPane, "span 4,center");
	}

	public void setUnionListener(final GroupListenerInterface groupListener){
		if(generations > 3){
			partner1Partner1Panel.setGroupListener(groupListener);
			partner1Partner2Panel.setGroupListener(groupListener);
			partner2Partner1Panel.setGroupListener(groupListener);
			partner2Partner2Panel.setGroupListener(groupListener);
		}
		partner1PartnersPanel.setGroupListener(groupListener);
		partner2PartnersPanel.setGroupListener(groupListener);
		homeGroupPanel.setGroupListener(groupListener);
	}

	@SuppressWarnings("unchecked")
	public void setPersonListener(final PersonListenerInterface personListener){
		if(generations > 3){
			partner1Partner1Panel.setChildren(!partner1Partner1Parents.isEmpty()
				? extractChildren(extractRecordID(partner1Partner1Parents))
				: new Map[]{partner1Partner1});
			partner1Partner1Panel.setPersonListener(personListener);
			partner1Partner2Panel.setChildren(!partner1Partner2Parents.isEmpty()
				? extractChildren(extractRecordID(partner1Partner2Parents))
				: new Map[]{partner1Partner2});
			partner1Partner2Panel.setPersonListener(personListener);
			partner2Partner1Panel.setChildren(!partner2Partner1Parents.isEmpty()
				? extractChildren(extractRecordID(partner2Partner1Parents))
				: new Map[]{partner2Partner1});
			partner2Partner1Panel.setPersonListener(personListener);
			partner2Partner2Panel.setChildren(!partner2Partner2Parents.isEmpty()
				? extractChildren(extractRecordID(partner2Partner2Parents))
				: new Map[]{partner2Partner2});
			partner2Partner2Panel.setPersonListener(personListener);
		}
		partner1PartnersPanel.setChildren(!partner1Parents.isEmpty()
			? extractChildren(extractRecordID(partner1Parents))
			: new Map[]{partner1});
		partner1PartnersPanel.setPersonListener(personListener);
		partner2PartnersPanel.setChildren(!partner2Parents.isEmpty()
			? extractChildren(extractRecordID(partner2Parents))
			: new Map[]{partner2});
		partner2PartnersPanel.setPersonListener(personListener);
		homeGroupPanel.setChildren(childrenPanel.getChildren());
		homeGroupPanel.setPersonListener(personListener);
		childrenPanel.setPersonListener(personListener);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object>[] extractChildren(final Integer unionID){
		final TreeMap<Integer, Map<String, Object>> persons = getRecords(TABLE_NAME_PERSON);
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals("child", extractRecordRole(entry)))
			.filter(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(unionID, extractRecordGroupID(entry)))
			.map(entry -> persons.get(extractRecordReferenceID(entry)))
			.toArray(Map[]::new);
	}

	@Override
	protected final void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			graphics2D.setStroke(GroupPanel.CONNECTION_STROKE);

			if(partner1Partner1Panel != null && partner1Partner1Panel.isVisible()){
				//partner1's partner1 entering connection
				final Point p1p1 = partner1PartnersPanel.getGroupPaintingPartner1EnterPoint();
				p1p1.setLocation(p1p1.x, p1p1.y - 2);
				final Point p1g1p = parentGrandParentsExitingConnection(partner1Partner1Panel, graphics2D);
				grandparentsEnteringConnection(p1p1, p1g1p, graphics2D);
			}
			if(partner1Partner2Panel != null && partner1Partner2Panel.isVisible()){
				//partner1's partner2 entering connection
				final Point p1p2 = partner1PartnersPanel.getGroupPaintingPartner2EnterPoint();
				p1p2.setLocation(p1p2.x, p1p2.y - 2);
				final Point p1g2p = parentGrandParentsExitingConnection(partner1Partner2Panel, graphics2D);
				grandparentsEnteringConnection(p1p2, p1g2p, graphics2D);
			}
			if(partner2Partner1Panel != null && partner2Partner1Panel.isVisible()){
				//partner2's partner1 entering connection
				final Point p2p1 = partner2PartnersPanel.getGroupPaintingPartner1EnterPoint();
				p2p1.setLocation(p2p1.x, p2p1.y - 2);
				final Point p2g1p = parentGrandParentsExitingConnection(partner2Partner1Panel, graphics2D);
				grandparentsEnteringConnection(p2p1, p2g1p, graphics2D);
			}
			if(partner2Partner2Panel != null && partner2Partner2Panel.isVisible()){
				//partner2's partner2 entering connection
				final Point p2p2 = partner2PartnersPanel.getGroupPaintingPartner2EnterPoint();
				p2p2.setLocation(p2p2.x, p2p2.y - 2);
				final Point p2g2p = parentGrandParentsExitingConnection(partner2Partner2Panel, graphics2D);
				grandparentsEnteringConnection(p2p2, p2g2p, graphics2D);
			}

			final Point p1p = partner1PartnersPanel.getGroupPaintingExitPoint();
			if(!partner1.isEmpty())
				//partner1's partners exiting connection
				grandparentsExitingConnection(p1p, 0, graphics2D);
			final Point p2p = partner2PartnersPanel.getGroupPaintingExitPoint();
			if(!partner2.isEmpty())
				//partner2's partners exiting connection
				grandparentsExitingConnection(p2p, 0, graphics2D);
			if(!partner1.isEmpty()){
				final Point hfp1 = homeGroupPanel.getGroupPaintingPartner1EnterPoint();
				//home union partner1 entering connection
				parentEnteringConnection(hfp1, GroupPanel.NAVIGATION_ARROW_HEIGHT, graphics2D);
				//line between partner1's partners and partner1
				grandparentsToParent(p1p, hfp1, GroupPanel.NAVIGATION_ARROW_HEIGHT, graphics2D);
			}
			if(!partner2.isEmpty()){
				final Point hfp2 = homeGroupPanel.getGroupPaintingPartner2EnterPoint();
				//home union partner2 entering connection
				parentEnteringConnection(hfp2, GroupPanel.NAVIGATION_ARROW_HEIGHT, graphics2D);
				//line between partner2's partners and partner2
				grandparentsToParent(p2p, hfp2, GroupPanel.NAVIGATION_ARROW_HEIGHT, graphics2D);
			}
			final Point[] c = childrenPanel.getChildrenPaintingEnterPoints();
			if(c.length > 0){
				//home union exiting connection
				final Point hf = homeGroupPanel.getGroupPaintingExitPoint();
				grandparentsExitingConnection(hf, ChildrenPanel.UNION_ARROW_HEIGHT, graphics2D);

				final Point origin = childrenScrollPane.getLocation();
				origin.x -= childrenScrollPane.getHorizontalScrollBar().getValue();
				//horizontal line from first to last child
				graphics2D.drawLine(origin.x + c[0].x, origin.y + c[0].y - GENERATION_SEPARATOR_SIZE / 2,
					origin.x + c[c.length - 1].x, origin.y + c[c.length - 1].y - GENERATION_SEPARATOR_SIZE / 2);
				//vertical line connecting the children
				for(int i = 0; i < c.length; i ++){
					final Point point = c[i];

					final boolean isAdopted = childrenPanel.isChildAdopted(i);
					if(isAdopted)
						graphics2D.setStroke(GroupPanel.CONNECTION_STROKE_ADOPTED);

					graphics2D.drawLine(origin.x + point.x, origin.y + point.y,
						origin.x + point.x, origin.y + point.y - GENERATION_SEPARATOR_SIZE / 2);

					if(isAdopted)
						graphics2D.setStroke(GroupPanel.CONNECTION_STROKE);
				}
			}

			graphics2D.dispose();
		}
	}

	private static Point parentGrandParentsExitingConnection(final GroupPanel parentGrandparentsPanel, final Graphics2D graphics2D){
		Point p = null;
		if(parentGrandparentsPanel != null){
			//parent's parent's parent exiting connection
			p = parentGrandparentsPanel.getGroupPaintingExitPoint();
			grandparentsExitingConnection(p, 0, graphics2D);
		}
		return p;
	}

	private static void grandparentsEnteringConnection(final Point g, final Point pg, final Graphics2D graphics2D){
		//parent's parent entering connection
		parentEnteringConnection(g, 0, graphics2D);

		if(pg != null)
			//line between grandparent and grandparent's parents
			grandparentsToParent(pg, g, 0, graphics2D);
	}

	private static void grandparentsExitingConnection(final Point g, final int offset, final Graphics2D graphics2D){
		//grandparent exiting connection
		graphics2D.drawLine(g.x, g.y,
			g.x, g.y + offset + GroupPanel.GROUP_EXITING_HEIGHT + GENERATION_SEPARATOR_SIZE / 2);
	}

	private static void parentEnteringConnection(final Point p, final int offset, final Graphics2D graphics2D){
		//parent entering connection
		graphics2D.drawLine(p.x, p.y + GroupPanel.NAVIGATION_ARROW_HEIGHT + offset,
			p.x, p.y - offset - GENERATION_SEPARATOR_SIZE / 2);
	}

	private static void grandparentsToParent(final Point g, final Point p, final int offset, final Graphics2D graphics2D){
		//line between grandparent and parent
		graphics2D.drawLine(g.x, g.y + GroupPanel.GROUP_EXITING_HEIGHT + GENERATION_SEPARATOR_SIZE / 2,
			p.x, p.y - offset - GENERATION_SEPARATOR_SIZE / 2);
	}


	public void loadDataFromUnion(final Map<String, Object> homeUnion){
		loadData(homeUnion, Collections.emptyMap(), Collections.emptyMap());
	}

	public void loadDataFromPerson(final Map<String, Object> partner1){
		//TODO prefer left position (`partner1`) if male or unknown, right if female (`partner2`)
		loadData(Collections.emptyMap(), partner1, Collections.emptyMap());
	}

	public final void loadData(final Map<String, Object> homeUnion, Map<String, Object> partner1, Map<String, Object> partner2){
		initComponents();

		prepareData(homeUnion, partner1, partner2);

		loadData();
	}

	private void prepareData(Map<String, Object> homeUnion, Map<String, Object> partner1, Map<String, Object> partner2){
		if(homeUnion.isEmpty()){
			//TODO prefer biological family
			homeUnion = Collections.emptyMap();
		}

		final Integer homeUnionID = extractRecordID(homeUnion);
		final List<Integer> personIDsInUnion = getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals("partner", extractRecordRole(entry)))
			.filter(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(homeUnionID, extractRecordGroupID(entry)))
			.map(TreePanel::extractRecordReferenceID)
			.toList();
		final Integer partner1ID = extractRecordID(partner1);
		if(!personIDsInUnion.contains(partner1ID)){
			LOGGER.warn("Person {} does not belongs to the union {} (this cannot be)", partner1ID, homeUnionID);

			partner1 = Collections.emptyMap();
		}
		final Integer partner2ID = extractRecordID(partner2);
		if(!personIDsInUnion.contains(partner2ID)){
			LOGGER.warn("Person {} does not belongs to the union {} (this cannot be)", partner2ID, homeUnionID);

			partner2 = Collections.emptyMap();
		}

		final TreeMap<Integer, Map<String, Object>> persons = getRecords(TABLE_NAME_PERSON);

		if((partner1.isEmpty() || partner2.isEmpty()) && !homeUnion.isEmpty()){
			//extract the first two persons from the union:
			final int size = personIDsInUnion.size();
			if(partner1.isEmpty())
				partner1 = persons.getOrDefault((size > 0? personIDsInUnion.get(0): null), Collections.emptyMap());
			if(partner2.isEmpty())
				partner2 = persons.getOrDefault((size > 1? personIDsInUnion.get(1): null), Collections.emptyMap());
		}

		this.homeUnion = homeUnion;
		this.partner1 = partner1;
		this.partner2 = partner2;
	}

	private void loadData(){
		partner1Parents = extractParents(partner1);
		partner2Parents = extractParents(partner2);
		if(generations > 3){
			final List<Integer> personIDsInGroup1 = getPartnerIDs(partner1Parents);
			final int personInGroupCount = personIDsInGroup1.size();
			final TreeMap<Integer, Map<String, Object>> persons = getRecords(TABLE_NAME_PERSON);
			partner1Partner1 = (personInGroupCount > 0
				? persons.get(personIDsInGroup1.get(0))
				: Collections.emptyMap());
			partner1Partner2 = (personInGroupCount > 1
				? persons.get(personIDsInGroup1.get(1))
				: Collections.emptyMap());
			partner1Partner1Parents = extractParents(partner1Partner1);
			partner1Partner2Parents = extractParents(partner1Partner2);

			final List<Integer> personIDsInGroup2 = getPartnerIDs(partner2Parents);
			partner2Partner1 = (personInGroupCount > 0
				? persons.get(personIDsInGroup2.get(0))
				: Collections.emptyMap());
			partner2Partner2 = (personInGroupCount > 1
				? persons.get(personIDsInGroup2.get(1))
				: Collections.emptyMap());
			partner2Partner1Parents = extractParents(partner2Partner1);
			partner2Partner2Parents = extractParents(partner2Partner2);

			partner1Partner1Panel.loadData(partner1Partner1Parents);
			partner1Partner1Panel.setVisible(!partner1Parents.isEmpty());
			partner1Partner2Panel.loadData(partner1Partner2Parents);
			partner1Partner2Panel.setVisible(!partner1Parents.isEmpty());
			partner2Partner1Panel.loadData(partner2Partner1Parents);
			partner2Partner1Panel.setVisible(!partner2Parents.isEmpty());
			partner2Partner2Panel.loadData(partner2Partner2Parents);
			partner2Partner2Panel.setVisible(!partner2Parents.isEmpty());
		}
		partner1PartnersPanel.loadData(partner1Parents);
		partner1PartnersPanel.setVisible(!partner1.isEmpty());
		partner2PartnersPanel.loadData(partner2Parents);
		partner2PartnersPanel.setVisible(!partner2.isEmpty());
		homeGroupPanel.loadData(homeUnion, partner1, partner2);
		final Integer homeUnionID = extractRecordID(homeUnion);
		childrenPanel.loadData(homeUnionID);


		if(!homeUnion.isEmpty()){
			//remember last scroll position, restore it if present
			final Integer childrenScrollbarPosition = CHILDREN_SCROLLBAR_POSITION.get(homeUnionID);
			//center halfway if it's the first time the children are painted
			final int scrollbarPositionX = (childrenScrollbarPosition == null?
				(childrenPanel.getPreferredSize().width - childrenScrollPane.getViewport().getWidth()) / 4 - 7:
				childrenScrollbarPosition);
			childrenScrollPane.getViewport().setViewPosition(new Point(scrollbarPositionX, 0));
		}
	}

	private List<Integer> getPartnerIDs(final Map<String, Object> union){
		final Integer groupID = extractRecordID(union);
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals("partner", extractRecordRole(entry)))
			.filter(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(groupID, extractRecordGroupID(entry)))
			.map(TreePanel::extractRecordReferenceID)
			.toList();
	}

	private Map<String, Object> extractParents(final Map<String, Object> child){
		Map<String, Object> parentsGroup = Collections.emptyMap();
		if(!child.isEmpty()){
			final Integer childID = extractRecordID(child);
			final List<Integer> groupIDs = getRecords(TABLE_NAME_GROUP_JUNCTION)
				.values().stream()
				.filter(entry -> Objects.equals("child", extractRecordRole(entry)))
				.filter(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
				.filter(entry -> Objects.equals(childID, extractRecordReferenceID(entry)))
				.map(TreePanel::extractRecordGroupID)
				.toList();
			if(groupIDs.size() > 1)
				LOGGER.warn("Person {} belongs to more than one union (this cannot be), select the first and hope for the best", childID);

			final Integer parentsID = (!groupIDs.isEmpty()? groupIDs.getFirst(): null);
			if(parentsID != null){
				final TreeMap<Integer, Map<String, Object>> groups = getRecords(TABLE_NAME_GROUP);
				parentsGroup = groups.get(parentsID);
			}
		}
		return parentsGroup;
	}

	protected static Integer extractRecordID(final Map<String, Object> record){
		return (record != null? (Integer)record.get("id"): null);
	}

	protected final TreeMap<Integer, Map<String, Object>> getRecords(final String tableName){
		return store.computeIfAbsent(tableName, k -> new TreeMap<>());
	}

	protected final TreeMap<Integer, Map<String, Object>> getFilteredRecords(final String tableName, final String filterReferenceTable,
		final Integer filterReferenceID){
		return getRecords(tableName).entrySet().stream()
			.filter(entry -> Objects.equals(filterReferenceTable, extractRecordReferenceTable(entry.getValue())))
			.filter(entry -> Objects.equals(filterReferenceID, extractRecordReferenceID(entry.getValue())))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
	}

	private static String extractRecordReferenceTable(final Map<String, Object> record){
		return (String)record.get("reference_table");
	}

	private static Integer extractRecordReferenceID(final Map<String, Object> record){
		return (Integer)record.get("reference_id");
	}

	private static String extractRecordRole(final Map<String, Object> record){
		return (String)record.get("role");
	}

	private static Integer extractRecordGroupID(final Map<String, Object> record){
		return (Integer)record.get("group_id");
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> persons = new TreeMap<>();
		store.put("person", persons);
		final Map<String, Object> person1 = new HashMap<>();
		person1.put("id", 1);
		persons.put((Integer)person1.get("id"), person1);
		final Map<String, Object> person2 = new HashMap<>();
		person2.put("id", 2);
		persons.put((Integer)person2.get("id"), person2);
		final Map<String, Object> person3 = new HashMap<>();
		person3.put("id", 3);
		persons.put((Integer)person3.get("id"), person3);
		final Map<String, Object> person4 = new HashMap<>();
		person4.put("id", 4);
		persons.put((Integer)person4.get("id"), person4);
		final Map<String, Object> person5 = new HashMap<>();
		person5.put("id", 5);
		persons.put((Integer)person5.get("id"), person5);

		final TreeMap<Integer, Map<String, Object>> groups = new TreeMap<>();
		store.put("group", groups);
		final Map<String, Object> group1 = new HashMap<>();
		group1.put("id", 1);
		group1.put("type", "family");
		groups.put((Integer)group1.get("id"), group1);
		final Map<String, Object> group2 = new HashMap<>();
		group2.put("id", 2);
		group2.put("type", "family");
		groups.put((Integer)group2.get("id"), group2);

		final TreeMap<Integer, Map<String, Object>> groupJunctions = new TreeMap<>();
		store.put("group_junction", groupJunctions);
		final Map<String, Object> groupJunction11 = new HashMap<>();
		groupJunction11.put("id", 1);
		groupJunction11.put("group_id", 1);
		groupJunction11.put("reference_table", "person");
		groupJunction11.put("reference_id", 1);
		groupJunction11.put("role", "partner");
		groupJunctions.put((Integer)groupJunction11.get("id"), groupJunction11);
		final Map<String, Object> groupJunction2 = new HashMap<>();
		groupJunction2.put("id", 2);
		groupJunction2.put("group_id", 1);
		groupJunction2.put("reference_table", "person");
		groupJunction2.put("reference_id", 2);
		groupJunction2.put("role", "partner");
		groupJunctions.put((Integer)groupJunction2.get("id"), groupJunction2);
		final Map<String, Object> groupJunction13 = new HashMap<>();
		groupJunction13.put("id", 3);
		groupJunction13.put("group_id", 2);
		groupJunction13.put("reference_table", "person");
		groupJunction13.put("reference_id", 1);
		groupJunction13.put("role", "partner");
		groupJunctions.put((Integer)groupJunction13.get("id"), groupJunction13);
		final Map<String, Object> groupJunction3 = new HashMap<>();
		groupJunction3.put("id", 4);
		groupJunction3.put("group_id", 2);
		groupJunction3.put("reference_table", "person");
		groupJunction3.put("reference_id", 3);
		groupJunction3.put("role", "partner");
		groupJunctions.put((Integer)groupJunction3.get("id"), groupJunction3);
		final Map<String, Object> groupJunction4 = new HashMap<>();
		groupJunction4.put("id", 5);
		groupJunction4.put("group_id", 1);
		groupJunction4.put("reference_table", "person");
		groupJunction4.put("reference_id", 4);
		groupJunction4.put("role", "child");
		groupJunctions.put((Integer)groupJunction4.get("id"), groupJunction4);
		final Map<String, Object> groupJunction5 = new HashMap<>();
		groupJunction5.put("id", 6);
		groupJunction5.put("group_id", 1);
		groupJunction5.put("reference_table", "person");
		groupJunction5.put("reference_id", 5);
		groupJunction5.put("role", "child");
		groupJunctions.put((Integer)groupJunction5.get("id"), groupJunction5);
		final Map<String, Object> groupJunction6 = new HashMap<>();
		groupJunction6.put("id", 7);
		groupJunction6.put("group_id", 2);
		groupJunction6.put("reference_table", "person");
		groupJunction6.put("reference_id", 4);
		groupJunction6.put("role", "partner");
		groupJunctions.put((Integer)groupJunction6.get("id"), groupJunction6);

		final TreeMap<Integer, Map<String, Object>> events = new TreeMap<>();
		store.put("event", events);
		final Map<String, Object> event1 = new HashMap<>();
		event1.put("id", 1);
		event1.put("type", "adoption");
		event1.put("reference_table", "person");
		event1.put("reference_id", 5);
		events.put((Integer)event1.get("id"), event1);

		final GroupListenerInterface unionListener = new GroupListenerInterface(){
			@Override
			public void onGroupEdit(final GroupPanel groupPanel){
				final Map<String, Object> group = groupPanel.getGroup();
				System.out.println("onEditGroup " + group.get("id"));
			}

			@Override
			public void onGroupLink(final GroupPanel groupPanel){
				System.out.println("onLinkGroup");
			}

			@Override
			public void onGroupUnlink(final GroupPanel groupPanel){
				final Map<String, Object> group = groupPanel.getGroup();
				System.out.println("onUnlinkGroup " + group.get("id"));
			}

			@Override
			public void onGroupRemove(final GroupPanel groupPanel){
				final Map<String, Object> group = groupPanel.getGroup();
				System.out.println("onRemoveGroup " + group.get("id"));
			}

			@Override
			public void onGroupPreviousParents(final GroupPanel groupPanel, final Map<String, Object> currentUnion,
					final Map<String, Object> otherUnion){
				final Map<String, Object> currentGroup = groupPanel.getGroup();
				System.out.println("onGroupPartnerPreviousParents this: " + currentUnion.get("id") + ", other: " + otherUnion.get("id")
					+ ", current group: " + currentGroup.get("id"));
			}

			@Override
			public void onGroupNextParents(final GroupPanel groupPanel, final Map<String, Object> currentParent,
					final Map<String, Object> otherParent){
				final Map<String, Object> currentGroup = groupPanel.getGroup();
				System.out.println("onGroupPartnerNextParents this: " + currentParent.get("id") + ", other: " + otherParent.get("id")
					+ ", current group: " + currentGroup.get("id"));
			}

			@Override
			public void onGroupPreviousUnion(final GroupPanel groupPanel, final Map<String, Object> currentParent,
					final Map<String, Object> otherParent){
				final Map<String, Object> currentGroup = groupPanel.getGroup();
				System.out.println("onGroupPartnerPreviousUnion this: " + currentParent.get("id") + ", other: " + otherParent.get("id")
					+ ", current group: " + currentGroup.get("id"));
			}

			@Override
			public void onGroupNextUnion(final GroupPanel groupPanel, final Map<String, Object> currentParent,
					final Map<String, Object> otherParent){
				final Map<String, Object> currentGroup = groupPanel.getGroup();
				System.out.println("onGroupPartnerNextUnion this: " + currentParent.get("id") + ", other: " + otherParent.get("id")
					+ ", current group: " + currentGroup.get("id"));
			}
		};
		final PersonListenerInterface personListener = new PersonListenerInterface(){
			@Override
			public void onPersonEdit(final PersonPanel boxPanel){
				final Map<String, Object> person = boxPanel.getPerson();
				System.out.println("onEditPerson " + person.get("id"));
			}

			@Override
			public void onPersonFocus(final PersonPanel boxPanel, final SelectedNodeType type){
				final Map<String, Object> person = boxPanel.getPerson();
				System.out.println("onFocusPerson " + person.get("id") + ", type is " + type);
			}

			@Override
			public void onPersonLink(final PersonPanel boxPanel, final SelectedNodeType type){
				final Map<String, Object> partner = boxPanel.getPartner();
				final Map<String, Object> marriage = boxPanel.getUnion();
				final Map<String, Object>[] children = boxPanel.getChildren();
				System.out.println("onLinkPerson (partner " + partner.get("id") + ", marriage " + marriage.get("id") + ", child "
					+ Arrays.toString(Arrays.stream(children).map(PersonPanel::extractRecordID).toArray(Integer[]::new)) + "), type is " + type);
			}

			@Override
			public void onPersonUnlink(final PersonPanel boxPanel){
				final Map<String, Object> person = boxPanel.getPerson();
				System.out.println("onUnlinkPerson " + person.get("id"));
			}

			@Override
			public void onPersonAdd(final PersonPanel boxPanel, final SelectedNodeType type){
				final Map<String, Object> partner = boxPanel.getPartner();
				final Map<String, Object> marriage = boxPanel.getUnion();
				final Map<String, Object>[] children = boxPanel.getChildren();
				System.out.println("onAddPerson (partner " + partner.get("id") + ", marriage " + marriage.get("id") + ", child "
					+ Arrays.toString(Arrays.stream(children).map(PersonPanel::extractRecordID).toArray(Integer[]::new)) + "), type is " + type);
			}

			@Override
			public void onPersonRemove(final PersonPanel boxPanel){
				final Map<String, Object> person = boxPanel.getPerson();
				System.out.println("onRemovePerson " + person.get("id"));
			}

			@Override
			public void onPersonAddImage(final PersonPanel boxPanel){
				final Map<String, Object> person = boxPanel.getPerson();
				System.out.println("onAddPreferredImage " + person.get("id"));
			}
		};

		EventQueue.invokeLater(() -> {
			final TreePanel panel = create(4, store);
			panel.loadDataFromUnion(group1);
			panel.setUnionListener(unionListener);
			panel.setPersonListener(personListener);

			EventBusService.subscribe(panel);

			final JFrame frame = new JFrame();
			final Container contentPane = frame.getContentPane();
			contentPane.setLayout(new BorderLayout());
			contentPane.add(panel, BorderLayout.NORTH);
			frame.pack();
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			frame.setSize(1200, 500);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
