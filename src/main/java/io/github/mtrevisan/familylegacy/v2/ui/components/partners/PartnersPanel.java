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
package io.github.mtrevisan.familylegacy.v2.ui.components.partners;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ResourceHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


public class PartnersPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 6664809287767332824L;


	// Colors
	private static final Color GROUP_BACKGROUND = Color.WHITE;
	private static final Color BORDER_COLOR = Color.BLACK;

	// Dimensions
	private static final double PREVIOUS_NEXT_WIDTH = 12.;
	private static final double PREVIOUS_NEXT_ASPECT_RATIO = 3501. / 2662.;
	private static final Dimension PREVIOUS_NEXT_SIZE = new Dimension((int)PREVIOUS_NEXT_WIDTH,
		(int)(PREVIOUS_NEXT_WIDTH * PREVIOUS_NEXT_ASPECT_RATIO));
	/** Height of the group line from the bottom of the individual panel [px]. */
	private static final int GROUP_CONNECTION_HEIGHT = 15;
	private static final Dimension GROUP_PANEL_DIMENSION = new Dimension(14, 12);
	public static final int GROUP_EXITING_HEIGHT = GROUP_CONNECTION_HEIGHT - GROUP_PANEL_DIMENSION.height / 2;
	private static final int HALF_PARTNER_SEPARATION = 6;
	public static final int GROUP_SEPARATION = HALF_PARTNER_SEPARATION + GROUP_PANEL_DIMENSION.width
		+ HALF_PARTNER_SEPARATION;
	/** Distance between navigation group arrow and box. */
	public static final int NAVIGATION_DESCENDANTS_ARROW_SEPARATION = 2;
	/** Distance between navigation parents arrow and box. */
	private static final int NAVIGATION_PARENTS_ARROW_SEPARATION = (NAVIGATION_DESCENDANTS_ARROW_SEPARATION << 1) + 3;
	public static final int NAVIGATION_ARROW_HEIGHT = (int)(PREVIOUS_NEXT_SIZE.getHeight()
		+ NAVIGATION_DESCENDANTS_ARROW_SEPARATION);
	private static final int DESCENDANTS_ARROWS_WIDTH = (int)Math.round(PREVIOUS_NEXT_WIDTH
		+ NAVIGATION_DESCENDANTS_ARROW_SEPARATION + PREVIOUS_NEXT_WIDTH);

	public static final Stroke CONNECTION_STROKE = new BasicStroke(1.f, BasicStroke.CAP_BUTT,
		BasicStroke.JOIN_BEVEL, 0.f);
	private static final Stroke CONNECTION_STROKE_ADOPTED = new BasicStroke(1.f, BasicStroke.CAP_BUTT,
		BasicStroke.JOIN_BEVEL, 0.f, new float[]{2.f}, 0.f);

	// Icons
	//https://thenounproject.com/search/?q=cut&i=3132059
	//https://snappygoat.com/free-public-domain-images-app_application_arrow_back_0/
	private static final ImageIcon ICON_PARENTS_PREVIOUS_ENABLED = ResourceHelper.getResizedImageFromResource("/images/parents_previous.png",
		PREVIOUS_NEXT_SIZE);
	private static final ImageIcon ICON_PARENTS_PREVIOUS_DISABLED = new ImageIcon(
		GrayFilter.createDisabledImage(ICON_PARENTS_PREVIOUS_ENABLED.getImage()));
	private static final ImageIcon ICON_PARENTS_NEXT_ENABLED = ResourceHelper.getResizedImageFromResource("/images/parents_next.png",
		PREVIOUS_NEXT_SIZE);
	private static final ImageIcon ICON_PARENTS_NEXT_DISABLED = new ImageIcon(
		GrayFilter.createDisabledImage(ICON_PARENTS_NEXT_ENABLED.getImage()));
	private static final ImageIcon ICON_UNION_PREVIOUS_ENABLED = ResourceHelper.getResizedImageFromResource("/images/union_previous.png",
		PREVIOUS_NEXT_SIZE);
	private static final ImageIcon ICON_UNION_PREVIOUS_DISABLED = new ImageIcon(
		GrayFilter.createDisabledImage(ICON_UNION_PREVIOUS_ENABLED.getImage()));
	private static final ImageIcon ICON_UNION_NEXT_ENABLED = ResourceHelper.getResizedImageFromResource("/images/union_next.png",
		PREVIOUS_NEXT_SIZE);
	private static final ImageIcon ICON_UNION_NEXT_DISABLED = new ImageIcon(
		GrayFilter.createDisabledImage(ICON_UNION_NEXT_ENABLED.getImage()));
	private static final Dimension NEXT_PREVIOUS_GROUP_PREFERRED_SIZE = new Dimension(ICON_UNION_PREVIOUS_ENABLED.getIconWidth(),
		ICON_UNION_PREVIOUS_ENABLED.getIconHeight());

	private static final String KEY_ENABLED = "enabled";

	// State
	private final JPanel groupPanel = new JPanel();
	private IndividualPanel fatherPanel;
	private IndividualPanel motherPanel;
	private final JLabel fatherArrowsSpacer = new JLabel();
	private final JLabel motherArrowsSpacer = new JLabel();
	private final JLabel fatherPreviousParentsLabel = new JLabel();
	private final JLabel fatherNextParentsLabel = new JLabel();
	private final JLabel fatherPreviousGroupLabel = new JLabel();
	private final JLabel fatherNextGroupLabel = new JLabel();
	private JPanel arrowFatherPanel;
	private final JLabel motherPreviousParentsLabel = new JLabel();
	private final JLabel motherNextParentsLabel = new JLabel();
	private final JLabel motherPreviousGroupLabel = new JLabel();
	private final JLabel motherNextGroupLabel = new JLabel();
	private JPanel arrowMotherPanel;

/*	private final JMenuItem editGroupItem = new JMenuItem("Edit Group…", 'E');
	private final JMenuItem addGroupItem = new JMenuItem("Add Group…", 'A');
//	private final JMenuItem linkGroupItem = new JMenuItem("Link Group…", 'L');
	private final JMenuItem removeGroupItem = new JMenuItem("Remove Group…", 'R');*/

	private final BoxPanelType boxType;

	private FLEFRecord individual;
	private FLEFRecord group;
	private FLEFRecord spouse;
	private IndividualData father;
	private IndividualData mother;

	private final FLEFModel model;

	private PartnersData data;

	private IndividualListener listener;


	public static PartnersPanel create(final BoxPanelType boxType, final FLEFModel model){
		return new PartnersPanel(boxType, model);
	}


	private PartnersPanel(final BoxPanelType boxType, final FLEFModel model){
		this.boxType = boxType;

		this.model = model;

		initComponents();

//		attachPopupMenu();

//		installMouseListeners();
	}


	private void initComponents(){
		groupPanel.setBackground(GROUP_BACKGROUND);
		groupPanel.setBorder(BorderFactory.createDashedBorder(BORDER_COLOR));

		fatherPanel = IndividualPanel.create(boxType, model);
//		EventBusService.subscribe(fatherPanel);
		motherPanel = IndividualPanel.create(boxType, model);
//		EventBusService.subscribe(motherPanel);

		fatherArrowsSpacer.setPreferredSize(new Dimension(DESCENDANTS_ARROWS_WIDTH, 0));
		motherArrowsSpacer.setPreferredSize(new Dimension(DESCENDANTS_ARROWS_WIDTH, 0));

		final JPanel arrow1Panel = new JPanel(new MigLayout("ins 0",
			"[]0[grow]" + NAVIGATION_PARENTS_ARROW_SEPARATION + "[grow]0[]" + NAVIGATION_DESCENDANTS_ARROW_SEPARATION + "[]"));
		arrow1Panel.add(fatherArrowsSpacer, StringUtils.EMPTY);
		arrow1Panel.add(fatherPreviousParentsLabel, "right");
		arrow1Panel.add(fatherNextParentsLabel, "left");
		arrow1Panel.add(fatherPreviousGroupLabel, "right");
		arrow1Panel.add(fatherNextGroupLabel, "right");
		arrow1Panel.setOpaque(false);

		arrowFatherPanel = new JPanel(new MigLayout("ins 0",
			"[grow,fill]",
			"[" + PREVIOUS_NEXT_SIZE.getHeight() + "]" + NAVIGATION_DESCENDANTS_ARROW_SEPARATION + "[]"));
		arrowFatherPanel.add(arrow1Panel, "wrap");
		arrowFatherPanel.add(fatherPanel, "right");
		arrowFatherPanel.setOpaque(false);

		final JPanel arrow2Panel = new JPanel(new MigLayout("ins 0",
			"[]" + NAVIGATION_DESCENDANTS_ARROW_SEPARATION + "[]0[grow]" + NAVIGATION_PARENTS_ARROW_SEPARATION + "[grow]0[]"));
		arrow2Panel.add(motherPreviousGroupLabel, "left");
		arrow2Panel.add(motherNextGroupLabel, "left");
		arrow2Panel.add(motherPreviousParentsLabel, "right");
		arrow2Panel.add(motherNextParentsLabel, "left");
		arrow2Panel.add(motherArrowsSpacer, "hidemode 2");
		arrow2Panel.setOpaque(false);

		arrowMotherPanel = new JPanel(new MigLayout("ins 0",
			"[grow,fill]",
			"[" + PREVIOUS_NEXT_SIZE.getHeight() + "]" + NAVIGATION_DESCENDANTS_ARROW_SEPARATION + "[]"));
		arrowMotherPanel.add(arrow2Panel, "wrap");
		arrowMotherPanel.add(motherPanel, "left");
		arrowMotherPanel.setOpaque(false);

		setLayout(new MigLayout("ins 0",
			"[right,grow]" + HALF_PARTNER_SEPARATION + "[center,grow]" + HALF_PARTNER_SEPARATION + "[left,grow]",
			"[bottom]"));
		add(arrowFatherPanel, "right,grow");
		add(groupPanel, "gapbottom " + GROUP_EXITING_HEIGHT);
		add(arrowMotherPanel, "left,grow");

		setOpaque(false);
	}

	@Override
	protected final void paintComponent(final Graphics g){
		if(g instanceof Graphics2D && arrowFatherPanel != null && arrowMotherPanel != null){
			final Graphics2D g2 = (Graphics2D)g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			g2.setStroke(CONNECTION_STROKE);

			final int xFrom = arrowFatherPanel.getX() + arrowFatherPanel.getWidth();
			final int xTo = arrowMotherPanel.getX();
			final int yFrom = arrowFatherPanel.getY() + arrowFatherPanel.getHeight() - GROUP_CONNECTION_HEIGHT;
			//horizontal line between partners
			g2.drawLine(xFrom, yFrom,
				xTo, yFrom);

			//for test purposes
//			pointTest(g2);

			g2.dispose();
		}
	}

	private void pointTest(final Graphics2D g2){
		final Point enterPoint1 = getPaintingFatherEnterPoint();

		GUIHelper.drawX(g2, enterPoint1);

		final Point enterPoint2 = getPaintingMotherEnterPoint();
		GUIHelper.drawX(g2, enterPoint2);

		final Point exitPoint = getPaintingExitPoint();
		GUIHelper.drawX(g2, exitPoint);
	}


	private boolean isPrimaryBox(){
		return (boxType == BoxPanelType.PRIMARY);
	}

	public final IndividualPanel getFatherPanel(){
		return fatherPanel;
	}

	public final IndividualPanel getMotherPanel(){
		return motherPanel;
	}


	public PartnersPanel withListener(final IndividualListener listener){
		this.listener = listener;

		fatherPanel.withListener(listener);
		motherPanel.withListener(listener);

		return this;
	}

	public PartnersPanel withBiologicalParents(final IndividualData father, final IndividualData mother){
		this.father = father;
		this.mother = mother;

		updateGroupData();

		return this;
	}

	private void updateGroupData(){
		fatherPanel.withIndividualData(father);
		motherPanel.withIndividualData(mother);

//		final String marriageTooltip = data.getMarriageTooltip();
//		groupPanel.setToolTipText(marriageTooltip);

//		groupPanel.setBorder(StringUtils.isNotEmpty(marriageTooltip)? BorderFactory.createLineBorder(BORDER_COLOR):
//			BorderFactory.createDashedBorder(BORDER_COLOR));
		groupPanel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));


//		if(boxType == BoxPanelType.PRIMARY){
//			final Integer groupId = extractRecordID(group);
//			updatePreviousNextGroupIcons(groupId, mother, fatherPreviousGroupLabel, fatherNextGroupLabel);
//			updatePreviousNextGroupIcons(groupId, father, motherPreviousGroupLabel, motherNextGroupLabel);
//
//			updatePreviousNextParentsIcons(father, fatherPreviousParentsLabel, fatherNextParentsLabel);
//			updatePreviousNextParentsIcons(mother, motherPreviousParentsLabel, motherNextParentsLabel);
//		}

//		refresh(ActionCommand.ACTION_COMMAND_GROUP);
	}

/*	public final void setGroupListener(final GroupListenerInterface groupListener){
		if(groupListener != null){
			groupPanel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt))
						groupListener.onGroupEdit(BiologicalParentsPanel.this);
				}
			});

			attachPopUpMenu(groupPanel, groupListener);


			fatherPreviousParentsLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)fatherPreviousParentsLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> individual = fatherPanel.getIndividual();

						//list the `groupID`s for the biological group and adopting groups of the `partner`
						final Integer adopteeID = extractRecordID(individual);
						final List<Integer> groupsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Integer partnerParentsID = TreePanel.extractParentsGroupID(individual, store);
						int newGroupID = -1;
						final int parentsCount = groupsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(partnerParentsID, groupsIDs.get(i))){
								if(i > 0)
									newGroupID = groupsIDs.get(i - 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(EntityManager.TABLE_NAME_GROUP);
						final Map<String, Object> newParents = groups.getOrDefault(newGroupID, Collections.emptyMap());
						groupListener.onIndividualChangeParents(BiologicalParentsPanel.this, fatherPanel, newParents);
					}
				}
			});
			fatherNextParentsLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)fatherNextParentsLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> individual = fatherPanel.getIndividual();

						//list the `groupID`s for the biological group and adopting groups of the `partner`
						final Integer adopteeID = extractRecordID(individual);
						final List<Integer> groupsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Integer partnerParentsID = TreePanel.extractParentsGroupID(individual, store);
						int newGroupID = -1;
						final int parentsCount = groupsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(partnerParentsID, groupsIDs.get(i))){
								if(i + 1 < parentsCount)
									newGroupID = groupsIDs.get(i + 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(EntityManager.TABLE_NAME_GROUP);
						final Map<String, Object> newParents = groups.getOrDefault(newGroupID, Collections.emptyMap());
						groupListener.onIndividualChangeParents(BiologicalParentsPanel.this, fatherPanel, newParents);
					}
				}
			});
			fatherPreviousGroupLabel.setPreferredSize(NEXT_PREVIOUS_GROUP_PREFERRED_SIZE);
			fatherPreviousGroupLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)fatherPreviousGroupLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> individual = motherPanel.getIndividual();

						//list the `groupID`s for the groups of the `other partner`
						final Integer otherPartnerID = extractRecordID(individual);
						final List<Integer> otherPartnerGroupIDs = getGroupIDs(otherPartnerID);

						//find current group in list
						final Integer groupID = extractRecordID(group);
						int newGroupID = -1;
						final int otherPartnerGroupsCount = otherPartnerGroupIDs.size();
						for(int i = 0; i < otherPartnerGroupsCount; i ++)
							if(Objects.equals(groupID, otherPartnerGroupIDs.get(i))){
								if(i > 0)
									newGroupID = otherPartnerGroupIDs.get(i - 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(EntityManager.TABLE_NAME_GROUP);
						final Map<String, Object> newGroup = groups.getOrDefault(newGroupID, Collections.emptyMap());

						Map<String, Object> newPartner = Collections.emptyMap();
						if(!newGroup.isEmpty()){
							final TreeMap<Integer, Map<String, Object>> individuals = getRecords(EntityManager.TABLE_NAME_INDIVIDUAL);
							final List<Integer> newIndividualIDs = getIndividualIDsInGroup(extractRecordID(newGroup));
							for(int i = 0, length = newIndividualIDs.size(); i < length; i ++)
								if(newIndividualIDs.get(i).equals(otherPartnerID)){
									if(i > 0)
										newPartner = individuals.get(newIndividualIDs.get(i - 1));
									break;
								}
						}

						groupListener.onIndividualChangeGroup(BiologicalParentsPanel.this, motherPanel, newPartner, newGroup);
					}
				}
			});
			fatherNextGroupLabel.setPreferredSize(NEXT_PREVIOUS_GROUP_PREFERRED_SIZE);
			fatherNextGroupLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)fatherNextGroupLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> individual = motherPanel.getIndividual();

						//list the `groupID`s for the groups of the `other partner`
						final Integer otherPartnerID = extractRecordID(individual);
						final List<Integer> otherPartnerGroupIDs = getGroupIDs(otherPartnerID);

						//find current group in list
						final Integer groupID = extractRecordID(group);
						int newGroupID = -1;
						final int otherPartnerGroupsCount = otherPartnerGroupIDs.size();
						for(int i = 0; i < otherPartnerGroupsCount; i ++)
							if(Objects.equals(groupID, otherPartnerGroupIDs.get(i))){
								if(i + 1 < otherPartnerGroupsCount)
									newGroupID = otherPartnerGroupIDs.get(i + 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(EntityManager.TABLE_NAME_GROUP);
						final Map<String, Object> newGroup = groups.getOrDefault(newGroupID, Collections.emptyMap());

						Map<String, Object> newPartner = Collections.emptyMap();
						if(!newGroup.isEmpty()){
							final TreeMap<Integer, Map<String, Object>> individuals = getRecords(EntityManager.TABLE_NAME_INDIVIDUAL);
							final List<Integer> newIndividualIDs = getIndividualIDsInGroup(extractRecordID(newGroup));
							for(int i = 0, length = newIndividualIDs.size(); i < length; i ++)
								if(newIndividualIDs.get(i).equals(otherPartnerID)){
									if(i + 1 < otherPartnerGroupsCount)
										newPartner = individuals.get(newIndividualIDs.get(i + 1));
									break;
								}
						}

						groupListener.onIndividualChangeGroup(BiologicalParentsPanel.this, motherPanel, newPartner, newGroup);
					}
				}
			});
			motherPreviousParentsLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)motherPreviousParentsLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> individual = motherPanel.getIndividual();

						//list the `groupID`s for the biological group and adopting groups of the `partner`
						final Integer adopteeID = extractRecordID(individual);
						final List<Integer> groupsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Integer partnerParentsID = TreePanel.extractParentsGroupID(individual, store);
						int newGroupID = -1;
						final int parentsCount = groupsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(partnerParentsID, groupsIDs.get(i))){
								if(i > 0)
									newGroupID = groupsIDs.get(i - 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(EntityManager.TABLE_NAME_GROUP);
						final Map<String, Object> newParents = groups.getOrDefault(newGroupID, Collections.emptyMap());
						groupListener.onIndividualChangeParents(BiologicalParentsPanel.this, motherPanel, newParents);
					}
				}
			});
			motherNextParentsLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)motherNextParentsLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> individual = motherPanel.getIndividual();

						//list the `groupID`s for the biological group and adopting groups of the `partner`
						final Integer adopteeID = extractRecordID(individual);
						final List<Integer> groupsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Integer partnerParentsID = TreePanel.extractParentsGroupID(individual, store);
						int newGroupID = -1;
						final int parentsCount = groupsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(partnerParentsID, groupsIDs.get(i))){
								if(i + 1 < parentsCount)
									newGroupID = groupsIDs.get(i + 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(EntityManager.TABLE_NAME_GROUP);
						final Map<String, Object> newParents = groups.getOrDefault(newGroupID, Collections.emptyMap());
						groupListener.onIndividualChangeParents(BiologicalParentsPanel.this, motherPanel, newParents);
					}
				}
			});
			motherPreviousGroupLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)motherPreviousGroupLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> individual = fatherPanel.getIndividual();

						//list the `groupID`s for the groups of the `other partner`
						final Integer otherPartnerID = extractRecordID(individual);
						final List<Integer> otherPartnerGroupIDs = getGroupIDs(otherPartnerID);

						//find current group in list
						final Integer groupID = extractRecordID(group);
						int newGroupID = -1;
						final int otherPartnerGroupsCount = otherPartnerGroupIDs.size();
						for(int i = 0; i < otherPartnerGroupsCount; i ++)
							if(Objects.equals(groupID, otherPartnerGroupIDs.get(i))){
								newGroupID = otherPartnerGroupIDs.get(i - 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(EntityManager.TABLE_NAME_GROUP);
						final Map<String, Object> newGroup = groups.get(newGroupID);

						Map<String, Object> newPartner = Collections.emptyMap();
						if(!newGroup.isEmpty()){
							final TreeMap<Integer, Map<String, Object>> individuals = getRecords(EntityManager.TABLE_NAME_INDIVIDUAL);
							final List<Integer> newIndividualIDs = getIndividualIDsInGroup(extractRecordID(newGroup));
							for(int i = 0, length = newIndividualIDs.size(); i < length; i ++)
								if(newIndividualIDs.get(i).equals(otherPartnerID)){
									if(i > 0)
										newPartner = individuals.get(newIndividualIDs.get(i - 1));
									break;
								}
						}

						groupListener.onIndividualChangeGroup(BiologicalParentsPanel.this, fatherPanel, newPartner, newGroup);
					}
				}
			});
			motherNextGroupLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)motherNextGroupLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> individual = fatherPanel.getIndividual();

						//list the `groupID`s for the groups of the `other partner`
						final Integer otherPartnerID = extractRecordID(individual);
						final List<Integer> otherPartnerGroupIDs = getGroupIDs(otherPartnerID);

						//find current group in list
						final Integer groupID = extractRecordID(group);
						int newGroupID = -1;
						final int otherPartnerGroupsCount = otherPartnerGroupIDs.size();
						for(int i = 0; i < otherPartnerGroupsCount; i ++)
							if(Objects.equals(groupID, otherPartnerGroupIDs.get(i))){
								if(i + 1 < otherPartnerGroupsCount)
									newGroupID = otherPartnerGroupIDs.get(i + 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(EntityManager.TABLE_NAME_GROUP);
						final Map<String, Object> newGroup = groups.get(newGroupID);

						Map<String, Object> newPartner = Collections.emptyMap();
						if(!newGroup.isEmpty()){
							final TreeMap<Integer, Map<String, Object>> individuals = getRecords(EntityManager.TABLE_NAME_INDIVIDUAL);
							final List<Integer> newIndividualIDs = getIndividualIDsInGroup(extractRecordID(newGroup));
							for(int i = 0, length = newIndividualIDs.size(); i < length; i ++)
								if(newIndividualIDs.get(i).equals(otherPartnerID)){
									if(i + 1 < otherPartnerGroupsCount)
										newPartner = individuals.get(newIndividualIDs.get(i + 1));
									break;
								}
						}

						groupListener.onIndividualChangeGroup(BiologicalParentsPanel.this, fatherPanel, newPartner, newGroup);
					}
				}
			});
		}
	}

	public final void setIndividualListener(final IndividualListenerInterface individualListener){
		fatherPanel.setIndividualListener(individualListener);
		motherPanel.setIndividualListener(individualListener);
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


	void loadData(final Map<String, Object> group, final Map<String, Object> father, final Map<String, Object> mother){
		prepareData(group, father, mother);

		loadData();
	}

	private void prepareData(Map<String, Object> group, Map<String, Object> father, Map<String, Object> mother){
		if(group.isEmpty()){
			final List<Map<String, Object>> groups = extractGroups(father);
			if(!groups.isEmpty())
				group = groups.getFirst();
		}

		if(!group.isEmpty()){
			final Integer homeGroupID = extractRecordID(group);
			final List<Integer> individualIDsInGroup = getIndividualIDsInGroup(homeGroupID);
			Integer fatherID = extractRecordID(father);
			if(fatherID != null && !individualIDsInGroup.contains(fatherID)){
				LOGGER.warn("Individual {} does not belong to the group {} (this cannot be)", fatherID, homeGroupID);

				father = Collections.emptyMap();
			}
			Integer motherID = extractRecordID(mother);
			if(motherID != null && !individualIDsInGroup.contains(motherID)){
				LOGGER.warn("Individual {} does not belong to the group {} (this cannot be)", motherID, homeGroupID);

				mother = Collections.emptyMap();
			}

			if(father.isEmpty() || mother.isEmpty()){
				final TreeMap<Integer, Map<String, Object>> individuals = getRecords(EntityManager.TABLE_NAME_INDIVIDUAL);

				//extract the first two individuals from the group:
				if(!father.isEmpty())
					individualIDsInGroup.remove(extractRecordID(father));
				if(!mother.isEmpty())
					individualIDsInGroup.remove(extractRecordID(mother));
				if(father.isEmpty() && !individualIDsInGroup.isEmpty()){
					fatherID = individualIDsInGroup.getFirst();
					if(individuals.containsKey(fatherID))
						father = individuals.get(fatherID);
					individualIDsInGroup.remove(fatherID);
				}
				if(mother.isEmpty() && !individualIDsInGroup.isEmpty()){
					motherID = individualIDsInGroup.getFirst();
					if(individuals.containsKey(motherID))
						mother = individuals.get(motherID);
					individualIDsInGroup.remove(motherID);
				}
			}
		}

		group = group;
		this.father = father;
		this.mother = mother;
	}

	private void loadData(){
		fatherPanel.loadData(extractRecordID(father));
		motherPanel.loadData(extractRecordID(mother));

		if(boxType == BoxPanelType.PRIMARY){
			final Integer groupID = extractRecordID(group);
			updatePreviousNextGroupIcons(groupID, mother, fatherPreviousGroupLabel, fatherNextGroupLabel);
			updatePreviousNextGroupIcons(groupID, father, motherPreviousGroupLabel, motherNextGroupLabel);

			updatePreviousNextParentsIcons(father, fatherPreviousParentsLabel, fatherNextParentsLabel);
			updatePreviousNextParentsIcons(mother, motherPreviousParentsLabel, motherNextParentsLabel);
		}

		refresh(ActionCommand.ACTION_COMMAND_GROUP);

		fatherPanel.repaint();
		motherPanel.repaint();
	}

	private List<Map<String, Object>> extractGroups(final Map<String, Object> individual){
		final List<Map<String, Object>> groupGroups = new ArrayList<>(0);
		if(!individual.isEmpty()){
			final Integer individualID = extractRecordID(individual);
			groupGroups.addAll(getGroupIDs(individualID));
		}
		return groupGroups;
	}

	private List<Map<String, Object>> getGroupIDs(final Integer individualID){
		final TreeMap<Integer, Map<String, Object>> groups = getRecords(EntityManager.TABLE_NAME_GROUP);
		return getRecords(EntityManager.TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals(EntityManager.TABLE_NAME_INDIVIDUAL, extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(individualID, extractRecordReferenceID(entry)))
			.filter(entry -> Objects.equals(EntityManager.GROUP_ROLE_PARTNER, extractRecordRole(entry)))
			.map(EntityManager::extractRecordGroupID)
			.map(groups::get)
			.toList();
	}

	/** Should be called whenever a modification on the store causes modifications on the UI. */
/*	@EventHandler
	@SuppressWarnings("NumberEquality")
	public final void refresh(final Integer actionCommand){
		if(actionCommand != ActionCommand.ACTION_COMMAND_GROUP)
			return;

		final boolean hasData = !group.isEmpty();
//		final boolean hasGroups = !getRecords(TABLE_NAME_GROUP).isEmpty();
//		final boolean hasChildren = (getChildren().length > 0);
		editGroupItem.setEnabled(hasData);
		addGroupItem.setEnabled(!hasData);
//		linkGroupItem.setEnabled(!hasData && hasGroups);
		removeGroupItem.setEnabled(hasData);
	}*/

/*	private void updatePreviousNextGroupIcons(final Integer groupID, final Map<String, Object> otherPartner,
			final JLabel previousLabel, final JLabel nextLabel){
		//list the `groupID`s for the groups of the `other partner`
		final Integer otherPartnerID = extractRecordID(otherPartner);
		final List<Integer> otherPartnerGroupIDs = getGroupIDs(otherPartnerID);

		//find current group in list
		int currentGroupIndex = -1;
		final int otherPartnerGroupsCount = otherPartnerGroupIDs.size();
		for(int i = 0; i < otherPartnerGroupsCount; i ++){
			final Integer otherGroupID = otherPartnerGroupIDs.get(i);

			if(Objects.equals(groupID, otherGroupID)){
				currentGroupIndex = i;
				break;
			}
		}

		final boolean hasMoreGroups = (otherPartnerGroupsCount > 1);

		final boolean partnerPreviousEnabled = (currentGroupIndex > 0);
		previousLabel.putClientProperty(KEY_ENABLED, partnerPreviousEnabled);
		previousLabel.setCursor(Cursor.getPredefinedCursor(partnerPreviousEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		ImageIcon icon = null;
		if(hasMoreGroups)
			icon = (partnerPreviousEnabled? ICON_GROUP_PREVIOUS_ENABLED: ICON_GROUP_PREVIOUS_DISABLED);
		previousLabel.setIcon(icon);

		final boolean partnerNextEnabled = (currentGroupIndex < otherPartnerGroupsCount - 1);
		nextLabel.putClientProperty(KEY_ENABLED, partnerNextEnabled);
		nextLabel.setCursor(Cursor.getPredefinedCursor(partnerNextEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		if(hasMoreGroups)
			icon = (partnerNextEnabled? ICON_GROUP_NEXT_ENABLED: ICON_GROUP_NEXT_DISABLED);
		nextLabel.setIcon(icon);
	}

	private void updatePreviousNextParentsIcons(final Map<String, Object> partner, final JLabel previousLabel, final JLabel nextLabel){
		//list the `groupID`s for the biological group and adopting groups of the `partner`
		final Integer adopteeID = extractRecordID(partner);
		final List<Integer> groupsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

		//find current parents in list
		final Integer partnerParentsID = TreePanel.extractParentsGroupID(motherPanel.getIndividual(), store);
		int currentGroupIndex = -1;
		final int parentsCount = groupsIDs.size();
		for(int i = 0; i < parentsCount; i ++)
			if(Objects.equals(partnerParentsID, groupsIDs.get(i))){
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


		final boolean isFather = Objects.equals(extractRecordID(partner), extractRecordID(fatherPanel.getIndividual()));
		final List<Integer> otherPartnerGroupIDs = getGroupIDs(extractRecordID(isFather? mother: father));
		final boolean hasMoreGroups = (otherPartnerGroupIDs.size() > 1);
		(isFather? fatherArrowsSpacer: motherArrowsSpacer).setVisible(hasMoreParents && hasMoreGroups);
	}


/*	private TreeMap<Integer, Map<String, Object>> getRecords(final String tableName){
		return store.computeIfAbsent(tableName, k -> new TreeMap<>());
	}

	protected final TreeMap<Integer, Map<String, Object>> getFilteredRecords(final String tableName, final String filterReferenceTable,
			final Integer filterReferenceID){
		return getRecords(tableName)
			.entrySet().stream()
			.filter(entry -> Objects.equals(filterReferenceTable, extractRecordReferenceTable(entry.getValue())))
			.filter(entry -> Objects.equals(filterReferenceID, extractRecordReferenceID(entry.getValue())))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
	}

	private List<Integer> getIndividualIDsInGroup(final Integer groupID){
		return new ArrayList<>(getRecords(EntityManager.TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> EntityManager.TABLE_NAME_INDIVIDUAL.equals(extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(groupID, extractRecordGroupID(entry)))
			.filter(entry -> Objects.equals(EntityManager.GROUP_ROLE_PARTNER, extractRecordRole(entry)))
			.map(EntityManager::extractRecordReferenceID)
			.filter(Objects::nonNull)
			.toList());
	}

	private List<Integer> getGroupIDs(final Integer partnerID){
		return getRecords(EntityManager.TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals(EntityManager.TABLE_NAME_INDIVIDUAL, extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(partnerID, extractRecordReferenceID(entry)))
			.filter(entry -> Objects.equals(EntityManager.GROUP_ROLE_PARTNER, extractRecordRole(entry)))
			.map(EntityManager::extractRecordGroupID)
			.filter(Objects::nonNull)
			.toList();
	}

	private List<Integer> getBiologicalAndAdoptingParentsIDs(final Integer adopteeID){
		return getRecords(EntityManager.TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals(EntityManager.TABLE_NAME_INDIVIDUAL, extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(adopteeID, extractRecordReferenceID(entry)))
			.filter(entry -> Objects.equals(EntityManager.GROUP_ROLE_CHILD, extractRecordRole(entry))
				|| Objects.equals(EntityManager.GROUP_ROLE_ADOPTEE, extractRecordRole(entry)))
			.map(EntityManager::extractRecordGroupID)
			.filter(Objects::nonNull)
			.toList();
	}

	private List<Map<String, Object>> extractChildren(final Integer groupID){
		final TreeMap<Integer, Map<String, Object>> individuals = getRecords(EntityManager.TABLE_NAME_INDIVIDUAL);
		return getRecords(EntityManager.TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> EntityManager.TABLE_NAME_INDIVIDUAL.equals(extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(groupID, extractRecordGroupID(entry)))
			.filter(entry -> Objects.equals(EntityManager.GROUP_ROLE_CHILD, extractRecordRole(entry)))
			.map(EntityManager::extractRecordReferenceID)
			.filter(Objects::nonNull)
			.map(individuals::get)
			.toList();
	}*/


	public final Point getPaintingFatherEnterPoint(){
		final Point p = fatherPanel.getPaintingEnterPoint();
		return SwingUtilities.convertPoint(fatherPanel, p, this);
	}

	public final Point getPaintingMotherEnterPoint(){
		final Point p = motherPanel.getPaintingEnterPoint();
		return SwingUtilities.convertPoint(motherPanel, p, this);
	}

	public final Point getPaintingExitPoint(){
		Point p1 = fatherPanel.getPaintingEnterPoint();
		p1 = SwingUtilities.convertPoint(fatherPanel, p1, this);
		Point p2 = motherPanel.getPaintingEnterPoint();
		p2 = SwingUtilities.convertPoint(motherPanel, p2, this);
		return new Point((p1.x + p2.x) / 2, getHeight() - GROUP_EXITING_HEIGHT);
	}


	public static void main(String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		String modelUri = "/tests/TGMZ.flef";
		String recordId = "I1";

		final String content;
		try(final InputStream is = PartnersPanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		final BoxPanelType boxType = BoxPanelType.PRIMARY;
//		final BoxPanelType boxType = BoxPanelType.SECONDARY;

//		final GroupListenerInterface groupListener = new GroupListenerInterface(){
//			@Override
//			public void onGroupEdit(final BiologicalParentsPanel groupPanel){
//				final Map<String, Object> group = groupPanel.getGroup();
//				System.out.println("onEditGroup " + extractRecordID(group));
//			}
//
//			@Override
//			public void onGroupAdd(final BiologicalParentsPanel groupPanel){
//				System.out.println("onAddGroup");
//			}
//
//			@Override
//			public void onGroupLink(final BiologicalParentsPanel groupPanel){
//				final PersonPanel father = groupPanel.getFatherPanel();
//				final PersonPanel mother = groupPanel.getMotherPanel();
//				final Map<String, Object> group = groupPanel.group;
//				System.out.println("onLinkPersonToSiblingGroup (partner 1: " + extractRecordID(father.getPerson())
//					+ ", partner 2: " + extractRecordID(mother.getPerson()) + ", group: " + extractRecordID(group));
//			}
//
//			@Override
//			public void onGroupRemove(final BiologicalParentsPanel groupPanel){
//				final Map<String, Object> group = groupPanel.getGroup();
//				System.out.println("onRemoveGroup " + extractRecordID(group));
//			}
//
//			@Override
//			public void onPersonChangeParents(final BiologicalParentsPanel groupPanel, final PersonPanel personPanel, final Map<String, Object> newParents){
//				System.out.println("onGroupChangeParents person: " + extractRecordID(personPanel.getPerson())
//					+ ", new parents: " + extractRecordID(newParents));
//			}
//
//			@Override
//			public void onPersonChangeGroup(final BiologicalParentsPanel groupPanel, final PersonPanel oldPartner, final Map<String, Object> newPartner,
//					final Map<String, Object> newGroup){
//				final Map<String, Object> oldGroup = groupPanel.getGroup();
//				System.out.println("onPersonChangeGroup old partner: " + extractRecordID(oldPartner.getPerson())
//					+ ", old group: " + oldGroup.get("id") + ", new partner: " + extractRecordID(newPartner)
//					+ ", new group: " + extractRecordID(newGroup));
//			}
//		};
//		final PersonListenerInterface personListener = new PersonListenerInterface(){
//			@Override
//			public void onPersonFocus(final PersonPanel personPanel){
//				final Map<String, Object> person = personPanel.getPerson();
//				System.out.println("onFocusPerson " + extractRecordID(person));
//			}
//
//			@Override
//			public void onPersonEdit(final PersonPanel personPanel){
//				final Map<String, Object> person = personPanel.getPerson();
//				System.out.println("onEditPerson " + extractRecordID(person));
//			}
//
//			@Override
//			public void onPersonLink(final PersonPanel personPanel){
//				System.out.println("onLinkPerson");
//			}
//
//			@Override
//			public void onPersonAdd(final PersonPanel personPanel){
//				System.out.println("onAddPerson");
//			}
//
//			@Override
//			public void onPersonRemove(final PersonPanel personPanel){
//				final Map<String, Object> person = personPanel.getPerson();
//				System.out.println("onRemovePerson " + extractRecordID(person));
//			}
//
//			@Override
//			public void onPersonUnlinkFromParentGroup(final PersonPanel personPanel){
//				final Map<String, Object> person = personPanel.getPerson();
//				System.out.println("onUnlinkPersonFromParentGroup " + extractRecordID(person));
//			}
//
//			@Override
//			public void onPersonAddToSiblingGroup(final PersonPanel personPanel){
//				final Map<String, Object> person = personPanel.getPerson();
//				System.out.println("onAddToSiblingGroupPerson " + extractRecordID(person));
//			}
//
//			@Override
//			public void onPersonUnlinkFromSiblingGroup(final PersonPanel personPanel){
//				final Map<String, Object> person = personPanel.getPerson();
//				System.out.println("onUnlinkPersonFromSiblingGroup " + extractRecordID(person));
//			}
//
//			@Override
//			public void onPersonAddPreferredImage(final PersonPanel personPanel){
//				final Map<String, Object> person = personPanel.getPerson();
//				System.out.println("onAddPreferredImage " + extractRecordID(person));
//			}
//
//			@Override
//			public void onPersonEditPreferredImage(final PersonPanel personPanel){
//				final Map<String, Object> person = personPanel.getPerson();
//				System.out.println("onEditPreferredImage " + extractRecordID(person));
//			}
//		};


		EventQueue.invokeLater(() -> {
			final PartnersPanel panel = PartnersPanel.create(BoxPanelType.PRIMARY, model);
//			panel.withBiologicalParents(recordId);
//			panel.setGroupListener(groupListener);
//			panel.setPersonListener(personListener);
//			EventBusService.subscribe(panel);

			final JFrame frame = new JFrame();
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.NORTH);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
