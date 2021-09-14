/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.panels;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.gedcom.parsers.calendars.DateParser;
import io.github.mtrevisan.familylegacy.ui.enums.BoxPanelType;
import io.github.mtrevisan.familylegacy.ui.enums.SelectedNodeType;
import io.github.mtrevisan.familylegacy.ui.interfaces.FamilyListenerInterface;
import io.github.mtrevisan.familylegacy.ui.interfaces.IndividualListenerInterface;
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
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


public class TreePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 4700955059623460223L;

	private static final Logger LOGGER = LoggerFactory.getLogger(TreePanel.class);

	private static final Color BACKGROUND_COLOR_APPLICATION = new Color(242, 238, 228);

	private static final int GENERATION_SEPARATOR_SIZE = 40;

	private static final Map<String, Integer> CHILDREN_SCROLLBAR_POSITION = new HashMap<>(0);


	private FamilyPanel parent1Parents1Panel;
	private FamilyPanel parent1Parents2Panel;
	private FamilyPanel parent2Parents1Panel;
	private FamilyPanel parent2Parents2Panel;
	private FamilyPanel parent1ParentsPanel;
	private FamilyPanel parent2ParentsPanel;
	private FamilyPanel homeFamilyPanel;
	private JScrollPane childrenScrollPane;
	private ChildrenPanel childrenPanel;

	private GedcomNode parent1;
	private GedcomNode parent2;
	private GedcomNode homeFamily;
	private final int generations;
	private final Flef store;
	private final IndividualListenerInterface individualListener;
	private final FamilyListenerInterface familyListener;


	public TreePanel(final GedcomNode homeFamily, final int generations, final Flef store, final FamilyListenerInterface familyListener,
			final IndividualListenerInterface individualListener){
		this.homeFamily = homeFamily;
		this.generations = generations;
		this.store = store;
		this.individualListener = individualListener;
		this.familyListener = familyListener;

		if(generations <= 3)
			initComponents3Generations(homeFamily);
		else
			initComponents4Generations(homeFamily);

		loadData();
	}

	private void initComponents3Generations(final GedcomNode family){
		parent1 = (family != null? store.getParent1(family): null);
		parent2 = (family != null? store.getParent2(family): null);

		final GedcomNode parent1Parents = extractParents(parent1);
		final GedcomNode parent2Parents = extractParents(parent2);

		GedcomNode childReference = extractFirstChild(parent1Parents, parent1);
		parent1ParentsPanel = new FamilyPanel(null, null, parent1Parents, childReference, store, BoxPanelType.SECONDARY,
			familyListener, individualListener);
		childReference = extractFirstChild(parent2Parents, parent2);
		parent2ParentsPanel = new FamilyPanel(null, null, parent2Parents, childReference, store, BoxPanelType.SECONDARY,
			familyListener, individualListener);
		childReference = extractFirstChild(homeFamily, null);
		homeFamilyPanel = new FamilyPanel(parent1, parent2, homeFamily, childReference, store, BoxPanelType.PRIMARY,
			familyListener, individualListener);
		childrenPanel = new ChildrenPanel(homeFamily, store, individualListener);

		setBackground(BACKGROUND_COLOR_APPLICATION);

		childrenScrollPane = new JScrollPane(new ScrollableContainerHost(childrenPanel, ScrollableContainerHost.ScrollType.VERTICAL));
		childrenScrollPane.setOpaque(false);
		childrenScrollPane.getViewport().setOpaque(false);
		childrenScrollPane.setBorder(null);
		childrenScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		childrenScrollPane.setAutoscrolls(true);
		//trigger repaint in order to move the connections between children and home family
		childrenScrollPane.getHorizontalScrollBar().addAdjustmentListener(e -> {
			//remember last scroll position, restore it if present
			CHILDREN_SCROLLBAR_POSITION.put(homeFamily.getID(), childrenScrollPane.getHorizontalScrollBar().getValue());

			repaint();
		});

		setLayout(new MigLayout("insets 0",
			"[grow,center]" + FamilyPanel.FAMILY_SEPARATION + "[grow,center]",
			"[]" + GENERATION_SEPARATOR_SIZE + "[]" + GENERATION_SEPARATOR_SIZE + "[]"));
		add(parent1ParentsPanel, "growx 50");
		add(parent2ParentsPanel, "growx 50,wrap");
		add(homeFamilyPanel, "span 2,wrap");
		add(childrenScrollPane, "span 2");
	}

	private void initComponents4Generations(final GedcomNode family){
		parent1 = (family != null? store.getParent1(family): null);
		parent2 = (family != null? store.getParent2(family): null);

		final GedcomNode parent1Parents = extractParents(parent1);
		final GedcomNode parent2Parents = extractParents(parent2);

		final GedcomNode parent1Parent1 = store.getParent1(parent1Parents);
		final GedcomNode parent1Parent2 = store.getParent2(parent1Parents);
		final GedcomNode parent1Parent1Parents = extractParents(parent1Parent1);
		final GedcomNode parent1Parent2Parents = extractParents(parent1Parent2);

		final GedcomNode parent2Parent1 = store.getParent1(parent2Parents);
		final GedcomNode parent2Parent2 = store.getParent2(parent2Parents);
		final GedcomNode parent2Parent1Parents = extractParents(parent2Parent1);
		final GedcomNode parent2Parent2Parents = extractParents(parent2Parent2);

		GedcomNode defaultChildReference = store.getParent1(parent1Parents);
		GedcomNode childReference = extractFirstChild(parent1Parent1Parents, defaultChildReference);
		parent1Parents1Panel = new FamilyPanel(null, null, parent1Parent1Parents, childReference, store,
			BoxPanelType.SECONDARY, familyListener, individualListener);
		defaultChildReference = store.getParent2(parent1Parents);
		childReference = extractFirstChild(parent1Parent2Parents, defaultChildReference);
		parent1Parents2Panel = new FamilyPanel(null, null, parent1Parent2Parents, childReference, store,
			BoxPanelType.SECONDARY, familyListener, individualListener);
		defaultChildReference = store.getParent1(parent2Parents);
		childReference = extractFirstChild(parent2Parent1Parents, defaultChildReference);
		parent2Parents1Panel = new FamilyPanel(null, null, parent2Parent1Parents, childReference, store,
			BoxPanelType.SECONDARY, familyListener, individualListener);
		defaultChildReference = store.getParent2(parent2Parents);
		childReference = extractFirstChild(parent2Parent2Parents, defaultChildReference);
		parent2Parents2Panel = new FamilyPanel(null, null, parent2Parent2Parents, childReference, store,
			BoxPanelType.SECONDARY, familyListener, individualListener);
		childReference = extractFirstChild(parent1Parents, parent1);
		parent1ParentsPanel = new FamilyPanel(null, null, parent1Parents, childReference, store, BoxPanelType.SECONDARY,
			familyListener, individualListener);
		childReference = extractFirstChild(parent2Parents, parent2);
		parent2ParentsPanel = new FamilyPanel(null, null, parent2Parents, childReference, store, BoxPanelType.SECONDARY,
			familyListener, individualListener);
		childReference = extractFirstChild(homeFamily, null);
		homeFamilyPanel = new FamilyPanel(parent1, parent2, homeFamily, childReference, store, BoxPanelType.PRIMARY, familyListener,
			individualListener);
		childrenPanel = new ChildrenPanel(homeFamily, store, individualListener);

		setBackground(BACKGROUND_COLOR_APPLICATION);

		childrenScrollPane = new JScrollPane(new ScrollableContainerHost(childrenPanel, ScrollableContainerHost.ScrollType.HORIZONTAL));
		childrenScrollPane.setOpaque(false);
		childrenScrollPane.getViewport().setOpaque(false);
		childrenScrollPane.setBorder(null);
		childrenScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		childrenScrollPane.setAutoscrolls(true);
		//trigger repaint in order to move the connections between children and home family
		childrenScrollPane.getHorizontalScrollBar().addAdjustmentListener(e -> {
			if(homeFamily != null){
				//remember last scroll position, restore it if present
				CHILDREN_SCROLLBAR_POSITION.put(homeFamily.getID(), childrenScrollPane.getHorizontalScrollBar().getValue());

				repaint();
			}
		});

		setLayout(new MigLayout("insets 0",
			"[grow,center]" + FamilyPanel.FAMILY_SEPARATION + "[grow,center]" + FamilyPanel.FAMILY_SEPARATION
				+ "[grow,center]" + FamilyPanel.FAMILY_SEPARATION + "[grow,center]",
			"[]" + GENERATION_SEPARATOR_SIZE + "[]" + GENERATION_SEPARATOR_SIZE + "[]" + GENERATION_SEPARATOR_SIZE + "[]"));
		add(parent1Parents1Panel, "growx 25");
		add(parent1Parents2Panel, "growx 25");
		add(parent2Parents1Panel, "growx 25");
		add(parent2Parents2Panel, "growx 25,wrap");
		add(parent1ParentsPanel, "span 2,growx 50");
		add(parent2ParentsPanel, "span 2,growx 50,wrap");
		add(homeFamilyPanel, "span 4,wrap");
		add(childrenScrollPane, "span 4,alignx center");
	}

	private GedcomNode extractFirstChild(final GedcomNode family, GedcomNode defaultChild){
		final List<GedcomNode> childrenReference = (family != null? family.getChildrenWithTag("CHILD"):
			Collections.emptyList());
		if(family != null && defaultChild == null){
			final List<GedcomNode> children = store.traverseAsList(family, "CHILD[]");
			defaultChild = (!children.isEmpty()? children.get(0): null);
		}
		return (!childrenReference.isEmpty()? childrenReference.get(0): defaultChild);
	}

	private GedcomNode extractParents(final GedcomNode child){
		if(child != null && !child.isEmpty()){
			final List<GedcomNode> familyChilds = store.traverseAsList(child, "FAMILY_CHILD[]");
			final Collection<GedcomNode> biologicalFamilyChilds = extractBiologicalFamilyChilds(familyChilds);
			if(!biologicalFamilyChilds.isEmpty()){
				familyChilds.clear();
				familyChilds.addAll(biologicalFamilyChilds);
			}

			if(familyChilds.size() > 1)
				LOGGER.warn("Individual {} belongs to more than one family (this cannot be), select the first and hope for the best",
					child.getID());
			if(!familyChilds.isEmpty())
				return store.getFamily(familyChilds.get(0).getXRef());
		}
		return null;
	}

	private Collection<GedcomNode> extractBiologicalFamilyChilds(final Collection<GedcomNode> familyChilds){
		final Collection<GedcomNode> biologicalFamilyChilds = new ArrayList<>(familyChilds.size());
		//check pedigree (prefers `biological` or <null>)
		for(final GedcomNode familyChild : familyChilds){
			final String pedigree1 = store.traverse(familyChild, "PEDIGREE.PARENT1").getValue();
			if(pedigree1 == null || "biological".equalsIgnoreCase(pedigree1))
				biologicalFamilyChilds.add(familyChild);
			else{
				final String pedigree2 = store.traverse(familyChild, "PEDIGREE.PARENT2").getValue();
				if(pedigree2 == null || "biological".equalsIgnoreCase(pedigree2))
					biologicalFamilyChilds.add(familyChild);
			}
		}
		return biologicalFamilyChilds;
	}

	public GedcomNode getPreferredFamily(final GedcomNode individual){
		GedcomNode family = null;
		//see if this individual belongs to a family
		List<GedcomNode> families = extractFamilies(individual);
		if(families.size() > 1){
			//if it belongs to more than one family, select those with the oldest event
			LocalDate oldestDate = null;
			final List<GedcomNode> oldestFamilies = new ArrayList<>(0);
			for(final GedcomNode f : families){
				final LocalDate oldestEventDate = extractOldestEventDate(f);
				if(oldestEventDate != null){
					final int cmp;
					if(oldestDate == null || (cmp = oldestEventDate.compareTo(oldestDate)) < 0){
						oldestDate = oldestEventDate;
						oldestFamilies.clear();
						oldestFamilies.add(f);
					}
					else if(cmp == 0)
						oldestFamilies.add(f);
				}
			}
			if(oldestFamilies.size() == 1)
				family = oldestFamilies.get(0);
			else{
				//choose the one with the lowest ID
				if(!oldestFamilies.isEmpty())
					families = oldestFamilies;
				final Map<Integer, GedcomNode> all = new TreeMap<>();
				for(final GedcomNode fam : families)
					all.put(Integer.parseInt(fam.getID().substring(1)), fam);
				family = all.values().iterator().next();
			}
		}
		else if(families.size() == 1)
			//the individual belongs to exact one family, choose it and load as the primary family
			family = families.get(0);
		return family;
	}

	private List<GedcomNode> extractFamilies(final GedcomNode individual){
		final List<GedcomNode> familyXRefs = store.traverseAsList(individual, "FAMILY_PARENT[]");
		final List<GedcomNode> families = new ArrayList<>(familyXRefs.size());
		for(final GedcomNode familyXRef : familyXRefs)
			families.add(store.getFamily(familyXRef.getXRef()));
		return families;
	}

	private LocalDate extractOldestEventDate(final GedcomNode node){
		final List<GedcomNode> events = store.traverseAsList(node, "EVENT[]");
		final SortedMap<LocalDate, GedcomNode> dateEvent = new TreeMap<>();
		for(final GedcomNode event : events){
			final GedcomNode eventDate = store.traverse(event, "DATE");
			if(!eventDate.isEmpty())
				dateEvent.put(DateParser.parse(eventDate.getValue()), event);
		}
		return (!dateEvent.isEmpty()? dateEvent.keySet().iterator().next(): null);
	}

	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			graphics2D.setStroke(FamilyPanel.CONNECTION_STROKE);

			if(parent1Parents1Panel != null && parent1Parents1Panel.isVisible()){
				//parent1's parent1 entering connection
				final Point p1p1 = parent1ParentsPanel.getFamilyPaintingParent1EnterPoint();
				final Point p1g1p = parentGrandParentsExitingConnection(parent1Parents1Panel, graphics2D);
				grandparentsEnteringConnection(p1p1, p1g1p, graphics2D);
			}
			if(parent1Parents2Panel != null && parent1Parents2Panel.isVisible()){
				//parent1's parent2 entering connection
				final Point p1p2 = parent1ParentsPanel.getFamilyPaintingParent2EnterPoint();
				final Point p1g2p = parentGrandParentsExitingConnection(parent1Parents2Panel, graphics2D);
				grandparentsEnteringConnection(p1p2, p1g2p, graphics2D);
			}
			if(parent2Parents1Panel != null && parent2Parents1Panel.isVisible()){
				//parent2's parent1 entering connection
				final Point p2p1 = parent2ParentsPanel.getFamilyPaintingParent1EnterPoint();
				final Point p2g1p = parentGrandParentsExitingConnection(parent2Parents1Panel, graphics2D);
				grandparentsEnteringConnection(p2p1, p2g1p, graphics2D);
			}
			if(parent2Parents2Panel != null && parent2Parents2Panel.isVisible()){
				//parent2's parent2 entering connection
				final Point p2p2 = parent2ParentsPanel.getFamilyPaintingParent2EnterPoint();
				final Point p2g2p = parentGrandParentsExitingConnection(parent2Parents2Panel, graphics2D);
				grandparentsEnteringConnection(p2p2, p2g2p, graphics2D);
			}

			final Point p1p = parent1ParentsPanel.getFamilyPaintingExitPoint();
			if(parent1 != null)
				//parent1's parents exiting connection
				grandparentsExitingConnection(p1p, 0, graphics2D);
			final Point p2p = parent2ParentsPanel.getFamilyPaintingExitPoint();
			if(parent2 != null)
				//parent2's parents exiting connection
				grandparentsExitingConnection(p2p, 0, graphics2D);
			if(parent1 != null){
				final Point hfp1 = homeFamilyPanel.getFamilyPaintingParent1EnterPoint();
				//home family parent1 entering connection
				parentEnteringConnection(hfp1, FamilyPanel.NAVIGATION_ARROW_HEIGHT, graphics2D);
				//line between parent1's parents and parent1
				grandparentsToParent(p1p, hfp1, FamilyPanel.NAVIGATION_ARROW_HEIGHT, graphics2D);
			}
			if(parent2 != null){
				final Point hfp2 = homeFamilyPanel.getFamilyPaintingParent2EnterPoint();
				//home family parent2 entering connection
				parentEnteringConnection(hfp2, FamilyPanel.NAVIGATION_ARROW_HEIGHT, graphics2D);
				//line between parent2's parents and parent2
				grandparentsToParent(p2p, hfp2, FamilyPanel.NAVIGATION_ARROW_HEIGHT, graphics2D);
			}
			final Point[] c = childrenPanel.getChildrenPaintingEnterPoints();
			if(c.length > 0){
				//home family exiting connection
				final Point hf = homeFamilyPanel.getFamilyPaintingExitPoint();
				grandparentsExitingConnection(hf, ChildrenPanel.FAMILY_ARROW_HEIGHT, graphics2D);

				final Point origin = childrenScrollPane.getLocation();
				origin.x -= childrenScrollPane.getHorizontalScrollBar().getValue();
				//horizontal line from first to last child
				graphics2D.drawLine(origin.x + c[0].x, origin.y + c[0].y - GENERATION_SEPARATOR_SIZE / 2,
					origin.x + c[c.length - 1].x, origin.y + c[c.length - 1].y - GENERATION_SEPARATOR_SIZE / 2);
				//vertical line connecting the children
				for(final Point point : c)
					graphics2D.drawLine(origin.x + point.x, origin.y + point.y,
						origin.x + point.x, origin.y + point.y - GENERATION_SEPARATOR_SIZE / 2);
			}

			graphics2D.dispose();
		}
	}

	private Point parentGrandParentsExitingConnection(final FamilyPanel parentGrandparentsPanel, final Graphics2D graphics2D){
		Point p = null;
		if(parentGrandparentsPanel != null){
			//parent's parent's parent exiting connection
			p = parentGrandparentsPanel.getFamilyPaintingExitPoint();
			grandparentsExitingConnection(p, 0, graphics2D);
		}
		return p;
	}

	private void grandparentsEnteringConnection(final Point g, final Point pg, final Graphics2D graphics2D){
		//parent's parent entering connection
		parentEnteringConnection(g, 0, graphics2D);

		if(pg != null)
			//line between grandparent and grandparent's parents
			grandparentsToParent(pg, g, 0, graphics2D);
	}

	private void grandparentsExitingConnection(final Point g, final int offset, final Graphics2D graphics2D){
		//grandparent exiting connection
		graphics2D.drawLine(g.x, g.y,
			g.x, g.y + offset + FamilyPanel.FAMILY_EXITING_HEIGHT + GENERATION_SEPARATOR_SIZE / 2);
	}

	private void parentEnteringConnection(final Point p, final int offset, final Graphics2D graphics2D){
		//parent entering connection
		graphics2D.drawLine(p.x, p.y + FamilyPanel.NAVIGATION_ARROW_HEIGHT + offset,
			p.x, p.y - offset - GENERATION_SEPARATOR_SIZE / 2);
	}

	private void grandparentsToParent(final Point g, final Point p, final int offset, final Graphics2D graphics2D){
		//line between grandparent and parent
		graphics2D.drawLine(g.x, g.y + FamilyPanel.FAMILY_EXITING_HEIGHT + GENERATION_SEPARATOR_SIZE / 2,
			p.x, p.y - offset - GENERATION_SEPARATOR_SIZE / 2);
	}


	public void loadData(final GedcomNode parent1, final GedcomNode parent2, final GedcomNode homeFamily){
		this.parent1 = parent1;
		this.parent2 = parent2;
		this.homeFamily = homeFamily;

		loadData();
	}

	private void loadData(){
		parent1 = (parent1 == null && homeFamily != null? store.getParent1(homeFamily): parent1);
		parent2 = (parent2 == null && homeFamily != null? store.getParent2(homeFamily): parent2);

		final GedcomNode parent1Parents = extractParents(parent1);
		final GedcomNode parent2Parents = extractParents(parent2);

		if(generations > 3){
			final GedcomNode parent1Parent1 = store.getParent1(parent1Parents);
			final GedcomNode parent1Parent2 = store.getParent2(parent1Parents);
			final GedcomNode parent1Grandparents1 = extractParents(parent1Parent1);
			final GedcomNode parent1Grandparents2 = extractParents(parent1Parent2);

			final GedcomNode parent2Parent1 = store.getParent1(parent2Parents);
			final GedcomNode parent2Parent2 = store.getParent2(parent2Parents);
			final GedcomNode parent2Grandparents1 = extractParents(parent2Parent1);
			final GedcomNode parent2Grandparents2 = extractParents(parent2Parent2);

			parent1Parents1Panel.setVisible(!store.traverse(parent1Parents, "PARENT1").isEmpty());
			parent1Parents2Panel.setVisible(!store.traverse(parent1Parents, "PARENT2").isEmpty());
			parent2Parents1Panel.setVisible(!store.traverse(parent2Parents, "PARENT1").isEmpty());
			parent2Parents2Panel.setVisible(!store.traverse(parent2Parents, "PARENT2").isEmpty());
			parent1Parents1Panel.loadData(parent1Grandparents1);
			parent1Parents2Panel.loadData(parent1Grandparents2);
			parent2Parents1Panel.loadData(parent2Grandparents1);
			parent2Parents2Panel.loadData(parent2Grandparents2);
		}
		parent1ParentsPanel.setVisible(parent1 != null);
		parent2ParentsPanel.setVisible(parent2 != null);
		parent1ParentsPanel.loadData(parent1Parents);
		parent2ParentsPanel.loadData(parent2Parents);
		homeFamilyPanel.loadData(parent1, parent2, homeFamily);
		childrenPanel.loadData(homeFamily);


		if(homeFamily != null){
			//remember last scroll position, restore it if present
			final Integer childrenScrollbarPosition = CHILDREN_SCROLLBAR_POSITION.get(homeFamily.getID());
			//center halfway if it's the first time the children are painted
			final int scrollbarPositionX = (childrenScrollbarPosition == null?
				(childrenPanel.getPreferredSize().width - childrenScrollPane.getViewport().getWidth()) / 4 - 7:
				childrenScrollbarPosition);
			childrenScrollPane.getViewport().setViewPosition(new Point(scrollbarPositionX, 0));
		}
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Store storeGedcom = new Gedcom();
		final Flef storeFlef = (Flef)storeGedcom.load("/gedg/gedcom_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged")
			.transform();
		final GedcomNode family = storeFlef.getFamilies().get(0);
//		final GedcomNode family = storeFlef.getFamilies().get(4);
//		final GedcomNode family = storeFlef.getFamilies().get(9);
//		final GedcomNode family = storeFlef.getFamilies().get(64);
//		final GedcomNode family = storeFlef.getFamilies().get(75);
//		final GedcomNode family = storeFlef.getFamily("F585");
//		final GedcomNode family = storeFlef.getFamily("F267");
//		GedcomNode family = null;

		final FamilyListenerInterface familyListener = new FamilyListenerInterface(){
			@Override
			public void onFamilyEdit(final FamilyPanel boxPanel, final GedcomNode family){
				System.out.println("onEditFamily " + family.getID());
			}

			@Override
			public void onFamilyLink(final FamilyPanel boxPanel){
				System.out.println("onLinkFamily");
			}

			@Override
			public void onFamilyUnlink(final FamilyPanel boxPanel, final GedcomNode family){
				System.out.println("onUnlinkFamily " + family.getID());
			}

			@Override
			public void onFamilyRemove(final FamilyPanel boxPanel, final GedcomNode family){
				System.out.println("onRemoveFamily " + family.getID());
			}

			@Override
			public void onFamilyPreviousParent(final FamilyPanel familyPanel, final GedcomNode thisParent, final GedcomNode otherCurrentParent,
					final GedcomNode currentFamily){
				System.out.println("onPrevParentFamily this: " + thisParent.getID() + ", other: " + otherCurrentParent.getID()
					+ ", family: " + currentFamily.getID());
			}

			@Override
			public void onFamilyNextParent(final FamilyPanel familyPanel, final GedcomNode thisParent, final GedcomNode otherCurrentParent,
					final GedcomNode currentFamily){
				System.out.println("onNextParentFamily this: " + thisParent.getID() + ", other: " + otherCurrentParent.getID()
					+ ", family: " + currentFamily.getID());
			}
		};
		final IndividualListenerInterface individualListener = new IndividualListenerInterface(){
			@Override
			public void onIndividualEdit(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onEditIndividual " + individual.getID());
			}

			@Override
			public void onIndividualFocus(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onFocusIndividual " + individual.getID());
			}

			@Override
			public void onIndividualLink(final IndividualPanel boxPanel, final SelectedNodeType type){
				System.out.println("onLinkIndividual " + type);
			}

			@Override
			public void onIndividualUnlink(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onUnlinkIndividual " + individual.getID());
			}

			@Override
			public void onIndividualAdd(final IndividualPanel boxPanel){
				System.out.println("onAddIndividual");
			}

			@Override
			public void onIndividualRemove(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onRemoveIndividual " + individual.getID());
			}

			@Override
			public void onIndividualAddPreferredImage(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onAddPreferredImage " + individual.getID());
			}
		};

		EventQueue.invokeLater(() -> {
			final TreePanel panel = new TreePanel(family, 4, storeFlef, familyListener, individualListener);

			final JFrame frame = new JFrame();
			frame.getContentPane().setLayout(new BorderLayout());
			frame.getContentPane().add(panel, BorderLayout.NORTH);
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
