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
package io.github.mtrevisan.familylegacy.ui.tools.maintenance.merge;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.merger.RecordAbsorber;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeGrammar;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergePlan;
import io.github.mtrevisan.familylegacy.io.merger.manual.RecordMerger;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import java.awt.Window;


/**
 * The two things that can be done with two records that may be one: merge them, after reviewing the whole record side by side, or
 * record that they are different. They are what the window that picks the two records does, and what a list of candidates (the
 * duplicates found) does for the pair it shows, without picking again what it already knows.
 * <p>
 * Nothing is written until the review is confirmed and the user confirms again; the file is changed only by
 * {@link RecordMerger#apply} and {@link RecordMerger#reject}.
 */
public final class RecordMergeWorkflow{

	private RecordMergeWorkflow(){}


	private static RecordMerger mergerOf(final FLEFModel model){
		// without the grammar references and cardinalities are unknown, and a merge would break the file
		return (MergeGrammar.instance() != null? new RecordMerger(model, MergeGrammar.instance()): null);
	}

	/** Whether the two records can be recorded as different: two records of one kind that can be the subject of an identity hypothesis. */
	public static boolean canRecordAsDifferent(final FLEFModel model, final FLEFRecord source, final FLEFRecord target){
		final RecordMerger merger = mergerOf(model);
		return (merger != null && merger.canReject(source, target));
	}

	/** Whether two records can be merged: two different records, and the grammar to merge them with. */
	public static boolean canMerge(final FLEFRecord source, final FLEFRecord target){
		return (MergeGrammar.instance() != null && source != null && target != null && source.getId() != null && target.getId() != null
			&& !source.getId().equals(target.getId()));
	}

	/**
	 * Merges the source into the target: shows the two side by side, with the result in the middle, for the user to decide it, then asks
	 * for a last confirmation and writes the merge.
	 *
	 * @param owner	The window the dialogs are on.
	 * @param model	The file the records are in.
	 * @return	Whether the records were merged.
	 */
	public static boolean merge(final Window owner, final FLEFModel model, final FLEFRecord source, final FLEFRecord target){
		final RecordMerger merger = mergerOf(model);
		if(merger == null){
			warn(owner, I18N.t("dialog.merge.records.warn.no_grammar"));
			return false;
		}
		if(source == null || target == null){
			warn(owner, I18N.t("dialog.merge.records.warn.choose_both"));
			return false;
		}
		if(source.getId().equals(target.getId())){
			warn(owner, I18N.t("dialog.merge.records.warn.same"));
			return false;
		}

		//the whole record is reviewed before anything is written: the user decides every field, not only the collisions
		final MergePlan plan;
		try{
			plan = merger.planAllFields(source.getId(), target.getId());
		}
		catch(final IllegalArgumentException ex){
			warn(owner, I18N.tf("dialog.merge.records.warn.plan_failed", source.getTag(), target.getTag()));
			return false;
		}
		final MergeRecordReviewDialog review = new MergeRecordReviewDialog(owner, plan, model, displayOf(source), displayOf(target));
		review.setVisible(true);
		if(!review.isConfirmed())
			return false;

		if(!confirm(owner, source, target))
			return false;

		try{
			final RecordMerger.MergeResult result = merger.apply(plan);

			final StringBuilder summary = new StringBuilder(I18N.t("dialog.merge.records.completed"))
				.append(StringUtils.LF).append(I18N.t("dialog.merge.records.fields.taken")).append(':').append(StringUtils.SPACE).append(result.fieldsTakenFromSource())
				.append(StringUtils.LF).append(I18N.t("dialog.merge.records.references.repointed")).append(':').append(StringUtils.SPACE).append(result.referencesRepointed());
			if(!result.collapsedIds().isEmpty())
				summary.append(StringUtils.LF).append(I18N.t("dialog.merge.records.collapsed")).append(':').append(StringUtils.SPACE).append(String.join(", ", result.collapsedIds()));
			if(!result.deletedIds().isEmpty())
				summary.append(StringUtils.LF).append(I18N.t("dialog.merge.records.deleted")).append(':').append(StringUtils.SPACE)
					.append(String.join(", ", result.deletedIds()));
			if(!result.conflicts().isEmpty())
				summary.append(StringUtils.LF).append(I18N.t("dialog.merge.records.conflicts")).append(':').append(StringUtils.LF)
					.append(String.join(StringUtils.LF, result.conflicts()));
			JOptionPane.showMessageDialog(owner, summary.toString(), I18N.t("dialog.merge.records.title"), JOptionPane.INFORMATION_MESSAGE);
			return true;
		}
		catch(final IllegalArgumentException ex){
			warn(owner, ex.getMessage());
			return false;
		}
	}

	/**
	 * Records that source and target are different entities, as an identity hypothesis disproven by a conclusion, instead of merging
	 * them: asks why (a blank answer is accepted).
	 *
	 * @return	Whether the pair was recorded as different.
	 */
	public static boolean recordAsDifferent(final Window owner, final FLEFModel model, final FLEFRecord source, final FLEFRecord target){
		final RecordMerger merger = mergerOf(model);
		if(merger == null || !merger.canReject(source, target))
			return false;

		final String narrative = JOptionPane.showInputDialog(owner,
			I18N.tf("dialog.merge.records.different.prompt", displayOf(source), displayOf(target)),
			I18N.t("dialog.merge.records.different.title"), JOptionPane.QUESTION_MESSAGE);
		if(narrative == null)
			return false;

		try{
			final RecordAbsorber.Rejection rejection = merger.reject(source.getId(), target.getId(),
				(StringUtils.isNotBlank(narrative)? narrative.strip(): I18N.t("dialog.merge.records.different.note")));

			JOptionPane.showMessageDialog(owner,
				I18N.tf("dialog.merge.records.different.recorded", rejection.hypothesisId(), rejection.conclusionId()),
				I18N.t("dialog.merge.records.different.title"), JOptionPane.INFORMATION_MESSAGE);
			return true;
		}
		catch(final IllegalArgumentException ex){
			warn(owner, ex.getMessage());
			return false;
		}
	}

	private static boolean confirm(final Window owner, final FLEFRecord source, final FLEFRecord target){
		final String message = I18N.tf("dialog.merge.records.confirm.message", displayOf(source), displayOf(target));
		final int choice = JOptionPane.showConfirmDialog(owner, message, I18N.t("dialog.merge.records.confirm.title"),
			JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		return (choice == JOptionPane.YES_OPTION);
	}

	private static void warn(final Window owner, final String message){
		JOptionPane.showMessageDialog(owner, message, I18N.t("dialog.merge.records.title"), JOptionPane.WARNING_MESSAGE);
	}

	/**
	 * Short human-readable identifier of a record, used in confirmation and error messages and in the title of the review.
	 */
	static String displayOf(final FLEFRecord record){
		if(record == null)
			return I18N.t("dialog.merge.record.none");

		final String id = record.getId();
		return (id != null? "[" + id + "] ": StringUtils.EMPTY) + record.getTag();
	}

}
