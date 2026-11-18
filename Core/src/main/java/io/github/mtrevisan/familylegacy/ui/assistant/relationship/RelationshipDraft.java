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
package io.github.mtrevisan.familylegacy.ui.assistant.relationship;


/**
 * What the Relationship Assistant prepares for a missing relationship: the start of a RelationshipRecord, for the researcher to
 * complete and confirm.
 *
 * @param subjectId the individual to put as the subject
 * @param targetId  the individual to put as the target
 * @param type      the type the evidence points to, or {@code null} if it does not tell (the researcher chooses)
 * @param eventId   the event that is the evidence of the relationship (its sources are the ones the draft starts with), or {@code null}
 * @param copyDate  whether the date of the event is the date from which the relationship is valid (a marriage is, the birth of a
 *                  child is not the date of the tie with whoever declared it)
 */
record RelationshipDraft(String subjectId, String targetId, String type, String eventId, boolean copyDate){

	RelationshipDraft(final String subjectId, final String targetId, final String type){
		this(subjectId, targetId, type, null, false);
	}
}
