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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Flef extends Store<Flef>{

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
	private static final String TAG_SUBMITTER = "SUBMITTER";
	private static final String TAG_CHARSET = "CHARSET";


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


	public static void main(final String[] args){
		try{
			final Flef store = new Flef();
			final Flef flef = store.load("/gedg/flef_0.0.1.gedg", "/ged/small.flef.ged");

			flef.transform();

			final OutputStream os = new FileOutputStream(new File("./tmp.ged"));
			flef.write(os);
		}
		catch(final Exception e){
			e.printStackTrace();
		}
	}

	@Override
	protected Flef create(final GedcomNode root) throws GedcomParseException{
		final Flef g = new Flef();
		g.root = root;
		final List<GedcomNode> headers = root.getChildrenWithTag(TAG_HEADER);
		if(headers.size() != 1)
			throw GedcomParseException.create("Required header tag missing");
		g.header = headers.get(0);
		g.individuals = root.getChildrenWithTag(TAG_INDIVIDUAL);
		g.families = root.getChildrenWithTag(TAG_FAMILY);
		g.places = root.getChildrenWithTag(TAG_PLACE);
		g.notes = root.getChildrenWithTag(TAG_NOTE);
		g.repositories = root.getChildrenWithTag(TAG_REPOSITORY);
		g.sources = root.getChildrenWithTag(TAG_SOURCE);
		g.submitters = root.getChildrenWithTag(TAG_SUBMITTER);

		g.individualIndex = generateIndexes(g.individuals);
		g.familyIndex = generateIndexes(g.families);
		g.placeIndex = generateIndexes(g.places);
		g.noteIndex = generateIndexes(g.notes);
		g.repositoryIndex = generateIndexes(g.repositories);
		g.sourceIndex = generateIndexes(g.sources);
		g.culturalRuleIndex = generateIndexes(g.culturalRules);
		g.submitterIndex = generateIndexes(g.submitters);

		return g;
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

	public String getNextIndividualID(){
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

	public String getNextFamilyID(){
		return ID_FAMILY_PREFIX + (families != null? families.size() + 1: 1);
	}

	public List<GedcomNode> getPlaces(){
		return (places != null? places: Collections.emptyList());
	}

	public GedcomNode getPlace(final String id){
		return placeIndex.get(id);
	}

	public String addPlace(final GedcomNode place){
		if(places == null){
			places = new ArrayList<>(1);
			placeIndex = new HashMap<>(1);
		}
		String placeID = place.getID();
		if(placeID == null){
			placeID = getNextPlaceID();
			place.withID(placeID);
		}
		places.add(place);
		placeIndex.put(place.getID(), place);
		return placeID;
	}

	public String getNextPlaceID(){
		return ID_PLACE_PREFIX + (places != null? places.size() + 1: 1);
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
			noteIndex = new HashMap<>(1);
		}
		String noteID = note.getID();
		if(noteID == null){
			noteID = getNextNoteID();
			note.withID(noteID);
		}
		notes.add(note);
		noteIndex.put(note.getID(), note);
		return noteID;
	}

	public String getNextNoteID(){
		return ID_NOTE_PREFIX + (notes != null? notes.size() + 1: 1);
	}

	public List<GedcomNode> getRepositories(){
		return repositories;
	}

	public GedcomNode getRepository(final String id){
		return repositoryIndex.get(id);
	}

	public String addRepository(final GedcomNode repository){
		if(repositories == null){
			repositories = new ArrayList<>(1);
			repositoryIndex = new HashMap<>(1);
		}
		String repositoryID = repository.getID();
		if(repositoryID == null){
			repositoryID = getNextRepositoryID();
			repository.withID(repositoryID);
		}
		repositories.add(repository);
		repositoryIndex.put(repository.getID(), repository);
		return repositoryID;
	}

	public String getNextRepositoryID(){
		return ID_REPOSITORY_PREFIX + (repositories != null? repositories.size() + 1: 1);
	}

	public List<GedcomNode> getSources(){
		return sources;
	}

	public GedcomNode getSource(final String id){
		return sourceIndex.get(id);
	}

	public String addSource(final GedcomNode source){
		if(sources == null){
			sources = new ArrayList<>(1);
			sourceIndex = new HashMap<>(1);
		}
		String sourceID = source.getID();
		if(sourceID == null){
			sourceID = getNextSourceID();
			source.withID(sourceID);
		}
		sources.add(source);
		sourceIndex.put(source.getID(), source);
		return sourceID;
	}

	public String getNextSourceID(){
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

	public String getNextCulturalRuleID(){
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

	public String getNextGroupID(){
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

	public String getNextSubmitterID(){
		return ID_SUBMITTER_PREFIX + (submitters != null? submitters.size() + 1: 1);
	}

}
