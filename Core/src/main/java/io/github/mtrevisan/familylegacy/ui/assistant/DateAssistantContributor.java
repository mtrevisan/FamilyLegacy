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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.ui.components.HistoricalDateAssistantDialog;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import java.util.ArrayList;
import java.util.List;


/**
 * The Historical Date Assistant, as actions of the Research Assistant: they are offered when the focus is in the editor of a
 * date, and open the assistant on the tool chosen.
 */
final class DateAssistantContributor implements AssistantContributor{

	/** The actions are listed before the ones of the other assistants: they are what the user needs in the editor of a date. */
	private static final int RELEVANCE = 100;


	private record DateAction(HistoricalDateAssistantDialog.Tool tool, String id, String key, int relevance)
			implements AssistantAction{

		@Override
		public AssistantCategory category(){
			return AssistantCategory.DATES;
		}

		@Override
		public String title(){
			return I18N.t("assistant.dates." + key);
		}

		@Override
		public String description(){
			return I18N.t("assistant.dates." + key + ".description");
		}

		@Override
		public boolean isApplicable(final AssistantContext context){
			return context.focusedAncestor(HistoricalDateAssistantHost.class).isPresent();
		}

		@Override
		public int relevance(final AssistantContext context){
			return relevance;
		}

		@Override
		public void execute(final AssistantContext context){
			context.focusedAncestor(HistoricalDateAssistantHost.class)
				.ifPresent(host -> host.openHistoricalDateAssistant(tool));
		}

	}


	@Override
	public List<AssistantAction> actions(){
		final List<AssistantAction> actions = new ArrayList<>();
		//the first one is the general entry point; the others open the assistant directly on a tool
		actions.add(new DateAction(HistoricalDateAssistantDialog.Tool.CONVERSION, "dates.assistant", "assistant", RELEVANCE + 1));
		actions.add(new DateAction(HistoricalDateAssistantDialog.Tool.CONVERSION, "dates.conversion", "conversion", RELEVANCE));
		actions.add(new DateAction(HistoricalDateAssistantDialog.Tool.LITURGICAL, "dates.liturgical", "liturgical", RELEVANCE));
		actions.add(new DateAction(HistoricalDateAssistantDialog.Tool.SUN, "dates.sun", "sun", RELEVANCE));
		actions.add(new DateAction(HistoricalDateAssistantDialog.Tool.RELATIVE, "dates.relative", "relative", RELEVANCE));
		actions.add(new DateAction(HistoricalDateAssistantDialog.Tool.EVENT, "dates.event", "event", RELEVANCE));
		actions.add(new DateAction(HistoricalDateAssistantDialog.Tool.AGE, "dates.age", "age", RELEVANCE));
		return actions;
	}

}
