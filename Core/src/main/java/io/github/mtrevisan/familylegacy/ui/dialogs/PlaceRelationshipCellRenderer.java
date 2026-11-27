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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Component;


/**
 * Shows a relationship of a place as a sentence, with no subject and target: from the side of the place that is part of another one
 * ({@code administrative part of Veneto, 1866 - 1946}), or from the side of the one that contains it ({@code Borgo: administrative
 * part of this place}).
 */
public final class PlaceRelationshipCellRenderer extends DefaultListCellRenderer{

	private final FLEFModel model;
	/** {@code true} for the list of the places this one is part of (it is the subject), {@code false} for the ones inside it. */
	private final boolean fromSubject;


	public PlaceRelationshipCellRenderer(final FLEFModel model, final boolean fromSubject){
		this.model = model;
		this.fromSubject = fromSubject;
	}


	@Override
	public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected,
			final boolean cellHasFocus){
		final JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if(value instanceof FLEFRecord record)
			label.setText(sentence(record));
		return label;
	}

	/** @return the relationship as a sentence, seen from one of its two places */
	String sentence(final FLEFRecord relationship){
		final String type = StringUtils.defaultString(PlaceRelationshipReader.extractType(relationship));
		final String typeLabel = I18N.t("enum.place_relationship.type." + type, type);

		final String other = placeName(relationship.extractReferencedId(fromSubject
			? PlaceRelationshipReader.TAG_TARGET: PlaceRelationshipReader.TAG_SUBJECT, PlaceReader.TYPE));
		final String dates = dates(relationship);
		final String sentence = (fromSubject
			? I18N.tf("text.place_relationship.from_subject", typeLabel, other)
			: I18N.tf("text.place_relationship.from_target", other, typeLabel));
		return (dates.isEmpty()? sentence: sentence + ", " + dates);
	}

	private String placeName(final String id){
		final FLEFRecord place = (id != null? model.getRecordById(id): null);
		return (place != null
			? StringUtils.normalizeSpace(PlaceHandler.getInstance().getDisplayText(place, model))
			: "--");
	}

	/** @return the years of the relationship ({@code 1866 - 1946}, {@code from 1866}, {@code until 1946}), empty if it has no dates */
	private static String dates(final FLEFRecord relationship){
		final String from = date(relationship, PlaceRelationshipReader.TAG_VALID_FROM);
		final String to = date(relationship, PlaceRelationshipReader.TAG_VALID_TO);
		if(!from.isEmpty() && !to.isEmpty())
			return from + " - " + to;
		if(!from.isEmpty())
			return I18N.tf("text.place_relationship.from_date", from);
		if(!to.isEmpty())
			return I18N.tf("text.place_relationship.until_date", to);
		return StringUtils.EMPTY;
	}

	private static String date(final FLEFRecord relationship, final String tag){
		final FLEFRecord date = FLEFRecordHelper.findChild(relationship, tag);
		if(date == null)
			return StringUtils.EMPTY;

		final String text = DateReader.extractPrettyPrintDate(date);
		return (text != null? Strings.CS.replace(text, "\n", " "): StringUtils.EMPTY);
	}

}
