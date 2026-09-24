package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.PopupMenuHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.help.ShortcutRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.KeyStroke;
import java.awt.Component;


/**
 * Pastes the entity stored in the clipboard context onto the target context.
 */
public final class PasteIndividualTool implements ToolOperation{

	@Override
	public String getName(){
		return ShortcutRegistry.EDIT_PASTE.action();
	}

	@Override
	public KeyStroke getAccelerator(){
		return ShortcutRegistry.EDIT_PASTE.keyStroke();
	}

	@Override
	public void run(final ToolContext context){
		if(isEnabled(context))
			context.performPaste();
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		if(context == null || !context.canPaste())
			return false;

		final Component view = context.selectedComponent();
		final FLEFModel model = context.model();
		if(view instanceof final IndividualPanel panel){
			final IndividualData data = panel.getData();
			final boolean hasData = (data != null && !data.isEmpty());
			return (!hasData && PopupMenuHelper.isPasteAllowed(model, panel));
		}
		if(view instanceof final GroupPanel panel){
			final GroupData data = panel.getData();
			final boolean hasData = (data != null && !data.isEmpty());
			return (!hasData && PopupMenuHelper.isPasteAllowed(model, panel));
		}

		return true;
	}

}
