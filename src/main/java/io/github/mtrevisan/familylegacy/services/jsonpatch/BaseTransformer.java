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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;

import java.util.ArrayList;
import java.util.List;


/**
 * @link <a href="http://jsonpatch.com/">JSON Patch</a>
 * @link <a href="https://github.com/tananaev/json-patch">json-patch</a>
 * @link <a href="https://github.com/java-json-tools/json-patch">JSON-patch</a>
 * @link <a href="https://sookocheff.com/post/api/understanding-json-patch/">Understanding JSON Patch</a>
 * @link <a href="http://adambien.blog/roller/abien/entry/java_ee_8_manipulating_jsonobjects">Java EE 8: Manipulating JSONObjects with JSONPatch</a>
 */
public final class BaseTransformer{

	private static final String SLASH = "/";

	private static final ObjectMapper OM = new ObjectMapper();
	static{
		OM.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		OM.configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, false);
	}


	private BaseTransformer(){}

	public static JsonPatchOperation createAdd(final String field, final Object value){
		return createAdd(field, convertToJSON(value));
	}

	public static JsonPatchOperation createAdd(final String field, final JsonNode value){
		return new AddOperation(createPointer(field), (value != null? value: NullNode.getInstance()));
	}

	public static JsonPatchOperation createCopy(final String sourceField, final String destinationField){
		return new CopyOperation(createPointer(sourceField), createPointer(destinationField));
	}

	public static JsonPatchOperation createMove(final String sourceField, final String destinationField){
		return new MoveOperation(createPointer(sourceField), createPointer(destinationField));
	}

	public static JsonPatchOperation createRemove(final String field){
		return new RemoveOperation(createPointer(field));
	}

	public static JsonPatchOperation createReplace(final String field, final Object value){
		return createReplace(field, convertToJSON(value));
	}

	public static JsonPatchOperation createReplace(final String field, final JsonNode value){
		return new ReplaceOperation(createPointer(field), value);
	}

	private static JsonPointer createPointer(final String path){
		JsonPointer pointer = JsonPointer.empty();
		final String[] fields = (path.startsWith(SLASH)? path.substring(1): path).split(SLASH);
		for(final String field : fields)
			pointer = pointer.append(field);
		return pointer;
	}


	public static <T> T getObject(final JsonNode data, final String path, final Class<T> objectClass){
		return OM.convertValue(data.path(path), objectClass);
	}

	public static String getTextOrNull(final JsonNode data, final String path){
		return getText(data, path, null);
	}

	public static String getText(final JsonNode data, final String path, final String defaultValue){
		final String result = asTextOrNull(data.path(path));
		return (result != null? result: defaultValue);
	}

	public static Boolean getBooleanOrNull(final JsonNode data, final String path){
		return getBoolean(data, path, null);
	}

	public static Boolean getBoolean(final JsonNode data, final String path, final Boolean defaultValue){
		final JsonNode node = data.path(path);
		return (asTextOrNull(node) != null? node.asBoolean(): defaultValue);
	}

	public static Long getLongOrNull(final JsonNode data, final String path){
		return getLong(data, path, null);
	}

	public static Long getLong(final JsonNode data, final String path, final Long defaultValue){
		final JsonNode node = data.path(path);
		return (asTextOrNull(node) != null? node.asLong(): defaultValue);
	}

	public static Double getDoubleOrNull(final JsonNode data, final String path){
		return getDouble(data, path, null);
	}

	public static Double getDouble(final JsonNode data, final String path, final Double defaultValue){
		final JsonNode node = data.path(path);
		return (asTextOrNull(node) != null? node.asDouble(): defaultValue);
	}

	private static String asTextOrNull(final JsonNode node){
		return node.asText(null);
	}

	public static JsonNode convertToJSON(final Object data){
		return (data != null? OM.valueToTree(data): NullNode.getInstance());
	}

	public static <T> T convertFromJSON(final JsonNode data, final Class<T> type) throws JsonProcessingException{
		return (data != null? OM.treeToValue(data, type): null);
	}

	public static String convertToString(final Object data) throws JsonProcessingException{
		return OM.writeValueAsString(data);
	}

	public static List<String> parseStringList(final JsonNode node){
		final List<String> result = new ArrayList<>(0);
		if(node.isArray()){
			final ArrayNode arrayNode = (ArrayNode)node;
			for(final JsonNode subArrayNode : arrayNode)
				result.add(subArrayNode.textValue());
		}
		return result;
	}

}
