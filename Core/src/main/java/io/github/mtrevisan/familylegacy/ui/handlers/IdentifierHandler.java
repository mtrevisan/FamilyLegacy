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
import io.github.mtrevisan.familylegacy.io.model.readers.IdentifierReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import java.awt.Window;
import java.util.Locale;


/**
 * Handler for {@code IDENTIFIER} structures (the identifiers of a source, and of a repository citation) according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * struct IdentifierStructure {
 *   scheme: enum { isbn, issn, doi, url, call_number, dewey, lcc, udc, colon, garside, fhl_film, fhl_digital_folder, fhl_ancestral_file, index_card } | Text
 *   value: Text
 * }
 * </pre>
 */
public class IdentifierHandler extends AbstractRecordTypeHandler<RecordDialog>{

	public static final String TYPE = IdentifierReader.TYPE;

	/** The client property of the root pane of the dialog that holds the identifiers: it tells which schemes to offer first. */
	public static final String SCOPE_PROPERTY = "identifier.scope";
	/** The value of {@link #SCOPE_PROPERTY} for the identifiers of a repository citation: where the source is found in the repository. */
	public static final String SCOPE_REPOSITORY = "repository";


	private static final class SingletonHelper{
		private static final IdentifierHandler INSTANCE = new IdentifierHandler();
	}


	public static IdentifierHandler getInstance(){
		return SingletonHelper.INSTANCE;
	}


	@Override
	public boolean isTopLevelEntity(){
		return false;
	}

	@Override
	public String getLabel(){
		return I18N.t("confirmation.exist_record.identifier");
	}

	@Override
	public String getType(){
		return TYPE;
	}

	/** @return {@code <scheme>: <value>}, with the scheme as the user reads it (translated, if it is one of the vocabulary) */
	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		final String scheme = StringUtils.trimToEmpty(IdentifierReader.extractScheme(record));
		final String value = StringUtils.trimToEmpty(IdentifierReader.extractValue(record));
		final String label = (scheme.isEmpty()? scheme: I18N.t("enum.identifier.scheme." + scheme.toLowerCase(Locale.ROOT), scheme));
		return GUIHelper.limitTextLength(label.isEmpty()? value: label + ": " + value);
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
