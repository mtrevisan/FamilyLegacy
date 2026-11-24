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
package io.github.mtrevisan.familylegacy.assistant.task;

import java.util.List;


/**
 * A research task that the Research Assistant proposes, from one of its findings: nothing exists in the file until the researcher
 * accepts it (see {@link TaskProposals#create}).
 *
 * @param origin      the key of the finding, that is kept in the task so that the finding is not proposed again
 * @param topic       what the finding asks
 * @param description what to do
 * @param rationale   why: what the finding says
 * @param priority    {@code low}, {@code normal} or {@code high}
 * @param dueDate     the proposed due date (an ISO date)
 * @param relatedIds  the records the task is about (the ones a research question can be about)
 * @param questionIds the research questions the finding is about: the task serves them
 * @param activityId  the research activity the task follows, or {@code null}
 */
public record TaskProposal(String origin, TaskTopic topic, String description, String rationale, String priority, String dueDate,
		List<String> relatedIds, List<String> questionIds, String activityId){

	public TaskProposal{
		relatedIds = List.copyOf(relatedIds);
		questionIds = List.copyOf(questionIds);
	}

}
