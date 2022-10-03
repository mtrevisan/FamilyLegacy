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
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
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

	private static final String PEDIGREE_BIOLOGICAL = "biological";


	private FamilyPanel partner1Partners1Panel;
	private FamilyPanel partner1Partners2Panel;
	private FamilyPanel partner2Partners1Panel;
	private FamilyPanel partner2Partners2Panel;
	private FamilyPanel partner1PartnersPanel;
	private FamilyPanel partner2PartnersPanel;
	private FamilyPanel homeFamilyPanel;
	private JScrollPane childrenScrollPane;
	private ChildrenPanel childrenPanel;

	private GedcomNode partner1;
	private GedcomNode partner2;
	private GedcomNode homeFamily;
	private final int generations;
	private final Flef store;


	public TreePanel(final GedcomNode homeFamily, final int generations, final Flef store){
		this.homeFamily = homeFamily;
		this.generations = generations;
		this.store = store;

		if(generations <= 3)
			initComponents3Generations(homeFamily);
		else
			initComponents4Generations(homeFamily);

		loadData();
	}

	public void setFamilyListener(final FamilyListenerInterface familyListener){
		partner1Partners1Panel.setFamilyListener(familyListener);
		partner1Partners2Panel.setFamilyListener(familyListener);
		partner2Partners1Panel.setFamilyListener(familyListener);
		partner2Partners2Panel.setFamilyListener(familyListener);
		partner1PartnersPanel.setFamilyListener(familyListener);
		partner2PartnersPanel.setFamilyListener(familyListener);
		homeFamilyPanel.setFamilyListener(familyListener);
	}

	public void setIndividualListener(final IndividualListenerInterface individualListener){
		partner1Partners1Panel.setIndividualListener(individualListener);
		partner1Partners2Panel.setIndividualListener(individualListener);
		partner2Partners1Panel.setIndividualListener(individualListener);
		partner2Partners2Panel.setIndividualListener(individualListener);
		partner1PartnersPanel.setIndividualListener(individualListener);
		partner2PartnersPanel.setIndividualListener(individualListener);
		homeFamilyPanel.setIndividualListener(individualListener);
		childrenPanel.setIndividualListener(individualListener);
	}

	private void initComponents3Generations(final GedcomNode family){
		partner1 = (!family.isEmpty()? store.getPartner1(family): store.createEmptyNode());
		partner2 = (!family.isEmpty()? store.getPartner2(family): store.createEmptyNode());

		final GedcomNode partner1Parents = extractPartners(partner1);
		final GedcomNode partner2Parents = extractPartners(partner2);

		GedcomNode childReference = extractFirstChild(partner1Parents, partner1);
		partner1PartnersPanel = new FamilyPanel(store.createEmptyNode(), store.createEmptyNode(), partner1Parents, childReference, store, BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner1PartnersPanel);
		childReference = extractFirstChild(partner2Parents, partner2);
		partner2PartnersPanel = new FamilyPanel(store.createEmptyNode(), store.createEmptyNode(), partner2Parents, childReference, store, BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner2PartnersPanel);
		childReference = extractFirstChild(homeFamily, null);
		homeFamilyPanel = new FamilyPanel(partner1, partner2, homeFamily, childReference, store, BoxPanelType.PRIMARY);
		EventBusService.subscribe(homeFamilyPanel);
		childrenPanel = new ChildrenPanel(homeFamily, store);

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
		add(partner1PartnersPanel, "growx 50");
		add(partner2PartnersPanel, "growx 50,wrap");
		add(homeFamilyPanel, "span 2,wrap");
		add(childrenScrollPane, "span 2");
	}

	private void initComponents4Generations(final GedcomNode family){
		partner1 = (!family.isEmpty()? store.getPartner1(family): store.createEmptyNode());
		partner2 = (!family.isEmpty()? store.getPartner2(family): store.createEmptyNode());

		final GedcomNode partner1Partners = extractPartners(partner1);
		final GedcomNode partner2Partners = extractPartners(partner2);

		final GedcomNode partner1Partner1 = store.getPartner1(partner1Partners);
		final GedcomNode partner1Partner2 = store.getPartner2(partner1Partners);
		final GedcomNode partner1Partenr1Partners = extractPartners(partner1Partner1);
		final GedcomNode partner1Partner2Partners = extractPartners(partner1Partner2);

		final GedcomNode partner2Partner1 = store.getPartner1(partner2Partners);
		final GedcomNode partner2Partner2 = store.getPartner2(partner2Partners);
		final GedcomNode partner2Partner1Partners = extractPartners(partner2Partner1);
		final GedcomNode partner2Partner2Partners = extractPartners(partner2Partner2);

		GedcomNode defaultChildReference = store.getPartner1(partner1Partners);
		GedcomNode childReference = extractFirstChild(partner1Partenr1Partners, defaultChildReference);
		partner1Partners1Panel = new FamilyPanel(store.createEmptyNode(), store.createEmptyNode(), partner1Partenr1Partners, childReference, store, BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner1Partners1Panel);
		defaultChildReference = store.getPartner2(partner1Partners);
		childReference = extractFirstChild(partner1Partner2Partners, defaultChildReference);
		partner1Partners2Panel = new FamilyPanel(store.createEmptyNode(), store.createEmptyNode(), partner1Partner2Partners, childReference, store, BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner1Partners2Panel);
		defaultChildReference = store.getPartner1(partner2Partners);
		childReference = extractFirstChild(partner2Partner1Partners, defaultChildReference);
		partner2Partners1Panel = new FamilyPanel(store.createEmptyNode(), store.createEmptyNode(), partner2Partner1Partners, childReference, store, BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner2Partners1Panel);
		defaultChildReference = store.getPartner2(partner2Partners);
		childReference = extractFirstChild(partner2Partner2Partners, defaultChildReference);
		partner2Partners2Panel = new FamilyPanel(store.createEmptyNode(), store.createEmptyNode(), partner2Partner2Partners, childReference, store, BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner2Partners2Panel);
		childReference = extractFirstChild(partner1Partners, partner1);
		partner1PartnersPanel = new FamilyPanel(store.createEmptyNode(), store.createEmptyNode(), partner1Partners, childReference, store, BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner1PartnersPanel);
		childReference = extractFirstChild(partner2Partners, partner2);
		partner2PartnersPanel = new FamilyPanel(store.createEmptyNode(), store.createEmptyNode(), partner2Partners, childReference, store, BoxPanelType.SECONDARY);
		EventBusService.subscribe(partner2PartnersPanel);
		childReference = extractFirstChild(homeFamily, store.createEmptyNode());
		homeFamilyPanel = new FamilyPanel(partner1, partner2, homeFamily, childReference, store, BoxPanelType.PRIMARY);
		EventBusService.subscribe(homeFamilyPanel);
		childrenPanel = new ChildrenPanel(homeFamily, store);

		setBackground(BACKGROUND_COLOR_APPLICATION);

		childrenScrollPane = new JScrollPane(new ScrollableContainerHost(childrenPanel, ScrollableContainerHost.ScrollType.HORIZONTAL));
		childrenScrollPane.setOpaque(false);
		childrenScrollPane.getViewport().setOpaque(false);
		childrenScrollPane.setBorder(null);
		childrenScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		childrenScrollPane.setAutoscrolls(true);
		//trigger repaint in order to move the connections between children and home family
		childrenScrollPane.getHorizontalScrollBar().addAdjustmentListener(e -> {
			if(!homeFamily.isEmpty()){
				//remember last scroll position, restore it if present
				CHILDREN_SCROLLBAR_POSITION.put(homeFamily.getID(), childrenScrollPane.getHorizontalScrollBar().getValue());

				repaint();
			}
		});

		setLayout(new MigLayout("insets 0",
			"[grow,center]" + FamilyPanel.FAMILY_SEPARATION + "[grow,center]" + FamilyPanel.FAMILY_SEPARATION
				+ "[grow,center]" + FamilyPanel.FAMILY_SEPARATION + "[grow,center]",
			"[]" + GENERATION_SEPARATOR_SIZE + "[]" + GENERATION_SEPARATOR_SIZE + "[]" + GENERATION_SEPARATOR_SIZE + "[]"));
		add(partner1Partners1Panel, "growx 25");
		add(partner1Partners2Panel, "growx 25");
		add(partner2Partners1Panel, "growx 25");
		add(partner2Partners2Panel, "growx 25,wrap");
		add(partner1PartnersPanel, "span 2,growx 50");
		add(partner2PartnersPanel, "span 2,growx 50,wrap");
		add(homeFamilyPanel, "span 4,wrap");
		add(childrenScrollPane, "span 4,alignx center");
	}

	private GedcomNode extractFirstChild(final GedcomNode family, GedcomNode defaultChild){
		final List<GedcomNode> childrenReference = (!family.isEmpty()? family.getChildrenWithTag("CHILD"):
			Collections.emptyList());
		if(!family.isEmpty() && defaultChild.isEmpty()){
			final List<GedcomNode> children = store.traverseAsList(family, "CHILD[]");
			defaultChild = (!children.isEmpty()? children.get(0): null);
		}
		return (!childrenReference.isEmpty()? childrenReference.get(0): defaultChild);
	}

	private GedcomNode extractPartners(final GedcomNode child){
		if(!child.isEmpty()){
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
		return store.createEmptyNode();
	}

	private Collection<GedcomNode> extractBiologicalFamilyChilds(final Collection<GedcomNode> familyChilds){
		final Collection<GedcomNode> biologicalFamilyChilds = new ArrayList<>(familyChilds.size());
		//check pedigree (prefers `biological` or <null>)
		for(final GedcomNode familyChild : familyChilds){
			final String pedigree1 = store.traverse(familyChild, "PEDIGREE.PARTNER1").getValue();
			if(PEDIGREE_BIOLOGICAL.equalsIgnoreCase(pedigree1))
				biologicalFamilyChilds.add(familyChild);
			else{
				final String pedigree2 = store.traverse(familyChild, "PEDIGREE.PARTNER2").getValue();
				if(PEDIGREE_BIOLOGICAL.equalsIgnoreCase(pedigree2))
					biologicalFamilyChilds.add(familyChild);
			}
		}
		return biologicalFamilyChilds;
	}

	public final GedcomNode getPreferredFamily(final GedcomNode individual){
		GedcomNode family = store.createEmptyNode();
		//see if this individual belongs to a family
		List<GedcomNode> families = extractFamilies(individual);
		if(families.size() == 1)
			//the individual belongs to exact one family, choose it and load as the primary family
			family = families.get(0);
		else if(families.size() > 1){
			//the individual belongs to more than one family, select those with the oldest event
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
		return family;
	}

	private List<GedcomNode> extractFamilies(final GedcomNode individual){
		final List<GedcomNode> familyXRefs = store.traverseAsList(individual, "FAMILY_PARTNER[]");
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
	protected final void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			graphics2D.setStroke(FamilyPanel.CONNECTION_STROKE);

			if(partner1Partners1Panel != null && partner1Partners1Panel.isVisible()){
				//partner1's partner1 entering connection
				final Point p1p1 = partner1PartnersPanel.getFamilyPaintingPartner1EnterPoint();
				final Point p1g1p = parentGrandParentsExitingConnection(partner1Partners1Panel, graphics2D);
				grandparentsEnteringConnection(p1p1, p1g1p, graphics2D);
			}
			if(partner1Partners2Panel != null && partner1Partners2Panel.isVisible()){
				//partner1's partner2 entering connection
				final Point p1p2 = partner1PartnersPanel.getFamilyPaintingPartner2EnterPoint();
				final Point p1g2p = parentGrandParentsExitingConnection(partner1Partners2Panel, graphics2D);
				grandparentsEnteringConnection(p1p2, p1g2p, graphics2D);
			}
			if(partner2Partners1Panel != null && partner2Partners1Panel.isVisible()){
				//partner2's partner1 entering connection
				final Point p2p1 = partner2PartnersPanel.getFamilyPaintingPartner1EnterPoint();
				final Point p2g1p = parentGrandParentsExitingConnection(partner2Partners1Panel, graphics2D);
				grandparentsEnteringConnection(p2p1, p2g1p, graphics2D);
			}
			if(partner2Partners2Panel != null && partner2Partners2Panel.isVisible()){
				//partner2's partner2 entering connection
				final Point p2p2 = partner2PartnersPanel.getFamilyPaintingPartner2EnterPoint();
				final Point p2g2p = parentGrandParentsExitingConnection(partner2Partners2Panel, graphics2D);
				grandparentsEnteringConnection(p2p2, p2g2p, graphics2D);
			}

			final Point p1p = partner1PartnersPanel.getFamilyPaintingExitPoint();
			if(!partner1.isEmpty())
				//partner1's partners exiting connection
				grandparentsExitingConnection(p1p, 0, graphics2D);
			final Point p2p = partner2PartnersPanel.getFamilyPaintingExitPoint();
			if(!partner2.isEmpty())
				//partner2's partners exiting connection
				grandparentsExitingConnection(p2p, 0, graphics2D);
			if(!partner1.isEmpty()){
				final Point hfp1 = homeFamilyPanel.getFamilyPaintingPartner1EnterPoint();
				//home family partner1 entering connection
				parentEnteringConnection(hfp1, FamilyPanel.NAVIGATION_ARROW_HEIGHT, graphics2D);
				//line between partner1's partners and partner1
				grandparentsToParent(p1p, hfp1, FamilyPanel.NAVIGATION_ARROW_HEIGHT, graphics2D);
			}
			if(!partner2.isEmpty()){
				final Point hfp2 = homeFamilyPanel.getFamilyPaintingPartner2EnterPoint();
				//home family partner2 entering connection
				parentEnteringConnection(hfp2, FamilyPanel.NAVIGATION_ARROW_HEIGHT, graphics2D);
				//line between partner2's partners and partner2
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
				final boolean[] adoptions = childrenPanel.getAdoptions();
				for(int i = 0; i < c.length; i ++){
					final Point point = c[i];

					if(adoptions[i])
						graphics2D.setStroke(FamilyPanel.CONNECTION_STROKE_ADOPTED);

					graphics2D.drawLine(origin.x + point.x, origin.y + point.y,
						origin.x + point.x, origin.y + point.y - GENERATION_SEPARATOR_SIZE / 2);

					if(adoptions[i])
						graphics2D.setStroke(FamilyPanel.CONNECTION_STROKE);
				}
			}

			graphics2D.dispose();
		}
	}

	private static Point parentGrandParentsExitingConnection(final FamilyPanel parentGrandparentsPanel, final Graphics2D graphics2D){
		Point p = null;
		if(parentGrandparentsPanel != null){
			//parent's parent's parent exiting connection
			p = parentGrandparentsPanel.getFamilyPaintingExitPoint();
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
			g.x, g.y + offset + FamilyPanel.FAMILY_EXITING_HEIGHT + GENERATION_SEPARATOR_SIZE / 2);
	}

	private static void parentEnteringConnection(final Point p, final int offset, final Graphics2D graphics2D){
		//parent entering connection
		graphics2D.drawLine(p.x, p.y + FamilyPanel.NAVIGATION_ARROW_HEIGHT + offset,
			p.x, p.y - offset - GENERATION_SEPARATOR_SIZE / 2);
	}

	private static void grandparentsToParent(final Point g, final Point p, final int offset, final Graphics2D graphics2D){
		//line between grandparent and parent
		graphics2D.drawLine(g.x, g.y + FamilyPanel.FAMILY_EXITING_HEIGHT + GENERATION_SEPARATOR_SIZE / 2,
			p.x, p.y - offset - GENERATION_SEPARATOR_SIZE / 2);
	}


	public final void loadData(final GedcomNode partner1, final GedcomNode partner2, final GedcomNode homeFamily){
		this.partner1 = partner1;
		this.partner2 = partner2;
		this.homeFamily = homeFamily;

		loadData();
	}

	@SuppressWarnings("InstanceVariableUsedBeforeInitialized")
	private void loadData(){
		partner1 = (partner1.isEmpty() && !homeFamily.isEmpty()? store.getPartner1(homeFamily): partner1);
		partner2 = (partner2.isEmpty() && !homeFamily.isEmpty()? store.getPartner2(homeFamily): partner2);

		final GedcomNode partner1Partners = extractPartners(partner1);
		final GedcomNode partner2Partners = extractPartners(partner2);

		if(generations > 3){
			final GedcomNode partner1Partner1 = store.getPartner1(partner1Partners);
			final GedcomNode partner1Partner2 = store.getPartner2(partner1Partners);
			final GedcomNode partner1Grandpartners1 = extractPartners(partner1Partner1);
			final GedcomNode partner1Grandpartners2 = extractPartners(partner1Partner2);

			final GedcomNode partner2Partner1 = store.getPartner1(partner2Partners);
			final GedcomNode partner2Partner2 = store.getPartner2(partner2Partners);
			final GedcomNode partner2Grandpartners1 = extractPartners(partner2Partner1);
			final GedcomNode partner2Grandpartners2 = extractPartners(partner2Partner2);

			partner1Partners1Panel.setVisible(!store.traverse(partner1Partners, "PARTNER1").isEmpty());
			partner1Partners2Panel.setVisible(!store.traverse(partner1Partners, "PARTNER2").isEmpty());
			partner2Partners1Panel.setVisible(!store.traverse(partner2Partners, "PARTNER1").isEmpty());
			partner2Partners2Panel.setVisible(!store.traverse(partner2Partners, "PARTNER2").isEmpty());
			partner1Partners1Panel.loadData(partner1Grandpartners1);
			partner1Partners2Panel.loadData(partner1Grandpartners2);
			partner2Partners1Panel.loadData(partner2Grandpartners1);
			partner2Partners2Panel.loadData(partner2Grandpartners2);
		}
		partner1PartnersPanel.setVisible(!partner1.isEmpty());
		partner2PartnersPanel.setVisible(!partner2.isEmpty());
		partner1PartnersPanel.loadData(partner1Partners);
		partner2PartnersPanel.loadData(partner2Partners);
		homeFamilyPanel.loadData(partner1, partner2, homeFamily);
		childrenPanel.loadData(homeFamily);


		if(!homeFamily.isEmpty()){
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
//		final GedcomNode family = storeFlef.createEmptyNode();

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
			public void onFamilyPreviousPartner(final FamilyPanel familyPanel, final GedcomNode thisParent, final GedcomNode otherCurrentParent,
															final GedcomNode currentFamily){
				System.out.println("onPrevParentFamily this: " + thisParent.getID() + ", other: " + otherCurrentParent.getID()
					+ ", family: " + currentFamily.getID());
			}

			@Override
			public void onFamilyNextPartner(final FamilyPanel familyPanel, final GedcomNode thisParent, final GedcomNode otherCurrentParent,
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
			public void onIndividualFocus(final IndividualPanel boxPanel, final SelectedNodeType type, final GedcomNode individual){
				System.out.println("onFocusIndividual " + individual.getID() + ", type is " + type);
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
			final TreePanel panel = new TreePanel(family, 4, storeFlef);
			panel.setFamilyListener(familyListener);
			panel.setIndividualListener(individualListener);

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
