package io.github.mtrevisan.familylegacy.v2.ui.components.genealogicaltree;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.components.biologicalparents.BiologicalParentsPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualPanel;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Panel responsible for rendering an ancestor tree layout using IndividualPanel and BiologicalParentsPanel components.
 */
public class GenealogicalTreePanel extends JPanel implements GenealogicalTreeChangeListener{

	@Serial
	private static final long serialVersionUID = 9011391311012465249L;


	/**
	 * Value record holding start and end coordinates for drawing connections.
	 */
	private record ParentChildConnection(Point start, Point end){}


	private static final Color CONNECTION_LINE_COLOR = Color.BLACK;
	private static final Stroke CONNECTION_STROKE = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);


	private String currentRootIndividualId;
	private int currentMaxGenerations;

	private final FLEFModel model;
	private final GenealogicalTreeService treeService;

	private final List<ParentChildConnection> connections = new ArrayList<>();


	public GenealogicalTreePanel(final FLEFModel model){
		this.model = model;

		this.treeService = new GenealogicalTreeService(model);

		setLayout(new MigLayout("ins 20, align center", StringUtils.EMPTY, StringUtils.EMPTY));
		setOpaque(false);
	}


	@Override
	public void onTreeStructureChanged(final String rootIndividualId){
		if(Objects.equals(currentRootIndividualId, rootIndividualId))
			refreshTree();
	}


	/**
	 * Loads and displays the ancestor tree for a given individual.
	 *
	 * @param rootIndividualId the ID of the individual at the root of the tree
	 * @param maxGenerations   maximum generations depth to display
	 */
	public void loadTree(final String rootIndividualId, final int maxGenerations){
		currentRootIndividualId = rootIndividualId;
		currentMaxGenerations = maxGenerations;

		refreshTree();
	}

	private void refreshTree(){
		if(!SwingUtilities.isEventDispatchThread()){
			SwingUtilities.invokeLater(this::refreshTree);

			return;
		}


		removeAll();
		connections.clear();

		final AncestorNode rootNode = treeService.buildAncestorTree(currentRootIndividualId, currentMaxGenerations);
		if(rootNode != null)
			buildUI(rootNode);

		revalidate();
		repaint();
	}

	private void buildUI(final AncestorNode root){
		// Create panel for the root individual
		final IndividualPanel rootIndividualPanel = IndividualPanel.create(BoxPanelType.PRIMARY, model);
		rootIndividualPanel.withIndividualData(root.getIndividualData());

		// Recursively build parent components
		final JPanel parentsTreeComponent = buildAncestorsComponent(root);

		if(parentsTreeComponent != null){
			add(parentsTreeComponent, "wrap,align center");
			add(rootIndividualPanel, "align center,gaptop 20");
		}
		else
			add(rootIndividualPanel, "align center");
	}

	private JPanel buildAncestorsComponent(final AncestorNode node){
		if(node == null || node.getFather() == null && node.getMother() == null)
			return null;

		// Post-order traversal using two stacks
		final Deque<AncestorNode> stack1 = new ArrayDeque<>();
		final Deque<AncestorNode> stack2 = new ArrayDeque<>();
		stack1.push(node);
		while(!stack1.isEmpty()){
			final AncestorNode current = stack1.pop();
			stack2.push(current);

			final AncestorNode father = current.getFather();
			final AncestorNode mother = current.getMother();
			if(father != null && (father.getFather() != null || father.getMother() != null))
				stack1.push(father);
			if(mother != null && (mother.getFather() != null || mother.getMother() != null))
				stack1.push(mother);
		}

		// Process nodes bottom-up to build UI containers
		final Map<AncestorNode, JPanel> nodeToPanelMap = new HashMap<>();
		while(!stack2.isEmpty()){
			final AncestorNode current = stack2.pop();

			final BiologicalParentsPanel parentsPanel = BiologicalParentsPanel.create(BoxPanelType.SECONDARY, model);
			parentsPanel.withBiologicalParents(current.getBiologicalParentsData());

			final JPanel container = new JPanel(new MigLayout("ins 0,align center", StringUtils.EMPTY, StringUtils.EMPTY));
			container.setOpaque(false);

			final JPanel fatherAncestors = nodeToPanelMap.get(current.getFather());
			final JPanel motherAncestors = nodeToPanelMap.get(current.getMother());
			if(fatherAncestors != null || motherAncestors != null){
				final JPanel upperPanel = new JPanel(new MigLayout("ins 0,align center", "[grow][grow]", StringUtils.EMPTY));
				upperPanel.setOpaque(false);

				if(fatherAncestors != null)
					upperPanel.add(fatherAncestors, "cell 0 0,align right");
				if(motherAncestors != null)
					upperPanel.add(motherAncestors, "cell 1 0,align left");

				container.add(upperPanel, "wrap,align center,gapbottom 15");
			}

			container.add(parentsPanel, "align center");
			nodeToPanelMap.put(current, container);
		}

		return nodeToPanelMap.get(node);
	}

	@Override
	protected void paintChildren(final Graphics g){
		super.paintChildren(g);

		// Draw connecting lines between ancestor parent blocks and children panels
		if(g instanceof Graphics2D graphics2D){
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setColor(CONNECTION_LINE_COLOR);
			graphics2D.setStroke(CONNECTION_STROKE);

			for(final ParentChildConnection conn : connections)
				graphics2D.drawLine(conn.start().x, conn.start().y, conn.end().x, conn.end().y);
		}
	}


	public static void main(String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		String modelUri = "/tests/TGMZ.flef";
		final String rootIndividualId = "I1";
		final int maxGenerations = 2;

		final String content;
		try(final InputStream is = GenealogicalTreePanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);


		SwingUtilities.invokeLater(() -> {
			final GenealogicalTreePanel panel = new GenealogicalTreePanel(model);
			panel.loadTree(rootIndividualId, maxGenerations);

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
