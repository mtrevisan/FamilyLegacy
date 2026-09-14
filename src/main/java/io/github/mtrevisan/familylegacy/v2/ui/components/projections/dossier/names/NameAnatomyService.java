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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.dossier.names;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Extracts the name anatomy of an individual or a group from a FLEF model.
 * <p>
 * <b>Personal names.</b> For an individual, each {@code name} child is a
 * {@code PersonalNameStructure} with an ordered list of {@code part}
 * children, each carrying a {@code type}, a {@code value} and optional
 * {@code variant} children. The service preserves the order of the parts
 * and normalizes empty fields to empty strings.
 * <p>
 * <b>Generic names.</b> For a group (and for any other entity that uses
 * the generic {@code NameStructure}), each {@code name} child carries a
 * single {@code value} plus optional variants on the name itself, with no
 * parts.
 * <p>
 * The service is stateless and not thread-safe; use it from the Swing
 * Event Dispatch Thread.
 */
public final class NameAnatomyService{

	// Tag names as defined by the FLEF protocol.
	private static final String TAG_NAME = "name";
	private static final String TAG_TYPE = "type";
	private static final String TAG_VALUE = "value";
	private static final String TAG_LOCALE = "locale";
	private static final String TAG_PART = "part";
	private static final String TAG_VARIANT = "variant";
	private static final String TAG_SYSTEM = "system";
	private static final String TAG_PHONETIC = "phonetic";
	private static final String TAG_TRANSCRIPTION = "transcription";


	private final FLEFModel model;


	public NameAnatomyService(final FLEFModel model){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");
		this.model = model;
	}


	/**
	 * Extracts the anatomy of every name of the given individual.
	 *
	 * @param individualId the individual id; may be {@code null}
	 * @return the ordered list of names, never {@code null}
	 */
	public List<NameAnatomy> extractForIndividual(final String individualId){
		final FLEFRecord individual = (individualId != null? model.getRecordById(individualId): null);
		if(individual == null)
			return List.of();

		final List<NameAnatomy> result = new ArrayList<>();
		for(final FLEFRecord nameRecord : FLEFRecordHelper.findChildren(individual, TAG_NAME))
			result.add(parsePersonalName(nameRecord));
		return result;
	}

	/**
	 * Extracts the anatomy of every name of the given group.
	 *
	 * @param groupId the group id; may be {@code null}
	 * @return the ordered list of names, never {@code null}
	 */
	public List<NameAnatomy> extractForGroup(final String groupId){
		final FLEFRecord group = (groupId != null? model.getRecordById(groupId): null);
		if(group == null)
			return List.of();

		final List<NameAnatomy> result = new ArrayList<>();
		for(final FLEFRecord nameRecord : FLEFRecordHelper.findChildren(group, TAG_NAME))
			result.add(parseGenericName(nameRecord));
		return result;
	}


	/* ======================================================================
	 *                          Personal name
	 * ====================================================================== */

	private NameAnatomy parsePersonalName(final FLEFRecord nameRecord){
		final String type = text(nameRecord, TAG_TYPE);
		final String locale = text(nameRecord, TAG_LOCALE);

		final List<NamePart> parts = new ArrayList<>();
		for(final FLEFRecord partRecord : FLEFRecordHelper.findChildren(nameRecord, TAG_PART))
			parts.add(parsePart(partRecord));

		final List<String> culturalNorms = extractReferences(nameRecord, CulturalNormHandler.TYPE);

		return new NameAnatomy(type, locale, StringUtils.EMPTY, parts, List.of(), culturalNorms, nameRecord);
	}

	private NamePart parsePart(final FLEFRecord partRecord){
		final String type = text(partRecord, TAG_TYPE);
		final String value = text(partRecord, TAG_VALUE);
		final List<NameVariant> variants = extractVariants(partRecord);
		return new NamePart(type, value, variants);
	}


	/* ======================================================================
	 *                          Generic name
	 * ====================================================================== */

	private NameAnatomy parseGenericName(final FLEFRecord nameRecord){
		final String type = text(nameRecord, TAG_TYPE);
		final String locale = text(nameRecord, TAG_LOCALE);
		final String value = text(nameRecord, TAG_VALUE);
		final List<NameVariant> variants = extractVariants(nameRecord);
		return new NameAnatomy(type, locale, value, List.of(), variants, List.of(), nameRecord);
	}


	/* ======================================================================
	 *                          Variants
	 * ====================================================================== */

	/**
	 * Extracts the {@code variant*} children of the given record.
	 * <p>
	 * Each {@code variant} child is a FLEF oneof, containing either a
	 * {@code phonetic} or a {@code transcription} child with the actual
	 * fields. This method flattens that structure into a single list of
	 * {@link NameVariant} records.
	 */
	private static List<NameVariant> extractVariants(final FLEFRecord parent){
		final List<NameVariant> variants = new ArrayList<>();
		for(final FLEFRecord variantWrapper : FLEFRecordHelper.findChildren(parent, TAG_VARIANT)){
			for(final FLEFRecord kindChild : variantWrapper.getChildren()){
				final String kind = kindChild.getTag();
				if(!TAG_PHONETIC.equals(kind) && !TAG_TRANSCRIPTION.equals(kind))
					continue;

				final String system = text(kindChild, TAG_SYSTEM);
				final String type = text(kindChild, TAG_TYPE);
				final String value = text(kindChild, TAG_VALUE);
				variants.add(new NameVariant(kind, system, type, value));
			}
		}
		return variants;
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static String text(final FLEFRecord record, final String tag){
		final String value = FLEFRecordHelper.getChildValue(record, tag);
		return (value != null? value: StringUtils.EMPTY);
	}

	/**
	 * Extracts a list of referenced ids from the given {@code tag}
	 * children of a record. Each child may wrap its reference one level
	 * deep.
	 */
	private static List<String> extractReferences(final FLEFRecord parent, final String tag){
		final List<String> result = new ArrayList<>();
		for(final FLEFRecord ref : FLEFRecordHelper.findChildren(parent, tag)){
			final FLEFRecord inner = ref.getTheOnlyChild();
			final String id = (inner != null && inner.getValue() != null
				? inner.getValue()
				: ref.getValue());
			if(id != null)
				result.add(id);
		}
		return result;
	}

}
