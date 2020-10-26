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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.github.fge.jackson.jsonpointer.JsonPointer;

import java.io.IOException;


/** Base class for patch operations taking a value in addition to a path */
abstract class PathValueOperation extends JsonPatchOperation{

	@JsonSerialize
	final JsonNode value;


	/**
	 * @param op	Operation name.
	 * @param path	Affected path.
	 * @param value	JSON value.
	 */
	PathValueOperation(final String op, final JsonPointer path, final JsonNode value){
		super(op, path);

		this.value = value.deepCopy();
	}

	@Override
	public final void serialize(final JsonGenerator jgen, final SerializerProvider provider) throws IOException{
		jgen.writeStartObject();
		jgen.writeStringField("op", op);
		jgen.writeStringField("path", path.toString());
		jgen.writeFieldName("value");
		jgen.writeTree(value);
		jgen.writeEndObject();
	}

	@Override
	public final void serializeWithType(final JsonGenerator jgen, final SerializerProvider provider, final TypeSerializer typeSer)
			throws IOException{
		serialize(jgen, provider);
	}

	@Override
	public final String toString(){
		return "op: " + op + "; path: \"" + path + "\"; value: " + value;
	}

}
