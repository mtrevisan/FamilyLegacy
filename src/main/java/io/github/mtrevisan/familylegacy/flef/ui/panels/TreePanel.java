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

import io.github.mtrevisan.familylegacy.flef.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.flef.helpers.parsers.DateParser;
import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.CitationDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.CommonListDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.CulturalNormDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.EventDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.GroupDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.MediaDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.NoteDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.PersonDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.PlaceDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.RepositoryDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.ResearchStatusDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.SearchDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.SourceDialog;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.ScrollableContainerHost;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.RecordListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.tree.GenealogicalTree;
import io.github.mtrevisan.familylegacy.flef.ui.tree.GenealogyNavigation;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDateID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDescription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordTypeID;


public class TreePanel extends JPanel implements RecordListenerInterface{

	@Serial
	private static final long serialVersionUID = 4700955059623460223L;

	private static final Logger LOGGER = LoggerFactory.getLogger(TreePanel.class);

	private static final Color BACKGROUND_COLOR_APPLICATION = new Color(242, 238, 228);

	private static final int GENERATION_SEPARATOR_SIZE = 36;

	private static final Map<Integer, Integer> CHILDREN_SCROLLBAR_POSITION = new HashMap<>(0);
	private static final List<Map.Entry<Set<Integer>, Map<String, Object>>> UNION_DEFAULT = new ArrayList<>(0);
	private static final List<Map.Entry<Set<Integer>, Map<String, Object>>> LEFT_PARTNER_DEFAULT = new ArrayList<>(0);

	private static final String ACTION_MAP_KEY_NAVIGATION_BACK = "navigationBack";
	private static final String ACTION_MAP_KEY_NAVIGATION_FORWARD = "navigationForward";


	private GroupPanel partner1Partner1Panel;
	private GroupPanel partner1Partner2Panel;
	private GroupPanel partner2Partner1Panel;
	private GroupPanel partner2Partner2Panel;
	private GroupPanel partner1PartnersPanel;
	private GroupPanel partner2PartnersPanel;
	private GroupPanel homeGroupPanel;
	private JScrollPane childrenScrollPane;
	private ChildrenPanel childrenPanel;
	private GenealogyNavigation genealogyNavigation;
	private SearchDialog searchDialog;
	public GenealogicalTree genealogicalTree;

	private Map<String, Object> homeUnion = new HashMap<>(0);
	private Map<String, Object> partner1 = new HashMap<>(0);
	private Map<String, Object> partner2 = new HashMap<>(0);
	private final int generations;


	public static TreePanel create(final int generations, final Frame parentFrame){
		return new TreePanel(generations, parentFrame);
	}


	private TreePanel(final int generations, final Frame parentFrame){
		this.generations = generations;


		initComponents(parentFrame);
	}


	private void initComponents(final Frame parentFrame){
		if(generations <= 3)
			initComponents3Generations();
		else
			initComponents4Generations();


		final InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK), ACTION_MAP_KEY_NAVIGATION_BACK);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK), ACTION_MAP_KEY_NAVIGATION_FORWARD);
		final ActionMap actionMap = getActionMap();
		actionMap.put(ACTION_MAP_KEY_NAVIGATION_BACK, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -4059299635711242193L;

			@Override
			public void actionPerformed(final ActionEvent evt){
				if(genealogyNavigation.goBack())
					updateDisplay();
			}
		});
		actionMap.put(ACTION_MAP_KEY_NAVIGATION_FORWARD, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -6542846485068325919L;

			@Override
			public void actionPerformed(final ActionEvent evt){
				if(genealogyNavigation.goForward())
					updateDisplay();
			}
		});

		searchDialog = SearchDialog.create(parentFrame)
			.withLinkListener(TreePanel.this);
		GUIHelper.addDoubleShiftListener(this, () -> {
			searchDialog.loadData();

			searchDialog.setLocationRelativeTo(TreePanel.this);
			searchDialog.setVisible(true);
		});
	}

	private void initComponents3Generations(){
		partner1PartnersPanel = GroupPanel.create(BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner1PartnersPanel);
		partner2PartnersPanel = GroupPanel.create(BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner2PartnersPanel);
		homeGroupPanel = GroupPanel.create(BoxPanelType.PRIMARY);
		EventBusService.subscribe(homeGroupPanel);
		childrenPanel = ChildrenPanel.create();
//		navigationPanel = GenealogyNavigationPanel.create(this);
		genealogyNavigation = new GenealogyNavigation();

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
				//remember last scroll position
				final Integer homeUnionID = extractRecordID(homeUnion);
				final int scrollBarPosition = childrenScrollPane.getHorizontalScrollBar().getValue();
				CHILDREN_SCROLLBAR_POSITION.put(homeUnionID, scrollBarPosition);
			}
		});

		//construct genealogical tree
		genealogicalTree = new GenealogicalTree(3, childrenPanel);
		genealogicalTree.addTo(0, homeGroupPanel);
		genealogicalTree.addTo(GenealogicalTree.getLeftChild(0), partner1PartnersPanel);
		genealogicalTree.addTo(GenealogicalTree.getRightChild(0), partner2PartnersPanel);

		setLayout(new MigLayout("insets 0",
			"[grow,center]" + GroupPanel.GROUP_SEPARATION + "[grow,center]",
			"[]" + GENERATION_SEPARATOR_SIZE + "[]" + GENERATION_SEPARATOR_SIZE + "[]"));
		add(partner1PartnersPanel, "grow");
		add(partner2PartnersPanel, "grow,wrap");
		add(homeGroupPanel, "span 2,wrap");
		add(childrenScrollPane, "span 2");
	}

	private void initComponents4Generations(){
		partner1Partner1Panel = GroupPanel.create(BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner1Partner1Panel);
		partner1Partner2Panel = GroupPanel.create(BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner1Partner2Panel);
		partner2Partner1Panel = GroupPanel.create(BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner2Partner1Panel);
		partner2Partner2Panel = GroupPanel.create(BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner2Partner2Panel);
		partner1PartnersPanel = GroupPanel.create(BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner1PartnersPanel);
		partner2PartnersPanel = GroupPanel.create(BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner2PartnersPanel);
		homeGroupPanel = GroupPanel.create(BoxPanelType.PRIMARY);
		EventBusService.subscribe(homeGroupPanel);
		childrenPanel = ChildrenPanel.create();
//		navigationPanel = GenealogyNavigationPanel.create(this);
		genealogyNavigation = new GenealogyNavigation();

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
				//remember last scroll position
				final Integer homeUnionID = extractRecordID(homeUnion);
				final int scrollBarPosition = childrenScrollPane.getHorizontalScrollBar().getValue();
				CHILDREN_SCROLLBAR_POSITION.put(homeUnionID, scrollBarPosition);
			}
		});

		//construct genealogical tree
		genealogicalTree = new GenealogicalTree(4, childrenPanel);
		genealogicalTree.addTo(0, homeGroupPanel);
		final int leftChild = GenealogicalTree.getLeftChild(0);
		genealogicalTree.addTo(leftChild, partner1PartnersPanel);
		final int rightChild = GenealogicalTree.getRightChild(0);
		genealogicalTree.addTo(rightChild, partner2PartnersPanel);
		genealogicalTree.addTo(GenealogicalTree.getLeftChild(leftChild), partner1Partner1Panel);
		genealogicalTree.addTo(GenealogicalTree.getRightChild(leftChild), partner1Partner2Panel);
		genealogicalTree.addTo(GenealogicalTree.getLeftChild(rightChild), partner2Partner1Panel);
		genealogicalTree.addTo(GenealogicalTree.getRightChild(rightChild), partner2Partner2Panel);

		setLayout(new MigLayout("insets 0",
			"[grow,center]" + GroupPanel.GROUP_SEPARATION
				+ "[grow,center]" + GroupPanel.GROUP_SEPARATION
				+ "[grow,center]" + GroupPanel.GROUP_SEPARATION
				+ "[grow,center]",
			"[]" + GENERATION_SEPARATOR_SIZE + "[]" + GENERATION_SEPARATOR_SIZE + "[]" + GENERATION_SEPARATOR_SIZE + "[]"));
		add(partner1Partner1Panel, "grow");
		add(partner1Partner2Panel, "grow");
		add(partner2Partner1Panel, "grow");
		add(partner2Partner2Panel, "grow,wrap");
		add(partner1PartnersPanel, "span 2,grow");
		add(partner2PartnersPanel, "span 2,grow,wrap");
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

	public void setPersonListener(final PersonListenerInterface personListener){
		if(generations > 3){
			partner1Partner1Panel.setPersonListener(personListener);
			partner1Partner2Panel.setPersonListener(personListener);
			partner2Partner1Panel.setPersonListener(personListener);
			partner2Partner2Panel.setPersonListener(personListener);
		}
		partner1PartnersPanel.setPersonListener(personListener);
		partner2PartnersPanel.setPersonListener(personListener);
		homeGroupPanel.setPersonListener(personListener);
		childrenPanel.setPersonListener(personListener);
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
				final Point p1p1 = partner1PartnersPanel.getPaintingPartner1EnterPoint();
				final Point p1g1p = parentGrandParentsExitingConnection(partner1Partner1Panel, graphics2D);
				grandparentsEnteringConnection(p1p1, p1g1p, graphics2D);
			}
			if(partner1Partner2Panel != null && partner1Partner2Panel.isVisible()){
				//partner1's partner2 entering connection
				final Point p1p2 = partner1PartnersPanel.getPaintingPartner2EnterPoint();
				final Point p1g2p = parentGrandParentsExitingConnection(partner1Partner2Panel, graphics2D);
				grandparentsEnteringConnection(p1p2, p1g2p, graphics2D);
			}
			if(partner2Partner1Panel != null && partner2Partner1Panel.isVisible()){
				//partner2's partner1 entering connection
				final Point p2p1 = partner2PartnersPanel.getPaintingPartner1EnterPoint();
				final Point p2g1p = parentGrandParentsExitingConnection(partner2Partner1Panel, graphics2D);
				grandparentsEnteringConnection(p2p1, p2g1p, graphics2D);
			}
			if(partner2Partner2Panel != null && partner2Partner2Panel.isVisible()){
				//partner2's partner2 entering connection
				final Point p2p2 = partner2PartnersPanel.getPaintingPartner2EnterPoint();
				final Point p2g2p = parentGrandParentsExitingConnection(partner2Partner2Panel, graphics2D);
				grandparentsEnteringConnection(p2p2, p2g2p, graphics2D);
			}

			final Point p1p = partner1PartnersPanel.getPaintingExitPoint();
			if(!partner1.isEmpty())
				//partner1's partners exiting connection
				grandparentsExitingConnection(p1p, 0, graphics2D);
			final Point p2p = partner2PartnersPanel.getPaintingExitPoint();
			if(!partner2.isEmpty())
				//partner2's partners exiting connection
				grandparentsExitingConnection(p2p, 0, graphics2D);
			if(!partner1.isEmpty()){
				final Point hfp1 = homeGroupPanel.getPaintingPartner1EnterPoint();
				//home union partner1 entering connection
				parentEnteringConnection(hfp1, graphics2D);
				//line between partner1's partners and partner1
				grandparentsToParent(p1p, hfp1, graphics2D);
			}
			if(!partner2.isEmpty()){
				final Point hfp2 = homeGroupPanel.getPaintingPartner2EnterPoint();
				//home union partner2 entering connection
				parentEnteringConnection(hfp2, graphics2D);
				//line between partner2's partners and partner2
				grandparentsToParent(p2p, hfp2, graphics2D);
			}
			final Point[] c = childrenPanel.getPaintingEnterPoints();
			if(c.length > 0){
				//home union exiting connection
				final Point hf = homeGroupPanel.getPaintingExitPoint();
				grandparentsExitingConnection(hf, ChildrenPanel.UNION_ARROW_HEIGHT, graphics2D);

				final Point origin = childrenScrollPane.getLocation();
				origin.x -= childrenScrollPane.getHorizontalScrollBar().getValue();
				//horizontal line from first to last child
				graphics2D.drawLine(origin.x + c[0].x, origin.y + c[0].y - GENERATION_SEPARATOR_SIZE / 2,
					origin.x + c[c.length - 1].x, origin.y + c[c.length - 1].y - GENERATION_SEPARATOR_SIZE / 2);
				//vertical line connecting the children
				for(int i = 0, length = c.length; i < length; i ++){
					final Point point = c[i];

					final boolean isAdopted = childrenPanel.isChildAdopted(i);
					if(isAdopted)
						graphics2D.setStroke(GroupPanel.CONNECTION_STROKE_ADOPTED);

					//vertical connection line between each child and horizontal children line
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
			//(vertical line) parents' parents' parents exiting connection
			p = parentGrandparentsPanel.getPaintingExitPoint();
			grandparentsExitingConnection(p, 0, graphics2D);
		}
		return p;
	}

	private static void grandparentsEnteringConnection(final Point g, final Point pg, final Graphics2D graphics2D){
		//(vertical) parents' parent entering connection
		parentEnteringConnection(g, graphics2D);

		if(pg != null)
			//(horizontal) line between grandparent and grandparents' parents
			grandparentsToParent(pg, g, graphics2D);
	}

	private static void grandparentsExitingConnection(final Point g, final int offset, final Graphics2D graphics2D){
		//grandparent exiting connection
		graphics2D.drawLine(g.x, g.y,
			g.x, g.y + offset + GroupPanel.GROUP_EXITING_HEIGHT + GENERATION_SEPARATOR_SIZE / 2);
	}

	private static void parentEnteringConnection(final Point p, final Graphics2D graphics2D){
		//(vertical line) parent entering connection
		graphics2D.drawLine(p.x, p.y,
			p.x, p.y - GroupPanel.NAVIGATION_ARROW_HEIGHT - GENERATION_SEPARATOR_SIZE / 2);
	}

	private static void grandparentsToParent(final Point g, final Point p, final Graphics2D graphics2D){
		//(horizontal) line between grandparent and parent
		graphics2D.drawLine(g.x, g.y + GroupPanel.GROUP_EXITING_HEIGHT + GENERATION_SEPARATOR_SIZE / 2,
			p.x, p.y - GroupPanel.NAVIGATION_ARROW_HEIGHT - GENERATION_SEPARATOR_SIZE / 2);
	}


	public void loadDataFromUnion(final Map<String, Object> homeUnion){
		loadData(homeUnion, Collections.emptyMap(), Collections.emptyMap());
	}

	public void loadDataFromPerson(final Map<String, Object> partner){
		loadData(Collections.emptyMap(), partner, Collections.emptyMap());
	}

	private String extractEarliestSex(final Integer personID){
		final Comparator<LocalDate> comparator = Comparator.naturalOrder();
		final Function<Map.Entry<LocalDate, Map<String, Object>>, String> extractor = entry -> extractRecordDescription(entry.getValue());
		return extractData(personID, List.of("sex"), comparator, extractor);
	}

	private <T> T extractData(final Integer referenceID, final Collection<String> eventTypes, final Comparator<LocalDate> comparator,
			final Function<Map.Entry<LocalDate, Map<String, Object>>, T> extractor){
		final Map<Integer, Map<String, Object>> storeEventTypes = Repository.findAllNavigable(EntityManager.NODE_EVENT_TYPE);
		final Map<Integer, Map<String, Object>> historicDates = Repository.findAllNavigable(EntityManager.NODE_HISTORIC_DATE);
		return Repository.findReferencingNodes(EntityManager.NODE_EVENT,
				EntityManager.NODE_PERSON, referenceID,
				EntityManager.RELATIONSHIP_FOR).stream()
			.filter(entry -> {
				final Integer recordTypeID = extractRecordTypeID(entry);
				final String recordType = extractRecordType(storeEventTypes.get(recordTypeID));
				return eventTypes.contains(recordType);
			})
			.map(entry -> {
				final Map<String, Object> dateEntry = historicDates.get(extractRecordDateID(entry));
				final String dateValue = extractRecordDate(dateEntry);
				final LocalDate parsedDate = DateParser.parse(dateValue);
				return (parsedDate != null? new AbstractMap.SimpleEntry<>(parsedDate, entry): null);
			})
			.filter(Objects::nonNull)
			.min(Map.Entry.comparingByKey(comparator))
			.map(extractor)
			.orElse(null);
	}

	public final void loadData(final Map<String, Object> homeUnion, final Map<String, Object> partner1, final Map<String, Object> partner2){
		prepareData(homeUnion, partner1, partner2);

		if(homeUnion.isEmpty())
//			navigationPanel.navigateToPerson(extractRecordID(partner1.isEmpty()? partner2: partner1));
			genealogyNavigation.navigateToPerson(extractRecordID(partner1.isEmpty()? partner2: partner1));
		else
//			navigationPanel.navigateToUnion(extractRecordID(this.homeUnion));
			genealogyNavigation.navigateToUnion(extractRecordID(this.homeUnion));

		loadData();
	}

	private void prepareData(Map<String, Object> homeUnion, Map<String, Object> partner1, Map<String, Object> partner2){
		if(homeUnion.isEmpty()){
			final Set<Map<String, Object>> unions = extractUnions(partner1);
			if(!unions.isEmpty()){
				//choose the last shown union, if any, otherwise choose the first
				final Map<String, Object> currentUnionDefault = getDefaultUnion(unions.stream()
					.map(EntityManager::extractRecordID)
					.collect(Collectors.toSet()));
				homeUnion = (currentUnionDefault != null? currentUnionDefault: unions.iterator().next());
			}
		}

		Set<Integer> unionPartition = null;
		if(!homeUnion.isEmpty()){
			//search current default union record
			final Integer homeUnionID = extractRecordID(homeUnion);
			unionPartition = getDefaultUnionKey(homeUnionID);
			if(unionPartition == null){
				//fill union partition:
				unionPartition = new HashSet<>();
				//extract all persons from current union
				final List<Integer> personIDsInGroup = getPersonIDsInGroup(homeUnionID);
				//extract all unions for each person
				for(final Integer personIDInGroup : personIDsInGroup)
					//extract all unions for this particular person
					getGroups(personIDInGroup).stream()
						.map(EntityManager::extractRecordID)
						.forEach(unionPartition::add);
			}
			//save current union as default
			UNION_DEFAULT.add(Map.entry(unionPartition, homeUnion));


			final List<Integer> personIDsInUnion = getPersonIDsInGroup(homeUnionID);
			Integer partner1ID = extractRecordID(partner1);
			if(partner1ID != null && !personIDsInUnion.contains(partner1ID)){
				LOGGER.warn("Person {} does not belongs to the union {} (this cannot be)", partner1ID, homeUnionID);

				partner1 = Collections.emptyMap();
			}
			Integer partner2ID = extractRecordID(partner2);
			if(partner2ID != null && !personIDsInUnion.contains(partner2ID)){
				LOGGER.warn("Person {} does not belongs to the union {} (this cannot be)", partner2ID, homeUnionID);

				partner2 = Collections.emptyMap();
			}

			if(partner1.isEmpty() || partner2.isEmpty()){
				final Map<Integer, Map<String, Object>> persons = Repository.findAllNavigable(EntityManager.NODE_PERSON);

				//extract the first two persons from the union:
				if(!partner1.isEmpty())
					personIDsInUnion.remove(extractRecordID(partner1));
				if(!partner2.isEmpty())
					personIDsInUnion.remove(extractRecordID(partner2));
				if(partner1.isEmpty() && !personIDsInUnion.isEmpty()){
					partner1ID = personIDsInUnion.getFirst();
					if(persons.containsKey(partner1ID))
						partner1 = persons.get(partner1ID);
					personIDsInUnion.remove(partner1ID);
				}
				if(partner2.isEmpty() && !personIDsInUnion.isEmpty()){
					partner2ID = personIDsInUnion.getFirst();
					if(persons.containsKey(partner2ID))
						partner2 = persons.get(partner2ID);
					personIDsInUnion.remove(partner2ID);
				}
			}
		}

		if(!partner1.isEmpty() || !partner2.isEmpty()){
			//switch partner1 with partner 2 if the last shown partner was `switched`
			final Integer homeUnionID = extractRecordID(homeUnion);
			final Map<String, Object> leftPartner = getDefaultLeftPartner(homeUnionID);
			if(leftPartner != null){
				final Integer leftPartnerID = extractRecordID(leftPartner);
				//prefer left position (`partner1`) if male or unknown, right if female (`partner2`)
				boolean switchPartner = false;
				if(!partner1.isEmpty()){
					final Integer partner1ID = extractRecordID(partner1);
					switchPartner |= (!Objects.equals(leftPartnerID, partner1ID)
						|| EntityManager.SEX_FEMALE.equalsIgnoreCase(extractEarliestSex(partner1ID)));
				}
				if(!partner2.isEmpty()){
					final Integer partner2ID = extractRecordID(partner2);
					switchPartner |= (Objects.equals(leftPartnerID, partner2ID)
						|| EntityManager.SEX_MALE.equalsIgnoreCase(extractEarliestSex(partner2ID)));
				}
				if(switchPartner){
					final Map<String, Object> tmp = partner1;
					partner1 = partner2;
					partner2 = tmp;
				}
			}
			else if(unionPartition != null && !partner1.isEmpty())
				//save current union as default
				LEFT_PARTNER_DEFAULT.add(Map.entry(unionPartition, partner1));
		}

		this.homeUnion = homeUnion;
		this.partner1 = partner1;
		this.partner2 = partner2;
	}

	private static Set<Integer> getDefaultUnionKey(final Integer unionID){
		if(unionID != null)
			for(final Map.Entry<Set<Integer>, Map<String, Object>> unionDefault : UNION_DEFAULT){
				final Set<Integer> key = unionDefault.getKey();
				if(key.contains(unionID))
					return key;
			}
		return null;
	}

	private static Map<String, Object> getDefaultUnion(final Set<Integer> unionIDs){
		for(final Map.Entry<Set<Integer>, Map<String, Object>> unionDefault : UNION_DEFAULT)
			if(!Collections.disjoint(unionDefault.getKey(), unionIDs))
				//some IDs are in common, return default union
				return unionDefault.getValue();
		return null;
	}

	private static Map<String, Object> getDefaultLeftPartner(final Integer unionID){
		if(unionID != null)
			for(final Map.Entry<Set<Integer>, Map<String, Object>> unionDefault : LEFT_PARTNER_DEFAULT)
				if(unionDefault.getKey().contains(unionID))
					return unionDefault.getValue();
		return null;
	}

	private List<Integer> getPersonIDsInGroup(final Integer groupID){
		return Repository.findReferencingNodes(EntityManager.NODE_GROUP,
				EntityManager.NODE_GROUP, groupID,
				EntityManager.RELATIONSHIP_OF, EntityManager.PROPERTY_ROLE, EntityManager.GROUP_ROLE_PARTNER).stream()
			.map(EntityManager::extractRecordID)
			.toList();
	}

	private void loadData(){
		final Integer partner1ParentsID = extractParentsGroupID(partner1);
		final Integer partner2ParentsID = extractParentsGroupID(partner2);
		if(generations > 3){
			final List<Integer> personIDsInGroup1 = getPartnerIDs(partner1ParentsID);
			final int personInGroup1Count = personIDsInGroup1.size();
			final Map<Integer, Map<String, Object>> persons = Repository.findAllNavigable(EntityManager.NODE_PERSON);
			final Map<String, Object> partner1Partner1 = (personInGroup1Count > 0
				? persons.get(personIDsInGroup1.get(0))
				: Collections.emptyMap());
			final Map<String, Object> partner1Partner2 = (personInGroup1Count > 1
				? persons.get(personIDsInGroup1.get(1))
				: Collections.emptyMap());
			final Integer partner1Partner1ParentsID = extractParentsGroupID(partner1Partner1);
			final Integer partner1Partner2ParentsID = extractParentsGroupID(partner1Partner2);

			final List<Integer> personIDsInGroup2 = getPartnerIDs(partner2ParentsID);
			final int personInGroup2Count = personIDsInGroup2.size();
			final Map<String, Object> partner2Partner1 = (personInGroup2Count > 0
				? persons.get(personIDsInGroup2.get(0))
				: Collections.emptyMap());
			final Map<String, Object> partner2Partner2 = (personInGroup2Count > 1
				? persons.get(personIDsInGroup2.get(1))
				: Collections.emptyMap());
			final Integer partner2Partner1ParentsID = extractParentsGroupID(partner2Partner1);
			final Integer partner2Partner2ParentsID = extractParentsGroupID(partner2Partner2);

			partner1Partner1Panel.loadData(partner1Partner1ParentsID);
			partner1Partner1Panel.setVisible(partner1ParentsID != null);
			partner1Partner2Panel.loadData(partner1Partner2ParentsID);
			partner1Partner2Panel.setVisible(partner1ParentsID != null);
			partner2Partner1Panel.loadData(partner2Partner1ParentsID);
			partner2Partner1Panel.setVisible(partner2ParentsID != null);
			partner2Partner2Panel.loadData(partner2Partner2ParentsID);
			partner2Partner2Panel.setVisible(partner2ParentsID != null);
		}
		partner1PartnersPanel.loadData(partner1ParentsID);
		partner1PartnersPanel.setVisible(!partner1.isEmpty());
		partner2PartnersPanel.loadData(partner2ParentsID);
		partner2PartnersPanel.setVisible(!partner2.isEmpty());
		homeGroupPanel.loadData(homeUnion, partner1, partner2);
		final Integer homeUnionID = extractRecordID(homeUnion);
		childrenPanel.loadData(homeUnionID);


		if(!homeUnion.isEmpty())
			scrollChildrenToLastPosition(homeUnionID);
	}

	private void scrollChildrenToLastPosition(final Integer unionID){
		//restore last scroll position
		final Integer childrenScrollbarPosition = CHILDREN_SCROLLBAR_POSITION.get(unionID);
		//if a child was selected, bring it to view
		final JViewport childrenViewport = childrenScrollPane.getViewport();
		final int scrollbarPositionX = (childrenScrollbarPosition == null?
			//center halfway if it's the first time the children are painted
			(childrenPanel.getPreferredSize().width - childrenViewport.getWidth()) / 4 - 7:
			childrenScrollbarPosition);
		childrenViewport.setViewPosition(new Point(scrollbarPositionX, 0));
	}

	public void refresh(){
		final Integer homeUnionID = extractRecordID(homeUnion);
		final Map<String, Object> union = (homeUnionID != null && Repository.findByID(EntityManager.NODE_GROUP, homeUnionID) != null
			? Collections.emptyMap()
			: homeUnion);
		loadData(union, partner1, partner2);
	}

	private void updateDisplay(){
		final Integer lastPersonID = genealogyNavigation.getLastPersonID();
		final Integer lastUnionID = genealogyNavigation.getLastUnionID();

		Map<String, Object> lastPerson = Collections.emptyMap();
		if(lastPersonID != null)
			lastPerson = Repository.findByID(EntityManager.NODE_PERSON, lastPersonID);
		Map<String, Object> lastUnion = Collections.emptyMap();
		if(lastUnionID != null)
			lastUnion = Repository.findByID(EntityManager.NODE_GROUP, lastUnionID);
		prepareData(lastUnion, lastPerson, Collections.emptyMap());

		loadData();
	}

	@Override
	public void onRecordSelect(final String table, final Integer id){
		searchDialog.setVisible(false);

		switch(table){
			case "person" -> {
				final Map<String, Object> person = Repository.findByID(EntityManager.NODE_PERSON, id);
				loadData(Collections.emptyMap(), person, Collections.emptyMap());
			}
			case "group" -> {
				final Map<String, Object> union = Repository.findByID(EntityManager.NODE_GROUP, id);
				loadData(union, Collections.emptyMap(), Collections.emptyMap());
			}
		}
	}

	@Override
	public void onRecordEdit(final String table, final Integer id){
		System.out.println("onRecordEdit " + table + " " + id);

		CommonListDialog recordDialog = null;
		switch(table){
			case "repository" -> recordDialog = RepositoryDialog.createEditOnly(null);
			case "source" -> recordDialog = SourceDialog.createEditOnly(null);
			case "citation" -> recordDialog = CitationDialog.createEditOnly(null);
			case "place" -> recordDialog = PlaceDialog.createEditOnly(null);
			case "media" -> recordDialog = MediaDialog.createEditOnly(null)
				.withBasePath(FileHelper.documentsDirectory());
			case "note" -> recordDialog = NoteDialog.createEditOnly(null);
			case "person" -> recordDialog = PersonDialog.createEditOnly(null);
			case "group" -> recordDialog = GroupDialog.createEditOnly(null);
			case "event" -> recordDialog = EventDialog.createEditOnly(null);
			case "cultural_norm" -> recordDialog = CulturalNormDialog.createEditOnly(null);
			case "research_status" -> recordDialog = ResearchStatusDialog.createEditOnly(null);
		}
		if(recordDialog != null){
			recordDialog.loadData(id);

			recordDialog.showDialog();
		}
	}


	private List<Integer> getPartnerIDs(final Integer partnerID){
		return Repository.findReferencingNodes(EntityManager.NODE_GROUP,
				EntityManager.NODE_PERSON, partnerID,
				EntityManager.RELATIONSHIP_OF, EntityManager.PROPERTY_ROLE, EntityManager.GROUP_ROLE_PARTNER).stream()
			.map(EntityManager::extractRecordID)
			.toList();
	}

	static Integer extractParentsGroupID(final Map<String, Object> child){
		Integer parentsGroupID = null;
		if(!child.isEmpty()){
			final Integer childID = extractRecordID(child);
			//prefer biological family
			final List<Integer> parentsIDs = getParentsIDs(childID, EntityManager.GROUP_ROLE_CHILD);
			if(parentsIDs.size() > 1)
				LOGGER.warn("Person {} belongs to more than one parents (this cannot be), select the first and hope for the best", childID);

			final Integer parentsID = (!parentsIDs.isEmpty()? parentsIDs.getFirst(): null);
			if(parentsID != null)
				parentsGroupID = parentsID;
			else{
				//prefer first adopting family
				final List<Integer> unionIDs = getParentsIDs(childID, EntityManager.GROUP_ROLE_ADOPTEE);
				if(!unionIDs.isEmpty())
					parentsGroupID = unionIDs.getFirst();
			}
		}
		return parentsGroupID;
	}

	private Set<Map<String, Object>> extractUnions(final Map<String, Object> person){
		final Set<Map<String, Object>> unionGroups = new HashSet<>(0);
		if(!person.isEmpty()){
			final Integer personID = extractRecordID(person);
			unionGroups.addAll(getGroups(personID));
		}
		return unionGroups;
	}

	private List<Map<String, Object>> getGroups(final Integer personID){
		return Repository.findReferencingNodes(EntityManager.NODE_GROUP,
			EntityManager.NODE_PERSON, personID,
			EntityManager.RELATIONSHIP_OF, EntityManager.PROPERTY_ROLE, EntityManager.GROUP_ROLE_PARTNER);
	}

	private static List<Integer> getParentsIDs(final Integer personID, final String personRole){
		return Repository.findReferencingNodes(EntityManager.NODE_GROUP,
				EntityManager.NODE_PERSON, personID,
				EntityManager.RELATIONSHIP_OF, EntityManager.PROPERTY_ROLE, personRole).stream()
			.map(EntityManager::extractRecordID)
			.toList();
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();
		Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);

		final Map<String, Object> group1 = new HashMap<>();
		group1.put("type", "family");
		Repository.upsert(group1, EntityManager.NODE_GROUP);
		final Map<String, Object> group2 = new HashMap<>();
		group2.put("type", "family");
		Repository.upsert(group2, EntityManager.NODE_GROUP);

		final Map<String, Object> groupJunction11 = new HashMap<>();
		groupJunction11.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_GROUP, extractRecordID(group1), EntityManager.NODE_PERSON, 1,
			EntityManager.RELATIONSHIP_OF, groupJunction11, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction2 = new HashMap<>();
		groupJunction2.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_GROUP, extractRecordID(group1), EntityManager.NODE_PERSON, 2,
			EntityManager.RELATIONSHIP_OF, groupJunction2, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction13 = new HashMap<>();
		groupJunction13.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_GROUP, extractRecordID(group2), EntityManager.NODE_PERSON, 1,
			EntityManager.RELATIONSHIP_OF, groupJunction13, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction3 = new HashMap<>();
		groupJunction3.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_GROUP, extractRecordID(group2), EntityManager.NODE_PERSON, 3,
			EntityManager.RELATIONSHIP_OF, groupJunction3, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction4 = new HashMap<>();
		groupJunction4.put("role", "child");
		Repository.upsertRelationship(EntityManager.NODE_GROUP, extractRecordID(group1), EntityManager.NODE_PERSON, 4,
			EntityManager.RELATIONSHIP_OF, groupJunction4, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction5 = new HashMap<>();
		groupJunction5.put("role", "child");
		Repository.upsertRelationship(EntityManager.NODE_GROUP, extractRecordID(group1), EntityManager.NODE_PERSON, 5,
			EntityManager.RELATIONSHIP_OF, groupJunction5, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction6 = new HashMap<>();
		groupJunction6.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_GROUP, extractRecordID(group2), EntityManager.NODE_PERSON, 4,
			EntityManager.RELATIONSHIP_OF, groupJunction6, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> event1 = new HashMap<>();
		event1.put("type_id", 1);
event1.put("reference_table", "person");
event1.put("reference_id", 5);
		Repository.upsert(event1, EntityManager.NODE_EVENT);

		final Map<String, Object> eventType1 = new HashMap<>();
		eventType1.put("type", "adoption");
		eventType1.put("category", "adoption");
		Repository.upsert(eventType1, EntityManager.NODE_EVENT_TYPE);

		final GroupListenerInterface unionListener = new GroupListenerInterface(){
			@Override
			public void onGroupEdit(final GroupPanel groupPanel){
				final Map<String, Object> group = groupPanel.getUnion();
				System.out.println("onEditGroup " + extractRecordID(group));
			}

			@Override
			public void onGroupAdd(final GroupPanel groupPanel){
				final PersonPanel partner1 = groupPanel.getPartner1();
				final PersonPanel partner2 = groupPanel.getPartner2();
				System.out.println("onAddGroup (partner 1 " + extractRecordID(partner1.getPerson())
					+ ", partner 2 " + extractRecordID(partner2.getPerson())
					+ ")");
			}

			@Override
			public void onGroupLink(final GroupPanel groupPanel){
				final PersonPanel partner1 = groupPanel.getPartner1();
				final PersonPanel partner2 = groupPanel.getPartner2();
				final Map<String, Object> group = groupPanel.getUnion();
				System.out.println("onLinkPersonToSiblingGroup (partner 1: " + extractRecordID(partner1.getPerson())
					+ ", partner 2: " + extractRecordID(partner2.getPerson()) + ", group: " + extractRecordID(group));
			}

			@Override
			public void onGroupRemove(final GroupPanel groupPanel){
				final Map<String, Object> group = groupPanel.getUnion();
				System.out.println("onRemoveGroup " + extractRecordID(group));
			}

			@Override
			public void onPersonChangeParents(final GroupPanel groupPanel, final PersonPanel personPanel, final Map<String, Object> newParents){
				System.out.println("onGroupChangeParents person: " + extractRecordID(personPanel.getPerson())
					+ ", new parents: " + extractRecordID(newParents));
			}

			@Override
			public void onPersonChangeUnion(final GroupPanel groupPanel, final PersonPanel oldPartner, final Map<String, Object> newPartner,
					final Map<String, Object> newUnion){
				final Map<String, Object> oldUnion = groupPanel.getUnion();
				System.out.println("onPersonChangeUnion old partner: " + extractRecordID(oldPartner.getPerson())
					+ ", old union: " + oldUnion.get("id") + ", new partner: " + extractRecordID(newPartner)
					+ ", new union: " + extractRecordID(newUnion));
			}
		};
		final PersonListenerInterface personListener = new PersonListenerInterface(){
			@Override
			public void onPersonFocus(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onFocusPerson " + extractRecordID(person));
			}

			@Override
			public void onPersonEdit(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onEditPerson " + extractRecordID(person));
			}

			@Override
			public void onPersonLink(final PersonPanel personPanel){
				System.out.println("onLinkPerson");
			}

			@Override
			public void onPersonAdd(final PersonPanel personPanel){
				System.out.println("onAddPerson");
			}

			@Override
			public void onPersonRemove(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onRemovePerson " + extractRecordID(person));
			}

			@Override
			public void onPersonUnlinkFromParentGroup(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onUnlinkPersonFromParentGroup " + extractRecordID(person));
			}

			@Override
			public void onPersonAddToSiblingGroup(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onAddToSiblingGroupPerson " + extractRecordID(person));
			}

			@Override
			public void onPersonUnlinkFromSiblingGroup(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onUnlinkPersonFromSiblingGroup " + extractRecordID(person));
			}

			@Override
			public void onPersonAddPreferredImage(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onAddPreferredImage " + extractRecordID(person));
			}

			@Override
			public void onPersonEditPreferredImage(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onEditPreferredImage " + extractRecordID(person));
			}
		};


		EventQueue.invokeLater(() -> {
			final TreePanel panel = create(4, null);
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
					System.out.println(Repository.logDatabase());

					System.exit(0);
				}
			});
			frame.setSize(1200, 500);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
