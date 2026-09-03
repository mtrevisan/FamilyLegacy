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
package io.github.mtrevisan.familylegacy.v2.io.grammar.contraints;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * {@code require field member_of container}:
 * The tag/type of 'field' must be present among the tags/types of the elements in 'containerField'.
 * Example: {@code require preferred member_of resolves}
 */
public final class MemberOfConstraint extends Constraint{

	private final String field;
	private final String containerField;


	public MemberOfConstraint(final String field, final String containerField){
		this.field = field;
		this.containerField = containerField;
	}


	public String getField(){
		return field;
	}

	public String getContainerField(){
		return containerField;
	}

	@Override
	public void validate(final String contextPath, final FLEFRecord record, final FLEFModel model,
			final List<String> errors){
		final List<FLEFRecord> fieldNodes = FLEFRecordHelper.findChildren(record, field);
		if(fieldNodes == null || fieldNodes.isEmpty())
			return;

		final List<String> containerTags = resolveContainerTags(record, containerField);

		for(final FLEFRecord fieldNode : fieldNodes){
			final String fieldTag = getFirstChildTag(fieldNode);
			if(fieldTag == null){
				errors.add(String.format("Constraint violation at '%s': field '%s' has no target tag, record %s",
					contextPath, field, record));

				continue;
			}

			if(!containerTags.contains(fieldTag))
				errors.add(String.format(
					"Constraint violation at '%s': tag '%s' of field '%s' must be one of the tags in '%s' %s, record %s",
					contextPath, field, fieldTag, containerField, containerTags, record));
		}
	}

	private static List<String> resolveContainerTags(final FLEFRecord record, final String path){
		final List<String> tags = new ArrayList<>();
		final List<FLEFRecord> containerNodes = FLEFRecordHelper.findChildren(record, path);
		if(containerNodes != null)
			for(final FLEFRecord containerNode : containerNodes){
				final String tag = getFirstChildTag(containerNode);
				if(tag != null)
					tags.add(tag);
			}
		return tags;
	}

	private static String getFirstChildTag(final FLEFRecord node){
		if(node == null)
			return null;

		if(node.getChildren() != null && !node.getChildren().isEmpty())
			return node.getChildren().getFirst().getTag();
		return node.getTag();
	}

	@Override
	public String toString(){
		return "require " + field + " member_of " + containerField;
	}

}
