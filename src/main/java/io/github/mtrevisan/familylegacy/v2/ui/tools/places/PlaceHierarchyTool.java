package io.github.mtrevisan.familylegacy.v2.ui.tools.places;

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.places.PlaceHierarchyPanel;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JFrame;
import java.awt.BorderLayout;


/**
 * Opens the place hierarchy panel in a non-modal window, so the user can
 * keep it side by side with the main frame. The window is created lazily
 * and reused on subsequent invocations: if it is already open, the call
 * brings it to front and reloads its content, so any change made to the
 * model in the meantime is reflected.
 */
public final class PlaceHierarchyTool implements ToolOperation{

	private JFrame hierarchyFrame;
	private PlaceHierarchyPanel hierarchyPanel;


	@Override
	public String getName(){
		return "Place Hierarchy…";
	}

	@Override
	public void run(final ToolContext context){
		if(hierarchyFrame == null){
			hierarchyFrame = new JFrame("Place Hierarchy");
			hierarchyFrame.setLayout(new BorderLayout());
			hierarchyPanel = new PlaceHierarchyPanel(context.model());
			hierarchyFrame.add(hierarchyPanel, BorderLayout.CENTER);
			hierarchyFrame.setSize(1000, 700);
			hierarchyFrame.setLocationRelativeTo(context.owner());
			hierarchyFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			hierarchyFrame.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosed(final java.awt.event.WindowEvent e){
					hierarchyFrame = null;
					hierarchyPanel = null;
				}
			});
		}
		else
			hierarchyPanel.reload();

		hierarchyFrame.setVisible(true);
		hierarchyFrame.toFront();
	}

}
