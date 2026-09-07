package io.github.mtrevisan.familylegacy.v2.ui.components.siblings;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree.TreeLayout;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ResourceHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * UI panel representing a line of sibling individual boxes with visual indicators for descendants.
 */
public class SiblingsPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -4829104812837192834L;


	private static final int DESCENDANTS_HEIGHT = 12;
	private static final double DESCENDANTS_ASPECT_RATIO = 3501. / 2662.;
	private static final Dimension DESCENDANTS_SIZE = new Dimension((int)((float)DESCENDANTS_HEIGHT / DESCENDANTS_ASPECT_RATIO), DESCENDANTS_HEIGHT);

	private static ImageIcon ICON_DESCENDANTS;

	private static final int SIBLING_SEPARATION = 14;
	public static final int DESCENDANTS_ARROW_HEIGHT = DESCENDANTS_HEIGHT
		+ PartnersPanel.NAVIGATION_DESCENDANTS_ARROW_SEPARATION;


	private final FLEFRecord father;
	private final FLEFRecord mother;
	private final BoxPanelType boxType;
	private final boolean showPartner;
	private final TreeLayout treeLayout;

	private final FLEFModel model;

	private final List<IndividualPanel> siblingBoxes = new ArrayList<>();
	private SiblingsData data;

	private IndividualListener listener;


	public static SiblingsPanel create(final FLEFRecord father, final FLEFRecord mother, final BoxPanelType boxType,
			final FLEFModel model, final boolean showPartner, final TreeLayout treeLayout){
		return new SiblingsPanel(father, mother, boxType, model, showPartner, treeLayout);
	}


	private SiblingsPanel(final FLEFRecord father, final FLEFRecord mother, final BoxPanelType boxType,
			final FLEFModel model, final boolean showPartner, final TreeLayout treeLayout){
		String iconDescendantsUri = (treeLayout == TreeLayout.VERTICAL? "/images/union.png": "/images/union_previous.png");
		ICON_DESCENDANTS = ResourceHelper.getResizedImageFromResource(iconDescendantsUri, DESCENDANTS_SIZE);

		this.father = father;
		this.mother = mother;
		this.boxType = boxType;
		this.showPartner = showPartner;
		this.treeLayout = treeLayout;

		this.model = model;


		initComponents();
	}


	private void initComponents(){
		setOpaque(false);

		setLayout(new MigLayout(treeLayout == TreeLayout.VERTICAL
			? "flowx,ins " + DESCENDANTS_ARROW_HEIGHT + " 0 0 0,alignx center"
			: "flowy,ins 0 0 0 " + DESCENDANTS_ARROW_HEIGHT + ",aligny center"));
	}


	@Override
	protected final void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D){
			final Graphics2D g2 = (Graphics2D)g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);


			//for test purposes
//			pointTest(g2);

			final Point[] enterPoints = getPaintingEnterPoints();
			if(enterPoints.length > 0){
				if(treeLayout == TreeLayout.VERTICAL){
					final int firstChildX = enterPoints[0].x;
					final int lastChildX = enterPoints[enterPoints.length - 1].x;

					// Horizontal line spanning from first to last child
					g2.drawLine(firstChildX, 0,
						lastChildX, 0);

					// Vertical line connecting the children
					for(int i = 0; i < enterPoints.length; i ++){
						final Point point = enterPoints[i];
						g2.drawLine(point.x, point.y,
							point.x, 0);
					}
				}
				else{
					final int firstChildY = enterPoints[0].y;
					final int lastChildY = enterPoints[enterPoints.length - 1].y;
					// Vertical line spanning from first to last child
					g2.drawLine(enterPoints[0].x, firstChildY,
						enterPoints[0].x, lastChildY);

					// Horizontal line connecting each child to the vertical bar
					for(int i = 0; i < enterPoints.length; i ++){
						final Component comp = ((Container)getComponent(i))
							.getComponent(0);

						final Point point = enterPoints[i];
						Point p = new Point(comp.getWidth(), (comp.getHeight() + DESCENDANTS_ARROW_HEIGHT - 1) / 2);
						p = SwingUtilities.convertPoint(comp, p, this);
						g2.drawLine(point.x, point.y,
							p.x, point.y);
					}
				}
			}

			g2.dispose();
		}
	}

	private void pointTest(final Graphics2D g2){
		final Point[] enterPoints = getPaintingEnterPoints();
		for(final Point enterPoint : enterPoints)
			GUIHelper.drawX(g2, enterPoint);
	}

	public SiblingsPanel withListener(final IndividualListener listener){
		this.listener = listener;

		return this;
	}

	/**
	 * Populates the panel with siblings data. Ensures at least one placeholder box exists.
	 *
	 * @param data	Data containing the list of siblings/children.
	 */
	public SiblingsPanel withSiblingsData(final SiblingsData data){
		this.data = data;

		refreshData();

		return this;
	}

	private void refreshData(){
		removeAll();
		siblingBoxes.clear();

		if(data != null){
			final List<IndividualData> siblings = data.getSiblings();
			for(int i = 0, siblingsCount = siblings.size(); i < siblingsCount; i ++){
				final IndividualData siblingData = siblings.get(i);
				final String siblingId = siblingData.getIndividualId();
				final boolean hasDescendants = data.hasDescendants(siblingId);

				final JPanel boxContainer = createSiblingContainer(hasDescendants);
				final IndividualPanel siblingBox = IndividualPanel.create(boxType, model)
					.withListener(listener)
					.withParent(father, mother)
					.withIndividualData(siblingData);
				boxContainer.add(siblingBox);
				final String constraint = (showPartner || i < siblingsCount - 1
					? (treeLayout == TreeLayout.VERTICAL ? "gapright " : "gapbottom " + SIBLING_SEPARATION)
					: StringUtils.EMPTY);
				add(boxContainer, constraint);
				siblingBoxes.add(siblingBox);
			}
		}

		if(data == null || !data.isOnlyRoot()){
			// Add empty placeholder box for adding a new sibling
			final JPanel emptyBoxContainer = createSiblingContainer(false);
			final IndividualPanel emptySiblingBox = IndividualPanel.create(boxType, model)
				.withListener(listener)
				.withParent(father, mother);
			emptyBoxContainer.add(emptySiblingBox);
			add(emptyBoxContainer);
			siblingBoxes.add(emptySiblingBox);
		}

		revalidate();
		repaint();
	}

	private JPanel createSiblingContainer(final boolean hasDescendants){
		final JPanel container;
		if(treeLayout == TreeLayout.VERTICAL)
			container = new JPanel(new MigLayout("flowy,ins 0", "[]", "[top]" + PartnersPanel.NAVIGATION_DESCENDANTS_ARROW_SEPARATION + "[]"));
		else
			container = new JPanel(new MigLayout("flowy,ins 0", "[right]" + PartnersPanel.NAVIGATION_DESCENDANTS_ARROW_SEPARATION + "[]", "[]"));
		container.setOpaque(false);

		final JLabel descendantsLabel = new JLabel();
		descendantsLabel.setPreferredSize(new Dimension(ICON_DESCENDANTS.getIconWidth(), ICON_DESCENDANTS.getIconHeight()));
		if(hasDescendants)
			descendantsLabel.setIcon(ICON_DESCENDANTS);
		container.add(descendantsLabel, (treeLayout == TreeLayout.VERTICAL? "right": "bottom"));
		return container;
	}

	public List<IndividualPanel> getSiblingBoxes(){
		return siblingBoxes;
	}

	public Point[] getPaintingEnterPoints(){
		final int count = getComponentCount();
		final Point[] enterPoints = new Point[count];
		if(treeLayout == TreeLayout.VERTICAL)
			for(int i = 0; i < count; i ++){
				final Component comp = getComponent(i);
				final Point p = new Point(comp.getWidth() / 2, DESCENDANTS_ARROW_HEIGHT - 1);
				enterPoints[i] = SwingUtilities.convertPoint(comp, p, this);
			}
		else
			for(int i = 0; i < count; i ++){
				final Container container = (Container)getComponent(i);
				// extract individual panel
				final Component comp = container.getComponent(container.getComponentCount() - 1);

				final Point p = new Point(comp.getWidth() + DESCENDANTS_ARROW_HEIGHT, (comp.getHeight() - 1) / 2);
				enterPoints[i] = SwingUtilities.convertPoint(comp, p, this);
			}
		return enterPoints;
	}


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final String modelUri = "/tests/TGMZ.flef";

		final String content;
		try(final InputStream is = SiblingsPanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		EventQueue.invokeLater(() -> {
			final SiblingsPanel panel = SiblingsPanel.create(null, null, BoxPanelType.SECONDARY, model,
				true, TreeLayout.VERTICAL);

			final JFrame frame = new JFrame();
			final Container contentPane = frame.getContentPane();
			contentPane.setLayout(new BorderLayout());
			contentPane.add(panel, BorderLayout.NORTH);
			frame.pack();
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
