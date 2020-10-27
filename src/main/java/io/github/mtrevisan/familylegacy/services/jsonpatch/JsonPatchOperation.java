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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.github.fge.jackson.jsonpointer.JsonPointer;


/**
 * Base abstract class for one patch operation
 *
 * <p>
 * Two more abstract classes extend this one according to the arguments of the operation:
 * </p>
 *
 * <ul>
 * <li>{@link DualPathOperation} for operations taking a second pointer as an argument ({@code copy} and {@code move});</li>
 * <li>{@link PathValueOperation} for operations taking a value as an argument ({@code add}, {@code replace} and {@code test}).</li>
 * </ul>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "op")
@JsonSubTypes({
	@JsonSubTypes.Type(name = "add", value = AddOperation.class),
	@JsonSubTypes.Type(name = "copy", value = CopyOperation.class),
	@JsonSubTypes.Type(name = "move", value = MoveOperation.class),
	@JsonSubTypes.Type(name = "remove", value = RemoveOperation.class),
	@JsonSubTypes.Type(name = "replace", value = ReplaceOperation.class)
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class JsonPatchOperation implements JsonSerializable{

	final String op;

	/*
	 * Note: No need for a custom deserializer, Jackson will try and find a constructor with a single string argument and use it.
	 * However, we need to serialize using `.toString()`.
	 */
	final JsonPointer path;


	/**
	 * @param op	The operation name.
	 * @param path	The JSON Pointer for this operation.
	 */
	JsonPatchOperation(final String op, final JsonPointer path){
		this.op = op;
		this.path = path;
	}

	/**
	 * Apply this operation to a JSON value.
	 *
	 * @param node	The value to patch.
	 * @return	The patched value.
	 * @throws JsonPatchException	Operation failed to apply to this value.
	 */
	public abstract JsonNode apply(final JsonNode node) throws JsonPatchException;

	@Override
	public abstract String toString();

}
