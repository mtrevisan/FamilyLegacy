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
package io.github.mtrevisan.familylegacy.ui.tools.groups;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.ui.dialogs.help.ShortcutRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.ToolOperation;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import java.util.List;
import java.util.Map;


/**
 * Deletes the currently selected group, together with every
 * relationship that involves it. The deletion is confirmed with a
 * dialog that lists how many relationships will be removed.
 */
public final class DeleteGroupTool implements ToolOperation{

	@Override
	public String getName(){
		return ShortcutRegistry.EDIT_DELETE_GROUP.action();
	}

	@Override
	public KeyStroke getAccelerator(){
		return ShortcutRegistry.EDIT_DELETE_GROUP.keyStroke();
	}

	@Override
	public void run(final ToolContext context){
		final String id = context.selectedEntityId();
		if(id == null)
			return;

		final FLEFModel model = context.model();
		final FLEFRecord group = model.getRecordById(id);
		if(group == null)
			return;

		final Map<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> relationshipIds = GroupHelper.relationshipIds(id, model);
		final String displayName = GroupReader.extractPrimaryName(group);
		int relationshipCount = 0;
		for(final Map.Entry<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> entry : relationshipIds.entrySet())
			relationshipCount += entry.getValue().size();
		final int confirm = JOptionPane.showConfirmDialog(context.owner(),
			I18N.tf("dialog.remove.record.message", GroupHandler.getInstance().getLabel(), displayName, id, (relationshipCount == 1), relationshipCount),
			I18N.t("dialog.remove.record.title"),
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE
		);
		if(confirm == JOptionPane.YES_OPTION){
			if(context.dispatcher() != null)
				context.performRemove(id);
			else{
				GroupHelper.deleteGroup(id, model);
				context.loadRoot(id);
			}
		}
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasSelectedGroup());
	}

}
