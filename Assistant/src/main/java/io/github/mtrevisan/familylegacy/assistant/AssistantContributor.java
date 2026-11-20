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
package io.github.mtrevisan.familylegacy.assistant;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;


/**
 * What an assistant adds to the Research Assistant. A future assistant is registered with
 * {@code ResearchAssistantService.register(AssistantContributor)}, or, from a plugin, as a {@link java.util.ServiceLoader}
 * provider, without changing the code of the Research Assistant.
 */
public interface AssistantContributor{

	/** @return the commands of the assistant */
	List<AssistantAction> actions();

	/** @return the sources of diagnostics of the assistant */
	default List<DiagnosticProvider> diagnosticProviders(){
		return List.of();
	}

	/**
	 * @return what identifies the assistant in the settings of the user (that switch it off): the name of its class without the
	 * {@code AssistantContributor} suffix, in lower case (e.g. {@code chronology})
	 */
	default String id(){
		final String name = (getClass().getSimpleName().isEmpty()? getClass().getName(): getClass().getSimpleName());
		final String stripped = StringUtils.removeEnd(StringUtils.removeEnd(name, "AssistantContributor"), "Contributor");
		return stripped.toLowerCase(Locale.ROOT);
	}

	/** @return the name of the assistant, as the user sees it (in the settings, and when it fails) */
	default String name(){
		return I18N.t("assistant.name." + id(), id());
	}

}
