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
package io.github.mtrevisan.familylegacy.ui.assistant.features;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContributor;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContexts;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.tools.research.ResearchToolRegistry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * The research tools as actions of the Research Assistant, at every interface level: the menus show only the tools of the level of the
 * user (progressive disclosure), the assistant reaches all of them, so that a basic user who needs the editor of a conclusion or of a
 * cultural norm is one search away from it.
 */
public final class ResearchToolsAssistantContributor implements AssistantContributor{

	/** Below the actions of the assistants that are about what the user is doing. */
	private static final int RELEVANCE = -10;


	private record ToolAction(ToolOperation tool) implements AssistantAction{

		@Override
		public String id(){
			return "tools." + tool.getClass().getSimpleName().toLowerCase(Locale.ROOT);
		}

		@Override
		public AssistantCategory category(){
			return AssistantCategory.TOOLS;
		}

		@Override
		public String title(){
			return I18N.tf("assistant.tools.open", cleanName());
		}

		@Override
		public String description(){
			final UxLevel level = tool.minimumLevel();
			return I18N.tf("assistant.tools.open.description", cleanName(),
				I18N.t("menu.settings.level." + level.name().toLowerCase(Locale.ROOT)));
		}

		@Override
		public boolean isApplicable(final AssistantContext context){
			return (context.model() != null && tool.isEnabled(toolContext(context)));
		}

		@Override
		public int relevance(final AssistantContext context){
			return RELEVANCE;
		}

		@Override
		public void execute(final AssistantContext context){
			tool.run(toolContext(context));
		}

		private String cleanName(){
			return Strings.CI.remove(tool.getName(), "…").trim();
		}

		private static ToolContext toolContext(final AssistantContext context){
			return ToolContexts.custom(context.model(), context.selectedRecordId(), context.window(), null);
		}

	}


	@Override
	public List<AssistantAction> actions(){
		final List<AssistantAction> actions = new ArrayList<>();
		for(final List<ToolOperation> group : List.of(ResearchToolRegistry.planningTools(), ResearchToolRegistry.contextTools(),
				ResearchToolRegistry.analysisTools(), ResearchToolRegistry.reportTools()))
			for(final ToolOperation tool : group)
				actions.add(new ToolAction(tool));
		return actions;
	}

}
