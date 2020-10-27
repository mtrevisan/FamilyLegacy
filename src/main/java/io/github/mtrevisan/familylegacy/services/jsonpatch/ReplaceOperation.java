/**
 * Copyright (c) 2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.services.jsonpatch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.google.common.collect.Iterables;


/**
 * JSON Patch {@code replace} operation.
 *
 * <p>For this operation, {@code path} points to the value to replace, and {@code value} is the replacement value.</p>
 * <p>No operation is done if {@code from} fails to be resolved.</p>
 */
class ReplaceOperation extends PathValueOperation{

	@JsonCreator
	public ReplaceOperation(@JsonProperty("path") final JsonPointer path, @JsonProperty("value") final JsonNode value){
		super("replace", path, value);
	}

	/*
	 * FIXME cannot quite be replaced by a remove + add because of arrays. For instance:
	 *
	 * { "op": "replace", "path": "/0", "value": 1 }
	 *
	 * with
	 *
	 * [ "x" ]
	 *
	 * If remove is done first, the array is empty and add rightly complains that there is no such index in the array.
	 */
	@Override
	public JsonNode apply(final JsonNode node) throws JsonPatchException{
		if(!path.path(node).isMissingNode()){
			final JsonNode replacement = value.deepCopy();
			if(path.isEmpty())
				return replacement;

			final JsonNode parent = path.parent().get(node);
			if(parent.isObject()){
				final String rawToken = Iterables.getLast(path).getToken().getRaw();
				((ObjectNode)parent).replace(rawToken, replacement);
			}
			else{
				ArrayNode array = (ArrayNode)parent;

				final int index = AddOperation.extractIndex(path, array);

				array.set(index, replacement);
			}
		}
		//else
			//cannot replace unknown path
		return node;
	}

}
