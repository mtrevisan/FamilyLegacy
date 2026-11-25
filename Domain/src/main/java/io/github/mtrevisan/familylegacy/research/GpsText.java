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
package io.github.mtrevisan.familylegacy.research;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Assessment;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Element;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Gap;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Readiness;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Severity;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Stage;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.StageState;

import java.util.Locale;


/** Writes what {@link GpsWorkflow} found in the language of the user. */
public final class GpsText{

	private GpsText(){}


	/** @return what is missing, in one sentence (the record it is about, if any, in brackets) */
	public static String gap(final Gap gap){
		final String message = I18N.t("text.gps.gap." + gap.kind().key());
		if(gap.subjectId() == null && gap.more() > 0)
			return I18N.tf("text.gps.gap.more", message, gap.more());
		return (gap.subjectText() != null? message + " (" + gap.subjectText() + ")": message);
	}

	public static String stage(final Stage stage){
		return I18N.t("text.gps.stage." + stage.name().toLowerCase(Locale.ROOT));
	}

	public static String stageState(final StageState state){
		return I18N.t("text.gps.stage_state." + state.name().toLowerCase(Locale.ROOT));
	}

	public static String element(final Element element){
		return I18N.t("text.gps.element." + element.name().toLowerCase(Locale.ROOT));
	}

	public static String severity(final Severity severity){
		return I18N.t("text.gps.severity." + severity.name().toLowerCase(Locale.ROOT));
	}

	public static String readiness(final Readiness readiness){
		return I18N.t("text.gps.readiness." + readiness.name().toLowerCase(Locale.ROOT));
	}

	/**
	 * @return the answer to "what is still missing before this conclusion can be considered reasonably proven?": the verdict, then the
	 * 	required things, then the recommended ones
	 */
	public static String missing(final Assessment assessment){
		final StringBuilder text = new StringBuilder(readiness(assessment.readiness())).append('\n');
		for(final Severity severity : Severity.values()){
			boolean first = true;
			for(final Gap gap : assessment.gaps())
				if(gap.severity() == severity){
					if(first)
						text.append('\n').append(severity(severity)).append('\n');
					first = false;
					text.append("- ").append(gap(gap)).append('\n');
				}
		}
		return text.toString();
	}

}
