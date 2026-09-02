package io.github.mtrevisan.familylegacy.v2.ui.components.siblings;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.components.biologicalparents.BiologicalParentsPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ResourceHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * UI panel representing a row of sibling individual boxes with visual indicators for descendants.
 */
public class SiblingsPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -4829104812837192834L;


	private static final double DESCENDANTS_HEIGHT = 12.;
	private static final double DESCENDANTS_ASPECT_RATIO = 3501. / 2662.;
	private static final Dimension DESCENDANTS_SIZE = new Dimension((int)(DESCENDANTS_HEIGHT / DESCENDANTS_ASPECT_RATIO), (int)DESCENDANTS_HEIGHT);

	private static final ImageIcon ICON_DESCENDANTS = ResourceHelper.getResizedImage("/images/union.png", DESCENDANTS_SIZE);

	private static final int SIBLING_SEPARATION = 15;
	public static final int DESCENDANTS_ARROW_HEIGHT = ICON_DESCENDANTS.getIconHeight()
		+ BiologicalParentsPanel.NAVIGATION_DESCENDANTS_ARROW_SEPARATION;


	private final FLEFModel model;
	private final BoxPanelType boxType;

	private final List<IndividualPanel> siblingBoxes = new ArrayList<>();
	private SiblingsData data;


	public static SiblingsPanel create(final BoxPanelType boxType, final FLEFModel model){
		return new SiblingsPanel(boxType, model);
	}


	private SiblingsPanel(final BoxPanelType boxType, final FLEFModel model){
		this.boxType = boxType;
		this.model = model;


		initComponents();
	}


	private void initComponents(){
		setOpaque(false);

		setLayout(new MigLayout("insets 0", "[]0[]"));
	}


//	//for test purposes
//	@Override
//	protected final void paintComponent(final Graphics g){
//		super.paintComponent(g);
//
//		if(g instanceof Graphics2D){
//			final Graphics2D g2d = (Graphics2D)g.create();
//			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//
//
//			final Point[] enterPoints = getPaintingEnterPoints();
//			//vertical line connecting the children
//			g2d.setColor(Color.RED);
//			for(int i = 0; i < enterPoints.length; i ++){
//				final Point point = enterPoints[i];
//
//				g2d.drawLine(point.x - 10, point.y - 10,
//					point.x + 10, point.y + 10);
//				g2d.drawLine(point.x + 10, point.y - 10,
//					point.x - 10, point.y + 10);
//			}
//			g2d.setColor(Color.BLACK);
//
//
//			g2d.dispose();
//		}
//	}

	public void withSiblingsData(final SiblingsData data){
		this.data = data;

		refreshData();
	}

	private void refreshData(){
		removeAll();
		siblingBoxes.clear();

		if(data != null){
			final List<IndividualData> siblings = data.getSiblings();

			for(final IndividualData siblingData : siblings){
				final String siblingId = siblingData.getIndividualId();
				final boolean hasDescendants = data.hasDescendants(siblingId);

				final JPanel boxContainer = createSiblingContainer(hasDescendants);
				final IndividualPanel siblingBox = IndividualPanel.create(boxType, model);
				siblingBox.withIndividualData(siblingData);

				boxContainer.add(siblingBox);
				add(boxContainer, "gapright " + SIBLING_SEPARATION);
				siblingBoxes.add(siblingBox);
			}
		}

		// Add empty placeholder box for adding a new sibling
		final JPanel emptyBoxContainer = createSiblingContainer(false);
		final IndividualPanel emptySiblingBox = IndividualPanel.create(boxType, model);
		emptyBoxContainer.add(emptySiblingBox);
		add(emptyBoxContainer);
		siblingBoxes.add(emptySiblingBox);

		revalidate();
		repaint();
	}

	private static JPanel createSiblingContainer(final boolean hasDescendants){
		final JPanel container = new JPanel();
		container.setOpaque(false);
		container.setLayout(new MigLayout("flowy,ins 0", "[]",
			"[]" + BiologicalParentsPanel.NAVIGATION_DESCENDANTS_ARROW_SEPARATION + "[]"));

		final JLabel descendantsLabel = new JLabel();
		descendantsLabel.setPreferredSize(new Dimension(ICON_DESCENDANTS.getIconWidth(), ICON_DESCENDANTS.getIconHeight()));
		if(hasDescendants)
			descendantsLabel.setIcon(ICON_DESCENDANTS);
		container.add(descendantsLabel, "right");
		return container;
	}

	public List<IndividualPanel> getSiblingBoxes(){
		return siblingBoxes;
	}

	public Point[] getPaintingEnterPoints(){
		final int count = getComponentCount();
		final Point[] enterPoints = new Point[count];
		for(int i = 0; i < count; i ++){
			final Component comp = getComponent(i);
			enterPoints[i] = new Point(comp.getX() + comp.getWidth() / 2, comp.getY() + DESCENDANTS_ARROW_HEIGHT);
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
			final SiblingsPanel panel = SiblingsPanel.create(BoxPanelType.SECONDARY, model);

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
