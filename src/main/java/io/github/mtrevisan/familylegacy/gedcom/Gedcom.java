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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mtrevisan.familylegacy.gedcom.transformations.FamilyTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.HeaderTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.IndividualTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.MultimediaTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.NoteTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Protocol;
import io.github.mtrevisan.familylegacy.gedcom.transformations.RepositoryTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.SourceTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.SubmitterTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Transformation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class Gedcom extends Store{

	private static final String ID_INDIVIDUAL_PREFIX = "I";
	private static final String ID_FAMILY_PREFIX = "F";
	private static final String ID_DOCUMENT_PREFIX = "D";
	private static final String ID_NOTE_PREFIX = "N";
	private static final String ID_REPOSITORY_PREFIX = "R";
	private static final String ID_SOURCE_PREFIX = "S";
	private static final String ID_OBJECT_PREFIX = "O";
	private static final String ID_SUBMITTER_PREFIX = "SUBM";

	private static final String TAG_HEADER = "HEAD";
	private static final String TAG_INDIVIDUAL = "INDI";
	private static final String TAG_FAMILY = "FAM";
	private static final String TAG_DOCUMENT = "OBJE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_REPOSITORY = "REPO";
	private static final String TAG_SOURCE = "SOUR";
	private static final String TAG_OBJECT = "OBJE";
	private static final String TAG_SUBMITTER = "SUBM";
	private static final String TAG_CHARSET = "CHAR";

	private static final Transformation<Gedcom, Flef> HEADER_TRANSFORMATION = new HeaderTransformation();
	private static final Transformation<Gedcom, Flef> INDIVIDUAL_TRANSFORMATION = new IndividualTransformation();
	private static final Transformation<Gedcom, Flef> FAMILY_TRANSFORMATION = new FamilyTransformation();
	private static final Transformation<Gedcom, Flef> NOTE_TRANSFORMATION = new NoteTransformation();
	private static final Transformation<Gedcom, Flef> REPOSITORY_TRANSFORMATION = new RepositoryTransformation();
	private static final Transformation<Gedcom, Flef> SOURCE_TRANSFORMATION = new SourceTransformation();
	private static final Transformation<Gedcom, Flef> MULTIMEDIA_TRANSFORMATION = new MultimediaTransformation();
	private static final Transformation<Gedcom, Flef> SUBMITTER_TRANSFORMATION = new SubmitterTransformation();

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private GedcomNode header;
	private List<GedcomNode> individuals;
	private List<GedcomNode> families;
	private List<GedcomNode> documents;
	private List<GedcomNode> notes;
	private List<GedcomNode> repositories;
	private List<GedcomNode> sources;
	private List<GedcomNode> objects;
	private List<GedcomNode> submitters;

	private TreeMap<String, GedcomNode> individualIndex;
	private TreeMap<String, GedcomNode> familyIndex;
	private TreeMap<String, GedcomNode> documentIndex;
	private TreeMap<String, GedcomNode> noteIndex;
	private TreeMap<String, GedcomNode> repositoryIndex;
	private TreeMap<String, GedcomNode> sourceIndex;
	private TreeMap<String, GedcomNode> objectIndex;
	private TreeMap<String, GedcomNode> submitterIndex;

	private Map<Integer, String> documentValue;
	private Map<Integer, String> noteValue;
	private Map<Integer, String> repositoryValue;
	private Map<Integer, String> sourceValue;
	private Map<Integer, String> objectValue;

	private int nextIndividualId = 1;
	private int nextFamilyId = 1;
	private int nextDocumentId = 1;
	private int nextNoteId = 1;
	private int nextRepositoryId = 1;
	private int nextSourceId = 1;
	private int nextObjectId = 1;
	private int nextSubmitterId = 1;


	public static void main(final String[] args){
		try{
			final Store store = new Gedcom();
//			store.load("/gedg/gedcom_5.5.1.gedg", "src/main/resources/ged/small.ged");
			store.load("/gedg/gedcom_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged");

			final Store storeFlef = store.transform();

			final File outputFile = new File("flef.ged");
			final OutputStream os = new FileOutputStream(outputFile);
//			gedcom.write(os);
			storeFlef.write(os);
			os.close();

			final Store storeFlef2 = new Flef();
			storeFlef2.load("/gedg/flef_0.0.7.gedg", outputFile.getPath());
			final Store storeGedcom = storeFlef2.transform();

			final File outputFile2 = new File("original.ged");
			final OutputStream os2 = new FileOutputStream(outputFile2);
			storeGedcom.write(os2);
			os2.close();
		}
		catch(final Exception e){
			e.printStackTrace();
		}
	}


	static Protocol extractProtocol(final String gedcomFile) throws GedcomParseException{
		Protocol protocol = null;
		protocolFinder:
		try(final BufferedReader br = GedcomHelper.getBufferedReader(new FileInputStream(gedcomFile))){
			int zeroLevelsFound = 0;
			String line;
			while(zeroLevelsFound < 2 && (line = br.readLine()) != null){
				//skip empty lines
				if(line.charAt(0) == ' ' || line.charAt(0) == '\t' || line.trim().isEmpty())
					continue;

				if(line.charAt(0) == '0')
					zeroLevelsFound ++;
				if("1 GEDC".equals(line)){
					protocol = Protocol.GEDCOM;
					while((line = br.readLine()) != null && line.charAt(0) == '2')
						if(line.startsWith("2 VERS ")){
							protocol.setVersion(line.substring("2 VERS ".length()));
							break protocolFinder;
						}
				}
			}
		}
		catch(final IllegalArgumentException | IOException e){
			throw new GedcomParseException((e.getMessage() == null? "GEDCOM file '{}' not found!": e.getMessage()), gedcomFile);
		}
		catch(final Exception e){
			throw new GedcomParseException("Failed to read file", e);
		}
		return protocol;
	}


	@Override
	protected void create(final GedcomNode root, final String basePath) throws GedcomParseException{
		super.create(root, basePath);

		final List<GedcomNode> heads = root.getChildrenWithTag(TAG_HEADER);
		if(heads.size() != 1)
			throw new GedcomParseException("Required header tag missing");

		header = heads.get(0);
		individuals = root.getChildrenWithTag(TAG_INDIVIDUAL);
		families = root.getChildrenWithTag(TAG_FAMILY);
		documents = root.getChildrenWithTag(TAG_DOCUMENT);
		notes = root.getChildrenWithTag(TAG_NOTE);
		repositories = root.getChildrenWithTag(TAG_REPOSITORY);
		sources = root.getChildrenWithTag(TAG_SOURCE);
		objects = root.getChildrenWithTag(TAG_OBJECT);
		submitters = root.getChildrenWithTag(TAG_SUBMITTER);

		individualIndex = generateIndexes(individuals);
		familyIndex = generateIndexes(families);
		documentIndex = generateIndexes(documents);
		noteIndex = generateIndexes(notes);
		repositoryIndex = generateIndexes(repositories);
		sourceIndex = generateIndexes(sources);
		objectIndex = generateIndexes(objects);
		submitterIndex = generateIndexes(submitters);

		documentValue = reverseMap(documentIndex);
		noteValue = reverseMap(noteIndex);
		repositoryValue = reverseMap(repositoryIndex);
		sourceValue = reverseMap(sourceIndex);

		if(!individualIndex.isEmpty())
			nextIndividualId = extractLastID(individualIndex.lastKey()) + 1;
		if(!familyIndex.isEmpty())
			nextFamilyId = extractLastID(familyIndex.lastKey()) + 1;
		if(!documentIndex.isEmpty())
			nextDocumentId = extractLastID(documentIndex.lastKey()) + 1;
		if(!noteIndex.isEmpty())
			nextNoteId = extractLastID(noteIndex.lastKey()) + 1;
		if(!repositoryIndex.isEmpty())
			nextRepositoryId = extractLastID(repositoryIndex.lastKey()) + 1;
		if(!sourceIndex.isEmpty())
			nextSourceId = extractLastID(sourceIndex.lastKey()) + 1;
		if(!objectIndex.isEmpty())
			nextObjectId = extractLastID(objectIndex.lastKey()) + 1;
		if(!submitterIndex.isEmpty())
			nextSubmitterId = extractLastID(submitterIndex.lastKey()) + 1;
	}

	@Override
	public Flef transform(){
		final Flef destination = new Flef();
		SUBMITTER_TRANSFORMATION.to(this, destination);
		HEADER_TRANSFORMATION.to(this, destination);
		NOTE_TRANSFORMATION.to(this, destination);
		REPOSITORY_TRANSFORMATION.to(this, destination);
		SOURCE_TRANSFORMATION.to(this, destination);
		MULTIMEDIA_TRANSFORMATION.to(this, destination);
		INDIVIDUAL_TRANSFORMATION.to(this, destination);
		FAMILY_TRANSFORMATION.to(this, destination);
		return destination;
	}

	@Override
	protected String getCharsetName(){
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

	@Override
	public void write(final OutputStream os) throws IOException{
		if(root == null)
			root = GedcomNodeBuilder.createRoot(Protocol.GEDCOM)
				.addChild(header)
				.addChildren(individuals)
				.addChildren(families)
				.addChildren(notes)
				.addChildren(repositories)
				.addChildren(sources)
				.addChildren(submitters)
				.addClosingChild("TRLR");

		super.write(os);
	}


	public GedcomNode getHeader(){
		return header;
	}

	public void setHeader(final GedcomNode header){
		this.header = header;
	}


	public List<GedcomNode> getIndividuals(){
		return individuals;
	}

	public GedcomNode getIndividual(final String id){
		return individualIndex.get(id);
	}

	public String addIndividual(final GedcomNode individual){
		if(individuals == null){
			individuals = new ArrayList<>(1);
			individualIndex = new TreeMap<>();
		}

		final String individualID = getNextIndividualID();
		individual.withID(individualID);

		individuals.add(individual);
		individualIndex.put(individual.getID(), individual);

		nextIndividualId ++;

		return individualID;
	}

	private String getNextIndividualID(){
		return ID_INDIVIDUAL_PREFIX + nextIndividualId;
	}


	public List<GedcomNode> getFamilies(){
		return families;
	}

	public GedcomNode getFamily(final String id){
		return familyIndex.get(id);
	}

	public String addFamily(final GedcomNode family){
		if(families == null){
			families = new ArrayList<>(1);
			familyIndex = new TreeMap<>();
		}

		final String familyID = getNextFamilyID();
		family.withID(familyID);

		families.add(family);
		familyIndex.put(family.getID(), family);

		nextFamilyId ++;

		return familyID;
	}

	private String getNextFamilyID(){
		return ID_FAMILY_PREFIX + nextFamilyId;
	}


	public List<GedcomNode> getDocuments(){
		return documents;
	}

	public GedcomNode getDocument(final String id){
		return documentIndex.get(id);
	}

	public String addDocument(final GedcomNode document){
		//search document
		String documentID = (!document.isEmpty() && documentValue != null? documentValue.get(document.hashCode()): null);
		if(documentID == null){
			//if document is not found:
			if(documents == null){
				documents = new ArrayList<>(1);
				documentIndex = new TreeMap<>();
				documentValue = new HashMap<>(1);
			}

			documentID = getNextDocumentID();
			document.withID(documentID);

			documents.add(document);
			documentIndex.put(documentID, document);
			documentValue.put(document.hashCode(), documentID);

			nextDocumentId ++;
		}
		return documentID;
	}

	private String getNextDocumentID(){
		return ID_DOCUMENT_PREFIX + nextDocumentId;
	}


	public List<GedcomNode> getNotes(){
		return notes;
	}

	public GedcomNode getNote(final String id){
		return noteIndex.get(id);
	}

	public String addNote(final GedcomNode note){
		if(notes == null){
			notes = new ArrayList<>(1);
			noteIndex = new TreeMap<>();
			noteValue = new HashMap<>(1);
		}

		final String noteID = getNextNoteID();
		note.withID(noteID);

		notes.add(note);
		noteIndex.put(noteID, note);
		noteValue.put(note.hashCode(), noteID);

		nextNoteId ++;

		return noteID;
	}

	private String getNextNoteID(){
		return ID_NOTE_PREFIX + nextNoteId;
	}


	public List<GedcomNode> getRepositories(){
		return repositories;
	}

	public GedcomNode getRepository(final String id){
		return repositoryIndex.get(id);
	}

	public String addRepository(final GedcomNode repository){
		//search repository
		String repositoryID = (!repository.isEmpty() && repositoryValue != null? repositoryValue.get(repository.hashCode()): null);
		if(repositoryID == null){
			//if repository is not found:
			if(repositories == null){
				repositories = new ArrayList<>(1);
				repositoryIndex = new TreeMap<>();
				repositoryValue = new HashMap<>(1);
			}

			repositoryID = getNextRepositoryID();
			repository.withID(repositoryID);

			repositories.add(repository);
			repositoryIndex.put(repositoryID, repository);
			repositoryValue.put(repository.hashCode(), repositoryID);

			nextRepositoryId ++;
		}
		return repositoryID;
	}

	private String getNextRepositoryID(){
		return ID_REPOSITORY_PREFIX + nextRepositoryId;
	}


	public List<GedcomNode> getSources(){
		return sources;
	}

	public GedcomNode getSource(final String id){
		return sourceIndex.get(id);
	}

	public String retrieveSourceID(final GedcomNode source){
		return (!source.isEmpty() && sourceValue != null? sourceValue.get(source.hashCode()): null);
	}

	public String addSource(final GedcomNode source){
		//search source
		String sourceID = (!source.isEmpty() && sourceValue != null? sourceValue.get(source.hashCode()): null);
		if(sourceID == null){
			//if source is not found:
			if(sources == null){
				sources = new ArrayList<>(1);
				sourceIndex = new TreeMap<>();
				sourceValue = new HashMap<>(1);
			}

			sourceID = getNextSourceID();
			source.withID(sourceID);

			sources.add(source);
			sourceIndex.put(sourceID, source);
			sourceValue.put(source.hashCode(), sourceID);

			nextSourceId ++;
		}
		return sourceID;
	}

	private String getNextSourceID(){
		return ID_SOURCE_PREFIX + nextSourceId;
	}


	public List<GedcomNode> getObjects(){
		return objects;
	}

	public GedcomNode getObject(final String id){
		return objectIndex.get(id);
	}

	public String addObject(final GedcomNode object){
		//search object
		String objectID = (!object.isEmpty() && objectValue != null? objectValue.get(object.hashCode()): null);
		if(objectID == null){
			//if object is not found:
			if(objects == null){
				objects = new ArrayList<>(1);
				objectIndex = new TreeMap<>();
				objectValue = new HashMap<>(1);
			}

			objectID = getNextObjectID();
			object.withID(objectID);

			objects.add(object);
			objectIndex.put(objectID, object);
			objectValue.put(object.hashCode(), objectID);

			nextObjectId ++;
		}
		return objectID;
	}

	private String getNextObjectID(){
		return ID_OBJECT_PREFIX + nextObjectId;
	}


	public List<GedcomNode> getSubmitters(){
		return submitters;
	}

	public GedcomNode getSubmitter(final String id){
		return (id != null? submitterIndex.get(id): null);
	}

	public String addSubmitter(final GedcomNode submitter){
		String submitterID = submitter.getID();
		if(!submitter.isEmpty()){
			if(submitters == null){
				submitters = new ArrayList<>(1);
				submitterIndex = new TreeMap<>();
			}

			submitterID = getNextSubmitterID();
			submitter.withID(submitterID);

			submitters.add(submitter);
			submitterIndex.put(submitter.getID(), submitter);

			nextSubmitterId ++;
		}
		return submitterID;
	}

	private String getNextSubmitterID(){
		return ID_SUBMITTER_PREFIX + nextSubmitterId;
	}

}
