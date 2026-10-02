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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.TextValueVariantReader;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.TextValueVariantStructureDialog;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.awt.Window;


public class TextValueVariantHandler extends AbstractRecordTypeHandler<TextValueVariantStructureDialog>{

	public static final String TYPE = "TEXT_VALUE_VARIANT";


	private static final class SingletonHelper{
		private static final TextValueVariantHandler INSTANCE = new TextValueVariantHandler();
	}


	public static TextValueVariantHandler getInstance(){
		return SingletonHelper.INSTANCE;
	}


	@Override
	public boolean isTopLevelEntity(){
		return false;
	}

	@Override
	public String getLabel(){
		return I18N.t("confirmation.exist.record.text.value.variant");
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		final FLEFRecord variant = record.getTheOnlyChild();
		String tag = variant.getTag();
		if(TextValueVariantReader.isPhonetic(tag)){
			final String system = TextValueVariantReader.extractPhoneticSystem(variant);
			final String value = TextValueVariantReader.extractPhoneticValue(variant);

			final StringBuilder details = new StringBuilder();
			if(StringUtils.isNotEmpty(system))
				details.append(system);
			if(!details.isEmpty())
				return String.format("%s [%s: %s]", value, I18N.t("text.value.variant.phonetic"), details);
			return String.format("%s [%s]", value, I18N.t("text.value.variant.phonetic"));
		}
		else if(TextValueVariantReader.isTranscription(tag)){
			final String system = TextValueVariantReader.extractTranscriptionSystem(variant);
			final String type = TextValueVariantReader.extractTranscriptionType(variant);
			final String value = TextValueVariantReader.extractTranscriptionValue(variant);

			final StringBuilder details = new StringBuilder();
			if(StringUtils.isNotEmpty(system))
				details.append(system);
			if(StringUtils.isNotEmpty(type)){
				if(!details.isEmpty())
					details.append(", ");
				details.append(type);
			}
			if(!details.isEmpty())
				return String.format("%s [%s: %s]", value, I18N.t("text.value.variant.transcription"), details);
			return String.format("%s [%s]", value, I18N.t("text.value.variant.transcription"));
		}

		return "[--]";
	}

	@Override
	public TextValueVariantStructureDialog createNewDialog(final Window parent, final FLEFModel model){
		return TextValueVariantStructureDialog.createNew(parent, model);
	}

	@Override
	public TextValueVariantStructureDialog createEditDialog(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return TextValueVariantStructureDialog.createEdit(parent, model, record);
	}

}
