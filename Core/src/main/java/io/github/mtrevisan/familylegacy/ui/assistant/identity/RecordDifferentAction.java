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
package io.github.mtrevisan.familylegacy.ui.assistant.identity;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticAction;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ConclusionRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.IdentityHypothesisRecordDialog;

import java.util.List;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;


/**
 * "Record as Different People" ("Different Records" for the kinds other than individuals): the way of the protocol to say that
 * two records are not the same for good is a conclusion that
 * disproves the identity hypothesis, so this opens the two editors in turn: the hypothesis (with the reasons the two looked alike), and,
 * when the researcher has saved it, a conclusion that resolves it, with the proof status {@code disproven} and a narrative that lists,
 * feature by feature, why they looked alike and what tells them apart.
 * <p>
 * Both are drafts and nothing is saved unless the researcher confirms: cancelling the first editor stops here, cancelling the second leaves
 * the hypothesis alone. A conclusion that disproves a hypothesis is also what keeps the merger from proposing the pair again. The
 * assistant decides nothing: the researcher judges the sources.
 * </p>
 */
final class RecordDifferentAction implements DiagnosticAction{

	/** The confidence proposed for a hypothesis that is being rejected. */
	private static final String CONFIDENCE = ConfidenceReader.ENUM_LOW;
	private static final String ISSUE_IDENTITY = "identity";

	private final IdentityDraft draft;
	private final boolean people;


	/** The action for two individuals. */
	RecordDifferentAction(final IdentityDraft draft){
		this(draft, true);
	}

	/**
	 * @param draft	The draft of the pair.
	 * @param people	Whether the records are individuals (the texts say "people") or another kind of record ("records").
	 */
	RecordDifferentAction(final IdentityDraft draft, final boolean people){
		this.draft = draft;
		this.people = people;
	}


	@Override
	public String title(){
		return I18N.t(people? "identity.different.action": "identity.different.action.records");
	}

	@Override
	public void execute(final AssistantContext context){
		final FLEFModel model = context.model();
		if(model == null)
			return;

		final FLEFRecord left = model.getRecordById(draft.leftId());
		final FLEFRecord right = model.getRecordById(draft.rightId());
		if(left == null || right == null)
			return;

		final IdentityHypothesisRecordDialog hypothesisDialog = IdentityHypothesisRecordDialog.createNew(context.owner(), model)
			.withCandidates(left, right, draft.rationale(), CONFIDENCE)
			.withSources(draft.sharedSourceIds());
		hypothesisDialog.setVisible(true);
		if(!hypothesisDialog.isSaved() || hypothesisDialog.getRecord() == null)
			return;

		final FLEFRecord hypothesis = model.getRecordById(hypothesisDialog.getRecord().getId());
		if(hypothesis == null)
			return;

		ConclusionRecordDialog.createNew(context.owner(), model)
			.withResolvedTarget(hypothesis, ISSUE_IDENTITY)
			.withDraft(ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN, narrative(model, left, right))
			.setVisible(true);
	}

	/** @return	The start of the narrative: the judgement, then every feature that made them look alike and every one that tells them apart. */
	String narrative(final FLEFModel model, final FLEFRecord left, final FLEFRecord right){
		return I18N.tf(people? "identity.different.narrative": "identity.different.narrative.records", RecordDisplay.describe(left, model), RecordDisplay.describe(right, model),
			lines(draft.similarities()), lines(draft.differences()));
	}

	private static String lines(final List<String> items){
		return (items.isEmpty()? "\n- " + I18N.t("identity.different.none"): "\n- " + String.join("\n- ", items));
	}

}
