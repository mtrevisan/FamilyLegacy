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

import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.PopupMouseAdapter;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.ResourceHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;


//http://www.miglayout.com/whitepaper.html
//http://www.miglayout.com/QuickStart.pdf
//https://www.oracle.com/technetwork/systems/ts-4928-159120.pdf
//https://stackoverflow.com/questions/25010068/miglayout-push-vs-grow
//https://docs.oracle.com/javase/tutorial/uiswing/layout/group.html
public class GroupPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 6664809287767332824L;

	private static final Logger LOGGER = LoggerFactory.getLogger(GroupPanel.class);

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
	private static final Dimension NEXT_PREVIOUS_UNION_PREFERRED_SIZE = new Dimension(ICON_UNION_PREVIOUS_ENABLED.getIconWidth(),
		ICON_UNION_PREVIOUS_ENABLED.getIconHeight());

	/** Height of the union line from the bottom of the person panel [px]. */
	private static final int GROUP_CONNECTION_HEIGHT = 15;
	private static final Dimension UNION_PANEL_DIMENSION = new Dimension(13, 12);
	static final int GROUP_EXITING_HEIGHT = GROUP_CONNECTION_HEIGHT - UNION_PANEL_DIMENSION.height / 2;
	private static final int HALF_PARTNER_SEPARATION = 6;
	static final int GROUP_SEPARATION = HALF_PARTNER_SEPARATION + UNION_PANEL_DIMENSION.width + HALF_PARTNER_SEPARATION;
	/** Distance between navigation union arrow and box. */
	static final int NAVIGATION_UNION_ARROW_SEPARATION = 2;
	/** Distance between navigation parents arrow and box. */
	private static final int NAVIGATION_PARENTS_ARROW_SEPARATION = (NAVIGATION_UNION_ARROW_SEPARATION << 1) + 3;

	static final int NAVIGATION_ARROW_HEIGHT = (int)(PREVIOUS_NEXT_SIZE.getHeight() + NAVIGATION_UNION_ARROW_SEPARATION);
	private static final int UNION_ARROWS_WIDTH = (int)Math.round(PREVIOUS_NEXT_WIDTH + NAVIGATION_UNION_ARROW_SEPARATION
		+ PREVIOUS_NEXT_WIDTH);

	private static final String KEY_ENABLED = "enabled";

	static final Stroke CONNECTION_STROKE = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f);
	static final Stroke CONNECTION_STROKE_ADOPTED = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f,
		new float[]{2.f}, 0.f);


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
	private final JMenuItem addGroupItem = new JMenuItem("Add Group…", 'A');
//	private final JMenuItem linkGroupItem = new JMenuItem("Link Group…", 'L');
	private final JMenuItem removeGroupItem = new JMenuItem("Remove Group…", 'R');

	private final BoxPanelType boxType;

	private Map<String, Object> union = new HashMap<>(0);
	private Map<String, Object> partner1 = new HashMap<>(0);
	private Map<String, Object> partner2 = new HashMap<>(0);


	static GroupPanel create(final BoxPanelType boxType){
		return new GroupPanel(boxType);
	}


	private GroupPanel(final BoxPanelType boxType){
		this.boxType = boxType;


		initComponents();
	}


	private void initComponents(){
		partner1Panel = PersonPanel.create(boxType);
		EventBusService.subscribe(partner1Panel);
		partner2Panel = PersonPanel.create(boxType);
		EventBusService.subscribe(partner2Panel);

		unionPanel.setBackground(Color.WHITE);

		partner1ArrowsSpacer.setPreferredSize(new Dimension(UNION_ARROWS_WIDTH, 0));
		partner2ArrowsSpacer.setPreferredSize(new Dimension(UNION_ARROWS_WIDTH, 0));

		final JPanel arrowPanel1 = new JPanel(new MigLayout("insets 0",
			"[]0[grow]" + NAVIGATION_PARENTS_ARROW_SEPARATION + "[grow]0[]" + NAVIGATION_UNION_ARROW_SEPARATION + "[]"));
		arrowPanel1.add(partner1ArrowsSpacer, "");
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
		arrowPanel2.add(partner2ArrowsSpacer, "hidemode 2");
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

	public final Map<String, Object> getUnion(){
		return union;
	}

	public final PersonPanel getPartner1(){
		return partner1Panel;
	}

	public final PersonPanel getPartner2(){
		return partner2Panel;
	}

	public final void setGroupListener(final GroupListenerInterface groupListener){
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
						final Map<String, Object> person = partner1Panel.getPerson();

						//list the `groupID`s for the biological union and adopting unions of the `partner`
						final Integer adopteeID = extractRecordID(person);
						final List<Integer> unionsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Integer partnerParentsID = TreePanel.extractParentsGroupID(person);
						int newGroupID = -1;
						final int parentsCount = unionsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(partnerParentsID, unionsIDs.get(i))){
								if(i > 0)
									newGroupID = unionsIDs.get(i - 1);
								break;
							}

						final Map<String, Object> group = Repository.findByID(EntityManager.NODE_GROUP, newGroupID);
						final Map<String, Object> newParents = (group != null? group: Collections.emptyMap());
						groupListener.onPersonChangeParents(GroupPanel.this, partner1Panel, newParents);
					}
				}
			});
			partner1NextParentsLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner1NextParentsLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> person = partner1Panel.getPerson();

						//list the `groupID`s for the biological union and adopting unions of the `partner`
						final Integer adopteeID = extractRecordID(person);
						final List<Integer> unionsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Integer partnerParentsID = TreePanel.extractParentsGroupID(person);
						int newGroupID = -1;
						final int parentsCount = unionsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(partnerParentsID, unionsIDs.get(i))){
								if(i + 1 < parentsCount)
									newGroupID = unionsIDs.get(i + 1);
								break;
							}

						final Map<String, Object> group = Repository.findByID(EntityManager.NODE_GROUP, newGroupID);
						final Map<String, Object> newParents = (group != null? group: Collections.emptyMap());
						groupListener.onPersonChangeParents(GroupPanel.this, partner1Panel, newParents);
					}
				}
			});
			partner1PreviousUnionLabel.setPreferredSize(NEXT_PREVIOUS_UNION_PREFERRED_SIZE);
			partner1PreviousUnionLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner1PreviousUnionLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> person = partner2Panel.getPerson();

						//list the `groupID`s for the unions of the `other partner`
						final Integer otherPartnerID = extractRecordID(person);
						final List<Integer> otherPartnerUnionIDs = getUnionIDs(otherPartnerID);

						//find current union in list
						final Integer groupID = extractRecordID(union);
						int newGroupID = -1;
						final int otherPartnerUnionsCount = otherPartnerUnionIDs.size();
						for(int i = 0; i < otherPartnerUnionsCount; i ++)
							if(Objects.equals(groupID, otherPartnerUnionIDs.get(i))){
								if(i > 0)
									newGroupID = otherPartnerUnionIDs.get(i - 1);
								break;
							}

						final Map<String, Object> group = Repository.findByID(EntityManager.NODE_GROUP, newGroupID);
						final Map<String, Object> newUnion = (group != null? group: Collections.emptyMap());

						Map<String, Object> newPartner = Collections.emptyMap();
						if(!newUnion.isEmpty()){
							final Map<Integer, Map<String, Object>> persons = Repository.findAllNavigable(EntityManager.NODE_PERSON);
							final List<Integer> newPersonIDs = getPersonIDsInGroup(extractRecordID(newUnion), EntityManager.GROUP_ROLE_PARTNER);
							for(int i = 0, length = newPersonIDs.size(); i < length; i ++)
								if(newPersonIDs.get(i).equals(otherPartnerID)){
									if(i > 0)
										newPartner = persons.get(newPersonIDs.get(i - 1));
									break;
								}
						}

						groupListener.onPersonChangeUnion(GroupPanel.this, partner2Panel, newPartner, newUnion);
					}
				}
			});
			partner1NextUnionLabel.setPreferredSize(NEXT_PREVIOUS_UNION_PREFERRED_SIZE);
			partner1NextUnionLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner1NextUnionLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> person = partner2Panel.getPerson();

						//list the `groupID`s for the unions of the `other partner`
						final Integer otherPartnerID = extractRecordID(person);
						final List<Integer> otherPartnerUnionIDs = getUnionIDs(otherPartnerID);

						//find current union in list
						final Integer groupID = extractRecordID(union);
						int newGroupID = -1;
						final int otherPartnerUnionsCount = otherPartnerUnionIDs.size();
						for(int i = 0; i < otherPartnerUnionsCount; i ++)
							if(Objects.equals(groupID, otherPartnerUnionIDs.get(i))){
								if(i + 1 < otherPartnerUnionsCount)
									newGroupID = otherPartnerUnionIDs.get(i + 1);
								break;
							}

						final Map<String, Object> group = Repository.findByID(EntityManager.NODE_GROUP, newGroupID);
						final Map<String, Object> newUnion = (group != null? group: Collections.emptyMap());

						Map<String, Object> newPartner = Collections.emptyMap();
						if(!newUnion.isEmpty()){
							final Map<Integer, Map<String, Object>> persons = Repository.findAllNavigable(EntityManager.NODE_PERSON);
							final List<Integer> newPersonIDs = getPersonIDsInGroup(extractRecordID(newUnion), EntityManager.GROUP_ROLE_PARTNER);
							for(int i = 0, length = newPersonIDs.size(); i < length; i ++)
								if(newPersonIDs.get(i).equals(otherPartnerID)){
									if(i + 1 < otherPartnerUnionsCount)
										newPartner = persons.get(newPersonIDs.get(i + 1));
									break;
								}
						}

						groupListener.onPersonChangeUnion(GroupPanel.this, partner2Panel, newPartner, newUnion);
					}
				}
			});
			partner2PreviousParentsLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner2PreviousParentsLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> person = partner2Panel.getPerson();

						//list the `groupID`s for the biological union and adopting unions of the `partner`
						final Integer adopteeID = extractRecordID(person);
						final List<Integer> unionsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Integer partnerParentsID = TreePanel.extractParentsGroupID(person);
						int newGroupID = -1;
						final int parentsCount = unionsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(partnerParentsID, unionsIDs.get(i))){
								if(i > 0)
									newGroupID = unionsIDs.get(i - 1);
								break;
							}

						final Map<String, Object> group = Repository.findByID(EntityManager.NODE_GROUP, newGroupID);
						final Map<String, Object> newParents = (group != null? group: Collections.emptyMap());
						groupListener.onPersonChangeParents(GroupPanel.this, partner2Panel, newParents);
					}
				}
			});
			partner2NextParentsLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner2NextParentsLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> person = partner2Panel.getPerson();

						//list the `groupID`s for the biological union and adopting unions of the `partner`
						final Integer adopteeID = extractRecordID(person);
						final List<Integer> unionsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Integer partnerParentsID = TreePanel.extractParentsGroupID(person);
						int newGroupID = -1;
						final int parentsCount = unionsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(partnerParentsID, unionsIDs.get(i))){
								if(i + 1 < parentsCount)
									newGroupID = unionsIDs.get(i + 1);
								break;
							}

						final Map<String, Object> group = Repository.findByID(EntityManager.NODE_GROUP, newGroupID);
						final Map<String, Object> newParents = (group != null? group: Collections.emptyMap());
						groupListener.onPersonChangeParents(GroupPanel.this, partner2Panel, newParents);
					}
				}
			});
			partner2PreviousUnionLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner2PreviousUnionLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> person = partner1Panel.getPerson();

						//list the `groupID`s for the unions of the `other partner`
						final Integer otherPartnerID = extractRecordID(person);
						final List<Integer> otherPartnerUnionIDs = getUnionIDs(otherPartnerID);

						//find current union in list
						final Integer groupID = extractRecordID(union);
						int newGroupID = -1;
						final int otherPartnerUnionsCount = otherPartnerUnionIDs.size();
						for(int i = 0; i < otherPartnerUnionsCount; i ++)
							if(Objects.equals(groupID, otherPartnerUnionIDs.get(i))){
								newGroupID = otherPartnerUnionIDs.get(i - 1);
								break;
							}

						final Map<String, Object> group = Repository.findByID(EntityManager.NODE_GROUP, newGroupID);
						final Map<String, Object> newUnion = (group != null? group: Collections.emptyMap());

						Map<String, Object> newPartner = Collections.emptyMap();
						if(!newUnion.isEmpty()){
							final Map<Integer, Map<String, Object>> persons = Repository.findAllNavigable(EntityManager.NODE_PERSON);
							final List<Integer> newPersonIDs = getPersonIDsInGroup(extractRecordID(newUnion), EntityManager.GROUP_ROLE_PARTNER);
							for(int i = 0, length = newPersonIDs.size(); i < length; i ++)
								if(newPersonIDs.get(i).equals(otherPartnerID)){
									if(i > 0)
										newPartner = persons.get(newPersonIDs.get(i - 1));
									break;
								}
						}

						groupListener.onPersonChangeUnion(GroupPanel.this, partner1Panel, newPartner, newUnion);
					}
				}
			});
			partner2NextUnionLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner2NextUnionLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> person = partner1Panel.getPerson();

						//list the `groupID`s for the unions of the `other partner`
						final Integer otherPartnerID = extractRecordID(person);
						final List<Integer> otherPartnerUnionIDs = getUnionIDs(otherPartnerID);

						//find current union in list
						final Integer groupID = extractRecordID(union);
						int newGroupID = -1;
						final int otherPartnerUnionsCount = otherPartnerUnionIDs.size();
						for(int i = 0; i < otherPartnerUnionsCount; i ++)
							if(Objects.equals(groupID, otherPartnerUnionIDs.get(i))){
								if(i + 1 < otherPartnerUnionsCount)
									newGroupID = otherPartnerUnionIDs.get(i + 1);
								break;
							}

						final Map<String, Object> group = Repository.findByID(EntityManager.NODE_GROUP, newGroupID);
						final Map<String, Object> newUnion = (group != null? group: Collections.emptyMap());

						Map<String, Object> newPartner = Collections.emptyMap();
						if(!newUnion.isEmpty()){
							final Map<Integer, Map<String, Object>> persons = Repository.findAllNavigable(EntityManager.NODE_PERSON);
							final List<Integer> newPersonIDs = getPersonIDsInGroup(extractRecordID(newUnion), EntityManager.GROUP_ROLE_PARTNER);
							for(int i = 0, length = newPersonIDs.size(); i < length; i ++)
								if(newPersonIDs.get(i).equals(otherPartnerID)){
									if(i + 1 < otherPartnerUnionsCount)
										newPartner = persons.get(newPersonIDs.get(i + 1));
									break;
								}
						}

						groupListener.onPersonChangeUnion(GroupPanel.this, partner1Panel, newPartner, newUnion);
					}
				}
			});
		}
	}

	public final void setPersonListener(final PersonListenerInterface personListener){
		partner1Panel.setPersonListener(personListener);
		partner2Panel.setPersonListener(personListener);
	}

	private void attachPopUpMenu(final JComponent component, final GroupListenerInterface groupListener){
		final JPopupMenu popupMenu = new JPopupMenu();

		editGroupItem.addActionListener(e -> groupListener.onGroupEdit(this));
		popupMenu.add(editGroupItem);

		addGroupItem.addActionListener(e -> groupListener.onGroupAdd(this));
		popupMenu.add(addGroupItem);

//		linkGroupItem.addActionListener(e -> groupListener.onGroupLink(this));
//		popupMenu.add(linkGroupItem);

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


	public void loadData(final Integer groupID){
		final Map<String, Object> group = (groupID != null
			? Repository.findByID(EntityManager.NODE_GROUP, groupID)
			: Collections.emptyMap());
		loadData(group, Collections.emptyMap(), Collections.emptyMap());
	}

	void loadData(final Map<String, Object> group, final Map<String, Object> partner1, final Map<String, Object> partner2){
		prepareData(group, partner1, partner2);

		loadData();
	}

	private void prepareData(Map<String, Object> group, Map<String, Object> partner1, Map<String, Object> partner2){
		if(group.isEmpty()){
			final List<Map<String, Object>> unions = extractUnions(partner1);
			if(!unions.isEmpty())
				group = unions.getFirst();
		}

		if(!group.isEmpty()){
			final Integer homeUnionID = extractRecordID(group);
			final List<Integer> personIDsInUnion = getPersonIDsInGroup(homeUnionID, EntityManager.GROUP_ROLE_PARTNER);
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

		union = group;
		this.partner1 = partner1;
		this.partner2 = partner2;
	}

	private void loadData(){
		partner1Panel.loadData(extractRecordID(partner1));
		partner2Panel.loadData(extractRecordID(partner2));

		if(boxType == BoxPanelType.PRIMARY){
			final Integer groupID = extractRecordID(union);
			updatePreviousNextUnionIcons(groupID, partner2, partner1PreviousUnionLabel, partner1NextUnionLabel);
			updatePreviousNextUnionIcons(groupID, partner1, partner2PreviousUnionLabel, partner2NextUnionLabel);

			updatePreviousNextParentsIcons(partner1, partner1PreviousParentsLabel, partner1NextParentsLabel);
			updatePreviousNextParentsIcons(partner2, partner2PreviousParentsLabel, partner2NextParentsLabel);
		}

		unionPanel.setBorder(!union.isEmpty()? BorderFactory.createLineBorder(BORDER_COLOR):
			BorderFactory.createDashedBorder(BORDER_COLOR));

		refresh(ActionCommand.ACTION_COMMAND_GROUP);

		partner1Panel.repaint();
		partner2Panel.repaint();
	}

	private List<Map<String, Object>> extractUnions(final Map<String, Object> person){
		final List<Map<String, Object>> unionGroups = new ArrayList<>(0);
		if(!person.isEmpty()){
			final Integer personID = extractRecordID(person);
			unionGroups.addAll(getUnions(personID));
		}
		return unionGroups;
	}

	/** Should be called whenever a modification on the store causes modifications on the UI. */
	@EventHandler
	@SuppressWarnings("NumberEquality")
	public final void refresh(final Integer actionCommand){
		if(actionCommand != ActionCommand.ACTION_COMMAND_GROUP)
			return;

		final boolean hasData = !union.isEmpty();
//		final boolean hasGroups = !getRecords(TABLE_NAME_GROUP).isEmpty();
//		final boolean hasChildren = (getChildren().length > 0);
		editGroupItem.setEnabled(hasData);
		addGroupItem.setEnabled(!hasData);
//		linkGroupItem.setEnabled(!hasData && hasGroups);
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
	}

	private void updatePreviousNextParentsIcons(final Map<String, Object> partner, final JLabel previousLabel, final JLabel nextLabel){
		//list the `groupID`s for the biological union and adopting unions of the `partner`
		final Integer adopteeID = extractRecordID(partner);
		final List<Integer> unionsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

		//find current parents in list
		final Integer partnerParentsID = TreePanel.extractParentsGroupID(partner2Panel.getPerson());
		int currentGroupIndex = -1;
		final int parentsCount = unionsIDs.size();
		for(int i = 0; i < parentsCount; i ++)
			if(Objects.equals(partnerParentsID, unionsIDs.get(i))){
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


		final boolean isPartner1 = Objects.equals(extractRecordID(partner), extractRecordID(partner1Panel.getPerson()));
		final List<Integer> otherPartnerUnionIDs = getUnionIDs(extractRecordID(isPartner1? partner2: partner1));
		final boolean hasMoreUnions = (otherPartnerUnionIDs.size() > 1);
		(isPartner1? partner1ArrowsSpacer: partner2ArrowsSpacer).setVisible(hasMoreParents && hasMoreUnions);
	}


	private List<Integer> getPersonIDsInGroup(final Integer groupID, final String role){
		return Repository.findReferencingNodes(EntityManager.NODE_PERSON,
				EntityManager.NODE_GROUP, groupID,
				EntityManager.RELATIONSHIP_BELONGS_TO, EntityManager.PROPERTY_ROLE, role).stream()
			.map(EntityManager::extractRecordID)
			.collect(Collectors.toList());
	}

	private List<Map<String, Object>> getUnions(final Integer personID){
		return Repository.findReferencingNodes(EntityManager.NODE_GROUP,
			EntityManager.NODE_PERSON, personID,
			EntityManager.RELATIONSHIP_OF, EntityManager.PROPERTY_ROLE, EntityManager.GROUP_ROLE_PARTNER);
	}

	private List<Integer> getUnionIDs(final Integer partnerID){
		return getUnions(partnerID).stream()
			.map(EntityManager::extractRecordID)
			.toList();
	}

	private List<Integer> getBiologicalAndAdoptingParentsIDs(final Integer adopteeID){
		final List<Integer> ids = Repository.findReferencingNodes(EntityManager.NODE_GROUP,
				EntityManager.NODE_PERSON, adopteeID,
				EntityManager.RELATIONSHIP_OF, EntityManager.PROPERTY_ROLE, EntityManager.GROUP_ROLE_CHILD).stream()
			.map(EntityManager::extractRecordID)
			.collect(Collectors.toList());
		final List<Integer> adoptingParentsIDs = Repository.findReferencingNodes(EntityManager.NODE_GROUP,
				EntityManager.NODE_PERSON, adopteeID,
				EntityManager.RELATIONSHIP_OF, EntityManager.PROPERTY_ROLE, EntityManager.GROUP_ROLE_ADOPTEE).stream()
			.map(EntityManager::extractRecordID)
			.toList();
		ids.addAll(adoptingParentsIDs);
		return ids;
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


		GraphDatabaseManager.clearDatabase();

		int person1ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person2ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person3ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);

		final Map<String, Object> personName11 = new HashMap<>();
		personName11.put("personal_name", "tòni");
		personName11.put("family_name", "bruxatin");
		personName11.put("locale", "vec-IT");
		personName11.put("type", "birth name");
		int personName11ID = Repository.upsert(personName11, EntityManager.NODE_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_PERSON_NAME, personName11ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> personName12 = new HashMap<>();
		personName12.put("personal_name", "antonio");
		personName12.put("family_name", "bruciatino");
		personName12.put("locale", "it-IT");
		personName12.put("type", "death name");
		int personName12ID = Repository.upsert(personName12, EntityManager.NODE_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_PERSON_NAME, personName12ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> personName21 = new HashMap<>();
		personName21.put("personal_name", "bèpi");
		personName21.put("family_name", "marangon");
		personName21.put("locale", "vec-IT");
		personName21.put("type", "birth name");
		int personName21ID = Repository.upsert(personName21, EntityManager.NODE_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_PERSON_NAME, personName21ID,
			EntityManager.NODE_PERSON, person2ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> group1 = new HashMap<>();
		group1.put("type", "family");
		int group1ID = Repository.upsert(group1, EntityManager.NODE_GROUP);
		final Map<String, Object> group2 = new HashMap<>();
		group2.put("type", "family");
		int group2ID = Repository.upsert(group2, EntityManager.NODE_GROUP);
		final Map<String, Object> group3 = new HashMap<>();
		group3.put("type", "family");
		int group3ID = Repository.upsert(group3, EntityManager.NODE_GROUP);
		final Map<String, Object> group4 = new HashMap<>();
		group4.put("type", "family");
		int group4ID = Repository.upsert(group4, EntityManager.NODE_GROUP);

		final Map<String, Object> groupRelationship11 = new HashMap<>();
		groupRelationship11.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 1,
			EntityManager.NODE_GROUP, group1ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship11, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship2 = new HashMap<>();
		groupRelationship2.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 2,
			EntityManager.NODE_GROUP, group1ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship2, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship13 = new HashMap<>();
		groupRelationship13.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 1,
			EntityManager.NODE_GROUP, group2ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship13, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship3 = new HashMap<>();
		groupRelationship3.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 3,
			EntityManager.NODE_GROUP, group2ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship3, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship4 = new HashMap<>();
		groupRelationship4.put("role", "child");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 2,
			EntityManager.NODE_GROUP, group4ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship4, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship5 = new HashMap<>();
		groupRelationship5.put("role", "adoptee");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 2,
			EntityManager.NODE_GROUP, group3ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship5, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final BoxPanelType boxType = BoxPanelType.PRIMARY;
//		final BoxPanelType boxType = BoxPanelType.SECONDARY;

		final GroupListenerInterface unionListener = new GroupListenerInterface(){
			@Override
			public void onGroupEdit(final GroupPanel groupPanel){
				final Map<String, Object> group = groupPanel.getUnion();
				System.out.println("onEditGroup " + extractRecordID(group));
			}

			@Override
			public void onGroupAdd(final GroupPanel groupPanel){
				System.out.println("onAddGroup");
			}

			@Override
			public void onGroupLink(final GroupPanel groupPanel){
				final PersonPanel partner1 = groupPanel.getPartner1();
				final PersonPanel partner2 = groupPanel.getPartner2();
				final Map<String, Object> group = groupPanel.union;
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
			final GroupPanel panel = create(boxType);
			panel.loadData(1);
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
					System.out.println(Repository.logDatabase());

					System.exit(0);
				}
			});
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
