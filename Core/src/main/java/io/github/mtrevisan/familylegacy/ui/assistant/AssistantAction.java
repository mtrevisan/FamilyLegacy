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


/**
 * A command offered by the Research Assistant.
 * <p>
 * The Research Assistant is a research aid and never an authority: an action may open a workflow, generate a draft or propose
 * a fix, but it must never merge records, create conclusions, modify assertions or resolve conflicts on its own. The
 * researcher always decides.
 * </p>
 */
public interface AssistantAction{

	/** @return a stable identifier, unique among the actions */
	String id();

	AssistantCategory category();

	/** @return the (localized) name of the action */
	String title();

	/** @return the (localized) explanation of what the action does */
	String description();

	/**
	 * @param context where the user is
	 * @return whether the action can be used in the context
	 */
	boolean isApplicable(AssistantContext context);

	/**
	 * @param context where the user is
	 * @return how relevant the action is in the context: the higher, the earlier it is listed
	 */
	default int relevance(final AssistantContext context){
		return 0;
	}

	/**
	 * Runs the action.
	 *
	 * @param context where the user was when the assistant was opened
	 */
	void execute(AssistantContext context);

}
