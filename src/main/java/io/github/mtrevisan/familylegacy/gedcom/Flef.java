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
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class Flef extends Store{

	/** Raised upon changes on the number of individuals in the store. */
	public static final Integer ACTION_COMMAND_INDIVIDUAL_COUNT = 0;
	/** Raised upon changes on the number of families in the store. */
	public static final Integer ACTION_COMMAND_FAMILY_COUNT = 1;
	/** Raised upon changes on the number of groups in the store. */
	public static final Integer ACTION_COMMAND_GROUP_COUNT = 2;
	/** Raised upon changes on the number of events in the store. */
	public static final Integer ACTION_COMMAND_EVENT_COUNT = 3;
	/** Raised upon changes on the number of places in the store. */
	public static final Integer ACTION_COMMAND_PLACE_COUNT = 4;
	/** Raised upon changes on the number of notes in the store. */
	public static final Integer ACTION_COMMAND_NOTE_COUNT = 5;
	/** Raised upon changes on the number of repositories in the store. */
	public static final Integer ACTION_COMMAND_REPOSITORY_COUNT = 6;
	/** Raised upon changes on the number of cultural rules in the store. */
	public static final Integer ACTION_COMMAND_CULTURAL_RULE_COUNT = 7;
	/** Raised upon changes on the number of sources in the store. */
	public static final Integer ACTION_COMMAND_SOURCE_COUNT = 8;
	/** Raised upon changes on the number of calendar in the store. */
	public static final Integer ACTION_COMMAND_CALENDAR_COUNT = 9;

	private static final String ID_INDIVIDUAL_PREFIX = "I";
	private static final String ID_FAMILY_PREFIX = "F";
	private static final String ID_GROUP_PREFIX = "G";
	private static final String ID_EVENT_PREFIX = "E";
	private static final String ID_PLACE_PREFIX = "P";
	private static final String ID_NOTE_PREFIX = "N";
	private static final String ID_REPOSITORY_PREFIX = "R";
	private static final String ID_CULTURAL_RULE_PREFIX = "C";
	private static final String ID_SOURCE_PREFIX = "S";
	private static final String ID_CALENDAR_PREFIX = "K";

	private static final String TAG_HEADER = "HEADER";
	private static final String TAG_INDIVIDUAL = "INDIVIDUAL";
	private static final String TAG_FAMILY = "FAMILY";
	private static final String TAG_GROUP = "GROUP";
	private static final String TAG_EVENT = "EVENT";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_REPOSITORY = "REPOSITORY";
	private static final String TAG_CULTURAL_RULE = "CULTURAL_RULE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_CALENDAR = "CALENDAR";

	private static final Transformation<Gedcom, Flef> HEADER_TRANSFORMATION = new HeaderTransformation();
	private static final Transformation<Gedcom, Flef> INDIVIDUAL_TRANSFORMATION = new IndividualTransformation();
	private static final Transformation<Gedcom, Flef> FAMILY_TRANSFORMATION = new FamilyTransformation();
	private static final Transformation<Gedcom, Flef> NOTE_TRANSFORMATION = new NoteTransformation();
	private static final Transformation<Gedcom, Flef> REPOSITORY_TRANSFORMATION = new RepositoryTransformation();
	private static final Transformation<Gedcom, Flef> SOURCE_TRANSFORMATION = new SourceTransformation();
	private static final Transformation<Gedcom, Flef> SUBMITTER_TRANSFORMATION = new SubmitterTransformation();

	private static final Transformer TRANSFORMER = new Transformer(Protocol.FLEF);


	private GedcomNode header;
	private List<GedcomNode> individuals;
	private List<GedcomNode> families;
	private List<GedcomNode> groups;
	private List<GedcomNode> events;
	private List<GedcomNode> places;
	private List<GedcomNode> notes;
	private List<GedcomNode> repositories;
	private List<GedcomNode> culturalRules;
	private List<GedcomNode> sources;
	private List<GedcomNode> calendars;

	private TreeMap<String, GedcomNode> individualIndex;
	private TreeMap<String, GedcomNode> familyIndex;
	private TreeMap<String, GedcomNode> groupIndex;
	private TreeMap<String, GedcomNode> eventIndex;
	private TreeMap<String, GedcomNode> placeIndex;
	private TreeMap<String, GedcomNode> noteIndex;
	private TreeMap<String, GedcomNode> repositoryIndex;
	private TreeMap<String, GedcomNode> culturalRuleIndex;
	private TreeMap<String, GedcomNode> sourceIndex;
	private TreeMap<String, GedcomNode> calendarIndex;

	private Map<Integer, String> eventValue;
	private Map<Integer, String> placeValue;
	private Map<Integer, String> noteValue;
	private Map<Integer, String> repositoryValue;
	private Map<Integer, String> sourceValue;
	private Map<Integer, String> calendarValue;

	private int individualId = 1;
	private int familyId = 1;
	private int groupId = 1;
	private int eventId = 1;
	private int placeId = 1;
	private int noteId = 1;
	private int repositoryId = 1;
	private int culturalRuleId = 1;
	private int sourceId = 1;
	private int calendarId = 1;



	static Protocol extractProtocol(final String gedcomFile) throws GedcomParseException{
		Protocol protocol = null;
		try(final BufferedReader br = GedcomHelper.getBufferedReader(new FileInputStream(gedcomFile))){
			int zeroLevelsFound = 0;
			String line;
			while(zeroLevelsFound < 2 && (line = br.readLine()) != null){
				//skip empty lines
				if(Character.isWhitespace(line.charAt(0)) || StringUtils.isBlank(line))
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
		groups = root.getChildrenWithTag(TAG_GROUP);
		events = root.getChildrenWithTag(TAG_EVENT);
		places = root.getChildrenWithTag(TAG_PLACE);
		notes = root.getChildrenWithTag(TAG_NOTE);
		repositories = root.getChildrenWithTag(TAG_REPOSITORY);
		culturalRules = root.getChildrenWithTag(TAG_CULTURAL_RULE);
		sources = root.getChildrenWithTag(TAG_SOURCE);
		calendars = root.getChildrenWithTag(TAG_CALENDAR);

		individualIndex = generateIndexes(individuals);
		familyIndex = generateIndexes(families);
		groupIndex = generateIndexes(groups);
		eventIndex = generateIndexes(events);
		placeIndex = generateIndexes(places);
		noteIndex = generateIndexes(notes);
		repositoryIndex = generateIndexes(repositories);
		culturalRuleIndex = generateIndexes(culturalRules);
		sourceIndex = generateIndexes(sources);
		calendarIndex = generateIndexes(calendars);

		eventValue = reverseMap(eventIndex);
		placeValue = reverseMap(placeIndex);
		noteValue = reverseMap(noteIndex);
		repositoryValue = reverseMap(repositoryIndex);
		sourceValue = reverseMap(sourceIndex);
		calendarValue = reverseMap(calendarIndex);

		if(!individualIndex.isEmpty())
			individualId = extractLastID(individualIndex.lastKey()) + 1;
		if(!familyIndex.isEmpty())
			familyId = extractLastID(familyIndex.lastKey()) + 1;
		if(!groupIndex.isEmpty())
			groupId = extractLastID(groupIndex.lastKey()) + 1;
		if(!eventIndex.isEmpty())
			eventId = extractLastID(eventIndex.lastKey()) + 1;
		if(!placeIndex.isEmpty())
			placeId = extractLastID(placeIndex.lastKey()) + 1;
		if(!noteIndex.isEmpty())
			noteId = extractLastID(noteIndex.lastKey()) + 1;
		if(!repositoryIndex.isEmpty())
			repositoryId = extractLastID(repositoryIndex.lastKey()) + 1;
		if(!culturalRuleIndex.isEmpty())
			culturalRuleId = extractLastID(culturalRuleIndex.lastKey()) + 1;
		if(!sourceIndex.isEmpty())
			sourceId = extractLastID(sourceIndex.lastKey()) + 1;
		if(!calendarIndex.isEmpty())
			calendarId = extractLastID(calendarIndex.lastKey()) + 1;
	}

	@Override
	public Gedcom transform(){
		final Gedcom destination = new Gedcom();
		HEADER_TRANSFORMATION.from(this, destination);
		SUBMITTER_TRANSFORMATION.from(this, destination);
		NOTE_TRANSFORMATION.from(this, destination);
		REPOSITORY_TRANSFORMATION.from(this, destination);
		SOURCE_TRANSFORMATION.from(this, destination);
		INDIVIDUAL_TRANSFORMATION.from(this, destination);
		FAMILY_TRANSFORMATION.from(this, destination);
		return destination;
	}

	@Override
	protected String getCharsetName(){
		return StandardCharsets.UTF_8.name();
	}

	@Override
	public void write(final OutputStream os) throws IOException{
		if(root == null)
			root = GedcomNodeBuilder.createRoot(Protocol.FLEF)
				.addChild(header)
				.addChildren(individuals)
				.addChildren(families)
				.addChildren(groups)
				.addChildren(events)
				.addChildren(places)
				.addChildren(notes)
				.addChildren(repositories)
				.addChildren(culturalRules)
				.addChildren(sources)
				.addChildren(calendars)
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
	public List<GedcomNode> traverseAsList(final GedcomNode origin, final String path){
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

	public void addIndividual(final GedcomNode individual){
		if(individuals == null){
			individuals = new ArrayList<>(1);
			individualIndex = new TreeMap<>();
		}

		String individualID = individual.getID();
		if(individualID == null){
			individualID = getNextIndividualID();
			individual.withID(individualID);
		}

		individuals.add(individual);
		individualIndex.put(individual.getID(), individual);

		EventBusService.publish(ACTION_COMMAND_INDIVIDUAL_COUNT);
	}

	public String removeIndividual(final GedcomNode individual){
		if(individuals != null){
			final String individualID = individual.getID();
			individuals.remove(individual);
			individualIndex.remove(individualID);

			EventBusService.publish(ACTION_COMMAND_INDIVIDUAL_COUNT);

			return individualID;
		}
		return null;
	}

	private String getNextIndividualID(){
		return ID_INDIVIDUAL_PREFIX + (individualId ++);
	}


	public boolean hasFamilies(){
		return (families != null && !families.isEmpty());
	}

	public List<GedcomNode> getFamilies(){
		return families;
	}

	public GedcomNode getFamily(final String id){
		return familyIndex.get(id);
	}

	public void addFamily(final GedcomNode family){
		if(families == null){
			families = new ArrayList<>(1);
			familyIndex = new TreeMap<>();
		}

		String familyID = family.getID();
		if(familyID == null){
			familyID = getNextFamilyID();
			family.withID(familyID);
		}

		families.add(family);
		familyIndex.put(family.getID(), family);

		EventBusService.publish(ACTION_COMMAND_FAMILY_COUNT);
	}

	public void linkFamilyToChild(final GedcomNode child, final GedcomNode family){
		child.addChild(TRANSFORMER.createWithReference("FAMILY", family.getID()));
	}

	public String removeFamily(final GedcomNode family){
		if(families != null){
			final String familyID = family.getID();
			families.remove(family);
			familyIndex.remove(familyID);

			EventBusService.publish(ACTION_COMMAND_FAMILY_COUNT);

			return familyID;
		}
		return null;
	}

	private String getNextFamilyID(){
		return ID_FAMILY_PREFIX + (familyId ++);
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
		return getSpouse(family, 0);
	}

	public GedcomNode getSpouse2(final GedcomNode family){
		return getSpouse(family, 1);
	}

	public GedcomNode getSpouse(final GedcomNode family, final int spouseIndex){
		final List<GedcomNode> individuals = traverseAsList(family, "INDIVIDUAL[]");
		GedcomNode individual = TRANSFORMER.createEmpty();
		if(spouseIndex < individuals.size()){
			final String individualXRef = individuals.get(spouseIndex).getXRef();
			individual = getIndividual(individualXRef);
		}
		return individual;
	}


	public List<GedcomNode> getGroups(){
		return groups;
	}

	public GedcomNode getGroup(final String id){
		return groupIndex.get(id);
	}

	public void addGroup(final GedcomNode group){
		if(groups == null){
			groups = new ArrayList<>(1);
			groupIndex = new TreeMap<>();
		}

		String groupID = group.getID();
		if(groupID == null){
			groupID = getNextGroupID();
			group.withID(groupID);
		}

		groups.add(group);
		groupIndex.put(group.getID(), group);

		EventBusService.publish(ACTION_COMMAND_GROUP_COUNT);
	}

	public String removeGroup(final GedcomNode group){
		if(groups != null){
			final String groupID = group.getID();
			groups.remove(group);
			groupIndex.remove(groupID);

			EventBusService.publish(ACTION_COMMAND_GROUP_COUNT);

			return groupID;
		}
		return null;
	}

	private String getNextGroupID(){
		return ID_GROUP_PREFIX + (groupId ++);
	}


	public boolean hasEvents(){
		return (events != null && !events.isEmpty());
	}

	public List<GedcomNode> getEvents(){
		return events;
	}

	public GedcomNode getEvent(final String id){
		return eventIndex.get(id);
	}

	public void addEvent(final GedcomNode event){
		//search event
		String eventID = (!event.isEmpty() && eventValue != null? eventValue.get(event.hashCode()): null);
		if(eventID == null){
			//if event is not found:
			if(events == null){
				events = new ArrayList<>(1);
				eventIndex = new TreeMap<>();
				eventValue = new HashMap<>(1);
			}

			eventID = getNextEventID();
			event.withID(eventID);

			events.add(event);
			eventIndex.put(event.getID(), event);
			eventValue.put(event.hashCode(), eventID);

			EventBusService.publish(ACTION_COMMAND_EVENT_COUNT);
		}
		else
			event.withID(eventID);
	}

	public String removeEvent(final GedcomNode event){
		if(events != null){
			final String eventID = event.getID();
			events.remove(event);
			eventIndex.remove(eventID);

			EventBusService.publish(ACTION_COMMAND_EVENT_COUNT);

			return eventID;
		}
		return null;
	}

	private String getNextEventID(){
		return ID_EVENT_PREFIX + (eventId ++);
	}


	public List<GedcomNode> getPlaces(){
		return (places != null? places: Collections.emptyList());
	}

	public GedcomNode getPlace(final String id){
		return placeIndex.get(id);
	}

	public void addPlace(final GedcomNode place){
		//search place
		String placeID = (!place.isEmpty() && placeValue != null? placeValue.get(place.hashCode()): null);
		if(placeID == null){
			//if place is not found:
			if(places == null){
				places = new ArrayList<>(1);
				placeIndex = new TreeMap<>();
				placeValue = new HashMap<>(1);
			}

			placeID = getNextPlaceID();
			place.withID(placeID);

			places.add(place);
			placeIndex.put(placeID, place);
			placeValue.put(place.hashCode(), placeID);

			EventBusService.publish(ACTION_COMMAND_PLACE_COUNT);
		}
		else
			place.withID(placeID);
	}

	public String removePlace(final GedcomNode place){
		if(places != null){
			final String placeID = place.getID();
			places.remove(place);
			placeIndex.remove(placeID);

			EventBusService.publish(ACTION_COMMAND_PLACE_COUNT);

			return placeID;
		}
		return null;
	}

	private String getNextPlaceID(){
		return ID_PLACE_PREFIX + (placeId ++);
	}


	public List<GedcomNode> getNotes(){
		return notes;
	}

	public GedcomNode getNote(final String id){
		return noteIndex.get(id);
	}

	public String addNote(final GedcomNode note){
		//search note
		String noteID = (!note.isEmpty() && noteValue != null? noteValue.get(note.hashCode()): null);
		if(noteID == null){
			//if note is not found:
			if(notes == null){
				notes = new ArrayList<>(1);
				noteIndex = new TreeMap<>();
				noteValue = new HashMap<>(1);
			}

			//FIXME what happen to all the references when T1 becomes N2?
			noteID = getNextNoteID();
			note.withID(noteID);

			notes.add(note);
			noteIndex.put(noteID, note);
			noteValue.put(note.hashCode(), noteID);

			EventBusService.publish(ACTION_COMMAND_NOTE_COUNT);
		}
		else
			note.withID(noteID);
		return noteID;
	}

	public String removeNote(final GedcomNode note){
		if(notes != null){
			final String noteID = note.getID();
			notes.remove(note);
			noteIndex.remove(noteID);

			EventBusService.publish(ACTION_COMMAND_NOTE_COUNT);

			return noteID;
		}
		return null;
	}

	private String getNextNoteID(){
		return ID_NOTE_PREFIX + (noteId ++);
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

			EventBusService.publish(ACTION_COMMAND_REPOSITORY_COUNT);
		}
		else
			repository.withID(repositoryID);
		return repositoryID;
	}

	public String removeRepository(final GedcomNode repository){
		if(repositories != null){
			final String repositoryID = repository.getID();
			repositories.remove(repository);
			repositoryIndex.remove(repositoryID);

			EventBusService.publish(ACTION_COMMAND_REPOSITORY_COUNT);

			return repositoryID;
		}
		return null;
	}

	private String getNextRepositoryID(){
		return ID_REPOSITORY_PREFIX + (repositoryId ++);
	}


	public List<GedcomNode> getCulturalRules(){
		return culturalRules;
	}

	public GedcomNode getCulturalRule(final String id){
		return culturalRuleIndex.get(id);
	}

	public void addCulturalRule(final GedcomNode culturalRule){
		if(culturalRules == null){
			culturalRules = new ArrayList<>(1);
			culturalRuleIndex = new TreeMap<>();
		}

		String culturalRuleID = culturalRule.getID();
		if(culturalRuleID == null){
			culturalRuleID = getNextCulturalRuleID();
			culturalRule.withID(culturalRuleID);
		}

		culturalRules.add(culturalRule);
		culturalRuleIndex.put(culturalRule.getID(), culturalRule);

		EventBusService.publish(ACTION_COMMAND_CULTURAL_RULE_COUNT);
	}

	public String removeCulturalRule(final GedcomNode culturalRule){
		if(culturalRules != null){
			final String culturalRuleID = culturalRule.getID();
			culturalRules.remove(culturalRule);
			culturalRuleIndex.remove(culturalRuleID);

			EventBusService.publish(ACTION_COMMAND_CULTURAL_RULE_COUNT);

			return culturalRuleID;
		}
		return null;
	}

	private String getNextCulturalRuleID(){
		return ID_CULTURAL_RULE_PREFIX + (culturalRuleId ++);
	}


	public List<GedcomNode> getSources(){
		return sources;
	}

	public GedcomNode getSource(final String id){
		return sourceIndex.get(id);
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

			EventBusService.publish(ACTION_COMMAND_SOURCE_COUNT);
		}
		else
			source.withID(sourceID);
		return sourceID;
	}

	public String removeSource(final GedcomNode source){
		if(sources != null){
			final String sourceID = source.getID();
			sources.remove(source);
			sourceIndex.remove(sourceID);

			EventBusService.publish(ACTION_COMMAND_SOURCE_COUNT);

			return sourceID;
		}
		return null;
	}

	private String getNextSourceID(){
		return ID_SOURCE_PREFIX + (sourceId ++);
	}


	public List<GedcomNode> getCalendars(){
		return calendars;
	}

	public GedcomNode getCalendar(final String id){
		return calendarIndex.get(id);
	}

	public GedcomNode getCalendarByType(final String type){
		if(calendars != null)
			for(final GedcomNode calendar : calendars)
				if(type.equals(traverse(calendar, "TYPE").getValue()))
					return calendar;
		return createEmptyNode();
	}

	public String addCalendar(final GedcomNode calendar){
		//search calendar
		String calendarID = (!calendar.isEmpty() && calendarValue != null? calendarValue.get(calendar.hashCode()): null);
		if(calendarID == null){
			//if calendar is not found:
			if(calendars == null){
				calendars = new ArrayList<>(1);
				calendarIndex = new TreeMap<>();
				calendarValue = new HashMap<>(1);
			}

			calendarID = getNextCalendarID();
			calendar.withID(calendarID);

			calendars.add(calendar);
			calendarIndex.put(calendarID, calendar);
			calendarValue.put(calendar.hashCode(), calendarID);

			EventBusService.publish(ACTION_COMMAND_CALENDAR_COUNT);
		}
		else
			calendar.withID(calendarID);
		return calendarID;
	}

	public String removeCalendar(final GedcomNode calendar){
		if(calendars != null){
			final String calendarID = calendar.getID();
			calendars.remove(calendar);
			calendarIndex.remove(calendarID);

			EventBusService.publish(ACTION_COMMAND_CALENDAR_COUNT);

			return calendarID;
		}
		return null;
	}

	private String getNextCalendarID(){
		return ID_CALENDAR_PREFIX + (calendarId ++);
	}

}
