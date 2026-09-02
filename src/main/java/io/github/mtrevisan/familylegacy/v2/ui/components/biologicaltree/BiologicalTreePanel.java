package io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.components.biologicalparents.BiologicalParentsPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.siblings.SiblingsData;
import io.github.mtrevisan.familylegacy.v2.ui.components.siblings.SiblingsPanel;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;


/**
 * Generalized panel responsible for rendering an N-generation genealogical tree layout dynamically.
 */
public class BiologicalTreePanel extends JPanel implements BiologicalTreeChangeListener{

	@Serial
	private static final long serialVersionUID = 9011391311012465249L;


	private static final Color BACKGROUND_COLOR_APPLICATION = new Color(242, 238, 228);
	private static final Color CONNECTION_LINE_COLOR = Color.BLACK;

	private static final int GENERATION_SEPARATOR_SIZE = 36;

	private final FLEFModel model;
	private final BiologicalTreeService treeService;

	private String currentRootIndividualId;
	private AncestorNode rootNode;
	private int currentMaxGenerations;

	// Map associating each AncestorNode with its UI BiologicalParentsPanel
	private final Map<AncestorNode, BiologicalParentsPanel> nodeToPanelMap = new HashMap<>();

	// Children block (Generation 1)
	private SiblingsPanel childrenPanel;
	private JScrollPane childrenScrollPane;


	public BiologicalTreePanel(final FLEFModel model){
		this.model = model;
		this.treeService = new BiologicalTreeService(model);

		setBackground(BACKGROUND_COLOR_APPLICATION);
		setOpaque(true);
	}


	@Override
	public void onTreeStructureChanged(final String rootIndividualId){
		if(Objects.equals(currentRootIndividualId, rootIndividualId))
			refreshTree();
	}

	public void loadTree(final String rootIndividualId, final int maxGenerations){
		this.currentRootIndividualId = rootIndividualId;
		this.currentMaxGenerations = maxGenerations;

		refreshTree();
	}

	private void refreshTree(){
		if(!SwingUtilities.isEventDispatchThread()){
			SwingUtilities.invokeLater(this::refreshTree);

			return;
		}

		removeAll();
		nodeToPanelMap.clear();

		rootNode = treeService.buildAncestorTree(currentRootIndividualId, currentMaxGenerations);
		if(rootNode != null)
			buildDynamicLayout(rootNode, currentMaxGenerations);

		revalidate();
		repaint();
	}

	private void buildDynamicLayout(final AncestorNode root, final int maxGenerations){
		// Number of ancestor levels
		final int ancestorLevels = Math.max(1, maxGenerations - 1);
		// 2^(ancestorLevels-1)
		final int maxLeafColumns = 1 << (ancestorLevels - 1);

		// Constructing MigLayout constraints
		final StringBuilder colConstraints = new StringBuilder();
		for(int i = 0; i < maxLeafColumns; i ++){
			if(i > 0)
				colConstraints.append(BiologicalParentsPanel.GROUP_SEPARATION);
			colConstraints.append("[grow,center]");
		}

		final StringBuilder rowConstraints = new StringBuilder();
		for(int i = 0; i < ancestorLevels; i ++){
			if(i > 0)
				rowConstraints.append(GENERATION_SEPARATOR_SIZE);
			rowConstraints.append("[]");
		}
		// Row for children
		rowConstraints.append(GENERATION_SEPARATOR_SIZE).append("[]");

		setLayout(new MigLayout("ins 20 0 20 0", colConstraints.toString(), rowConstraints.toString()));

		// Collect nodes level by level (top to bottom)
		final List<List<AncestorNode>> levels = new ArrayList<>();
		for(int level = ancestorLevels - 1; level >= 0; level --){
			final List<AncestorNode> levelNodes = collectNodesAtDepth(root, level);
			levels.add(levelNodes);
		}

		// Add panels to layout from the topmost level down to the home level (root)
		int currentSpan = 1;
		int levelCount = levels.size();
		for(final List<AncestorNode> levelNodes : levels){
			for(int j = 0; j < levelNodes.size(); j ++){
				final AncestorNode node = levelNodes.get(j);
				final BiologicalParentsPanel panel = createBiologicalParentsPanel(node, levelCount);

				if(node != null)
					nodeToPanelMap.put(node, panel);

				final boolean isEndOfRow = (j == levelNodes.size() - 1);
				final String spanStr = (currentSpan > 1? "span " + currentSpan + ",": StringUtils.EMPTY);
				final String wrapStr = (isEndOfRow? ",wrap": StringUtils.EMPTY);

				add(panel, spanStr + "grow" + wrapStr);
			}

			currentSpan <<= 1;
			levelCount --;
		}

		// Add children row (Generation 1)
		childrenPanel = createChildrenPanel(root);
		childrenScrollPane = createChildrenScrollPane(childrenPanel);
		add(childrenScrollPane, "span " + maxLeafColumns + ",center");
	}

	/**
	 * Iteratively collects all nodes at the specified target depth using a level-order (BFS) traversal.
	 * Placeholders (nulls) are maintained to preserve layout spacing in a balanced binary structure.
	 */
	private List<AncestorNode> collectNodesAtDepth(final AncestorNode root, final int targetDepth){
		List<AncestorNode> currentLevel = new ArrayList<>();
		currentLevel.add(root);

		for(int depth = 0; depth < targetDepth; depth ++){
			final List<AncestorNode> nextLevel = new ArrayList<>(currentLevel.size() << 1);
			for(final AncestorNode node : currentLevel){
				if(node != null){
					nextLevel.add(node.getFather());
					nextLevel.add(node.getMother());
				}
				else{
					// Preserve structural layout space in the balanced binary tree
					nextLevel.add(null);
					nextLevel.add(null);
				}
			}
			currentLevel = nextLevel;
		}

		return currentLevel;
	}

	private BiologicalParentsPanel createBiologicalParentsPanel(final AncestorNode node, final int level){
		final BiologicalParentsPanel panel = BiologicalParentsPanel.create(
			(level == 1? BoxPanelType.PRIMARY: BoxPanelType.SECONDARY), model);
		if(node != null){
			final AncestorNode father = node.getFather();
			final AncestorNode mother = node.getMother();
			panel.withBiologicalParents((father != null? father.getIndividualData(): null),
				(mother != null? mother.getIndividualData(): null));
		}
		return panel;
	}

	private SiblingsPanel createChildrenPanel(final AncestorNode root){
		final SiblingsPanel panel = SiblingsPanel.create(BoxPanelType.SECONDARY, model);
		if(root != null && root.getMotherBiologicalChildrenData() != null){
			final Map<IndividualData, SiblingsData> motherBiologicalChildrenData = root.getMotherBiologicalChildrenData();
			//TODO choose mother
			final SiblingsData childrenData = motherBiologicalChildrenData.values().stream()
				.findFirst()
				.orElse(null);
			panel.withSiblingsData(childrenData);
		}
		return panel;
	}

	private JScrollPane createChildrenScrollPane(final JPanel content){
		final JScrollPane scrollPane = new JScrollPane(content);
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setBorder(null);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		scrollPane.getHorizontalScrollBar().addAdjustmentListener(e -> repaint());

		// Dynamically compute horizontal scrollbar height to avoid overlapping children panels
		final int scrollBarHeight = scrollPane.getHorizontalScrollBar()
			.getPreferredSize()
			.height;
		content.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, scrollBarHeight, 0));

		return scrollPane;
	}

	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D g2d){
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setColor(CONNECTION_LINE_COLOR);
			g2d.setStroke(BiologicalParentsPanel.CONNECTION_STROKE);

			drawTreeConnections(g2d);
			drawChildrenConnections(g2d);
		}
	}

	/**
	 * Iteratively traverses all tree nodes using a Queue (BFS) to draw connection lines between parents and children.
	 */
	private void drawTreeConnections(final Graphics2D g2d){
		if(rootNode == null)
			return;

		final Queue<AncestorNode> queue = new ArrayDeque<>();
		queue.add(rootNode);
		while(!queue.isEmpty()){
			final AncestorNode node = queue.poll();
			final BiologicalParentsPanel nodePanel = nodeToPanelMap.get(node);

			final AncestorNode father = node.getFather();
			if(father != null){
				final BiologicalParentsPanel fatherPanel = nodeToPanelMap.get(father);
				if(fatherPanel != null && nodePanel != null){
					Point enter = nodePanel.getPaintingFatherEnterPoint();
					enter = SwingUtilities.convertPoint(nodePanel, enter, this);
					connectParentToChild(fatherPanel, enter, g2d);
				}
				queue.add(father);
			}

			final AncestorNode mother = node.getMother();
			if(mother != null){
				final BiologicalParentsPanel motherPanel = nodeToPanelMap.get(mother);
				if(motherPanel != null && nodePanel != null){
					Point enter = nodePanel.getPaintingMotherEnterPoint();
					enter = SwingUtilities.convertPoint(nodePanel, enter, this);
					connectParentToChild(motherPanel, enter, g2d);
				}
				queue.add(mother);
			}
		}
	}

	private void connectParentToChild(final BiologicalParentsPanel parentGroupPanel, final Point childEnterPoint,
			final Graphics2D g2d){
		Point parentExit = parentGroupPanel.getPaintingExitPoint();
		parentExit = SwingUtilities.convertPoint(parentGroupPanel, parentExit, this);

		// Vertical line extending out from parent group
		final int midY = (childEnterPoint.y + parentExit.y + BiologicalParentsPanel.GROUP_EXITING_HEIGHT) / 2;
		g2d.drawLine(parentExit.x, parentExit.y,
			parentExit.x, midY);

		// Vertical line entering into child panel
		g2d.drawLine(childEnterPoint.x, childEnterPoint.y,
			childEnterPoint.x, midY);

		// Horizontal connecting line
		g2d.drawLine(parentExit.x, midY,
			childEnterPoint.x, midY);
	}

	private void drawChildrenConnections(final Graphics2D g2d){
		if(rootNode == null || childrenPanel == null)
			return;

		final BiologicalParentsPanel homePanel = nodeToPanelMap.get(rootNode);
		final Point[] childEnterPoints = childrenPanel.getPaintingEnterPoints();

		if(homePanel != null && childEnterPoints.length > 0){
			Point homeExit = homePanel.getPaintingExitPoint();
			homeExit = SwingUtilities.convertPoint(homePanel, homeExit, this);

			final Point origin = childrenScrollPane.getLocation();
			origin.x -= childrenScrollPane.getHorizontalScrollBar()
				.getValue();

			final int firstChildX = origin.x + childEnterPoints[0].x;
			final int lastChildX = origin.x + childEnterPoints[childEnterPoints.length - 1].x;
			final int connectY = origin.y + childEnterPoints[0].y - GENERATION_SEPARATOR_SIZE / 2;

			// Vertical line exiting home group
			g2d.drawLine(homeExit.x, homeExit.y,
				homeExit.x, connectY);

			// Horizontal line spanning from first to last child
			g2d.drawLine(firstChildX, connectY,
				lastChildX, connectY);

			// Vertical lines targeting each child enter point
			for(final Point point : childEnterPoints){
				final int childX = origin.x + point.x;
				final int childY = origin.y + point.y;
				g2d.drawLine(childX, childY,
					childX, connectY);
			}
		}
	}


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final String modelUri = "/tests/TGMZ.flef";
		final String rootIndividualId = "I1";
		final int maxGenerations = 4;

		final String content;
		try(final InputStream is = BiologicalTreePanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			final BiologicalTreePanel panel = new BiologicalTreePanel(model);
			panel.loadTree(rootIndividualId, maxGenerations);

			final JFrame frame = new JFrame();
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.CENTER);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
