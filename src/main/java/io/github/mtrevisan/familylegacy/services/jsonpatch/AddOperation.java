/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.services.jsonpatch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jackson.jsonpointer.TokenResolver;
import com.google.common.collect.Iterables;


/**
 * JSON Patch {@code add} operation
 *
 * <p>For this operation, {@code path} is the JSON Pointer where the value
 * should be added, and {@code value} is the value to add.</p>
 *
 * <p>Note that if the target value pointed to by {@code path} already exists,
 * it is replaced. In this case, {@code add} is equivalent to {@code replace}.
 * </p>
 *
 * <p>Note also that a value will be created at the target path <b>if and only
 * if</b> the immediate parent of that value exists (and is of the correct
 * type).</p>
 *
 * <p>Finally, if the last reference token of the JSON Pointer is {@code -} and
 * the immediate parent is an array, the given value is added at the end of the
 * array. For instance, applying:</p>
 *
 * <pre>
 *     { "op": "add", "path": "/-", "value": 3 }
 * </pre>
 *
 * <p>to:</p>
 *
 * <pre>
 *     [ 1, 2 ]
 * </pre>
 *
 * <p>will give:</p>
 *
 * <pre>
 *     [ 1, 2, 3 ]
 * </pre>
 */
class AddOperation extends PathValueOperation{

	private static final String LAST_ARRAY_ELEMENT = "-";


	@JsonCreator
	public AddOperation(@JsonProperty("path") final JsonPointer path, @JsonProperty("value") final JsonNode value){
		super("add", path, value);
	}

	@Override
	public JsonNode apply(final JsonNode node) throws JsonPatchException{
		if(path.isEmpty())
			return value;

		//check the parent node: it must exist and be a container (ie, an array or an object) for the add operation to work
		final JsonNode parentNode = path.parent().path(node);
		if(parentNode.isMissingNode())
			throw JsonPatchException.create("Parent of node to add ({}) does not exist", path);
		if(!parentNode.isContainerNode())
			throw JsonPatchException.create("Parent of path to add to ({}) is not a container", path);

		final JsonNode ret = node.deepCopy();
		return (parentNode.isArray()? addToArray(path, ret): addToObject(path, ret));
	}

	private JsonNode addToArray(final JsonPointer path, final JsonNode node) throws JsonPatchException{
		final ArrayNode target = (ArrayNode)path.parent().get(node);

		final int index = extractIndex(path, target);

		target.insert(index, value);
		return node;
	}

	public static int extractIndex(final JsonPointer path, final ArrayNode target) throws JsonPatchException{
		final TokenResolver<JsonNode> token = Iterables.getLast(path);
		final int index;
		try{
			index = (token.getToken().getRaw().equals(LAST_ARRAY_ELEMENT)? target.size(): Integer.parseInt(token.toString()));
		}
		catch(final NumberFormatException ignored){
			throw JsonPatchException.create("Reference token is not an array index ('{}' from '{}')", token, path);
		}

		if(index < 0 || index > target.size())
			throw JsonPatchException.create("No such index ('{}') in target array", index);
		return index;
	}

	private JsonNode addToObject(final JsonPointer path, final JsonNode node){
		final ObjectNode target = (ObjectNode)path.parent().get(node);

		target.set(Iterables.getLast(path).getToken().getRaw(), value);

		return node;
	}

}
