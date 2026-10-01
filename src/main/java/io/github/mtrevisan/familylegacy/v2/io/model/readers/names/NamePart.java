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
package io.github.mtrevisan.familylegacy.v2.io.model.readers.names;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.TextValueVariantReader;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * A single part of a personal name.
 * <p>
 * Mirrors the FLEF {@code PartStructure}. The {@code type} field is one of
 * the protocol values (given, family, patronymic, matronymic, kunya,
 * lineage, house, clan, tribal, caste, toponymic, title, occupational,
 * prefix, suffix, nickname, regnal, religious, posthumous, generation) or
 * a custom string. The order of parts inside a personal name is
 * significant and is preserved by the enclosing {@code NameAnatomy}.
 * <p>
 * The record is immutable.
 */
public record NamePart(String type, String value, List<TextValueVariant> variants){

	public NamePart{
		if(type == null)
			type = StringUtils.EMPTY;
		if(value == null)
			value = StringUtils.EMPTY;
		variants = (variants != null? List.copyOf(variants): List.of());
	}


	static NamePart parsePart(final FLEFRecord partRecord){
		final String type = NameReader.extractPartType(partRecord);

		final String value = NameReader.extractPartValue(partRecord);

		final List<TextValueVariant> variants = extractVariants(partRecord);

		return new NamePart(type, value, variants);
	}

	/**
	 * Extracts the {@code variant*} children of the given record.
	 * <p>
	 * Each {@code variant} child is a FLEF oneof, containing either a
	 * {@code phonetic} or a {@code transcription} child with the actual
	 * fields. This method flattens that structure into a single list of
	 * {@link TextValueVariant} records.
	 */
	static List<TextValueVariant> extractVariants(final FLEFRecord partRecord){
		final List<TextValueVariant> variants = new ArrayList<>();
		final List<FLEFRecord> variantWrappers = FLEFRecordHelper.findChildren(partRecord, NameReader.TAG_PART_VARIANT);
		for(final FLEFRecord variantWrapper : variantWrappers){
			for(final FLEFRecord kindChild : variantWrapper.getChildren()){
				final String kind = kindChild.getTag();
				if(!TextValueVariantReader.isPhonetic(kind) && !TextValueVariantReader.isTranscription(kind))
					continue;

				variants.add(TextValueVariant.create(kindChild));
			}
		}
		return variants;
	}

	/**
	 * Returns whether this part carries at least one variant.
	 *
	 * @return {@code true} if the variant list is not empty
	 */
	public boolean hasVariants(){
		return !variants.isEmpty();
	}

	/**
	 * Returns the display label of the part type, with underscores
	 * replaced by spaces.
	 *
	 * @return the display label, never {@code null}
	 */
	public String displayType(){
		return type.replace('_', ' ');
	}

}
