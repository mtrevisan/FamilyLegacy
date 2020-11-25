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

import io.github.mtrevisan.familylegacy.gedcom.transformations.DocumentTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.FamilyTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.HeaderTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.IndividualTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.NoteTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Protocol;
import io.github.mtrevisan.familylegacy.gedcom.transformations.RepositoryTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.SourceTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.SubmitterTransformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Transformation;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Transformer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Flef extends Store{

	private static final String ID_INDIVIDUAL_PREFIX = "I";
	private static final String ID_FAMILY_PREFIX = "F";
	private static final String ID_PLACE_PREFIX = "P";
	private static final String ID_NOTE_PREFIX = "N";
	private static final String ID_REPOSITORY_PREFIX = "R";
	private static final String ID_SOURCE_PREFIX = "S";
	private static final String ID_CULTURAL_RULE_PREFIX = "A";
	private static final String ID_GROUP_PREFIX = "G";
	private static final String ID_SUBMITTER_PREFIX = "M";

	private static final String TAG_HEADER = "HEADER";
	private static final String TAG_INDIVIDUAL = "INDIVIDUAL";
	private static final String TAG_FAMILY = "FAMILY";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_REPOSITORY = "REPOSITORY";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_CULTURAL_RULE = "CULTURAL_RULE";
	private static final String TAG_GROUP = "GROUP";
	private static final String TAG_SUBMITTER = "SUBMITTER";
	private static final String TAG_CHARSET = "CHARSET";

	private static final Transformation<Gedcom, Flef> HEADER_TRANSFORMATION = new HeaderTransformation();
	private static final Transformation<Gedcom, Flef> INDIVIDUAL_TRANSFORMATION = new IndividualTransformation();
	private static final Transformation<Gedcom, Flef> FAMILY_TRANSFORMATION = new FamilyTransformation();
	private static final Transformation<Gedcom, Flef> DOCUMENT_TRANSFORMATION = new DocumentTransformation();
	private static final Transformation<Gedcom, Flef> NOTE_TRANSFORMATION = new NoteTransformation();
	private static final Transformation<Gedcom, Flef> REPOSITORY_TRANSFORMATION = new RepositoryTransformation();
	private static final Transformation<Gedcom, Flef> SOURCE_TRANSFORMATION = new SourceTransformation();
	private static final Transformation<Gedcom, Flef> SUBMITTER_TRANSFORMATION = new SubmitterTransformation();

	private static final Transformer TRANSFORMER = new Transformer(Protocol.FLEF);


	private GedcomNode header;
	private List<GedcomNode> individuals;
	private List<GedcomNode> families;
	private List<GedcomNode> places;
	private List<GedcomNode> notes;
	private List<GedcomNode> repositories;
	private List<GedcomNode> sources;
	private List<GedcomNode> culturalRules;
	private List<GedcomNode> groups;
	private List<GedcomNode> submitters;

	private Map<String, GedcomNode> individualIndex;
	private Map<String, GedcomNode> familyIndex;
	private Map<String, GedcomNode> placeIndex;
	private Map<String, GedcomNode> noteIndex;
	private Map<String, GedcomNode> repositoryIndex;
	private Map<String, GedcomNode> sourceIndex;
	private Map<String, GedcomNode> culturalRuleIndex;
	private Map<String, GedcomNode> groupIndex;
	private Map<String, GedcomNode> submitterIndex;

	private Map<GedcomNode, String> placeValue;
	private Map<GedcomNode, String> noteValue;
	private Map<GedcomNode, String> repositoryValue;
	private Map<GedcomNode, String> sourceValue;



	public static Protocol extractProtocol(final String gedcomFile) throws GedcomParseException{
		Protocol protocol = null;
		try(final BufferedReader br = GedcomHelper.getBufferedReader(new FileInputStream(new File(gedcomFile)))){
			int zeroLevelsFound = 0;
			String line;
			while(zeroLevelsFound < 2 && (line = br.readLine()) != null){
				//skip empty lines
				if(line.charAt(0) == ' ' || line.charAt(0) == '\t' || line.trim().isEmpty())
					continue;

				if(line.charAt(0) == '0')
					zeroLevelsFound ++;
				if(line.startsWith("1 PROTOCOL FLEF")){
					protocol = Protocol.FLEF;
					while((line = br.readLine()) != null && line.charAt(0) == '2')
						if(line.startsWith("2 VERSION ")){
							protocol.setVersion(line.substring("2 VERSION ".length()));
							break;
						}
					break;
				}
			}
		}
		catch(final IllegalArgumentException | IOException e){
			throw GedcomParseException.create((e.getMessage() == null? "GEDCOM file '{}' not found!": e.getMessage()), gedcomFile);
		}
		catch(final Exception e){
			throw GedcomParseException.create("Failed to read file", e);
		}
		return protocol;
	}


	@Override
	protected void create(final GedcomNode root) throws GedcomParseException{
		this.root = root;
		final List<GedcomNode> headers = root.getChildrenWithTag(TAG_HEADER);
		if(headers.size() != 1)
			throw GedcomParseException.create("Required header tag missing");
		header = headers.get(0);
		individuals = root.getChildrenWithTag(TAG_INDIVIDUAL);
		families = root.getChildrenWithTag(TAG_FAMILY);
		places = root.getChildrenWithTag(TAG_PLACE);
		notes = root.getChildrenWithTag(TAG_NOTE);
		repositories = root.getChildrenWithTag(TAG_REPOSITORY);
		sources = root.getChildrenWithTag(TAG_SOURCE);
		culturalRules = root.getChildrenWithTag(TAG_CULTURAL_RULE);
		groups = root.getChildrenWithTag(TAG_GROUP);
		submitters = root.getChildrenWithTag(TAG_SUBMITTER);

		individualIndex = generateIndexes(individuals);
		familyIndex = generateIndexes(families);
		placeIndex = generateIndexes(places);
		noteIndex = generateIndexes(notes);
		repositoryIndex = generateIndexes(repositories);
		sourceIndex = generateIndexes(sources);
		culturalRuleIndex = generateIndexes(culturalRules);
		groupIndex = generateIndexes(groups);
		submitterIndex = generateIndexes(submitters);

		placeValue = reverseMap(placeIndex);
		noteValue = reverseMap(noteIndex);
		repositoryValue = reverseMap(repositoryIndex);
		sourceValue = reverseMap(sourceIndex);
	}

	@Override
	public Gedcom transform(){
		final Gedcom destination = new Gedcom();
		HEADER_TRANSFORMATION.from(this, destination);
		SUBMITTER_TRANSFORMATION.from(this, destination);
		NOTE_TRANSFORMATION.from(this, destination);
		REPOSITORY_TRANSFORMATION.from(this, destination);
		SOURCE_TRANSFORMATION.from(this, destination);
		DOCUMENT_TRANSFORMATION.from(this, destination);
		INDIVIDUAL_TRANSFORMATION.from(this, destination);
		FAMILY_TRANSFORMATION.from(this, destination);
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
			root = GedcomNodeBuilder.createRoot(Protocol.FLEF)
				.addChild(header)
				.addChildren(individuals)
				.addChildren(families)
				.addChildren(places)
				.addChildren(notes)
				.addChildren(repositories)
				.addChildren(sources)
				.addChildren(culturalRules)
				.addChildren(groups)
				.addChildren(submitters)
				.addClosingChild("EOF");

		super.write(os);
	}

	public GedcomNode createEmptyNode(){
		return TRANSFORMER.createEmpty();
	}

	public GedcomNode create(final String tag){
		return TRANSFORMER.create(tag);
	}

	/**
	 * @param origin	Origin node from which to start the traversal.
	 * @param path	The path to follow from the origin in the form `tag#id{value}[index]` or `(tag1|tag2)#id{value}[index]` and separated by dots.
	 * @return	The final node.
	 */
	public GedcomNode traverse(final GedcomNode origin, final String path){
		return TRANSFORMER.traverse(origin, path);
	}

	/**
	 * @param origin	Origin node from which to start the traversal.
	 * @param path	The path to follow from the origin in the form `tag#id{value}[]` or `(tag1|tag2)#id{value}[]` and separated by dots.
	 * 	<p>The void array MUST BE last in the sequence.</p>
	 * @return	The final node list.
	 */
	public List<GedcomNode> traverseAsList(final GedcomNode origin, String path){
		return TRANSFORMER.traverseAsList(origin, path);
	}

	public GedcomNode getHeader(){
		return header;
	}

	public void setHeader(final GedcomNode header){
		this.header = header;
	}

	public boolean hasIndividuals(){
		return (individuals != null && !individuals.isEmpty());
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
			individualIndex = new HashMap<>(1);
		}

		String individualID = individual.getID();
		if(individualID == null){
			individualID = getNextIndividualID();
			individual.withID(individualID);
		}

		individuals.add(individual);
		individualIndex.put(individual.getID(), individual);
		return individualID;
	}

	private String getNextIndividualID(){
		return ID_INDIVIDUAL_PREFIX + (individuals != null? individuals.size() + 1: 1);
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
			familyIndex = new HashMap<>(1);
		}

		String familyID = family.getID();
		if(familyID == null){
			familyID = getNextFamilyID();
			family.withID(familyID);
		}

		families.add(family);
		familyIndex.put(family.getID(), family);
		return familyID;
	}

	public void linkFamilyToChild(final GedcomNode child, final GedcomNode family){
		child.addChild(TRANSFORMER.createWithReference("FAMILY_CHILD", family.getID()));
	}

	private String getNextFamilyID(){
		return ID_FAMILY_PREFIX + (families != null? families.size() + 1: 1);
	}

	public List<GedcomNode> getParent1s(final GedcomNode child){
		final List<GedcomNode> familyChilds = traverseAsList(child, "FAMILY_CHILD[]");
		final List<GedcomNode> parent1s = new ArrayList<>(familyChilds.size());
		for(final GedcomNode familyChild : familyChilds){
			final GedcomNode family = getFamily(familyChild.getXRef());
			parent1s.add(getIndividual(TRANSFORMER.traverse(family, "SPOUSE1").getXRef()));
		}
		return parent1s;
	}

	public List<GedcomNode> getParent2s(final GedcomNode child){
		final List<GedcomNode> familyChilds = traverseAsList(child, "FAMILY_CHILD[]");
		final List<GedcomNode> parent2s = new ArrayList<>(familyChilds.size());
		for(final GedcomNode familyChild : familyChilds){
			final GedcomNode family = getFamily(familyChild.getXRef());
			parent2s.add(getIndividual(TRANSFORMER.traverse(family, "SPOUSE2").getXRef()));
		}
		return parent2s;
	}

	public GedcomNode getSpouse1(final GedcomNode family){
		return getSpouse(family, "SPOUSE1");
	}

	public GedcomNode getSpouse2(final GedcomNode family){
		return getSpouse(family, "SPOUSE2");
	}

	public GedcomNode getSpouse(final GedcomNode family, final String spouseTag){
		return getIndividual(traverse(family, spouseTag).getXRef());
	}

	public List<GedcomNode> getPlaces(){
		return (places != null? places: Collections.emptyList());
	}

	public GedcomNode getPlace(final String id){
		return placeIndex.get(id);
	}

	public String addPlace(final GedcomNode place){
		//search place
		final GedcomNode placeCloned = GedcomNodeBuilder.createCloneWithoutID(Protocol.FLEF, place);
		String placeID = (placeValue != null? placeValue.get(placeCloned): null);
		if(placeID == null){
			//if place is not found:
			if(places == null){
				places = new ArrayList<>(1);
				placeIndex = new HashMap<>(1);
				placeValue = new HashMap<>(1);
			}

			placeID = place.getID();
			if(placeID == null){
				placeID = getNextPlaceID();
				place.withID(placeID);
			}

			places.add(place);
			placeIndex.put(placeID, place);
			placeValue.put(placeCloned, placeID);
		}
		return placeID;
	}

	private String getNextPlaceID(){
		return ID_PLACE_PREFIX + (places != null? places.size() + 1: 1);
	}

	public List<GedcomNode> getNotes(){
		return notes;
	}

	public GedcomNode getNote(final String id){
		return noteIndex.get(id);
	}

	public String addNote(final GedcomNode note){
		//search note
		final GedcomNode noteCloned = GedcomNodeBuilder.createCloneWithoutID(Protocol.FLEF, note);
		String noteID = (noteValue != null? noteValue.get(noteCloned): null);
		if(noteID == null){
			//if note is not found:
			if(notes == null){
				notes = new ArrayList<>(1);
				noteIndex = new HashMap<>(1);
				noteValue = new HashMap<>(1);
			}

			noteID = note.getID();
			if(noteID == null){
				noteID = getNextNoteID();
				note.withID(noteID);
			}

			notes.add(note);
			noteIndex.put(noteID, note);
			noteValue.put(noteCloned, noteID);
		}
		return noteID;
	}

	private String getNextNoteID(){
		return ID_NOTE_PREFIX + (notes != null? notes.size() + 1: 1);
	}

	public List<GedcomNode> getRepositories(){
		return repositories;
	}

	public GedcomNode getRepository(final String id){
		return repositoryIndex.get(id);
	}

	public String addRepository(final GedcomNode repository){
		//search repository
		final GedcomNode repositoryCloned = GedcomNodeBuilder.createCloneWithoutID(Protocol.FLEF, repository);
		String repositoryID = (repositoryValue != null? repositoryValue.get(repositoryCloned): null);
		if(repositoryID == null){
			//if repository is not found:
			if(repositories == null){
				repositories = new ArrayList<>(1);
				repositoryIndex = new HashMap<>(1);
				repositoryValue = new HashMap<>(1);
			}

			repositoryID = repository.getID();
			if(repositoryID == null){
				repositoryID = getNextRepositoryID();
				repository.withID(repositoryID);
			}

			repositories.add(repository);
			repositoryIndex.put(repositoryID, repository);
			repositoryValue.put(repositoryCloned, repositoryID);
		}
		return repositoryID;
	}

	private String getNextRepositoryID(){
		return ID_REPOSITORY_PREFIX + (repositories != null? repositories.size() + 1: 1);
	}

	public List<GedcomNode> getSources(){
		return sources;
	}

	public GedcomNode getSource(final String id){
		return sourceIndex.get(id);
	}

	public String addSource(final GedcomNode source){
		//search source
		final GedcomNode sourceCloned = GedcomNodeBuilder.createCloneWithoutID(Protocol.FLEF, source);
		String sourceID = (sourceValue != null? sourceValue.get(sourceCloned): null);
		if(sourceID == null){
			//if source is not found:
			if(sources == null){
				sources = new ArrayList<>(1);
				sourceIndex = new HashMap<>(1);
				sourceValue = new HashMap<>(1);
			}

			sourceID = source.getID();
			if(sourceID == null){
				sourceID = getNextSourceID();
				source.withID(sourceID);
			}

			sources.add(source);
			sourceIndex.put(sourceID, source);
			sourceValue.put(sourceCloned, sourceID);
		}
		return sourceID;
	}

	private String getNextSourceID(){
		return ID_SOURCE_PREFIX + (sources != null? sources.size() + 1: 1);
	}

	public List<GedcomNode> getCulturalRules(){
		return culturalRules;
	}

	public GedcomNode getCulturalRule(final String id){
		return culturalRuleIndex.get(id);
	}

	public String addCulturalRule(final GedcomNode culturalRule){
		if(culturalRules == null){
			culturalRules = new ArrayList<>(1);
			culturalRuleIndex = new HashMap<>(1);
		}

		String culturalRuleID = culturalRule.getID();
		if(culturalRuleID == null){
			culturalRuleID = getNextCulturalRuleID();
			culturalRule.withID(culturalRuleID);
		}

		culturalRules.add(culturalRule);
		culturalRuleIndex.put(culturalRule.getID(), culturalRule);
		return culturalRuleID;
	}

	private String getNextCulturalRuleID(){
		return ID_CULTURAL_RULE_PREFIX + (culturalRules != null? culturalRules.size() + 1: 1);
	}

	public List<GedcomNode> getGroups(){
		return groups;
	}

	public GedcomNode getGroup(final String id){
		return groupIndex.get(id);
	}

	public String addGroup(final GedcomNode group){
		if(groups == null){
			groups = new ArrayList<>(1);
			groupIndex = new HashMap<>(1);
		}

		String groupID = group.getID();
		if(groupID == null){
			groupID = getNextGroupID();
			group.withID(groupID);
		}

		groups.add(group);
		groupIndex.put(group.getID(), group);
		return groupID;
	}

	private String getNextGroupID(){
		return ID_GROUP_PREFIX + (groups != null? groups.size() + 1: 1);
	}

	public List<GedcomNode> getSubmitters(){
		return submitters;
	}

	public GedcomNode getSubmitter(final String id){
		return submitterIndex.get(id);
	}

	public String addSubmitter(final GedcomNode submitter){
		if(submitters == null){
			submitters = new ArrayList<>(1);
			submitterIndex = new HashMap<>(1);
		}

		String submitterID = submitter.getID();
		if(submitterID == null){
			submitterID = getNextSubmitterID();
			submitter.withID(submitterID);
		}

		submitters.add(submitter);
		submitterIndex.put(submitter.getID(), submitter);
		return submitterID;
	}

	private String getNextSubmitterID(){
		return ID_SUBMITTER_PREFIX + (submitters != null? submitters.size() + 1: 1);
	}

}
