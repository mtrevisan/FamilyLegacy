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
package io.github.mtrevisan.familylegacy.ui.assistant.citation;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContributor;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import java.util.List;


/** The Source Citation Assistant, as an action of the Research Assistant: it is offered when the focus is in the editor of a citation. */
public final class CitationAssistantContributor implements AssistantContributor{

	/** Listed before the generic actions: it is what the user needs in the editor of a citation. */
	private static final int RELEVANCE = 100;


	private static final class OpenAction implements AssistantAction{

		@Override
		public String id(){
			return "sources.citation";
		}

		@Override
		public AssistantCategory category(){
			return AssistantCategory.SOURCES;
		}

		@Override
		public String title(){
			return I18N.t("assistant.sources.citation");
		}

		@Override
		public String description(){
			return I18N.t("assistant.sources.citation.description");
		}

		@Override
		public boolean isApplicable(final AssistantContext context){
			return context.focusedAncestor(CitationHost.class).isPresent();
		}

		@Override
		public int relevance(final AssistantContext context){
			return RELEVANCE;
		}

		@Override
		public void execute(final AssistantContext context){
			context.focusedAncestor(CitationHost.class).ifPresent(host -> {
				final FLEFModel model = context.model();
				new CitationAssistantDialog(context.owner(), model, host.citedSource(), host.locator(), host::applyLocator,
					(host.acceptsRepository()? host::applyRepository: null))
					.setVisible(true);
			});
		}

	}


	@Override
	public List<AssistantAction> actions(){
		return List.of(new OpenAction());
	}

}
