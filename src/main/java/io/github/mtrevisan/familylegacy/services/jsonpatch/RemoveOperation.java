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
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.google.common.collect.Iterables;

import java.io.IOException;


/**
 * JSON Path {@code remove} operation.
 *
 * <p>This operation only takes one pointer ({@code path}) as an argument. No operation is done if {@code from} fails to be resolved.</p>
 */
class RemoveOperation extends JsonPatchOperation{

	@JsonCreator
	public RemoveOperation(@JsonProperty("path") final JsonPointer path){
		super("remove", path);
	}

	@Override
	public JsonNode apply(final JsonNode node) throws JsonPatchException{
		if(path.isEmpty())
			return MissingNode.getInstance();

		JsonNode ret = node;
		if(!path.path(node).isMissingNode()){
			ret = node.deepCopy();

			final JsonNode parentNode = path.parent().get(ret);
			if(parentNode.isObject()){
				final String raw = Iterables.getLast(path).getToken().getRaw();
				((ObjectNode)parentNode).remove(raw);
			}
			else{
				ArrayNode array = (ArrayNode)parentNode;

				final int index = AddOperation.extractIndex(path, array);

				array.remove(index);
			}
		}
		//else
			//cannot remove unknown path
		return ret;
	}

	@Override
	public void serialize(final JsonGenerator jgen, final SerializerProvider provider) throws IOException{
		jgen.writeStartObject();
		jgen.writeStringField("op", "remove");
		jgen.writeStringField("path", path.toString());
		jgen.writeEndObject();
	}

	@Override
	public void serializeWithType(final JsonGenerator jgen, final SerializerProvider provider, final TypeSerializer typeSer)
			throws IOException{
		serialize(jgen, provider);
	}

	@Override
	public String toString(){
		return "op: " + op + "; path: \"" + path + "\"";
	}

}
