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
package io.github.mtrevisan.familylegacy.ui.handlers;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.AgentStructureDialog;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import java.awt.Window;


/**
 * Handler for {@code AGENT_STRUCTURE} entities according to FLEF 0.2.0.
 * <p>
 * This handler provides the necessary operations for managing name structures:
 * creation, editing, display name generation, and type identification.
 * </pre>
 */
public class AgentHandler extends AbstractRecordTypeHandler<AgentStructureDialog>{

	public static final String TYPE = "AGENT_STRUCTURE";
	public static final String CITED_TYPE = "AGENT";


	private static final class SingletonHelper{
		private static final AgentHandler INSTANCE = new AgentHandler();
	}


	public static AgentHandler getInstance(){
		return SingletonHelper.INSTANCE;
	}


	@Override
	public boolean isTopLevelEntity(){
		return false;
	}

	@Override
	public String getLabel(){
		return I18N.t("confirmation.exist_record.agent");
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getCitedType(){
		return (!isTopLevelEntity()? CITED_TYPE: null);
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		final FLEFRecord child = record.getTheOnlyChild();
		if(IndividualHandler.isOfType(child)){
			final FLEFRecord individual = model.getRecordById(child.getValue());
			return IndividualHandler.getInstance()
				.getDisplayText(individual, model);
		}

		if(GroupHandler.isOfType(child)){
			final FLEFRecord group = model.getRecordById(child.getValue());
			return GroupHandler.getInstance()
				.getDisplayText(group, model);
		}

		return child.getValue();
	}

	@Override
	public AgentStructureDialog createNewDialog(final Window parent, final FLEFModel model){
		return AgentStructureDialog.createNew(parent, model);
	}

	@Override
	public AgentStructureDialog createEditDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		return AgentStructureDialog.createEdit(parent, model, record);
	}

}
