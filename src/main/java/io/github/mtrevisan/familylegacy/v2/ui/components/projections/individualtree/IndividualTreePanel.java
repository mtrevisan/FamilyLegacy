/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.TreeOperation;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.Side;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.IndividualRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.SexType;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.RelationClipboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * Generalized panel responsible for rendering an N-generation genealogical tree layout dynamically.
 */
public class IndividualTreePanel extends JPanel implements TreeChangeListener, IndividualListener{

	@Serial
	private static final long serialVersionUID = 9011391311012465249L;


	private static final Logger LOGGER = LoggerFactory.getLogger(IndividualTreePanel.class);


	private static final Color BACKGROUND_COLOR_APPLICATION = new Color(242, 238, 228);
	private static final Color CONNECTION_LINE_COLOR = Color.BLACK;

	private static final String TAG_TYPE = "type";
	private static final String TAG_TARGET = "target";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_SEX = "sex";

	private static final String ENUM_TYPE_BIOLOGICAL_CHILD = "biological_child";
	private static final String ENUM_TYPE_ADOPTIVE_CHILD = "adoptive_child";
	private static final String ENUM_TYPE_STEP_CHILD = "step_child";
	private static final String ENUM_TYPE_FOSTER_CHILD = "foster_child";
	private static final String ENUM_TYPE_GUARDED_CHILD = "guarded_child";


	private final TreeType treeType;
	private final Predicate<String> treeTypeFilter;
	private TreeLayout treeLayout;
	private boolean showPartner;

	private final FLEFModel model;
	private final IndividualTreeService treeService;
	private final AncestorTreeMutator treeMutator;

	private String currentRootIndividualId;
	private AncestorNode rootNode;
	private int currentMaxGenerations;

	// Map associating each AncestorNode with its UI BiologicalParentsPanel
	private final Map<AncestorNode, PartnersPanel> nodeToPanelMap = new HashMap<>();

	// Children block (Generation 1)
	private SiblingsPanel childrenPanel;

	private JPanel selectedPanel;


	public IndividualTreePanel(final TreeType treeType, final TreeLayout treeLayout, final FLEFModel model){
		this.treeType = treeType;
		treeTypeFilter = switch(treeType){
			case BIOLOGICAL -> type -> type.equalsIgnoreCase(ENUM_TYPE_BIOLOGICAL_CHILD);
			case FAMILY -> type -> (type.equalsIgnoreCase(ENUM_TYPE_BIOLOGICAL_CHILD)
				|| type.equalsIgnoreCase(ENUM_TYPE_ADOPTIVE_CHILD)
				|| type.equalsIgnoreCase(ENUM_TYPE_STEP_CHILD)
				|| type.equalsIgnoreCase(ENUM_TYPE_FOSTER_CHILD)
				|| type.equalsIgnoreCase(ENUM_TYPE_GUARDED_CHILD));
			//TODO?
//			case GROUP ->
		};
		this.treeLayout = treeLayout;

		this.model = model;

		this.treeService = new IndividualTreeService(treeTypeFilter, model);
		this.treeMutator = new AncestorTreeMutator(treeTypeFilter, model, treeService, this);

		setBackground(BACKGROUND_COLOR_APPLICATION);
		setOpaque(true);


		setupLayoutShortcut(this);
	}


	public IndividualTreePanel withShowPartner(){
		showPartner = true;

		return this;
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

		// Build tree hierarchy from model
		final AncestorNode rootIndividualNode = treeService.buildTree(currentRootIndividualId, showPartner,
			currentMaxGenerations - 1);

		// Clear previous UI sub-components
		removeAll();
		nodeToPanelMap.clear();

		rootNode = rootIndividualNode;
		if(rootIndividualNode != null && showPartner){
			final IndividualData partnerData = rootIndividualNode.getPartnerData();
			final String partnerId = (partnerData != null? partnerData.getId(): null);
			final AncestorNode partnerNode = treeService.buildTree(partnerId, showPartner,
				currentMaxGenerations - 1);
			final SexType sex = rootIndividualNode.getIndividualData()
				.getSex();

			rootNode = new AncestorNode(rootIndividualNode.getBiologicalChildrenData());
			if(sex == SexType.FEMALE){
				rootNode.setFather(partnerNode);
				rootNode.setMother(rootIndividualNode);
			}
			else{
				rootNode.setFather(rootIndividualNode);
				rootNode.setMother(partnerNode);
			}
		}

		buildLayout();

		// Force Swing recalculate layout and repaint (including parent context if available)
		revalidate();
		repaint();
		if(getParent() != null){
			getParent().revalidate();
			getParent().repaint();
		}
	}

	/**
	 * Builds the dynamic layout using cell placement.
	 * Each node represents a couple (individual and partner).
	 */
	private void buildLayout(){
		final IndividualTreeLayoutBuilder.LayoutResult result = IndividualTreeLayoutBuilder.buildLayout(this,
			rootNode, showPartner, currentMaxGenerations, model, nodeToPanelMap, this, treeMutator, treeTypeFilter,
			treeLayout);

		this.childrenPanel = result.childrenPanel();
	}

	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D g2){
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setColor(CONNECTION_LINE_COLOR);
			g2.setStroke(PartnersPanel.CONNECTION_STROKE);

			IndividualTreeRenderer.drawTree(g2, treeLayout, rootNode, nodeToPanelMap, childrenPanel, this);
		}
	}


	@Override
	public void onTreeStructureChanged(final String rootIndividualId){
		// Run UI updates on the Swing Event Dispatch Thread
		SwingUtilities.invokeLater(() -> loadTree(rootIndividualId, currentMaxGenerations));
	}


	@Override
	public void onEntityEdit(final FLEFRecord individual){
		if(individual == null)
			return;

		final FLEFRecord editedIndividual = showEditIndividualDialog(individual);
		if(editedIndividual != null){
			LOGGER.debug("Individual edited: {}", editedIndividual.getId());

			// Invalidate indices to clear cached IndividualData/Events and rebuild UI
			treeMutator.editIndividual(editedIndividual, getRootIndividualId());
		}
	}


	@Override
	public void onEntitySelected(final FLEFRecord individual){
		if(individual != null && individual.getId() != null)
			treeMutator.navigateToRoot(individual.getId());
	}

	@Override
	public void onEntityRemove(final FLEFRecord individual){
		if(individual == null)
			return;

		final int confirm = JOptionPane.showConfirmDialog(
			this,
			"Are you sure you want to remove individual "
				+ IndividualHandler.getInstance().getDisplayText(individual, model) + "?",
			"Confirm Removal",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE
		);
		if(confirm == JOptionPane.YES_OPTION){
			LOGGER.debug("Individual remove {}", individual.getId());

			// Pass the current root individual ID to allow fallback logic if root is deleted
			treeMutator.removeIndividual(individual, getRootIndividualId());
		}
	}

	@Override
	public void onPanelSelected(final JPanel panel){
		selectedPanel = panel;
	}

	@Override
	public void onIndividualAddOrConnect(final TreeOperation operation, final FLEFRecord father,
			final FLEFRecord mother){
		final String rootIndividualId = getRootIndividualId();
		final Function<SexType, FLEFRecord> fnOperation = (operation == TreeOperation.ADD
			? this::showCreateIndividualDialog
			: this::showSearchIndividualDialog);

		final TreeContextHelper.Context ctx = TreeContextHelper.determineContext(selectedPanel, nodeToPanelMap, model);
		SexType sex = null;
		if(ctx != null && ctx.side != null)
			sex = (ctx.side == Side.LEFT? SexType.MALE: SexType.FEMALE);

		final FLEFRecord individual = fnOperation.apply(sex);
		if(individual == null)
			return;

		performParentRelationOperation(individual, ctx, false, rootIndividualId);
	}

	@Override
	public void onChildAddOrConnect(final TreeOperation operation, final FLEFRecord father, final FLEFRecord mother){
		final String rootIndividualId = getRootIndividualId();
		final Function<SexType, FLEFRecord> fnOperation = (operation == TreeOperation.ADD
			? this::showCreateIndividualDialog
			: this::showSearchIndividualDialog);

		// Do not preset sex for children
		final FLEFRecord individual = fnOperation.apply(null);
		if(individual == null)
			return;

		// Resolve the specific parent record from the selected panel context
		final TreeContextHelper.Context ctx = TreeContextHelper.determineContext(selectedPanel, nodeToPanelMap, model);
		FLEFRecord targetFather = father;
		FLEFRecord targetMother = mother;
		if(targetFather == null && targetMother == null && ctx != null && ctx.individual != null){
			final String rawSex = FLEFRecordHelper.getChildValue(individual, TAG_SEX);
			final SexType sex = (rawSex != null? Enum.valueOf(SexType.class, rawSex.toUpperCase(Locale.ROOT)): null);
			if(sex == SexType.MALE)
				targetFather = ctx.individual;
			else
				targetMother = ctx.individual;
		}

		// Perform child relation operation
		performChildRelationOperation(individual, targetFather, targetMother, false, rootIndividualId);
	}

	/**
	 * Opens a dialog allowing the user to select which relationships of the given individual to unlink.
	 */
	@Override
	public void showUnlinkDialog(final FLEFRecord individual){
		if(individual == null)
			return;

		final Window parent = SwingUtilities.getWindowAncestor(this);
		final UnlinkRelationshipsDialog dialog = UnlinkRelationshipsDialog.create(parent, model, treeTypeFilter,
			individual.getId());
		dialog.setVisible(true);

		final List<String> selectedIds = dialog.getSelectedRelationshipIds();
		if(selectedIds.isEmpty())
			return;

		// Confirm with the user
		final int confirm = JOptionPane.showConfirmDialog(this,
			"Are you sure you want to remove the selected relationships?",
			"Confirm Unlink",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if(confirm == JOptionPane.YES_OPTION){
			treeMutator.removeRelationships(selectedIds);

			treeMutator.invalidateAndNotifyTreeChanged(getRootIndividualId());
		}
	}

	/**
	 * Finds all children of the given individual.
	 * Searches for relationship records where the object is the individual
	 * and the type is one of the parent‑child relationship types.
	 *
	 * @param individualId the ID of the parent (without @)
	 * @return a list of FLEFRecord objects representing the children
	 */
	private List<FLEFRecord> findChildren(final String individualId){
		final List<FLEFRecord> children = new ArrayList<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(!type.equalsIgnoreCase(ENUM_TYPE_BIOLOGICAL_CHILD))
				continue;

			// The target must be the individual
			final String objectId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
			if(!individualId.equals(objectId))
				continue;

			// The subject is the child
			final String childId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
			if(childId == null)
				continue;

			final FLEFRecord child = model.getRecordById(childId);
			if(child != null)
				children.add(child);
		}
		return children;
	}


	private FLEFRecord showEditIndividualDialog(final FLEFRecord individual){
		final Window parent = SwingUtilities.getWindowAncestor(this);
		final IndividualHandler handler = IndividualHandler.getInstance();
		final IndividualRecordDialog dialog = handler.createEditDialog(parent, model, individual);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	private FLEFRecord showCreateIndividualDialog(final SexType sex){
		final Window parent = SwingUtilities.getWindowAncestor(this);
		final IndividualHandler handler = IndividualHandler.getInstance();
		final IndividualRecordDialog dialog = handler.createNewDialog(parent, model);
		dialog.witSex(sex);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	private FLEFRecord showSearchIndividualDialog(final SexType sex){
		final Window parent = SwingUtilities.getWindowAncestor(this);
		final FLEFRecord[] result = {null};
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(parent, model,
			(record, handler) -> result[0] = record,
			IndividualHandler.class);
		if(sex != null)
			dialog.withFilter("sex", sex.name().toLowerCase());
		dialog.setVisible(true);

		return result[0];
	}


	@Override
	public void onEntityRelocate(final FLEFRecord individual){
		if(individual == null)
			return;

		LOGGER.debug("Relocate individual {} to clipboard", individual.getId());

		RelationClipboard.getInstance()
			.setRecord(individual);
	}

	@Override
	public void onIndividualPaste(final FLEFRecord father, final FLEFRecord mother){
		final RelationClipboard clipboard = RelationClipboard.getInstance();
		if(!clipboard.hasRecord())
			return;

		final FLEFRecord source = clipboard.getRecord();

		// If father/mother details are present, paste as child; otherwise paste as parent
		if(father != null || mother != null)
			performChildRelationOperation(source, father, mother, true, getRootIndividualId());
		else{
			final TreeContextHelper.Context ctx = TreeContextHelper.determineContext(selectedPanel, nodeToPanelMap, model);
			if(ctx != null)
				performParentRelationOperation(source, ctx, true, getRootIndividualId());
		}

		clipboard.clear();
	}

	/**
	 * Associates or creates a CHILD with respect to the given parents.
	 */
	private void performChildRelationOperation(final FLEFRecord individual, final FLEFRecord father,
			final FLEFRecord mother, final boolean isPaste, final String rootId){
		if(isPaste){
			final List<String> relIds = extractRelationships(individual.getId());
			treeMutator.removeRelationships(relIds);
		}

		final String[] allowedTypes = getAllowedRelationshipTypes();
		performRelationOperationChild(father, mother, allowedTypes, individual);

		treeMutator.invalidateAndNotifyTreeChanged(rootId);
	}

	/**
	 * Associates or creates a PARENT/PARTNER.
	 */
	private void performParentRelationOperation(final FLEFRecord individual, final TreeContextHelper.Context ctx,
			final boolean isPaste, final String rootId){
		if(ctx == null)
			return;

		if(isPaste){
			final List<String> relIds = extractRelationships(individual.getId());
			treeMutator.removeRelationships(relIds);
		}

		final String[] allowedTypes = getAllowedRelationshipTypes();
		performRelationOperationParent(ctx, allowedTypes, individual);

		treeMutator.invalidateAndNotifyTreeChanged(rootId);
	}

	/**
	 * Performs the core logic for adding, connecting, or pasting an individual.
	 *
	 * @param individualSupplier provides the individual to be added/connected/pasted
	 * @param father             the father record (it may be {@code null})
	 * @param mother             the mother record (it may be {@code null})
	 * @param isPaste            if true, the source individual will be unlinked from all previous relations
	 * @param rootId             the current root id for refreshing the tree
	 */
	private void performRelationOperation(final Supplier<FLEFRecord> individualSupplier, final FLEFRecord father,
			final FLEFRecord mother, final boolean isPaste, final String rootId){
		final TreeContextHelper.Context ctx = TreeContextHelper.determineContext(selectedPanel, nodeToPanelMap, model);
		if(ctx == null)
			return;

		final FLEFRecord individual = individualSupplier.get();
		if(individual == null)
			return;

		// If paste, unlink from all previous relations
		if(isPaste){
			final List<String> relIds = extractRelationships(individual.getId());
			treeMutator.removeRelationships(relIds);
		}

		final String[] allowedTypes = getAllowedRelationshipTypes();

		if(Objects.requireNonNull(ctx.type) == TreeContextHelper.Context.Type.CHILD)
			performRelationOperationChild(father, mother, allowedTypes, individual);
		else if(ctx.type == TreeContextHelper.Context.Type.PARENT)
			performRelationOperationParent(ctx, allowedTypes, individual);

		// Invalidate cache and refresh tree
		treeMutator.invalidateAndNotifyTreeChanged(rootId);
	}

	private String[] getAllowedRelationshipTypes(){
		return switch(treeType){
			case BIOLOGICAL -> new String[]{ENUM_TYPE_BIOLOGICAL_CHILD};
			case FAMILY -> new String[]{ENUM_TYPE_BIOLOGICAL_CHILD, ENUM_TYPE_ADOPTIVE_CHILD, ENUM_TYPE_FOSTER_CHILD,
				ENUM_TYPE_GUARDED_CHILD, ENUM_TYPE_STEP_CHILD};
		};
	}

	private void performRelationOperationChild(final FLEFRecord father, final FLEFRecord mother,
			final String[] allowedTypes, final FLEFRecord individual){
		final List<String> selectedTypes;
		if(allowedTypes.length == 1)
			selectedTypes = Collections.nCopies(2, allowedTypes[0]);
		else{
			// We need to choose a type for the father and mother separately
			final List<RelationshipTypeSelectionDialog.Item> items = new ArrayList<>();

			final String fatherLabel = (father != null
				? IndividualHandler.getInstance().getDisplayText(father, model)
				: "Father");
			final String motherLabel = (mother != null
				? IndividualHandler.getInstance().getDisplayText(mother, model)
				: "Mother");

			items.add(new RelationshipTypeSelectionDialog.Item(fatherLabel, allowedTypes[0]));
			items.add(new RelationshipTypeSelectionDialog.Item(motherLabel, allowedTypes[0]));

			selectedTypes = RelationshipTypeSelectionDialog.showDialog(null, items, allowedTypes);
			if(selectedTypes == null)
				return;
		}

		final String fatherType = selectedTypes.getFirst();
		final String motherType = selectedTypes.getLast();
		final String fatherId = (father != null? father.getId(): null);
		final String motherId = (mother != null? mother.getId(): null);
		treeMutator.addChildToParents(fatherId, motherId, individual, fatherType, motherType);
	}

	private void performRelationOperationParent(final TreeContextHelper.Context ctx, final String[] allowedTypes,
			final FLEFRecord individual){
		// We need to choose a type for each child we are connecting this parent to.
		// Determine which children.
		if(ctx.childrenId.isEmpty())
			return;

		final List<String> selectedTypes;
		if(allowedTypes.length == 1)
			selectedTypes = Collections.nCopies(2, allowedTypes[0]);
		else{
			// Build items: each child label + default type
			final List<RelationshipTypeSelectionDialog.Item> items = new ArrayList<>();
			for(final String childId : ctx.childrenId){
				final FLEFRecord child = model.getRecordById(childId);
				if(child == null)
					continue;

				final String label = IndividualHandler.getInstance().getDisplayText(child, model);
				items.add(new RelationshipTypeSelectionDialog.Item(label, allowedTypes[0]));
			}
			if(items.isEmpty())
				return;

			selectedTypes = RelationshipTypeSelectionDialog.showDialog(null, items, allowedTypes);
			if(selectedTypes == null)
				return;
		}

		// Connect this parent to each child with its selected type
		treeMutator.addParentToChild(ctx.childrenId, individual, selectedTypes);

		treeMutator.addPartnerToIndividual(ctx.targetId, individual);
	}

	/**
	 * Extracts relationships from the model and groups them into parents, partners, and children.
	 */
	private List<String> extractRelationships(final String individualId){
		final List<String> result = new ArrayList<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(type == null)
				continue;

			final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
			final String targetId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
			final boolean involves = (individualId.equals(subjectId) || individualId.equals(targetId));
			if(involves)
				result.add(relationship.getId());
		}
		return result;
	}


	/**
	 * Retrieves the ID of the current root individual rendered in the tree.
	 *
	 * @return the record ID of the root individual, or {@code null} if no tree is loaded.
	 */
	public String getRootIndividualId(){
		if(rootNode == null || rootNode.getFather() == null)
			return null;

		return rootNode.getFather()
			.getIndividual()
			.getId();
	}


	public void setupLayoutShortcut(final JComponent component){
		final InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = component.getActionMap();

		inputMap.put(GUIHelper.CTRL_L_STROKE, "toggleTreeLayout");
		actionMap.put("toggleTreeLayout", new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -2768341528368853112L;

			@Override
			public void actionPerformed(final ActionEvent e){
				toggleLayout();
			}
		});
	}

	private void toggleLayout(){
		this.treeLayout = (this.treeLayout == TreeLayout.VERTICAL
			? TreeLayout.HORIZONTAL
			: TreeLayout.VERTICAL);

		refreshTree();

		final Window window = SwingUtilities.getWindowAncestor(this);
		if(window != null){
			window.pack();
			window.setLocationRelativeTo(null);
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
		try(final InputStream is = IndividualTreePanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			final IndividualTreePanel panel = new IndividualTreePanel(TreeType.BIOLOGICAL, TreeLayout.VERTICAL, model)
//			final IndividualTreePanel panel = new IndividualTreePanel(TreeType.FAMILY, TreeLayout.VERTICAL, model)
				.withShowPartner();
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
