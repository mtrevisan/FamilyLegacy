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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Flef{

	private static final String CHARSET_X_MAC_ROMAN = "x-MacRoman";
	private static final String CRLF = StringUtils.CR + StringUtils.LF;
	private static final String TAG_HEADER = "HEADER";
	private static final String TAG_INDIVIDUAL = "INDIVIDUAL";
	private static final String TAG_FAMILY = "FAMILY";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_DOCUMENT = "DOCUMENT";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_REPOSITORY = "REPOSITORY";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_SUBMITTER = "SUBMITTER";
	private static final String TAG_CHARSET = "CHARSET";

	private static final ObjectMapper OM = new ObjectMapper();
	static{
		OM.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		OM.configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, false);
	}


	private GedcomNode root;

	private GedcomNode header;
	private List<GedcomNode> people;
	private List<GedcomNode> families;
	private List<GedcomNode> places;
	private List<GedcomNode> documents;
	private List<GedcomNode> notes;
	private List<GedcomNode> repositories;
	private List<GedcomNode> sources;
	private List<GedcomNode> submitters;

	private Map<String, GedcomNode> personIndex;
	private Map<String, GedcomNode> familyIndex;
	private Map<String, GedcomNode> placeIndex;
	private Map<String, GedcomNode> documentIndex;
	private Map<String, GedcomNode> noteIndex;
	private Map<String, GedcomNode> repositoryIndex;
	private Map<String, GedcomNode> sourceIndex;
	private Map<String, GedcomNode> submitterIndex;


	public static void main(final String[] args){
		try{
			final Flef flef = load("/gedg/flef_0.0.1.gedg", "/ged/small.flef.ged");

			flef.transform();

			final OutputStream os = new FileOutputStream(new File("./tmp.ged"));
			flef.write(os);
		}
		catch(final Exception e){
			e.printStackTrace();
		}
	}

	public static Flef load(final String grammarFile, final String gedcomFile) throws GedcomGrammarParseException, GedcomParseException{
		final GedcomGrammar grammar = GedcomGrammar.create(grammarFile);

		final GedcomNode root = GedcomParser.parse(gedcomFile, grammar);

		return create(root);
	}

	private static Flef create(final GedcomNode root) throws GedcomParseException{
		final Flef g = new Flef();
		g.root = root;
		final List<GedcomNode> headers = root.getChildrenWithTag(TAG_HEADER);
		if(headers.size() != 1)
			throw GedcomParseException.create("Required header tag missing");
		g.header = headers.get(0);
		g.people = root.getChildrenWithTag(TAG_INDIVIDUAL);
		g.families = root.getChildrenWithTag(TAG_FAMILY);
		g.places = root.getChildrenWithTag(TAG_PLACE);
		g.documents = root.getChildrenWithTag(TAG_DOCUMENT);
		g.notes = root.getChildrenWithTag(TAG_NOTE);
		g.repositories = root.getChildrenWithTag(TAG_REPOSITORY);
		g.sources = root.getChildrenWithTag(TAG_SOURCE);
		g.submitters = root.getChildrenWithTag(TAG_SUBMITTER);

		g.personIndex = generateIndexes(g.people);
		g.familyIndex = generateIndexes(g.families);
		g.placeIndex = generateIndexes(g.places);
		g.documentIndex = generateIndexes(g.documents);
		g.noteIndex = generateIndexes(g.notes);
		g.repositoryIndex = generateIndexes(g.repositories);
		g.sourceIndex = generateIndexes(g.sources);
		g.submitterIndex = generateIndexes(g.submitters);

		return g;
	}

	/**
	 * Prints the GEDCOM file without indentation.
	 */
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

	private String getCharsetName(){
		final List<GedcomNode> source = header.getChildrenWithTag(TAG_SOURCE);
		final String generator = (!source.isEmpty()? source.get(0).getValue(): null);
		final List<GedcomNode> characterSet = header.getChildrenWithTag(TAG_CHARSET);
		String charset = (!characterSet.isEmpty()? characterSet.get(0).getValue(): null);
		final List<GedcomNode> characterSetVersion = (!characterSet.isEmpty()? characterSet.get(0).getChildren(): Collections.emptyList());
		final String version = (!characterSetVersion.isEmpty()? characterSetVersion.get(0).getValue(): null);
		charset = GedcomHelper.getCorrectedCharsetName(generator, charset, version);
		if(charset.isEmpty())
			//default
			charset = StandardCharsets.UTF_8.name();
		return charset;
	}

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

	private static Map<String, GedcomNode> generateIndexes(final Collection<GedcomNode> list){
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

	public GedcomNode getHeader(){
		return header;
	}

	public List<GedcomNode> getPeople(){
		return people;
	}

	public GedcomNode getPerson(final String id){
		return personIndex.get(id);
	}

	public List<GedcomNode> getFamilies(){
		return families;
	}

	public GedcomNode getFamily(final String id){
		return familyIndex.get(id);
	}

	public List<GedcomNode> getPlaces(){
		return places;
	}

	public GedcomNode getPlace(final String id){
		return placeIndex.get(id);
	}

	public List<GedcomNode> getDocuments(){
		return documents;
	}

	public GedcomNode getDocument(final String id){
		return documentIndex.get(id);
	}

	public List<GedcomNode> getNotes(){
		return notes;
	}

	public GedcomNode getNote(final String id){
		return noteIndex.get(id);
	}

	public List<GedcomNode> getRepositories(){
		return repositories;
	}

	public GedcomNode getRepository(final String id){
		return repositoryIndex.get(id);
	}

	public List<GedcomNode> getSources(){
		return sources;
	}

	public GedcomNode getSource(final String id){
		return sourceIndex.get(id);
	}

	public List<GedcomNode> getSubmitters(){
		return submitters;
	}

	public GedcomNode getSubmitter(final String id){
		return submitterIndex.get(id);
	}

}
