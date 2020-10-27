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
package io.github.mtrevisan.familylegacy.gedcom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class Store<T>{

	private static final String CHARSET_X_MAC_ROMAN = "x-MacRoman";
	private static final String CRLF = StringUtils.CR + StringUtils.LF;

	static final ObjectMapper OM = new ObjectMapper();
	static{
		OM.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		OM.configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, false);
	}


	protected GedcomNode root;


	public T load(final String grammarFile, final String gedcomFile) throws GedcomGrammarParseException, GedcomParseException{
		final GedcomGrammar grammar = GedcomGrammar.create(grammarFile);

		final GedcomNode root = GedcomParser.parse(gedcomFile, grammar);

		return create(root);
	}

	protected abstract T create(final GedcomNode root) throws GedcomParseException;

	public void write(final OutputStream os) throws IOException{
		final String charset = getCharsetName();
		final String eol = (CHARSET_X_MAC_ROMAN.equals(charset)? StringUtils.CR: CRLF);
		final OutputStreamWriter writer = (AnselInputStreamReader.CHARACTER_ENCODING.equals(charset)?
			new AnselOutputStreamWriter(os): new OutputStreamWriter(os, charset));
		final Writer out = new BufferedWriter(writer);

		final Deque<GedcomNode> nodeStack = new ArrayDeque<>();
		//skip root node and add its children
		for(final GedcomNode child : root.getChildren())
			nodeStack.addLast(child);
		while(!nodeStack.isEmpty()){
			final GedcomNode child = nodeStack.pop();
			final List<GedcomNode> children = child.getChildren();
			for(int i = children.size() - 1; i >= 0; i --)
				nodeStack.addFirst(children.get(i));

			out.write(Integer.toString(child.getLevel()));
			if(child.getLevel() == 0){
				appendID(out, child.getID());
				appendElement(out, child.getTag());
			}
			else{
				appendElement(out, child.getTag());
				appendID(out, child.getXRef());
				appendID(out, child.getID());
			}
			if(child.getValue() != null)
				appendElement(out, child.getValue());
			out.write(eol);
		}
		out.flush();
	}

	public GedcomNode transform() throws JsonProcessingException{
//		final List<JsonPatchOperation> operations = new ArrayList<>(Arrays.asList(
//			BaseTransformer.createMove("children[-1:].TRLR", "EOF")
//			BaseTransformer.createAdd("evtType", "ACKNOWLEDGE"),
//			BaseTransformer.createAdd("ackCommand", command),
//			BaseTransformer.createCopy(ParsingLabels.KEY_DEVICE_NAME, "id"),
//			BaseTransformer.createRemove(MessageData.KEY_DEVICE_TYPE_CODE),
//			BaseTransformer.createMove(MessageData.KEY_DEVICE_TYPE_NAME, "deviceType"),
//			BaseTransformer.createCopy(ParsingLabels.KEY_FIRMWARE_VERSION, "softwareVersion"),
//			BaseTransformer.createRemove(ParsingLabels.KEY_EVENT_TIME),
//			BaseTransformer.createAdd("eventTimestamp", DateTimeUtils.formatDateTimePlain(DateTimeUtils.parseDateTimeIso8601(eventTime))),
//			BaseTransformer.createRemove(ParsingLabels.KEY_RECEPTION_TIME),
//			BaseTransformer.createAdd("receptionTimestamp", DateTimeUtils.formatDateTimePlain(DateTimeUtils.parseDateTimeIso8601(receptionTime))),
//			BaseTransformer.createReplace(ParsingLabels.KEY_CORRELATION_ID, String.format("%04X", correlationID)),
//			BaseTransformer.createMove(ParsingLabels.KEY_CORRELATION_ID, "correlationID"),
//			BaseTransformer.createAdd("messageID", BaseTransformer.getText(data, ParsingLabels.KEY_MESSAGE_ID)),
//			BaseTransformer.createRemove(ParsingLabels.KEY_MESSAGE_ID)
//		));

		//https://github.com/json-path/JsonPath
		//https://support.smartbear.com/alertsite/docs/monitors/api/endpoint/jsonpath.html
		final String json = OM.writeValueAsString(root);
		final DocumentContext parsed = JsonPath.parse(json);
//		parsed.set("$.children[-1:].tag", "EOF");
		Predicate filter = new Predicate(){
			@Override
			public boolean apply(PredicateContext ctx){
				return false;
			}
		};
//		parsed.set("$.children[*]", "EOF", filter);
		@SuppressWarnings("unchecked")
		final Map<String, Object> node = (Map<String, Object>)((JSONArray)parsed.read("$.children[?(@.tag=='TRLR')]")).get(0);
		node.put("tag", "EOF");
		final String transformedJson = parsed.jsonString();

		//orignal data
//		final JsonNode data = BaseTransformer.convertToJSON(root);
//		final JsonPatch patch = new JsonPatch(operations);
//		final JsonNode transformedData = patch.apply(data);
//		return BaseTransformer.convertFromJSON(transformedData, GedcomNode.class);
		return null;
	}

	protected abstract String getCharsetName();

	private void appendID(final Writer out, final String id) throws IOException{
		if(id != null){
			out.write(' ');
			out.write('@');
			out.write(id);
			out.write('@');
		}
	}

	private void appendElement(final Writer out, final String elem) throws IOException{
		out.write(' ');
		out.write(elem);
	}

	protected static Map<String, GedcomNode> generateIndexes(final Collection<GedcomNode> list){
		final Map<String, GedcomNode> indexes;
		if(!list.isEmpty()){
			indexes = new HashMap<>(list.size());
			for(final GedcomNode elem : list)
				indexes.put(elem.getID(), elem);
		}
		else
			indexes = Collections.emptyMap();
		return indexes;
	}

}
