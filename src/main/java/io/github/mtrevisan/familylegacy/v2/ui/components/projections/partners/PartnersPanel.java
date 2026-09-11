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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.EntityPopupMenuFactory;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeLayout;
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
import java.awt.Component;
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


/**
 * Panel representing a couple (two partners) with their biological parent
 * information.
 * <p>
 * The panel is built from three visual pieces: a father panel, a mother
 * panel, and a central group panel between them. Arrow rows above each
 * partner carry navigation shortcuts.
 * <p>
 * <b>Placeholder mode.</b> When the constructor receives a {@code null}
 * model, the panel is built as a placeholder: it uses
 * {@link IndividualPanel#createEmpty(BoxPanelType)} for both partners and
 * skips every data-dependent update. The result is a panel with exactly
 * the same preferred size as a real one, but with no individual data and
 * no listeners. This is what allows empty ancestor slots in the tree to
 * keep the same height as populated slots, so the root individual stays at
 * a fixed vertical position regardless of how many generations of
 * ancestors are actually present in the data.
 */
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
	private static final Dimension GROUP_PANEL_DIMENSION = new Dimension(12, 12);
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
	public static final Stroke CONNECTION_STROKE_ADOPTED = new BasicStroke(1.f, BasicStroke.CAP_BUTT,
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

	// State
	private JPanel groupPanel;
	private IndividualPanel fatherPanel;
	private IndividualPanel motherPanel;
	private JLabel fatherArrowsSpacer;
	private JLabel motherArrowsSpacer;
	private JLabel fatherPreviousParentsLabel;
	private JLabel fatherNextParentsLabel;
	private JLabel fatherPreviousGroupLabel;
	private JLabel fatherNextGroupLabel;
	private JPanel panel;
	private JLabel motherPreviousParentsLabel;
	private JLabel motherNextParentsLabel;
	private JLabel motherPreviousGroupLabel;
	private JLabel motherNextGroupLabel;
	private JPanel arrowMotherPanel;

	private final BoxPanelType boxType;
	private final TreeLayout treeLayout;

	private IndividualData father;
	private IndividualData mother;

	private final FLEFModel model;


	public static PartnersPanel create(final BoxPanelType boxType, final TreeLayout treeLayout, final FLEFModel model){
		return new PartnersPanel(boxType, treeLayout, model);
	}

	/**
	 * Creates a placeholder panel with no model, no data, and no listeners.
	 * The panel has exactly the same preferred size as a real one, so it can
	 * fill an empty ancestor slot without collapsing the row height.
	 *
	 * @param boxType    the panel type (PRIMARY or SECONDARY)
	 * @param treeLayout the tree orientation
	 * @return a placeholder panel
	 */
	public static PartnersPanel createEmpty(final BoxPanelType boxType, final TreeLayout treeLayout){
		return new PartnersPanel(boxType, treeLayout, null);
	}


	private PartnersPanel(final BoxPanelType boxType, final TreeLayout treeLayout, final FLEFModel model){
		this.boxType = boxType;
		this.treeLayout = treeLayout;

		this.model = model;

		initComponents();

//		attachPopupMenu();

//		installMouseListeners();
	}


	private void initComponents(){
		// When no model is provided, use empty individual panels for both
		// partners. The rest of the layout is built identically, so the
		// placeholder ends up with the same preferred size as a real panel.
		if(model == null){
			final boolean isVertical = (treeLayout == TreeLayout.VERTICAL);

			// Combine layout constraints dynamically based on direction
			final String colConstraints = (isVertical? "[grow,fill]": "[left]");
			final String rowConstraints = (isVertical
				? (PREVIOUS_NEXT_SIZE.getHeight() + NAVIGATION_DESCENDANTS_ARROW_SEPARATION) + "[bottom]"
				: "[bottom,grow]");

			setLayout(new MigLayout("ins 0", colConstraints, rowConstraints));
			setOpaque(false);

			// Direct addition of the empty component
			final JLabel label = new JLabel();
			final Dimension size = IndividualPanel.getDimension(boxType);
			label.setPreferredSize(size);
			label.setMaximumSize(size);
			add(label, isVertical? "right": StringUtils.EMPTY);

			return;
		}


		fatherPanel = IndividualPanel.create(boxType, model);
		motherPanel = IndividualPanel.create(boxType, model);

		groupPanel = new JPanel();
		groupPanel.setMinimumSize(GROUP_PANEL_DIMENSION);
		groupPanel.setMaximumSize(GROUP_PANEL_DIMENSION);
		groupPanel.setBackground(GROUP_BACKGROUND);
		groupPanel.setBorder(BorderFactory.createDashedBorder(BORDER_COLOR));

		fatherArrowsSpacer = new JLabel();
		motherArrowsSpacer = new JLabel();
		fatherArrowsSpacer.setPreferredSize(new Dimension(DESCENDANTS_ARROWS_WIDTH, 0));
		motherArrowsSpacer.setPreferredSize(new Dimension(DESCENDANTS_ARROWS_WIDTH, 0));

		fatherPreviousParentsLabel = new  JLabel();
		fatherNextParentsLabel = new  JLabel();
		fatherPreviousGroupLabel = new  JLabel();
		fatherNextGroupLabel = new  JLabel();
		final JPanel arrow1Panel = new JPanel(new MigLayout("ins 0",
			"[]0[grow]" + NAVIGATION_PARENTS_ARROW_SEPARATION + "[grow]0[]" + NAVIGATION_DESCENDANTS_ARROW_SEPARATION + "[]"));
		arrow1Panel.add(fatherArrowsSpacer, StringUtils.EMPTY);
		arrow1Panel.add(fatherPreviousParentsLabel, "right");
		arrow1Panel.add(fatherNextParentsLabel, "left");
		arrow1Panel.add(fatherPreviousGroupLabel, "right");
		arrow1Panel.add(fatherNextGroupLabel, "right");
		arrow1Panel.setOpaque(false);

		panel = new JPanel(new MigLayout("ins 0",
			"[grow,fill]",
			"[" + PREVIOUS_NEXT_SIZE.getHeight() + "]" + NAVIGATION_DESCENDANTS_ARROW_SEPARATION + "[]"));
		panel.add(arrow1Panel, "wrap");
		panel.add(fatherPanel, "right");
		panel.setOpaque(false);

		motherPreviousGroupLabel = new JLabel();
		motherNextGroupLabel = new JLabel();
		motherPreviousParentsLabel = new JLabel();
		motherNextParentsLabel = new JLabel();
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

		if(treeLayout == TreeLayout.VERTICAL){
			setLayout(new MigLayout("ins 0",
				"[right,grow]" + HALF_PARTNER_SEPARATION + "[center,grow]" + HALF_PARTNER_SEPARATION + "[left,grow]",
				"[bottom]"));
			add(panel, "right,grow");
			add(groupPanel, "gapbottom " + GROUP_EXITING_HEIGHT);
			add(arrowMotherPanel, "left,grow");
		}
		else{
			setLayout(new MigLayout("ins 0",
				"[left]",
				"[bottom,grow]" + HALF_PARTNER_SEPARATION + "[center]" + HALF_PARTNER_SEPARATION + "[top,grow]"));
			add(panel, "wrap");
			add(groupPanel, "gapleft " + GROUP_EXITING_HEIGHT + ",gaptop " + NAVIGATION_ARROW_HEIGHT + ",wrap");
			add(arrowMotherPanel, "grow");
		}
		setOpaque(false);
	}

	@Override
	protected final void paintComponent(final Graphics g){
		if(groupPanel == null || !groupPanel.isVisible())
			return;

		if(g instanceof Graphics2D && panel != null && arrowMotherPanel != null){
			final Graphics2D g2 = (Graphics2D)g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			g2.setStroke(CONNECTION_STROKE);

			if(treeLayout == TreeLayout.VERTICAL){
				final int xFrom = panel.getX() + panel.getWidth();
				final int xTo = arrowMotherPanel.getX();
				final int y = panel.getY() + panel.getHeight() - GROUP_CONNECTION_HEIGHT;

				// Horizontal connection line between partners
				g2.drawLine(xFrom, y,
					xTo, y);
			}
			else{
				final int x = panel.getX() + GROUP_CONNECTION_HEIGHT;
				final int yFrom = panel.getY() + panel.getHeight();
				final int yTo = arrowMotherPanel.getY() + NAVIGATION_ARROW_HEIGHT;

				// Vertical connection line between partners
				g2.drawLine(x, yFrom,
					x, yTo);
			}

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


	public final IndividualPanel getFatherPanel(){
		return fatherPanel;
	}

	public final IndividualPanel getMotherPanel(){
		return motherPanel;
	}


	/**
	 * Attaches the given listener and popup factory to both partner panels.
	 * <p>
	 * On a placeholder (no model), this method is a no-op, because
	 * placeholders have no interactive content and no backing records.
	 */
	public PartnersPanel withListener(final IndividualListener listener,
			final EntityPopupMenuFactory<IndividualPanel, IndividualListener> factory){
		if(model == null)
			return this;

		fatherPanel.withListener(listener, factory);
		motherPanel.withListener(listener, factory);

		return this;
	}

	/**
	 * Sets the biological parents of this couple.
	 * <p>
	 * On a placeholder (no model), this method is a no-op.
	 */
	public PartnersPanel withBiologicalParents(final IndividualData father, final IndividualData mother){
		if(model == null)
			return this;

		this.father = father;
		this.mother = mother;

		updateData();

		return this;
	}

	private void updateData(){
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

		if(isPrimaryBox()){
			final boolean hasFather = (father != null && !father.isEmpty());
			final boolean hasMother = (mother != null && !mother.isEmpty());
			final boolean hasChildren = (hasFather && fatherPanel.getData().hasChildren()
				|| hasMother && motherPanel.getData().hasChildren());
			if(!hasChildren){
				if(!hasFather)
					fatherPanel.setVisible(false);
				if(!hasMother)
					motherPanel.setVisible(false);
				groupPanel.setVisible(hasFather && hasMother);
			}
		}
	}

//	private boolean hasChildren(final String fatherId, final String motherId){
//		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
//		for(final FLEFRecord relationship : relationships){
//			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
//			if(type != null && relationshipTypeFilter.test(type)){
//				final String targetId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
//				if(fatherId.equals(targetId))
//					return true;
//			}
//		}
//		return false;
//	}


	/**
	 * Finds the nearest PartnersPanel ancestor, if any.
	 *
	 * @param parent the component to start searching from
	 * @return the PartnersPanel ancestor, or {@code null} if none
	 */
	public static PartnersPanel findContainingPartnersPanel(Component parent){
		while(parent != null && !(parent instanceof PartnersPanel))
			parent = parent.getParent();
		return (PartnersPanel)parent;
	}


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
	}*/


	private boolean isPrimaryBox(){
		return (boxType == BoxPanelType.PRIMARY);
	}

	public final Point getPaintingFatherEnterPoint(){
		final Point p;
		if(treeLayout == TreeLayout.VERTICAL)
			p = fatherPanel.getPaintingVerticalEnterPoint();
		else
			p = new Point(fatherPanel.getWidth() / 2, 0);
		return SwingUtilities.convertPoint(fatherPanel, p, this);
	}

	public final Point getPaintingMotherEnterPoint(){
		final Point p;
		if(treeLayout == TreeLayout.VERTICAL)
			p = motherPanel.getPaintingVerticalEnterPoint();
		else
			p = new Point(motherPanel.getWidth() / 2, motherPanel.getHeight() - 1);
		return SwingUtilities.convertPoint(motherPanel, p, this);
	}

	public final Point getPaintingExitPoint(){
		if(treeLayout == TreeLayout.VERTICAL){
			Point p1 = fatherPanel.getPaintingVerticalEnterPoint();
			p1 = SwingUtilities.convertPoint(fatherPanel, p1, this);
			Point p2 = motherPanel.getPaintingVerticalEnterPoint();
			p2 = SwingUtilities.convertPoint(motherPanel, p2, this);
			return new Point((p1.x + p2.x) / 2, getHeight() - GROUP_EXITING_HEIGHT - 1);
		}
		Point p1 = fatherPanel.getPaintingHorizontalEnterPoint();
		p1 = SwingUtilities.convertPoint(fatherPanel, p1, this);
		Point p2 = motherPanel.getPaintingHorizontalEnterPoint();
		p2 = SwingUtilities.convertPoint(motherPanel, p2, this);
		return new Point(GROUP_EXITING_HEIGHT, (p1.y + p2.y - 1) / 2);
	}


	public Side getSideOf(final IndividualPanel panel){
		if(panel == fatherPanel)
			return Side.LEFT;
		if(panel == motherPanel)
			return Side.RIGHT;
		return null;
	}

	public boolean isEmpty(){
		return (father == null && mother == null);
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


		EventQueue.invokeLater(() -> {
			final PartnersPanel panel = PartnersPanel.create(BoxPanelType.PRIMARY, TreeLayout.VERTICAL, model);
//			panel.withBiologicalParents(recordId);
//			panel.setGroupListener(groupListener);
//			panel.setPersonListener(personListener);

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
