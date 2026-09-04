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
package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.IndividualAttributeRecordDialog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.awt.Dialog;
import java.util.List;


/**
 * Handler for {@code INDIVIDUAL_ATTRIBUTE_RECORD} entities according to FLEF 0.1.2.
 */
public class IndividualAttributeHandler extends AbstractRecordTypeHandler<IndividualAttributeRecordDialog>{

	public static final String TYPE = "INDIVIDUAL_ATTRIBUTE";
	public static final String ID_PREFIX = "IA";

	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_INDIVIDUAL = "INDIVIDUAL";


	private static final class SingletonHelper{
		private static final IndividualAttributeHandler INSTANCE = new IndividualAttributeHandler();

	}


	public static IndividualAttributeHandler getInstance(){
		return IndividualAttributeHandler.SingletonHelper.INSTANCE;
	}


	@Override
	public String getLabel(){
		return "Individual Attribute";
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIdPrefix(){
		return ID_PREFIX;
	}

	@Override
	public List<FLEFRecord> findReferences(final FLEFModel model, final String recordId,
			final String parentEntityType){
		return model.getRecordsByType(TYPE).stream()
			.filter(attribute -> {
				final List<FLEFRecord> individuals = FLEFRecordHelper.findChildren(attribute, TAG_INDIVIDUAL);
				for(final FLEFRecord individual : individuals){
					final String resolveTag = individual.getTag();
					final String resolveXRef = individual.getValue();
					if(Strings.CI.equals(resolveTag, parentEntityType) && resolveXRef.equals(recordId))
						return true;

					break;
				}
				return false;
			})
			.toList();
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		final String value = FLEFRecordHelper.getChildValue(record, TAG_VALUE);
		final StringBuilder sb = new StringBuilder();
		if(type != null)
			sb.append('(')
				.append(type)
				.append(')');
		if(StringUtils.isNotEmpty(value)){
			if(!sb.isEmpty())
				sb.append(StringUtils.SPACE);
			sb.append(value);
		}
		return sb.toString();
	}

	@Override
	public IndividualAttributeRecordDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return IndividualAttributeRecordDialog.createNew(parent, model);
	}

	@Override
	public IndividualAttributeRecordDialog createEditDialog(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return IndividualAttributeRecordDialog.createEdit(parent, model, record);
	}

}
