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
import com.github.fge.jackson.jsonpointer.JsonPointer;


/**
 * JSON Patch {@code move} operation
 *
 * <p>For this operation, {@code from} points to the value to move, and {@code
 * path} points to the new location of the moved value.</p>
 *
 * <p>As for {@code add}:</p>
 *
 * <ul>
 *     <li>the value at the destination path is either created or replaced;</li>
 *     <li>it is created only if the immediate parent exists;</li>
 *     <li>{@code -} appends at the end of an array.</li>
 * </ul>
 *
 * <p>No operation is done if {@code from} fails to be resolved.</p>
 *
 * <p>The specification adds another rule that the {@code from} path must not be
 * an immediate parent of {@code path}. Unfortunately, that doesn't really work.
 * Consider this patch:</p>
 *
 * <pre>
 *     { "op": "move", "from": "/0", "path": "/0/x" }
 * </pre>
 *
 * <p>Even though {@code /0} is an immediate parent of {@code /0/x}, when this
 * patch is applied to:</p>
 *
 * <pre>
 *     [ "victim", {} ]
 * </pre>
 *
 * <p>it actually succeeds and results in the patched value:</p>
 *
 * <pre>
 *     [ { "x": "victim" } ]
 * </pre>
 */
class MoveOperation extends DualPathOperation{

	@JsonCreator
	public MoveOperation(@JsonProperty("from") final JsonPointer from, @JsonProperty("path") final JsonPointer path){
		super("move", from, path);
	}

	@Override
	public JsonNode apply(JsonNode node) throws JsonPatchException{
		if(from.equals(path))
			node = node.deepCopy();
		else{
			final JsonNode movedNode = from.path(node);
			if(!movedNode.isMissingNode()){
				final JsonPatchOperation remove = new RemoveOperation(from);
				final JsonPatchOperation add = new AddOperation(path, movedNode);
				node = add.apply(remove.apply(node));
			}
			//else
				//cannot move unknown path
		}
		return node;
	}

}
