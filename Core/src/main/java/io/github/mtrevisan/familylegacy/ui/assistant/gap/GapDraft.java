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
package io.github.mtrevisan.familylegacy.ui.assistant.gap;

import java.util.List;


/**
 * What the Research Gap Assistant prepares for a gap: the start of a research question (what is missing, to be found out) or of a
 * research task (what to do next), for the researcher to complete and confirm.
 *
 * @param kind     what the draft is
 * @param title    the title of a question (empty for a task)
 * @param text     the question of a question, or the description of a task
 * @param records  the records it is about: the targets of a question, the questions a task supports
 * @param priority the priority proposed for a task ({@code low}, {@code normal}, {@code high}), or {@code null}
 */
public record GapDraft(Kind kind, String title, String text, List<String> records, String priority){

	/** What a draft starts. */
	public enum Kind{
		QUESTION,
		TASK
	}

}
