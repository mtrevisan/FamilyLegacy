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
package io.github.mtrevisan.familylegacy.v2.ui.components.biologicalparents;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.structures.NoteStructureDialog;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ResourceHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
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


public class BiologicalParentsPanel extends JPanel{

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
	/** Height of the union line from the bottom of the individual panel [px]. */
	private static final int GROUP_CONNECTION_HEIGHT = 15;
	private static final Dimension UNION_PANEL_DIMENSION = new Dimension(13, 12);
	private static final int GROUP_EXITING_HEIGHT = GROUP_CONNECTION_HEIGHT - UNION_PANEL_DIMENSION.height / 2;
	private static final int HALF_PARTNER_SEPARATION = 6;
	private static final int GROUP_SEPARATION = HALF_PARTNER_SEPARATION + UNION_PANEL_DIMENSION.width + HALF_PARTNER_SEPARATION;
	/** Distance between navigation union arrow and box. */
	private static final int NAVIGATION_UNION_ARROW_SEPARATION = 2;
	/** Distance between navigation parents arrow and box. */
	private static final int NAVIGATION_PARENTS_ARROW_SEPARATION = (NAVIGATION_UNION_ARROW_SEPARATION << 1) + 3;
	private static final int NAVIGATION_ARROW_HEIGHT = (int)(PREVIOUS_NEXT_SIZE.getHeight() + NAVIGATION_UNION_ARROW_SEPARATION);
	private static final int UNION_ARROWS_WIDTH = (int)Math.round(PREVIOUS_NEXT_WIDTH + NAVIGATION_UNION_ARROW_SEPARATION
		+ PREVIOUS_NEXT_WIDTH);
	private static final Stroke CONNECTION_STROKE = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f);
	private static final Stroke CONNECTION_STROKE_ADOPTED = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f,
		new float[]{2.f}, 0.f);

	// Icons
	//https://thenounproject.com/search/?q=cut&i=3132059
	//https://snappygoat.com/free-public-domain-images-app_application_arrow_back_0/
	private static final ImageIcon ICON_PARENTS_PREVIOUS_ENABLED = ResourceHelper.getResizedImage("/images/parents_previous.png",
		PREVIOUS_NEXT_SIZE);
	private static final ImageIcon ICON_PARENTS_PREVIOUS_DISABLED = new ImageIcon(
		GrayFilter.createDisabledImage(ICON_PARENTS_PREVIOUS_ENABLED.getImage()));
	private static final ImageIcon ICON_PARENTS_NEXT_ENABLED = ResourceHelper.getResizedImage("/images/parents_next.png",
		PREVIOUS_NEXT_SIZE);
	private static final ImageIcon ICON_PARENTS_NEXT_DISABLED = new ImageIcon(
		GrayFilter.createDisabledImage(ICON_PARENTS_NEXT_ENABLED.getImage()));
	private static final ImageIcon ICON_UNION_PREVIOUS_ENABLED = ResourceHelper.getResizedImage("/images/union_previous.png",
		PREVIOUS_NEXT_SIZE);
	private static final ImageIcon ICON_UNION_PREVIOUS_DISABLED = new ImageIcon(
		GrayFilter.createDisabledImage(ICON_UNION_PREVIOUS_ENABLED.getImage()));
	private static final ImageIcon ICON_UNION_NEXT_ENABLED = ResourceHelper.getResizedImage("/images/union_next.png",
		PREVIOUS_NEXT_SIZE);
	private static final ImageIcon ICON_UNION_NEXT_DISABLED = new ImageIcon(
		GrayFilter.createDisabledImage(ICON_UNION_NEXT_ENABLED.getImage()));
	private static final Dimension NEXT_PREVIOUS_UNION_PREFERRED_SIZE = new Dimension(ICON_UNION_PREVIOUS_ENABLED.getIconWidth(),
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
	private final JLabel fatherPreviousUnionLabel = new JLabel();
	private final JLabel fatherNextUnionLabel = new JLabel();
	private JPanel arrowFatherPanel;
	private final JLabel motherPreviousParentsLabel = new JLabel();
	private final JLabel motherNextParentsLabel = new JLabel();
	private final JLabel motherPreviousUnionLabel = new JLabel();
	private final JLabel motherNextUnionLabel = new JLabel();
	private JPanel arrowMotherPanel;
/*	private final JMenuItem editGroupItem = new JMenuItem("Edit Group…", 'E');
	private final JMenuItem addGroupItem = new JMenuItem("Add Group…", 'A');
//	private final JMenuItem linkGroupItem = new JMenuItem("Link Group…", 'L');
	private final JMenuItem removeGroupItem = new JMenuItem("Remove Group…", 'R');*/

	private final BoxPanelType boxType;

	private FLEFRecord individual;
	private FLEFRecord group;
	private FLEFRecord father;
	private FLEFRecord mother;

	private final FLEFModel model;

	private BiologicalParentsData data;


	static BiologicalParentsPanel create(final BoxPanelType boxType, final FLEFModel model){
		return new BiologicalParentsPanel(boxType, model);
	}


	private BiologicalParentsPanel(final BoxPanelType boxType, final FLEFModel model){
		this.boxType = boxType;

		this.model = model;

		initComponents();

//		attachPopupMenu();

//		installMouseListeners();
	}


	private void initComponents(){
		groupPanel.setBackground(GROUP_BACKGROUND);
		fatherPanel = IndividualPanel.create(boxType, model);
//		EventBusService.subscribe(fatherPanel);
		motherPanel = IndividualPanel.create(boxType, model);
//		EventBusService.subscribe(motherPanel);

		fatherArrowsSpacer.setPreferredSize(new Dimension(UNION_ARROWS_WIDTH, 0));
		motherArrowsSpacer.setPreferredSize(new Dimension(UNION_ARROWS_WIDTH, 0));

		final JPanel arrow1Panel = new JPanel(new MigLayout("ins 0",
			"[]0[grow]" + NAVIGATION_PARENTS_ARROW_SEPARATION + "[grow]0[]" + NAVIGATION_UNION_ARROW_SEPARATION + "[]"));
		arrow1Panel.add(fatherArrowsSpacer, "");
		arrow1Panel.add(fatherPreviousParentsLabel, "right");
		arrow1Panel.add(fatherNextParentsLabel, "left");
		arrow1Panel.add(fatherPreviousUnionLabel, "right");
		arrow1Panel.add(fatherNextUnionLabel, "right");
		arrow1Panel.setOpaque(false);

		arrowFatherPanel = new JPanel(new MigLayout("ins 0",
			"[grow,fill]",
			"[" + PREVIOUS_NEXT_SIZE.getHeight() + "]" + NAVIGATION_UNION_ARROW_SEPARATION + "[]"));
		arrowFatherPanel.add(arrow1Panel, "wrap");
		arrowFatherPanel.add(fatherPanel, "right");
		arrowFatherPanel.setOpaque(false);

		final JPanel arrow2Panel = new JPanel(new MigLayout("ins 0",
			"[]" + NAVIGATION_UNION_ARROW_SEPARATION + "[]0[grow]" + NAVIGATION_PARENTS_ARROW_SEPARATION + "[grow]0[]"));
		arrow2Panel.add(motherPreviousUnionLabel, "left");
		arrow2Panel.add(motherNextUnionLabel, "left");
		arrow2Panel.add(motherPreviousParentsLabel, "right");
		arrow2Panel.add(motherNextParentsLabel, "left");
		arrow2Panel.add(motherArrowsSpacer, "hidemode 2");
		arrow2Panel.setOpaque(false);

		arrowMotherPanel = new JPanel(new MigLayout("ins 0",
			"[grow,fill]",
			"[" + PREVIOUS_NEXT_SIZE.getHeight() + "]" + NAVIGATION_UNION_ARROW_SEPARATION + "[]"));
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
		super.paintComponent(g);

		if(g instanceof Graphics2D && arrowFatherPanel != null && arrowMotherPanel != null){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			graphics2D.setStroke(CONNECTION_STROKE);

			final int xFrom = arrowFatherPanel.getX() + arrowFatherPanel.getWidth();
			final int xTo = arrowMotherPanel.getX();
			final int yFrom = arrowFatherPanel.getY() + arrowFatherPanel.getHeight() - GROUP_CONNECTION_HEIGHT;
			//horizontal line between partners
			graphics2D.drawLine(xFrom, yFrom, xTo, yFrom);


			//for test purposes
//			pointTest(graphics2D);


			graphics2D.dispose();
		}
	}

	private void pointTest(final Graphics2D graphics2D){
		final Point enterPoint1 = getPaintingFatherEnterPoint();
		graphics2D.setColor(Color.RED);
		graphics2D.drawLine(enterPoint1.x - 10, enterPoint1.y - 10, enterPoint1.x + 10, enterPoint1.y + 10);
		graphics2D.drawLine(enterPoint1.x + 10, enterPoint1.y - 10, enterPoint1.x - 10, enterPoint1.y + 10);

		final Point enterPoint2 = getPaintingMotherEnterPoint();
		graphics2D.drawLine(enterPoint2.x - 10, enterPoint2.y - 10, enterPoint2.x + 10, enterPoint2.y + 10);
		graphics2D.drawLine(enterPoint2.x + 10, enterPoint2.y - 10, enterPoint2.x - 10, enterPoint2.y + 10);

		final Point exitPoint = getPaintingExitPoint();
		graphics2D.drawLine(exitPoint.x - 10, exitPoint.y - 10, exitPoint.x + 10, exitPoint.y + 10);
		graphics2D.drawLine(exitPoint.x + 10, exitPoint.y - 10, exitPoint.x - 10, exitPoint.y + 10);
		graphics2D.setColor(Color.BLACK);
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


	public void withBiologicalParentsOf(final String individualId){
		individual = model.getRecordById(individualId);

		updateGroupData();
	}

	private void updateGroupData(){
		data = new BiologicalParentsData(individual, boxType, model);
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

						//list the `groupID`s for the biological union and adopting unions of the `partner`
						final Integer adopteeID = extractRecordID(individual);
						final List<Integer> unionsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Integer partnerParentsID = TreePanel.extractParentsGroupID(individual, store);
						int newGroupID = -1;
						final int parentsCount = unionsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(partnerParentsID, unionsIDs.get(i))){
								if(i > 0)
									newGroupID = unionsIDs.get(i - 1);
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

						//list the `groupID`s for the biological union and adopting unions of the `partner`
						final Integer adopteeID = extractRecordID(individual);
						final List<Integer> unionsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Integer partnerParentsID = TreePanel.extractParentsGroupID(individual, store);
						int newGroupID = -1;
						final int parentsCount = unionsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(partnerParentsID, unionsIDs.get(i))){
								if(i + 1 < parentsCount)
									newGroupID = unionsIDs.get(i + 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(EntityManager.TABLE_NAME_GROUP);
						final Map<String, Object> newParents = groups.getOrDefault(newGroupID, Collections.emptyMap());
						groupListener.onIndividualChangeParents(BiologicalParentsPanel.this, fatherPanel, newParents);
					}
				}
			});
			fatherPreviousUnionLabel.setPreferredSize(NEXT_PREVIOUS_UNION_PREFERRED_SIZE);
			fatherPreviousUnionLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)fatherPreviousUnionLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> individual = motherPanel.getIndividual();

						//list the `groupID`s for the unions of the `other partner`
						final Integer otherPartnerID = extractRecordID(individual);
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

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(EntityManager.TABLE_NAME_GROUP);
						final Map<String, Object> newUnion = groups.getOrDefault(newGroupID, Collections.emptyMap());

						Map<String, Object> newPartner = Collections.emptyMap();
						if(!newUnion.isEmpty()){
							final TreeMap<Integer, Map<String, Object>> individuals = getRecords(EntityManager.TABLE_NAME_INDIVIDUAL);
							final List<Integer> newIndividualIDs = getIndividualIDsInGroup(extractRecordID(newUnion));
							for(int i = 0, length = newIndividualIDs.size(); i < length; i ++)
								if(newIndividualIDs.get(i).equals(otherPartnerID)){
									if(i > 0)
										newPartner = individuals.get(newIndividualIDs.get(i - 1));
									break;
								}
						}

						groupListener.onIndividualChangeUnion(BiologicalParentsPanel.this, motherPanel, newPartner, newUnion);
					}
				}
			});
			fatherNextUnionLabel.setPreferredSize(NEXT_PREVIOUS_UNION_PREFERRED_SIZE);
			fatherNextUnionLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)fatherNextUnionLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> individual = motherPanel.getIndividual();

						//list the `groupID`s for the unions of the `other partner`
						final Integer otherPartnerID = extractRecordID(individual);
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

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(EntityManager.TABLE_NAME_GROUP);
						final Map<String, Object> newUnion = groups.getOrDefault(newGroupID, Collections.emptyMap());

						Map<String, Object> newPartner = Collections.emptyMap();
						if(!newUnion.isEmpty()){
							final TreeMap<Integer, Map<String, Object>> individuals = getRecords(EntityManager.TABLE_NAME_INDIVIDUAL);
							final List<Integer> newIndividualIDs = getIndividualIDsInGroup(extractRecordID(newUnion));
							for(int i = 0, length = newIndividualIDs.size(); i < length; i ++)
								if(newIndividualIDs.get(i).equals(otherPartnerID)){
									if(i + 1 < otherPartnerUnionsCount)
										newPartner = individuals.get(newIndividualIDs.get(i + 1));
									break;
								}
						}

						groupListener.onIndividualChangeUnion(BiologicalParentsPanel.this, motherPanel, newPartner, newUnion);
					}
				}
			});
			motherPreviousParentsLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)motherPreviousParentsLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> individual = motherPanel.getIndividual();

						//list the `groupID`s for the biological union and adopting unions of the `partner`
						final Integer adopteeID = extractRecordID(individual);
						final List<Integer> unionsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Integer partnerParentsID = TreePanel.extractParentsGroupID(individual, store);
						int newGroupID = -1;
						final int parentsCount = unionsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(partnerParentsID, unionsIDs.get(i))){
								if(i > 0)
									newGroupID = unionsIDs.get(i - 1);
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

						//list the `groupID`s for the biological union and adopting unions of the `partner`
						final Integer adopteeID = extractRecordID(individual);
						final List<Integer> unionsIDs = getBiologicalAndAdoptingParentsIDs(adopteeID);

						//find current parents in list
						final Integer partnerParentsID = TreePanel.extractParentsGroupID(individual, store);
						int newGroupID = -1;
						final int parentsCount = unionsIDs.size();
						for(int i = 0; i < parentsCount; i ++)
							if(Objects.equals(partnerParentsID, unionsIDs.get(i))){
								if(i + 1 < parentsCount)
									newGroupID = unionsIDs.get(i + 1);
								break;
							}

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(EntityManager.TABLE_NAME_GROUP);
						final Map<String, Object> newParents = groups.getOrDefault(newGroupID, Collections.emptyMap());
						groupListener.onIndividualChangeParents(BiologicalParentsPanel.this, motherPanel, newParents);
					}
				}
			});
			motherPreviousUnionLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)motherPreviousUnionLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> individual = fatherPanel.getIndividual();

						//list the `groupID`s for the unions of the `other partner`
						final Integer otherPartnerID = extractRecordID(individual);
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

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(EntityManager.TABLE_NAME_GROUP);
						final Map<String, Object> newUnion = groups.get(newGroupID);

						Map<String, Object> newPartner = Collections.emptyMap();
						if(!newUnion.isEmpty()){
							final TreeMap<Integer, Map<String, Object>> individuals = getRecords(EntityManager.TABLE_NAME_INDIVIDUAL);
							final List<Integer> newIndividualIDs = getIndividualIDsInGroup(extractRecordID(newUnion));
							for(int i = 0, length = newIndividualIDs.size(); i < length; i ++)
								if(newIndividualIDs.get(i).equals(otherPartnerID)){
									if(i > 0)
										newPartner = individuals.get(newIndividualIDs.get(i - 1));
									break;
								}
						}

						groupListener.onIndividualChangeUnion(BiologicalParentsPanel.this, fatherPanel, newPartner, newUnion);
					}
				}
			});
			motherNextUnionLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)motherNextUnionLabel.getClientProperty(KEY_ENABLED)){
						final Map<String, Object> individual = fatherPanel.getIndividual();

						//list the `groupID`s for the unions of the `other partner`
						final Integer otherPartnerID = extractRecordID(individual);
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

						final TreeMap<Integer, Map<String, Object>> groups = getRecords(EntityManager.TABLE_NAME_GROUP);
						final Map<String, Object> newUnion = groups.get(newGroupID);

						Map<String, Object> newPartner = Collections.emptyMap();
						if(!newUnion.isEmpty()){
							final TreeMap<Integer, Map<String, Object>> individuals = getRecords(EntityManager.TABLE_NAME_INDIVIDUAL);
							final List<Integer> newIndividualIDs = getIndividualIDsInGroup(extractRecordID(newUnion));
							for(int i = 0, length = newIndividualIDs.size(); i < length; i ++)
								if(newIndividualIDs.get(i).equals(otherPartnerID)){
									if(i + 1 < otherPartnerUnionsCount)
										newPartner = individuals.get(newIndividualIDs.get(i + 1));
									break;
								}
						}

						groupListener.onIndividualChangeUnion(BiologicalParentsPanel.this, fatherPanel, newPartner, newUnion);
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
			final List<Map<String, Object>> unions = extractUnions(father);
			if(!unions.isEmpty())
				group = unions.getFirst();
		}

		if(!group.isEmpty()){
			final Integer homeUnionID = extractRecordID(group);
			final List<Integer> individualIDsInUnion = getIndividualIDsInGroup(homeUnionID);
			Integer fatherID = extractRecordID(father);
			if(fatherID != null && !individualIDsInUnion.contains(fatherID)){
				LOGGER.warn("Individual {} does not belong to the union {} (this cannot be)", fatherID, homeUnionID);

				father = Collections.emptyMap();
			}
			Integer motherID = extractRecordID(mother);
			if(motherID != null && !individualIDsInUnion.contains(motherID)){
				LOGGER.warn("Individual {} does not belong to the union {} (this cannot be)", motherID, homeUnionID);

				mother = Collections.emptyMap();
			}

			if(father.isEmpty() || mother.isEmpty()){
				final TreeMap<Integer, Map<String, Object>> individuals = getRecords(EntityManager.TABLE_NAME_INDIVIDUAL);

				//extract the first two individuals from the union:
				if(!father.isEmpty())
					individualIDsInUnion.remove(extractRecordID(father));
				if(!mother.isEmpty())
					individualIDsInUnion.remove(extractRecordID(mother));
				if(father.isEmpty() && !individualIDsInUnion.isEmpty()){
					fatherID = individualIDsInUnion.getFirst();
					if(individuals.containsKey(fatherID))
						father = individuals.get(fatherID);
					individualIDsInUnion.remove(fatherID);
				}
				if(mother.isEmpty() && !individualIDsInUnion.isEmpty()){
					motherID = individualIDsInUnion.getFirst();
					if(individuals.containsKey(motherID))
						mother = individuals.get(motherID);
					individualIDsInUnion.remove(motherID);
				}
			}
		}

		union = group;
		this.father = father;
		this.mother = mother;
	}

	private void loadData(){
		fatherPanel.loadData(extractRecordID(father));
		motherPanel.loadData(extractRecordID(mother));

		if(boxType == BoxPanelType.PRIMARY){
			final Integer groupID = extractRecordID(union);
			updatePreviousNextUnionIcons(groupID, mother, fatherPreviousUnionLabel, fatherNextUnionLabel);
			updatePreviousNextUnionIcons(groupID, father, motherPreviousUnionLabel, motherNextUnionLabel);

			updatePreviousNextParentsIcons(father, fatherPreviousParentsLabel, fatherNextParentsLabel);
			updatePreviousNextParentsIcons(mother, motherPreviousParentsLabel, motherNextParentsLabel);
		}

		groupPanel.setBorder(!union.isEmpty()? BorderFactory.createLineBorder(BORDER_COLOR):
			BorderFactory.createDashedBorder(BORDER_COLOR));

		refresh(ActionCommand.ACTION_COMMAND_GROUP);

		fatherPanel.repaint();
		motherPanel.repaint();
	}

	private List<Map<String, Object>> extractUnions(final Map<String, Object> individual){
		final List<Map<String, Object>> unionGroups = new ArrayList<>(0);
		if(!individual.isEmpty()){
			final Integer individualID = extractRecordID(individual);
			unionGroups.addAll(getGroupIDs(individualID));
		}
		return unionGroups;
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
		final Integer partnerParentsID = TreePanel.extractParentsGroupID(motherPanel.getIndividual(), store);
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


		final boolean isFather = Objects.equals(extractRecordID(partner), extractRecordID(fatherPanel.getIndividual()));
		final List<Integer> otherPartnerUnionIDs = getUnionIDs(extractRecordID(isFather? mother: father));
		final boolean hasMoreUnions = (otherPartnerUnionIDs.size() > 1);
		(isFather? fatherArrowsSpacer: motherArrowsSpacer).setVisible(hasMoreParents && hasMoreUnions);
	}


	private TreeMap<Integer, Map<String, Object>> getRecords(final String tableName){
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

	private List<Integer> getUnionIDs(final Integer partnerID){
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

	private List<Map<String, Object>> extractChildren(final Integer unionID){
		final TreeMap<Integer, Map<String, Object>> individuals = getRecords(EntityManager.TABLE_NAME_INDIVIDUAL);
		return getRecords(EntityManager.TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> EntityManager.TABLE_NAME_INDIVIDUAL.equals(extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(unionID, extractRecordGroupID(entry)))
			.filter(entry -> Objects.equals(EntityManager.GROUP_ROLE_CHILD, extractRecordRole(entry)))
			.map(EntityManager::extractRecordReferenceID)
			.filter(Objects::nonNull)
			.map(individuals::get)
			.toList();
	}*/


	public final Point getPaintingFatherEnterPoint(){
		final Point p1 = fatherPanel.getPaintingEnterPoint();
		final Point origin = getLocation();
		return new Point(origin.x + p1.x,
			origin.y + p1.y);
	}

	public final Point getPaintingMotherEnterPoint(){
		final Point p1 = fatherPanel.getPaintingEnterPoint();
		final Point p2 = motherPanel.getPaintingEnterPoint();
		final Point origin = getLocation();
		return new Point(origin.x + getWidth() + (p2.x - p1.x - motherPanel.getWidth()) / 2,
			origin.y + p2.y);
	}

	public final Point getPaintingExitPoint(){
		final Point p1 = fatherPanel.getPaintingEnterPoint();
		final Point p2 = motherPanel.getPaintingEnterPoint();
		final Point origin = getLocation();
		return new Point(origin.x + ((p1.x + p2.x - motherPanel.getWidth()) / 2 + getWidth()) / 2,
			origin.y + getHeight() - GROUP_EXITING_HEIGHT);
	}


	public static void main(String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		String modelUri = "/tests/TGMZ.flef";
		String recordId = "I1";

		final String content;
		try(final InputStream is = NoteStructureDialog.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		final BoxPanelType boxType = BoxPanelType.PRIMARY;
//		final BoxPanelType boxType = BoxPanelType.SECONDARY;

//		final GroupListenerInterface unionListener = new GroupListenerInterface(){
//			@Override
//			public void onGroupEdit(final BiologicalParentsPanel groupPanel){
//				final Map<String, Object> group = groupPanel.getUnion();
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
//				final Map<String, Object> group = groupPanel.union;
//				System.out.println("onLinkPersonToSiblingGroup (partner 1: " + extractRecordID(father.getPerson())
//					+ ", partner 2: " + extractRecordID(mother.getPerson()) + ", group: " + extractRecordID(group));
//			}
//
//			@Override
//			public void onGroupRemove(final BiologicalParentsPanel groupPanel){
//				final Map<String, Object> group = groupPanel.getUnion();
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
//			public void onPersonChangeUnion(final BiologicalParentsPanel groupPanel, final PersonPanel oldPartner, final Map<String, Object> newPartner,
//					final Map<String, Object> newUnion){
//				final Map<String, Object> oldUnion = groupPanel.getUnion();
//				System.out.println("onPersonChangeUnion old partner: " + extractRecordID(oldPartner.getPerson())
//					+ ", old union: " + oldUnion.get("id") + ", new partner: " + extractRecordID(newPartner)
//					+ ", new union: " + extractRecordID(newUnion));
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
			final BiologicalParentsPanel panel = BiologicalParentsPanel.create(BoxPanelType.PRIMARY, model);
			panel.withBiologicalParentsOf(recordId);
//			panel.setGroupListener(unionListener);
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
