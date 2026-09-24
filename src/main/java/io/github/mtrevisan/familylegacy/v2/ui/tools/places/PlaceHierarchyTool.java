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
package io.github.mtrevisan.familylegacy.v2.ui.tools.places;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
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
			final FLEFModel model = context.model();
			hierarchyPanel = new PlaceHierarchyPanel(model);
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

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasAnyPlaces());
	}

}
