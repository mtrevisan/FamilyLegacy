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
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.github.fge.jackson.JacksonUtils;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.List;


/**
 * Implementation of JSON Patch
 *
 * <p><a href="http://tools.ietf.org/html/draft-ietf-appsawg-json-patch-10">JSON
 * Patch</a>, as its name implies, is an IETF draft describing a mechanism to
 * apply a patch to any JSON value. This implementation covers all operations
 * according to the specification; however, there are some subtle differences
 * with regards to some operations which are covered in these operations'
 * respective documentation.</p>
 *
 * <p>An example of a JSON Patch is as follows:</p>
 *
 * <pre>
 *     [
 *         {
 *             "op": "add",
 *             "path": "/-",
 *             "value": {
 *                 "productId": 19,
 *                 "name": "Duvel",
 *                 "type": "beer"
 *             }
 *         }
 *     ]
 * </pre>
 *
 * <p>This patch contains a single operation which adds an item at the end of
 * an array. A JSON Patch can contain more than one operation; in this case, all
 * operations are applied to the input JSON value in their order of appearance,
 * until all operations are applied or an error condition is encountered.</p>
 *
 * <p>The main point where this implementation differs from the specification
 * is initial JSON parsing. The draft says:</p>
 *
 * <pre>
 *     Operation objects MUST have exactly one "op" member
 * </pre>
 *
 * <p>and:</p>
 *
 * <pre>
 *     Additionally, operation objects MUST have exactly one "path" member.
 * </pre>
 *
 * <p>However, obeying these to the letter forces constraints on the JSON
 * <b>parser</b>. Here, these constraints are not enforced, which means:</p>
 *
 * <pre>
 *     [ { "op": "add", "op": "remove", "path": "/x" } ]
 * </pre>
 *
 * <p>is parsed (as a {@code remove} operation, since it appears last).</p>
 *
 * <p><b>IMPORTANT NOTE:</b> the JSON Patch is supposed to be VALID when the
 * constructor for this class ({@link JsonPatch#fromJson(JsonNode)} is used.</p>
 *
 * @see <a href="https://github.com/java-json-tools/json-patch">Json patch</a>
 */
public class JsonPatch implements JsonSerializable{

	/**
	 * List of operations
	 */
	private final List<JsonPatchOperation> operations;


	/**
	 * @param operations	The list of operations for this patch.
	 * @see JsonPatchOperation
	 */
	@JsonCreator
	public JsonPatch(final List<JsonPatchOperation> operations){
		this.operations = ImmutableList.copyOf(operations);
	}

	/**
	 * Static factory method to build a JSON Patch out of a JSON representation.
	 *
	 * @param node	The JSON representation of the generated JSON Patch.
	 * @return	A JSON Patch.
	 * @throws IOException	Input is not a valid JSON patch.
	 * @throws NullPointerException	Input is {@code null}.
	 */
	public static JsonPatch fromJson(final JsonNode node) throws IOException{
		if(node == null)
			throw new IOException("input cannot be null");

		return JacksonUtils.getReader()
			.forType(JsonPatch.class)
			.readValue(node);
	}

	/**
	 * Apply this patch to a JSON value.
	 *
	 * @param node	The value to apply the patch to.
	 * @return	The patched JSON value.
	 * @throws JsonPatchException	Failed to apply patch.
	 * @throws NullPointerException	Input is {@code null}.
	 */
	public JsonNode apply(final JsonNode node) throws JsonPatchException{
		if(node == null)
			throw JsonPatchException.create("input cannot be null");

		JsonNode ret = node.deepCopy();
		for(final JsonPatchOperation operation : operations)
			ret = operation.apply(ret);
		return ret;
	}

	@Override
	public String toString(){
		return operations.toString();
	}

	@Override
	public void serialize(final JsonGenerator jgen, final SerializerProvider provider) throws IOException{
		jgen.writeStartArray();
		for(final JsonPatchOperation op : operations)
			op.serialize(jgen, provider);
		jgen.writeEndArray();
	}

	@Override
	public void serializeWithType(final JsonGenerator jgen, final SerializerProvider provider, final TypeSerializer typeSer)
			throws IOException{
		serialize(jgen, provider);
	}

}
