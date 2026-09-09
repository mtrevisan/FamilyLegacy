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
package io.github.mtrevisan.familylegacy.v2.ui.components.grouptree;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree.TreeChangeListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree.TreeLayout;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualOperation;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.IndividualRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
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
import java.awt.Dialog;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Generalized panel responsible for rendering non-biological group and entity trees dynamically.
 */
public class GroupIndividualTreePanel extends JPanel implements TreeChangeListener, IndividualListener{

	@Serial
	private static final long serialVersionUID = -4819204910249102941L;


	private static final Logger LOGGER = LoggerFactory.getLogger(GroupIndividualTreePanel.class);


	private static final Color BACKGROUND_COLOR_APPLICATION = new Color(242, 238, 228);
	private static final Color CONNECTION_LINE_COLOR = Color.BLACK;


	private TreeLayout treeLayout;

	private final FLEFModel model;
	private final GroupTreeService treeService;

	private String currentRootEntityId;
	private GroupTreeNode rootNode;
	private int currentMaxGenerations;

	// Map associating each GroupTreeNode with its UI JPanel
	private final Map<GroupTreeNode, JPanel> nodeToPanelMap = new HashMap<>();

//	// Children block (Generation 1)
//	private SiblingsPanel childrenPanel;

	private IndividualPanel selectedPanel;


	public GroupIndividualTreePanel(final TreeLayout treeLayout, final FLEFModel model){
		this.treeLayout = treeLayout;

		this.model = model;

		this.treeService = new GroupTreeService(model);

		setBackground(BACKGROUND_COLOR_APPLICATION);
		setOpaque(true);


		setupLayoutShortcut(this);
	}


	public void loadTree(final String rootEntityId, final int maxGenerations){
		this.currentRootEntityId = rootEntityId;
		this.currentMaxGenerations = maxGenerations;

		refreshTree();
	}

	private void refreshTree(){
		if(!SwingUtilities.isEventDispatchThread()){
			SwingUtilities.invokeLater(this::refreshTree);

			return;
		}

		// Build tree hierarchy from model
		rootNode = treeService.buildTree(currentRootEntityId, currentMaxGenerations - 1);

		// Clear previous UI sub-components
		removeAll();
		nodeToPanelMap.clear();

		buildLayout();

		// Force Swing recalculate layout and repaint
		revalidate();
		repaint();
		if(getParent() != null){
			getParent().revalidate();
			getParent().repaint();
		}
	}

	/**
	 * Builds the dynamic layout using cell placement.
	 */
	private void buildLayout(){
		final GroupIndividualTreeLayoutBuilder.LayoutResult result = GroupIndividualTreeLayoutBuilder.buildLayout(
			this, rootNode, currentMaxGenerations, model, nodeToPanelMap, this, treeLayout);

//		this.childrenPanel = result.childrenPanel();
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

			GroupIndividualTreeRenderer.drawTree(g2, treeLayout, rootNode, nodeToPanelMap, this);
		}
	}


	@Override
	public void onTreeStructureChanged(final String rootEntityId){
		// Run UI updates on the Swing Event Dispatch Thread
		SwingUtilities.invokeLater(() -> loadTree(rootEntityId, currentMaxGenerations));
	}


	@Override
	public void onIndividualEdit(final FLEFRecord record){
		if(record == null)
			return;

		final FLEFRecord editedRecord = showEditRecordDialog(record);
		if(editedRecord != null){
			LOGGER.debug("Entity edited: {}", editedRecord.getId());

			// Invalidate indices to clear cached IndividualData/Events and rebuild UI
			treeService.invalidateIndices();
			refreshTree();
		}
	}


	@Override
	public void onIndividualSelected(final FLEFRecord record){
		if(record != null && record.getId() != null)
			loadTree(record.getId(), currentMaxGenerations);
	}

	@Override
	public void onIndividualRemove(final FLEFRecord record){
		if(record == null)
			return;

		final int confirm = JOptionPane.showConfirmDialog(
			this,
			"Are you sure you want to remove entity " + record.getId() + "?",
			"Confirm Removal",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE
		);
		if(confirm == JOptionPane.YES_OPTION){
			LOGGER.debug("Removing entity {}", record.getId());

			model.removeRecord(record.getId());
			treeService.invalidateIndices();
			refreshTree();
		}
	}

	@Override
	public void onPanelSelected(final IndividualPanel panel){
		selectedPanel = panel;
	}

	@Override
	public void onIndividualAddOrLink(final IndividualOperation operation, final FLEFRecord father, final FLEFRecord mother){
		final Function<FLEFRecord, FLEFRecord> fnOperation = (operation == IndividualOperation.ADD
			? this::showCreateRecordDialog
			: this::showSearchRecordDialog);

		performRelationOperation(() -> fnOperation.apply(father));
	}

	/**
	 * Opens a dialog allowing the user to select which relationships of the given individual to unlink.
	 */
	@Override
	public void showUnlinkDialog(final FLEFRecord record){
		if(record == null){
			return;
		}

		LOGGER.debug("Showing unlink dialog for record {}", record.getId());
	}

	private FLEFRecord showEditRecordDialog(final FLEFRecord record){
		final Window window = SwingUtilities.getWindowAncestor(this);
		final Dialog parent = (window instanceof Dialog dialog ? dialog : null);

		if(GroupHandler.TYPE.equalsIgnoreCase(record.getTag()))
			return record;

		final IndividualHandler handler = IndividualHandler.getInstance();
		final IndividualRecordDialog dialog = handler.createEditDialog(parent, model, record);
		dialog.setVisible(true);

		return (dialog.isSaved() ? dialog.getRecord() : null);
	}

	private FLEFRecord showCreateRecordDialog(final FLEFRecord contextRecord){
		final Window window = SwingUtilities.getWindowAncestor(this);
		final Dialog parent = (window instanceof Dialog dialog ? dialog : null);

		final IndividualHandler handler = IndividualHandler.getInstance();
		final IndividualRecordDialog dialog = handler.createNewDialog(parent, model);
		dialog.setVisible(true);

		return (dialog.isSaved() ? dialog.getRecord() : null);
	}

	private FLEFRecord showSearchRecordDialog(final FLEFRecord contextRecord){
		final Window window = SwingUtilities.getWindowAncestor(this);
		final Dialog parent = (window instanceof Dialog dialog ? dialog : null);

		final FLEFRecord[] result = {null};
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(parent, model,
			(record, handler) -> result[0] = record,
			IndividualHandler.class);
		dialog.setVisible(true);

		return result[0];
	}


	@Override
	public void onIndividualMove(final FLEFRecord record){
		if(record == null)
			return;

		LOGGER.debug("Moving entity {} to clipboard", record.getId());

		RelationClipboard.getInstance()
			.setRecord(record);
	}

	@Override
	public void onIndividualPaste(final FLEFRecord father, final FLEFRecord mother){
		final RelationClipboard clipboard = RelationClipboard.getInstance();
		if(!clipboard.hasRecord())
			return;

		final FLEFRecord source = clipboard.getRecord();
		performRelationOperation(() -> source);

		clipboard.clear();
	}

	/**
	 * Performs the core logic for adding, linking, or pasting an individual.
	 *
	 * @param recordSupplier provides the individual to be added/linked/pasted
	 */
	private void performRelationOperation(final Supplier<FLEFRecord> recordSupplier){
		final FLEFRecord record = recordSupplier.get();
		if(record == null){
			return;
		}

		treeService.invalidateIndices();
		refreshTree();
	}

	public void setupLayoutShortcut(final JComponent component){
		final InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = component.getActionMap();

		inputMap.put(GUIHelper.CTRL_L_STROKE, "toggleGroupTreeLayout");
		actionMap.put("toggleGroupTreeLayout", new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -2810481029481203984L;

			@Override
			public void actionPerformed(final ActionEvent e){
				toggleLayout();
			}
		});
	}

	private void toggleLayout(){
		this.treeLayout = (this.treeLayout == TreeLayout.VERTICAL)
			? TreeLayout.HORIZONTAL
			: TreeLayout.VERTICAL;

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
		final String rootEntityId = "I1";
		final int maxGenerations = 4;

		final String content;
		try(final InputStream is = GroupIndividualTreePanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			final GroupIndividualTreePanel panel = new GroupIndividualTreePanel(TreeLayout.VERTICAL, model);
			panel.loadTree(rootEntityId, maxGenerations);

			final JFrame frame = new JFrame("Group Tree");
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.CENTER);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
