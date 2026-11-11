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
package io.github.mtrevisan.familylegacy.ui.assistant.context;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticKeys;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Finds the events whose date is an estimate by convention ({@code approximate.basis = conventional}: deduced by applying a custom of the
 * time and place) with no cultural norm that justifies it. The norm is what lets a reader check the estimate, so the finding suggests to
 * document it; it never decides that the estimate is right or wrong.
 */
final class NormEstimateChecker{

	private NormEstimateChecker(){}


	/**
	 * @param events the events of the model
	 * @return one finding for each event that has a conventional estimate with no cultural norm
	 */
	static List<Diagnostic> findings(final List<FLEFRecord> events){
		final List<Diagnostic> findings = new ArrayList<>();
		for(final FLEFRecord event : events){
			final FLEFRecord date = FLEFRecordHelper.findChild(event, EventReader.TAG_DATE);
			if(date == null || event.getId() == null || !hasUnsupportedConvention(date))
				continue;

			final String type = EventReader.extractType(event);
			final String what = I18N.t("enum.event.type." + type, String.valueOf(type));
			findings.add(new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.CONTEXT,
				I18N.tf("context.norm_estimate.title", what, event.getId()),
				I18N.tf("context.norm_estimate.explanation", what, event.getId()),
				List.of(event.getId()), new CreateCulturalNormDraftAction(),
				DiagnosticKeys.of("context.norm_estimate", List.of(event.getId()))));
		}
		return findings;
	}

	/** @return whether somewhere in the date there is an estimate by convention that cites no cultural norm. */
	static boolean hasUnsupportedConvention(final FLEFRecord node){
		for(final FLEFRecord child : node.getChildren()){
			if(DateReader.TAG_APPROXIMATE.equals(child.getTag())
					&& DateReader.ENUM_BASIS_CONVENTIONAL.equals(FLEFRecordHelper.getChildValue(child, DateReader.TAG_BASIS))
					&& !citesNorm(child))
				return true;
			if(hasUnsupportedConvention(child))
				return true;
		}
		return false;
	}

	private static boolean citesNorm(final FLEFRecord node){
		for(final FLEFRecord child : node.getChildren())
			if((DateReader.TAG_CULTURAL_NORM.equals(child.getTag()) && StringUtils.isNotBlank(child.getValue())) || citesNorm(child))
				return true;
		return false;
	}

}
