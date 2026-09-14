package io.github.mtrevisan.familylegacy.v2.ui.tools.files;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JOptionPane;


/**
 * Base class for the import/export tools that are not yet implemented.
 * <p>
 * Every subclass declares its own name; the {@link #run(ToolContext)}
 * method is shared and shows a placeholder dialog with a clear
 * description of the missing feature. When the actual implementation
 * lands, the subclass overrides {@code run} and the placeholder
 * disappears.
 * <p>
 * Keeping the placeholder behaviour in one place avoids ten copies of
 * the same dialog.
 */
public abstract class PlaceholderFileTool implements ToolOperation{


	@Override
	public void run(final ToolContext context){
		JOptionPane.showMessageDialog(context.owner(),
			"\"" + getName().replace("…", "").trim() + "\" is not implemented yet.",
			"Feature Not Available", JOptionPane.INFORMATION_MESSAGE);
	}

}
