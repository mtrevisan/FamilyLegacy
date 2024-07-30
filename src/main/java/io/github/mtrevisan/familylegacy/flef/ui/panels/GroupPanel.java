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

import io.github.mtrevisan.familylegacy.flef.ui.helpers.PopupMouseAdapter;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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


//http://www.miglayout.com/whitepaper.html
//http://www.miglayout.com/QuickStart.pdf
//https://www.oracle.com/technetwork/systems/ts-4928-159120.pdf
//https://stackoverflow.com/questions/25010068/miglayout-push-vs-grow
//https://docs.oracle.com/javase/tutorial/uiswing/layout/group.html
public class GroupPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 6664809287767332824L;

	private static final Color BORDER_COLOR = Color.BLACK;

	private static final double PREVIOUS_NEXT_WIDTH = 12.;
	private static final double PREVIOUS_NEXT_ASPECT_RATIO = 3501. / 2662.;
	private static final Dimension PREVIOUS_NEXT_SIZE = new Dimension((int)PREVIOUS_NEXT_WIDTH,
		(int)(PREVIOUS_NEXT_WIDTH * PREVIOUS_NEXT_ASPECT_RATIO));

	//https://thenounproject.com/search/?q=cut&i=3132059
	private static final ImageIcon ICON_PARENTS_PREVIOUS_ENABLED = ResourceHelper.getImage("/images/parents_previous.png",
		PREVIOUS_NEXT_SIZE);
	private static final ImageIcon ICON_PARENTS_PREVIOUS_DISABLED = new ImageIcon(
		GrayFilter.createDisabledImage(ICON_PARENTS_PREVIOUS_ENABLED.getImage()));
	private static final ImageIcon ICON_PARENTS_NEXT_ENABLED = ResourceHelper.getImage("/images/parents_next.png",
		PREVIOUS_NEXT_SIZE);
	private static final ImageIcon ICON_PARENTS_NEXT_DISABLED = new ImageIcon(
		GrayFilter.createDisabledImage(ICON_PARENTS_NEXT_ENABLED.getImage()));

	//https://snappygoat.com/free-public-domain-images-app_application_arrow_back_0/
	private static final ImageIcon ICON_UNION_PREVIOUS_ENABLED = ResourceHelper.getImage("/images/union_previous.png",
		PREVIOUS_NEXT_SIZE);
	private static final ImageIcon ICON_UNION_PREVIOUS_DISABLED = new ImageIcon(
		GrayFilter.createDisabledImage(ICON_UNION_PREVIOUS_ENABLED.getImage()));
	private static final ImageIcon ICON_UNION_NEXT_ENABLED = ResourceHelper.getImage("/images/union_next.png",
		PREVIOUS_NEXT_SIZE);
	private static final ImageIcon ICON_UNION_NEXT_DISABLED = new ImageIcon(
		GrayFilter.createDisabledImage(ICON_UNION_NEXT_ENABLED.getImage()));

	/** Height of the union line from the bottom of the person panel [px]. */
	private static final int GROUP_CONNECTION_HEIGHT = 15;
	private static final Dimension UNION_PANEL_DIMENSION = new Dimension(13, 12);
	static final int GROUP_EXITING_HEIGHT = GROUP_CONNECTION_HEIGHT - UNION_PANEL_DIMENSION.height / 2;
	private static final int HALF_PARTNER_SEPARATION = 6;
	static final int GROUP_SEPARATION = HALF_PARTNER_SEPARATION + UNION_PANEL_DIMENSION.width + HALF_PARTNER_SEPARATION;
	/** Distance between navigation union arrow and box. */
	static final int NAVIGATION_UNION_ARROW_SEPARATION = 2;
	/** Distance between navigation parents arrow and box. */
	private static final int NAVIGATION_PARENTS_ARROW_SEPARATION = (NAVIGATION_UNION_ARROW_SEPARATION << 1) + 1;

	static final int NAVIGATION_ARROW_HEIGHT = (int)(PREVIOUS_NEXT_SIZE.getHeight() + NAVIGATION_UNION_ARROW_SEPARATION);
	private static final int UNION_ARROWS_WIDTH = (int)Math.round(PREVIOUS_NEXT_WIDTH + NAVIGATION_UNION_ARROW_SEPARATION
		+ PREVIOUS_NEXT_WIDTH);

	private static final String KEY_ENABLED = "enabled";

	static final Stroke CONNECTION_STROKE = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f);
	static final Stroke CONNECTION_STROKE_ADOPTED = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f,
		new float[]{2.f}, 0.f);

	private static final String TABLE_NAME_GROUP_JUNCTION = "group_junction";
	private static final String TABLE_NAME_PERSON = "person";
	private static final String TABLE_NAME_GROUP = "group";


	private final JLabel partner1ArrowsSpacer = new JLabel();
	private final JLabel partner2ArrowsSpacer = new JLabel();
	private JPanel arrowPersonPanel1;
	private JPanel arrowPersonPanel2;
	private PersonPanel partner1Panel;
	private PersonPanel partner2Panel;
	private final JLabel partner1PreviousParentsLabel = new JLabel();
	private final JLabel partner1NextParentsLabel = new JLabel();
	private final JLabel partner1PreviousUnionLabel = new JLabel();
	private final JLabel partner1NextUnionLabel = new JLabel();
	private final JLabel partner2PreviousParentsLabel = new JLabel();
	private final JLabel partner2NextParentsLabel = new JLabel();
	private final JLabel partner2PreviousUnionLabel = new JLabel();
	private final JLabel partner2NextUnionLabel = new JLabel();
	private final JPanel unionPanel = new JPanel();
	private final JMenuItem editGroupItem = new JMenuItem("Edit Group…", 'E');
	private final JMenuItem linkGroupItem = new JMenuItem("Link Group…", 'L');
	private final JMenuItem unlinkGroupItem = new JMenuItem("Unlink Group…", 'U');
	private final JMenuItem addGroupItem = new JMenuItem("Add Group…", 'A');
	private final JMenuItem removeGroupItem = new JMenuItem("Remove Group…", 'R');

	private Map<String, Object> group;
	private Map<String, Object> partner1;
	private Map<String, Object> partner2;
	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;
	private final BoxPanelType boxType;


	static GroupPanel create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final BoxPanelType boxType){
		return new GroupPanel(store, boxType);
	}


	private GroupPanel(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final BoxPanelType boxType){
		this.store = store;
		this.boxType = boxType;
	}


	void initComponents(){
		partner1Panel = PersonPanel.create(store, boxType, SelectedNodeType.PARTNER);
		partner1Panel.initComponents();
		EventBusService.subscribe(partner1Panel);
		partner2Panel = PersonPanel.create(store, boxType, SelectedNodeType.PARTNER);
		partner2Panel.initComponents();
		EventBusService.subscribe(partner2Panel);

		unionPanel.setBackground(Color.WHITE);

		partner1ArrowsSpacer.setPreferredSize(new Dimension(UNION_ARROWS_WIDTH, 0));
		partner2ArrowsSpacer.setPreferredSize(new Dimension(UNION_ARROWS_WIDTH, 0));

		final JPanel arrowPanel1 = new JPanel(new MigLayout("insets 0",
			"[]0[grow]" + NAVIGATION_PARENTS_ARROW_SEPARATION + "[grow]0[]" + NAVIGATION_UNION_ARROW_SEPARATION + "[]"));
		arrowPanel1.add(partner1ArrowsSpacer);
		arrowPanel1.add(partner1PreviousParentsLabel, "right");
		arrowPanel1.add(partner1NextParentsLabel, "left");
		arrowPanel1.add(partner1PreviousUnionLabel, "right");
		arrowPanel1.add(partner1NextUnionLabel, "right");
		arrowPanel1.setOpaque(false);

		arrowPersonPanel1 = new JPanel(new MigLayout("insets 0",
			"[grow,fill]",
			"[" + PREVIOUS_NEXT_SIZE.getHeight() + "]" + NAVIGATION_UNION_ARROW_SEPARATION + "[]"));
		arrowPersonPanel1.add(arrowPanel1, "wrap");
		arrowPersonPanel1.add(partner1Panel, "right");
		arrowPersonPanel1.setOpaque(false);

		final JPanel arrowPanel2 = new JPanel(new MigLayout("insets 0",
			"[]" + NAVIGATION_UNION_ARROW_SEPARATION + "[]0[grow]" + NAVIGATION_PARENTS_ARROW_SEPARATION + "[grow]0[]"));
		arrowPanel2.add(partner2PreviousUnionLabel, "left");
		arrowPanel2.add(partner2NextUnionLabel, "left");
		arrowPanel2.add(partner2PreviousParentsLabel, "right");
		arrowPanel2.add(partner2NextParentsLabel, "left");
		arrowPanel2.add(partner2ArrowsSpacer);
		arrowPanel2.setOpaque(false);

		arrowPersonPanel2 = new JPanel(new MigLayout("insets 0",
			"[grow,fill]",
			"[" + PREVIOUS_NEXT_SIZE.getHeight() + "]" + NAVIGATION_UNION_ARROW_SEPARATION + "[]"));
		arrowPersonPanel2.add(arrowPanel2, "wrap");
		arrowPersonPanel2.add(partner2Panel, "left");
		arrowPersonPanel2.setOpaque(false);

		setLayout(new MigLayout("insets 0",
			"[right,grow]" + HALF_PARTNER_SEPARATION + "[center,grow]" + HALF_PARTNER_SEPARATION + "[left,grow]",
			"[bottom]"));
		add(arrowPersonPanel1, "right,grow");
		add(unionPanel, "gapbottom " + GROUP_EXITING_HEIGHT);
		add(arrowPersonPanel2, "left,grow");

		setOpaque(false);
	}

	public final Map<String, Object> getGroup(){
		return group;
	}

	public final Map<String, Object> getPartner1(){
		return partner1;
	}

	public final Map<String, Object> getPartner2(){
		return partner2;
	}

	public final Map<String, Object>[] getChildren(){
		return partner1Panel.getChildren();
	}

	final void setChildren(final Map<String, Object>[] children){
		partner1Panel.setChildren(children);
		partner2Panel.setChildren(children);
	}

	final void setGroupListener(final GroupListenerInterface groupListener){
		if(groupListener != null){
			unionPanel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt))
						groupListener.onGroupEdit(GroupPanel.this);
				}
			});

			attachPopUpMenu(unionPanel, groupListener);


			partner1PreviousParentsLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner1PreviousParentsLabel.getClientProperty(KEY_ENABLED)){
						//list the `groupID`s for the biological union and adopting unions of the `partner`
						final Integer adopteeID = extractRecordID(partner1);
						final List<Integer> unionsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Map<String, Object> partnerParents = TreePanel.extractParents(partner1, store);
						final Integer actualUnionID = extractRecordID(partnerParents);
						int newGroupID = -1;
						final int parentsCount = unionsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(actualUnionID, unionsIDs.get(i))){
								newGroupID = unionsIDs.get(i - 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(TABLE_NAME_GROUP);
						final Map<String, Object> newPartner1Parents = groups.get(newGroupID);
						groupListener.onGroupChangeParents(GroupPanel.this, partner1, newPartner1Parents);
					}
				}
			});
			partner1NextParentsLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner1NextParentsLabel.getClientProperty(KEY_ENABLED)){
						//list the `groupID`s for the biological union and adopting unions of the `partner`
						final Integer adopteeID = extractRecordID(partner1);
						final List<Integer> unionsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Map<String, Object> partnerParents = TreePanel.extractParents(partner1, store);
						final Integer actualUnionID = extractRecordID(partnerParents);
						int newGroupID = -1;
						final int parentsCount = unionsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(actualUnionID, unionsIDs.get(i))){
								newGroupID = unionsIDs.get(i + 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(TABLE_NAME_GROUP);
						final Map<String, Object> newPartner1Parents = groups.get(newGroupID);
						groupListener.onGroupChangeParents(GroupPanel.this, partner1, newPartner1Parents);
					}
				}
			});
			partner1PreviousUnionLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner1PreviousUnionLabel.getClientProperty(KEY_ENABLED)){
						//list the `groupID`s for the unions of the `other partner`
						final Integer otherPartnerID = extractRecordID(partner2);
						final List<Integer> otherPartnerUnionIDs = getUnionIDs(otherPartnerID);

						//find current union in list
						final Integer groupID = extractRecordID(group);
						int newGroupID = -1;
						final int otherPartnerUnionsCount = otherPartnerUnionIDs.size();
						for(int i = 0; i < otherPartnerUnionsCount; i ++)
							if(Objects.equals(groupID, otherPartnerUnionIDs.get(i))){
								newGroupID = otherPartnerUnionIDs.get(i - 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(TABLE_NAME_GROUP);
						final Map<String, Object> newPartner1Union = groups.get(newGroupID);
						groupListener.onGroupChangeUnion(GroupPanel.this, partner1, newPartner1Union);
					}
				}
			});
			partner1NextUnionLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner1NextUnionLabel.getClientProperty(KEY_ENABLED)){
						//list the `groupID`s for the unions of the `other partner`
						final Integer otherPartnerID = extractRecordID(partner2);
						final List<Integer> otherPartnerUnionIDs = getUnionIDs(otherPartnerID);

						//find current union in list
						final Integer groupID = extractRecordID(group);
						int newGroupIndex = -1;
						final int otherPartnerUnionsCount = otherPartnerUnionIDs.size();
						for(int i = 0; i < otherPartnerUnionsCount; i ++)
							if(Objects.equals(groupID, otherPartnerUnionIDs.get(i))){
								newGroupIndex = otherPartnerUnionIDs.get(i + 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(TABLE_NAME_GROUP);
						final Map<String, Object> newPartner1Union = groups.get(newGroupIndex);
						groupListener.onGroupChangeUnion(GroupPanel.this, partner1, newPartner1Union);
					}
				}
			});
			partner2PreviousParentsLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner2PreviousParentsLabel.getClientProperty(KEY_ENABLED)){
						//list the `groupID`s for the biological union and adopting unions of the `partner`
						final Integer adopteeID = extractRecordID(partner2);
						final List<Integer> unionsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Map<String, Object> partnerParents = TreePanel.extractParents(partner2, store);
						final Integer actualUnionID = extractRecordID(partnerParents);
						int newGroupID = -1;
						final int parentsCount = unionsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(actualUnionID, unionsIDs.get(i))){
								newGroupID = unionsIDs.get(i - 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(TABLE_NAME_GROUP);
						final Map<String, Object> newPartner2Parents = groups.get(newGroupID);
						groupListener.onGroupChangeParents(GroupPanel.this, partner2, newPartner2Parents);
					}
				}
			});
			partner2NextParentsLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner2NextParentsLabel.getClientProperty(KEY_ENABLED)){
						//list the `groupID`s for the biological union and adopting unions of the `partner`
						final Integer adopteeID = extractRecordID(partner2);
						final List<Integer> unionsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Map<String, Object> partnerParents = TreePanel.extractParents(partner2, store);
						final Integer actualUnionID = extractRecordID(partnerParents);
						int newGroupID = -1;
						final int parentsCount = unionsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(actualUnionID, unionsIDs.get(i))){
								newGroupID = unionsIDs.get(i + 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(TABLE_NAME_GROUP);
						final Map<String, Object> newPartner2Parents = groups.get(newGroupID);
						groupListener.onGroupChangeParents(GroupPanel.this, partner2, newPartner2Parents);
					}
				}
			});
			partner2PreviousUnionLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner2PreviousUnionLabel.getClientProperty(KEY_ENABLED)){
						//list the `groupID`s for the unions of the `other partner`
						final Integer otherPartnerID = extractRecordID(partner1);
						final List<Integer> otherPartnerUnionIDs = getUnionIDs(otherPartnerID);

						//find current union in list
						final Integer groupID = extractRecordID(group);
						int newGroupID = -1;
						final int otherPartnerUnionsCount = otherPartnerUnionIDs.size();
						for(int i = 0; i < otherPartnerUnionsCount; i ++)
							if(Objects.equals(groupID, otherPartnerUnionIDs.get(i))){
								newGroupID = otherPartnerUnionIDs.get(i - 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(TABLE_NAME_GROUP);
						final Map<String, Object> newPartner2Union = groups.get(newGroupID);
						groupListener.onGroupChangeUnion(GroupPanel.this, partner2, newPartner2Union);
					}
				}
			});
			partner2NextUnionLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner2NextUnionLabel.getClientProperty(KEY_ENABLED)){
						//list the `groupID`s for the unions of the `other partner`
						final Integer otherPartnerID = extractRecordID(partner1);
						final List<Integer> otherPartnerUnionIDs = getUnionIDs(otherPartnerID);

						//find current union in list
						final Integer groupID = extractRecordID(group);
						int newGroupID = -1;
						final int otherPartnerUnionsCount = otherPartnerUnionIDs.size();
						for(int i = 0; i < otherPartnerUnionsCount; i ++)
							if(Objects.equals(groupID, otherPartnerUnionIDs.get(i))){
								newGroupID = otherPartnerUnionIDs.get(i + 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(TABLE_NAME_GROUP);
						final Map<String, Object> newPartner2Union = groups.get(newGroupID);
						groupListener.onGroupChangeUnion(GroupPanel.this, partner2, newPartner2Union);
					}
				}
			});
		}
	}

	final void setPersonListener(final PersonListenerInterface personListener){
		partner1Panel.setPartner(partner2);
		partner1Panel.setUnion(group);
		partner1Panel.setPersonListener(personListener);

		partner2Panel.setPartner(partner1);
		partner2Panel.setUnion(group);
		partner2Panel.setPersonListener(personListener);
	}

	private void attachPopUpMenu(final JComponent component, final GroupListenerInterface groupListener){
		final JPopupMenu popupMenu = new JPopupMenu();

		editGroupItem.addActionListener(e -> groupListener.onGroupEdit(this));
		popupMenu.add(editGroupItem);

		linkGroupItem.addActionListener(e -> groupListener.onGroupLink(this));
		popupMenu.add(linkGroupItem);

		unlinkGroupItem.addActionListener(e -> groupListener.onGroupUnlink(this));
		popupMenu.add(unlinkGroupItem);

		addGroupItem.addActionListener(e -> groupListener.onGroupAdd(this));
		popupMenu.add(addGroupItem);

		removeGroupItem.addActionListener(e -> groupListener.onGroupRemove(this));
		popupMenu.add(removeGroupItem);

		component.addMouseListener(new PopupMouseAdapter(popupMenu, component));
	}

	@Override
	protected final void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D && arrowPersonPanel1 != null && arrowPersonPanel2 != null){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			graphics2D.setStroke(CONNECTION_STROKE);

			final int xFrom = arrowPersonPanel1.getX() + arrowPersonPanel1.getWidth();
			final int xTo = arrowPersonPanel2.getX();
			final int yFrom = arrowPersonPanel1.getY() + arrowPersonPanel1.getHeight() - GROUP_CONNECTION_HEIGHT;
			//horizontal line between partners
			graphics2D.drawLine(xFrom, yFrom, xTo, yFrom);


			//for test purposes
//			final Point enterPoint1 = getPaintingPartner1EnterPoint();
//			graphics2D.setColor(Color.RED);
//			graphics2D.drawLine(enterPoint1.x - 10, enterPoint1.y - 10, enterPoint1.x + 10, enterPoint1.y + 10);
//			graphics2D.drawLine(enterPoint1.x + 10, enterPoint1.y - 10, enterPoint1.x - 10, enterPoint1.y + 10);
//			final Point enterPoint2 = getPaintingPartner2EnterPoint();
//			graphics2D.drawLine(enterPoint2.x - 10, enterPoint2.y - 10, enterPoint2.x + 10, enterPoint2.y + 10);
//			graphics2D.drawLine(enterPoint2.x + 10, enterPoint2.y - 10, enterPoint2.x - 10, enterPoint2.y + 10);
//			final Point exitPoint = getPaintingExitPoint();
//			graphics2D.drawLine(exitPoint.x - 10, exitPoint.y - 10, exitPoint.x + 10, exitPoint.y + 10);
//			graphics2D.drawLine(exitPoint.x + 10, exitPoint.y - 10, exitPoint.x - 10, exitPoint.y + 10);
//			graphics2D.setColor(Color.BLACK);


			graphics2D.dispose();
		}
	}


	void loadData(final Map<String, Object> group){
		loadData(group, Collections.emptyMap(), Collections.emptyMap());
	}

	void loadData(final Map<String, Object> group, Map<String, Object> partner1, Map<String, Object> partner2){
		prepareData(group, partner1, partner2);

		loadData();
	}

	private void prepareData(final Map<String, Object> group, Map<String, Object> partner1, Map<String, Object> partner2){
		if((partner1.isEmpty() || partner2.isEmpty()) && !group.isEmpty()){
			//extract the first two persons from the group:
			final Integer groupID = extractRecordID(group);
			final List<Integer> personIDs = getPersonIDsInGroup(groupID);
			final TreeMap<Integer, Map<String, Object>> persons = getRecords(TABLE_NAME_PERSON);
			final int size = personIDs.size();
			if(partner1.isEmpty())
				partner1 = persons.getOrDefault((size > 0? personIDs.get(0): null), Collections.emptyMap());
			if(partner2.isEmpty())
				partner2 = persons.getOrDefault((size > 1? personIDs.get(1): null), Collections.emptyMap());
		}

		this.group = group;
		this.partner1 = partner1;
		this.partner2 = partner2;
	}

	private void loadData(){
		partner1Panel.loadData(partner1);
		partner2Panel.loadData(partner2);

		if(boxType == BoxPanelType.PRIMARY){
			final Integer groupID = extractRecordID(group);
			updatePreviousNextUnionIcons(groupID, partner2, partner1PreviousUnionLabel, partner1NextUnionLabel);
			updatePreviousNextUnionIcons(groupID, partner1, partner2PreviousUnionLabel, partner2NextUnionLabel);

			updatePreviousNextParentsIcons(partner1, partner1PreviousParentsLabel, partner1NextParentsLabel);
			updatePreviousNextParentsIcons(partner2, partner2PreviousParentsLabel, partner2NextParentsLabel);
		}

		unionPanel.setBorder(!group.isEmpty()? BorderFactory.createLineBorder(BORDER_COLOR):
			BorderFactory.createDashedBorder(BORDER_COLOR));

		refresh(ActionCommand.ACTION_COMMAND_GROUP_COUNT);
	}

	/** Should be called whenever a modification on the store causes modifications on the UI. */
	@EventHandler
	@SuppressWarnings("NumberEquality")
	public final void refresh(final Integer actionCommand){
		if(actionCommand != ActionCommand.ACTION_COMMAND_GROUP_COUNT)
			return;

		final boolean hasGroups = !getRecords(TABLE_NAME_GROUP).isEmpty();
		final boolean hasData = !group.isEmpty();
		editGroupItem.setEnabled(hasData);
		linkGroupItem.setEnabled(!hasData && hasGroups);
		unlinkGroupItem.setEnabled(hasData);
		addGroupItem.setEnabled(!hasData && hasGroups);
		removeGroupItem.setEnabled(hasData);
	}

	private void updatePreviousNextUnionIcons(final Integer groupID, final Map<String, Object> otherPartner, final JLabel previousLabel,
			final JLabel nextLabel){
		//list the `groupID`s for the unions of the `other partner`
		final Integer otherPartnerID = extractRecordID(otherPartner);
		final List<Integer> otherPartnerUnionIDs = getUnionIDs(otherPartnerID);

		//find current union in list
		int currentGroupIndex = -1;
		final int otherPartnerUnionsCount = otherPartnerUnionIDs.size();
		for(int i = 0; i < otherPartnerUnionsCount; i ++){
			final Integer otherUnionID = otherPartnerUnionIDs.get(i);

			if(Objects.equals(groupID, otherUnionID)){
				currentGroupIndex = i;
				break;
			}
		}

		final boolean hasMoreUnions = (otherPartnerUnionsCount > 1);

		final boolean partnerPreviousEnabled = (currentGroupIndex > 0);
		previousLabel.putClientProperty(KEY_ENABLED, partnerPreviousEnabled);
		previousLabel.setCursor(Cursor.getPredefinedCursor(partnerPreviousEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		ImageIcon icon = null;
		if(hasMoreUnions)
			icon = (partnerPreviousEnabled? ICON_UNION_PREVIOUS_ENABLED: ICON_UNION_PREVIOUS_DISABLED);
		previousLabel.setIcon(icon);

		final boolean partnerNextEnabled = (currentGroupIndex < otherPartnerUnionsCount - 1);
		nextLabel.putClientProperty(KEY_ENABLED, partnerNextEnabled);
		nextLabel.setCursor(Cursor.getPredefinedCursor(partnerNextEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		if(hasMoreUnions)
			icon = (partnerNextEnabled? ICON_UNION_NEXT_ENABLED: ICON_UNION_NEXT_DISABLED);
		nextLabel.setIcon(icon);


		(otherPartner == partner2? partner1ArrowsSpacer: partner2ArrowsSpacer)
			.setVisible(hasMoreUnions);
	}

	private void updatePreviousNextParentsIcons(final Map<String, Object> partner, final JLabel previousLabel, final JLabel nextLabel){
		//list the `groupID`s for the biological union and adopting unions of the `partner`
		final Integer adopteeID = extractRecordID(partner);
		final List<Integer> unionsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

		//find current parents in list
		final Map<String, Object> partnerParents = TreePanel.extractParents(partner2, store);
		final Integer actualUnionID = extractRecordID(partnerParents);
		int currentGroupIndex = -1;
		final int parentsCount = unionsIDs.size();
		for(int i = 0; i < parentsCount; i ++)
			if(Objects.equals(actualUnionID, unionsIDs.get(i))){
				currentGroupIndex = i;
				break;
			}

		final boolean hasMoreParents = (parentsCount > 1);

		final boolean parentsPreviousEnabled = (currentGroupIndex > 0);
		previousLabel.putClientProperty(KEY_ENABLED, parentsPreviousEnabled);
		previousLabel.setCursor(Cursor.getPredefinedCursor(parentsPreviousEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		ImageIcon icon = null;
		if(hasMoreParents)
			icon = (parentsPreviousEnabled? ICON_PARENTS_PREVIOUS_ENABLED: ICON_PARENTS_PREVIOUS_DISABLED);
		previousLabel.setIcon(icon);

		final boolean parentsNextEnabled = (currentGroupIndex < parentsCount - 1);
		nextLabel.putClientProperty(KEY_ENABLED, parentsNextEnabled);
		nextLabel.setCursor(Cursor.getPredefinedCursor(parentsNextEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		if(hasMoreParents)
			icon = (parentsNextEnabled? ICON_PARENTS_NEXT_ENABLED: ICON_PARENTS_NEXT_DISABLED);
		nextLabel.setIcon(icon);
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

	private List<Integer> getPersonIDsInGroup(final Integer groupID){
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals(groupID, extractRecordGroupID(entry)))
			.filter(entry -> Objects.equals("partner", extractRecordRole(entry)))
			.filter(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
			.map(GroupPanel::extractRecordReferenceID)
			.toList();
	}

	private List<Integer> getUnionIDs(final Integer partnerID){
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals(TABLE_NAME_PERSON, extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(partnerID, extractRecordReferenceID(entry)))
			.filter(entry -> Objects.equals("partner", extractRecordRole(entry)))
			.map(GroupPanel::extractRecordGroupID)
			.toList();
	}

	private List<Integer> getBiologicalAndAdoptingParentsIDs(final Integer adopteeID){
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals(TABLE_NAME_PERSON, extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(adopteeID, extractRecordReferenceID(entry)))
			.filter(entry -> Objects.equals("child", extractRecordRole(entry)) || Objects.equals("adoptee", extractRecordRole(entry)))
			.map(GroupPanel::extractRecordGroupID)
			.toList();
	}


	final Point getPaintingPartner1EnterPoint(){
		final Point p1 = partner1Panel.getPaintingEnterPoint();
		final Point origin = getLocation();
		return new Point(origin.x + p1.x,
			origin.y + p1.y);
	}

	final Point getPaintingPartner2EnterPoint(){
		final Point p1 = partner1Panel.getPaintingEnterPoint();
		final Point p2 = partner2Panel.getPaintingEnterPoint();
		final Point origin = getLocation();
		return new Point(origin.x + getWidth() + (p2.x - p1.x - partner2Panel.getWidth()) / 2,
			origin.y + p2.y);
	}

	final Point getPaintingExitPoint(){
		final Point p1 = partner1Panel.getPaintingEnterPoint();
		final Point p2 = partner2Panel.getPaintingEnterPoint();
		final Point origin = getLocation();
		return new Point(origin.x + ((p1.x + p2.x - partner2Panel.getWidth()) / 2 + getWidth()) / 2,
			origin.y + getHeight() - GROUP_EXITING_HEIGHT);
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

		final TreeMap<Integer, Map<String, Object>> personNames = new TreeMap<>();
		store.put("person_name", personNames);
		final Map<String, Object> personName11 = new HashMap<>();
		personName11.put("id", 1);
		personName11.put("person_id", 1);
		personName11.put("personal_name", "tòni");
		personName11.put("family_name", "bruxatin");
		personName11.put("name_locale", "vec-IT");
		personName11.put("type", "birth name");
		personNames.put((Integer)personName11.get("id"), personName11);
		final Map<String, Object> personName12 = new HashMap<>();
		personName12.put("id", 2);
		personName12.put("person_id", 1);
		personName12.put("personal_name", "antonio");
		personName12.put("family_name", "bruciatino");
		personName12.put("name_locale", "it-IT");
		personName12.put("type", "death name");
		personNames.put((Integer)personName12.get("id"), personName12);
		final Map<String, Object> personName21 = new HashMap<>();
		personName21.put("id", 3);
		personName21.put("person_id", 2);
		personName21.put("personal_name", "bèpi");
		personName21.put("family_name", "marangon");
		personName21.put("name_locale", "vec-IT");
		personName21.put("type", "birth name");
		personNames.put((Integer)personName21.get("id"), personName21);

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
		final Map<String, Object> group3 = new HashMap<>();
		group3.put("id", 3);
		group3.put("type", "family");
		groups.put((Integer)group3.get("id"), group3);
		final Map<String, Object> group4 = new HashMap<>();
		group4.put("id", 4);
		group4.put("type", "family");
		groups.put((Integer)group4.get("id"), group4);

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
		groupJunction4.put("group_id", 4);
		groupJunction4.put("reference_table", "person");
		groupJunction4.put("reference_id", 2);
		groupJunction4.put("role", "child");
		groupJunctions.put((Integer)groupJunction4.get("id"), groupJunction4);
		final Map<String, Object> groupJunction5 = new HashMap<>();
		groupJunction5.put("id", 6);
		groupJunction5.put("group_id", 3);
		groupJunction5.put("reference_table", "person");
		groupJunction5.put("reference_id", 2);
		groupJunction5.put("role", "adoptee");
		groupJunctions.put((Integer)groupJunction5.get("id"), groupJunction5);

		final BoxPanelType boxType = BoxPanelType.PRIMARY;
//		final BoxPanelType boxType = BoxPanelType.SECONDARY;

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
			public void onGroupAdd(final GroupPanel groupPanel){
				System.out.println("onAddGroup");
			}

			@Override
			public void onGroupRemove(final GroupPanel groupPanel){
				final Map<String, Object> group = groupPanel.getGroup();
				System.out.println("onRemoveGroup " + group.get("id"));
			}

			@Override
			public void onGroupChangeParents(final GroupPanel groupPanel, final Map<String, Object> person,
					final Map<String, Object> newUnion){
				final Map<String, Object> currentUnion = groupPanel.getGroup();
				System.out.println("onGroupChangeParents person: " + person.get("id") + ", current: " + currentUnion.get("id")
					+ ", new: " + newUnion.get("id"));
			}

			@Override
			public void onGroupChangeUnion(final GroupPanel groupPanel, final Map<String, Object> person, final Map<String, Object> newUnion){
				final Map<String, Object> currentUnion = groupPanel.getGroup();
				System.out.println("onGroupChangeUnion person: " + person.get("id") + ", current: " + currentUnion.get("id")
					+ ", new: " + newUnion.get("id"));
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
			final GroupPanel panel = create(store, boxType);
			panel.initComponents();
			panel.loadData(group1);
			panel.setGroupListener(unionListener);
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
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
