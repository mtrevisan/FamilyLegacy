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
package io.github.mtrevisan.familylegacy.ui.handlers;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.preferences.MediaPathDisplay;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import java.awt.Window;


/**
 * Handler for EXTRACT records: the transcriptions, translations, summaries, and excerpts of a source, that the citations of the
 * source reference.
 */
public class ExtractHandler extends AbstractRecordTypeHandler<RecordDialog>{

	public static final String TYPE = ExtractReader.TYPE;
	public static final String ID_PREFIX = ExtractReader.ID_PREFIX;


	private static final class SingletonHelper{
		private static final ExtractHandler INSTANCE = new ExtractHandler();
	}


	public static ExtractHandler getInstance(){
		return SingletonHelper.INSTANCE;
	}


	@Override
	public String getLabel(){
		return I18N.t("confirmation.exist_record.extract");
	}

	@Override
	public String getType(){
		return TYPE;
	}

	public static boolean isOfType(final FLEFRecord record){
		return ExtractReader.isOfType(record);
	}

	@Override
	public String getIdPrefix(){
		return ID_PREFIX;
	}

	/**
	 * The locale, the text (or the first media part, when there is no text), and the type of the extract, e.g.
	 * {@code [la] Anno domini 1723 die 3 ... (verbatim)}.
	 */
	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		//a reference (as in a citation) is shown as the extract it references
		final FLEFRecord extract = (record != null && record.getId() == null && model != null
			? model.getRecordById(record.getValue())
			: record);
		if(extract == null)
			return "--";

		final String text = ExtractReader.extractText(extract);
		final String type = ExtractReader.extractType(extract);
		final String locale = ExtractReader.extractLocale(extract);

		final StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotEmpty(locale))
			sb.append('[')
				.append(locale)
				.append("] ");
		if(StringUtils.isNotEmpty(text))
			sb.append(GUIHelper.limitTextLength(StringUtils.replaceChars(text, '\n', '|')));
		else
			sb.append(firstMediaPart(extract, model));

		if(StringUtils.isNotEmpty(type)){
			if(!sb.isEmpty())
				sb.append(' ');
			sb.append('(')
				.append(I18N.t("enum.extract_type." + type, type))
				.append(')');
		}

		if(sb.isEmpty() && extract.getId() != null)
			sb.append('[')
				.append(extract.getId())
				.append(']');
		return (!sb.isEmpty()? sb.toString(): "--");
	}

	private static String firstMediaPart(final FLEFRecord extract, final FLEFModel model){
		final String mediaId = FLEFRecordHelper.getChildValue(extract, ExtractReader.TAG_MEDIA_PART_MEDIA);
		final FLEFRecord media = (model != null? model.getRecordById(mediaId): null);
		if(media == null)
			return StringUtils.EMPTY;

		final String description = MediaReader.extractDescription(media);
		if(StringUtils.isNotEmpty(description))
			return description;

		final String uri = MediaReader.extractUri(media);
		if(StringUtils.isNotEmpty(uri))
			return MediaPathDisplay.display(uri);

		return "[" + media.getId() + "]";
	}

	@Override
	public RecordDialog createNewDialog(final Window parent, final FLEFModel model){
		return RecordDialogs.createNew(TYPE, parent, model);
	}

	@Override
	public RecordDialog createEditDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		return RecordDialogs.createEdit(TYPE, parent, model, record);
	}

}
