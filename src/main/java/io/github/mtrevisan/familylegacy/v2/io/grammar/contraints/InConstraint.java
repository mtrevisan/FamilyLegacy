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
 * {@code require field in container}:
 * the value of 'field' must be among the values of 'container'.
 * Esempio: {@code require preferred in resolves}
 */
public final class InConstraint extends Constraint{

	private final String field;
	private final String containerField;


	public InConstraint(final String field, final String containerField){
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
		// 1. Fetch all matching target records for field (e.g. extract.document_part.document)
		final List<FLEFRecord> fieldNodes = FLEFRecordHelper.findChildren(record, field);
		if(fieldNodes == null || fieldNodes.isEmpty())
			return;

		// 2. Resolve container values (supporting Xref navigation like source -> SourceRecord -> document)
		final List<String> containerValues = resolveContainerValues(record, containerField, model);

		// 3. Validate every target field value against container values
		for(final FLEFRecord fieldNode : fieldNodes){
			final String fieldValue = fieldNode.getValue();

			if(fieldValue == null || fieldValue.isEmpty()){
				errors.add(String.format("Constraint violation at '%s': field '%s' has no value, record %s",
					contextPath, field, record));

				continue;
			}

			if(!containerValues.contains(fieldValue))
				errors.add(String.format(
					"Constraint violation at '%s': '%s' (value: %s) must be one of the values in '%s' %s, record %s",
					contextPath, field, fieldValue, containerField, containerValues, record));
		}
	}

	/**
	 * Navigates containerField path. If an intermediate or leaf node contains an Xref ID,
	 * it resolves it against FLEFModel.
	 */
	private List<String> resolveContainerValues(final FLEFRecord record, final String path, final FLEFModel model){
		final List<String> values = new ArrayList<>();
		final String[] segments = path.split("\\.");

		final List<FLEFRecord> currentNodes = new ArrayList<>();
		currentNodes.add(record);

		for(final String segment : segments){
			final List<FLEFRecord> nextNodes = new ArrayList<>();

			for(final FLEFRecord current : currentNodes){
				final List<FLEFRecord> children = FLEFRecordHelper.findChildren(current, segment);
				if(children == null)
					continue;

				for(FLEFRecord child : children){
					// If this node represents an Xref reference to another main record, resolve it
					if(model != null && child.getValue() != null && !child.getValue().isEmpty()){
						final FLEFRecord targetMainRecord = model.getRecordById(child.getValue());
						if(targetMainRecord != null)
							child = targetMainRecord;
					}
					nextNodes.add(child);
				}
			}

			if(nextNodes.isEmpty())
				break;

			currentNodes.clear();
			currentNodes.addAll(nextNodes);
		}

		for(final FLEFRecord leaf : currentNodes)
			values.add(leaf.getId());

		return values;
	}

	@Override
	public String toString(){
		return "require " + field + " in " + containerField;
	}

}
