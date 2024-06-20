/**
 * Copyright (c) 2021 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef;

import io.github.mtrevisan.familylegacy.flef.gedcom.GedcomDataException;
import io.github.mtrevisan.familylegacy.flef.gedcom.GedcomGrammarException;
import io.github.mtrevisan.familylegacy.flef.sql.SQLDataException;
import io.github.mtrevisan.familylegacy.flef.sql.SQLGrammarException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;


public class Main{

	private static final String JDBC_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
	private static final String USER = "sa";
	private static final String PASSWORD = "";

	private static final List<String> REMOVAL_TAGS = Arrays.asList("SUBN", ".CHAN ", ".CHAN.DATE ", ".CHAN.DATE.TIME ", ".ADDR.", ".CONC ",
		".FORM ", "._PUBL ", "._CUT Y", ".NAME //");

	private static final List<String> SUFFIXES = Arrays.asList(".NAME ", ".EDUC ", ".EVEN ", ".OCCU ", ".SEX ", ".PLAC ", ".NOTE ", ".SSN ",
		".DSCR ", ".RELI ", ".PROP ", ".NCHI ", ".TEXT ");

	private static final Set<String> SUFFIXES_Y = new HashSet<>(Arrays.asList("BIRT Y", "DEAT Y", "CREM Y", "BURI Y", "RETI Y", "EMIG Y",
		"IMMI Y", "ADOP Y", "NATU Y", "CENS Y", "GRAD Y", "ORDN Y", "WILL Y", "ENGA Y", "MARR Y", "MARL Y", "MARB Y", "MARS Y", "DIV Y"));

	private static final String[] IDENTIFIERS = {"SEX", "MARR", "MARL", "MARB", "MARS", "BIRT", "DEAT", "CREM", "BURI", "RETI", "RESI",
		"RELI", "ENGA", "DSCR", "SSN", "EMIG", "IMMI", "ADOP", "PROP", "NATU", "CENS", "GRAD", "ORDN", "NCHI", "WILL", "DIV"};

	private static class GedcomObject{
		String type;
		int id;
		final Map<String, GedcomObject> children = new HashMap<>();
		final Map<String, String> attributes = new HashMap<>();
	}


	public static void main(final String[] args) throws SQLGrammarException, SQLDataException, GedcomGrammarException, GedcomDataException,
			SQLException, IOException{
//		final GedcomFileParser gedcomParser = new GedcomFileParser();
//		gedcomParser.load("/gedg/gedcom_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged");

//		final SQLFileParser sqlParser = new SQLFileParser();
//		final String grammarFile = "src/main/resources/gedg/treebard/FLeF.sql";
//		sqlParser.load(grammarFile, "src/main/resources/gedg/treebard/FLeF.data");

//		final DatabaseManager dbManager = new DatabaseManager(JDBC_URL, USER, PASSWORD);
//		dbManager.initialize(grammarFile);

//		final String baseDirectory = "C:\\Users\\mauro\\Projects\\FamilyLegacy\\src\\main\\resources\\ged\\";
		final String baseDirectory = "C:\\mauro\\mine\\projects\\FamilyLegacy\\src\\main\\resources\\ged\\";
		final String gedcomFilename = "TGMZ.ged";
		String flatGedcomFilename = "TGMZ.txt";
		flatGedcomFilename = null;
		final List<String> lines = flatGedcom(baseDirectory, gedcomFilename, flatGedcomFilename);
		final GedcomObject root = extractor(lines);
		final Map<String, List<Map<String, Object>>> tables = transfer(root);
	}

	private static List<String> flatGedcom(final String baseDirectory, final String gedcomFilename, final String flatGedcomFilename)
			throws IOException{
		Path path = Paths.get(baseDirectory + gedcomFilename);
		List<String> lines = Files.readAllLines(path);

		lines.replaceAll(s -> s.replaceAll("@@", "@"));

		List<String> output = new ArrayList<>();

		String id = null;
		String baseType = null;
		int objeID = 1;
		for(String line : lines){
			if(line.contains("0 HEAD"))
				baseType = "HEAD";
			else if(line.contains("0 TRLR"))
				break;
			else if(line.startsWith("0 ")){
				id = line.split(" ")[1].substring(2);
				id = id.substring(0, id.indexOf('@'));
				baseType = line.split(" ")[2];
				String[] lineComponents = line.split(" ", 4);
				if(lineComponents.length == 4){
					output.add("0 " + baseType + "[" + id + "]");
					output.add("0 " + baseType + "[" + id + "].CONT " + lineComponents[3]);
				}
				else
					output.add("0 " + baseType + "[" + id + "]");
			}
			else if(!"HEAD".equals(baseType)){
				if(line.startsWith("1 ")){
					line = line.substring(2);
					if(line.matches("[A-Z]+\\s+@[A-Z]\\d+@")){
            		String number = line.replaceAll("[^\\d]", "");
            		line = line.replaceAll("\\s+@[A-Z](\\d+)@", "[" + number + "]");
            	}
					if(line.startsWith("CONC ")){
						int prevIndex = output.size() - 1;
						line = output.get(prevIndex) + line.split(" ", 2)[1];
						output.set(prevIndex, line);
					}
					else{
						line = "0 " + baseType + "[" + id + "]." + line;
						if(line.endsWith(".OBJE")){
							line += "[" + objeID + "]";
							objeID ++;
						}
						output.add(line);
					}
				}
				else
					output.add(line);
			}
		}

		int dataID = 1;
		int descrID = 1;
		int propID = 1;
		for(int stage = 2; stage < 6; stage ++){
			for(int i = 0; i < output.size(); i ++){
				String line = output.get(i);
				if(line.endsWith(".DATA")){
					line += "[" + dataID + "]";
					output.set(i, line);
					dataID ++;
				}
				else if(line.endsWith(".OBJE")){
					line += "[" + objeID + "]";
					output.set(i, line);
					objeID ++;
				}
				else if(line.endsWith(".DSCR")){
					line += "[" + descrID + "]";
					output.set(i, line);
					descrID ++;
				}
				else if(line.endsWith(".PROP")){
					line += "[" + propID + "]";
					output.set(i, line);
					propID ++;
				}

				if(line.startsWith("0 "))
					baseType = line.split(" ")[1];
				else if(line.startsWith(stage + " ")){
					if(line.matches("\\d\\s+[A-Z]+\\s+@[A-Z]\\d+@")){
						String number = line.substring(2).replaceAll("[^\\d]", "");
						line = line.replaceAll("\\s+@[A-Z](\\d+)@", "[" + number + "]");
					}
					output.set(i, "0 " + baseType + "." + line.substring(2));
				}
			}
		}

		output.replaceAll(s -> s.substring(2).trim());
		output.replaceAll(s -> SUFFIXES_Y.stream().anyMatch(s::endsWith)? s.substring(0, s.length() - 2): s);

		output.removeIf(line -> line.contains("].NAME.GIVN ") || line.contains("].NAME.SURN "));
		output.removeIf(line -> line.contains(".RIN "));
		output.removeIf(line -> line.contains(".ADDR.POST "));

		for(int i = 0, length = output.size(); i < length; i ++){
			String line = output.get(i);
			String dateLine = null;
			String timeLine = null;
			if(line.endsWith(".CHAN")){
				dateLine = output.get(i + 1);
				if(dateLine.contains(".CHAN.DATE ")){
					dateLine = dateLine.substring(dateLine.indexOf(" ") + 1);

					timeLine = output.get(i + 2);
					if(timeLine.contains(".CHAN.DATE.TIME "))
						timeLine = timeLine.substring(timeLine.indexOf(" ") + 1);
					else
						timeLine = null;
				}
			}
			if(dateLine != null)
				output.set(i, line + " " + dateLine + (timeLine != null? " " + timeLine: ""));

			if(line.contains(".ADDR.")){
				String prevLine = output.get(i - 1);
				if(prevLine.endsWith(".ADDR")){
					StringJoiner sj = new StringJoiner(", ");
					int j = i;
					while(j < output.size() && output.get(j).contains(".ADDR.")){
						sj.add(output.get(j)
							.split(" ", 2)[1]);

						j ++;
					}
					output.set(i - 1, prevLine + " " + sj);
				}
			}

			if(line.contains(".CONC ")){
				String prevLine = output.get(i - 1);
				int j = i;
				while(j < output.size() && output.get(j).contains(".CONC ")){
					prevLine += output.get(j).split(" ", 2)[1];

					j ++;
				}
				output.set(i - 1, prevLine);
			}
		}

		output.removeIf(line -> REMOVAL_TAGS.stream().anyMatch(line::contains));

		for(int i = 0; i < output.size(); i ++){
			String line = output.get(i);

			if(SUFFIXES.stream().anyMatch(line::contains)){
				String[] components = line.split(" ", 2);
				output.set(i, components[0]);
				output.add(i + 1, components[0] + ".DESC " + components[1]);
			}
		}

		int nameID = 1;
		int educID = 1;
		int evenID = 1;
		int occuID = 1;
		int mapID = 1;
		int placID = 1;
		int conclusionID = 1;
		int noteID = 10_000;
		int textID = 20_000;
		for(int i = 0; i < output.size(); i ++){
			nameID = manageID("NAME", nameID, output, i);
			educID = manageID("EDUC", educID, output, i);
			occuID = manageID("OCCU", occuID, output, i);
			mapID = manageID("MAP", mapID, output, i);
			placID = manageID("PLAC", placID, output, i);
			textID = manageID("TEXT", textID, output, i);
			noteID = manageID("NOTE", noteID, output, i);
			evenID = manageID("EVEN", evenID, output, i);
			for(final String identifier : IDENTIFIERS)
				conclusionID = manageID(identifier, conclusionID, output, i);
		}
		for(int i = 0; i < output.size(); i ++)
			evenID = manageID("EVEN", evenID, output, i);

		output.replaceAll(s -> {
			final String[] split = s.split(" ", 2);
			return (split.length == 1
				? s + (s.endsWith("]")? "": "=")
				: split[0] + "=" + split[1]);
		});
		output.replaceAll(s -> {
			if(s.matches("SOUR\\[\\d+]\\.REPO\\[\\d+]")){
				int cutIndex = s.lastIndexOf("[");
				String pre = s.substring(0, cutIndex);
				String repoID = s.substring(cutIndex + 1, s.length() - 1);
				s = pre + "=REPO[" + repoID + "]";
			}
			return s;
		});

		if(flatGedcomFilename != null)
			Files.write(Paths.get(baseDirectory + flatGedcomFilename), output);

		return output;
	}

	private static int manageID(final String tag, final int id, final List<String> output, final int i){
		String line = output.get(i);
		if(line.contains("." + tag + ".")){
			line = line.replaceFirst("\\." + tag + "\\.", "." + tag + "[" + (id - 1) + "].");
			output.set(i, line);
		}
		else if(line.endsWith("." + tag)){
			line += "[" + id + "]";
			output.set(i, line);
			return id + 1;
		}
		return id;
	}

	private static GedcomObject extractor(final List<String> lines){
		final GedcomObject root = new GedcomObject();
		Map<String, GedcomObject> context = new HashMap<>();
		context.put("", root);

		for(String line : lines){
			line = line.trim();

			if(!line.contains("=") && line.matches(".*\\[\\d+]$")){
				final int lastDotIndex = line.lastIndexOf('.');
				final String parentKey = (lastDotIndex == - 1? "": line.substring(0, lastDotIndex));
				final String key = line.substring(lastDotIndex + 1);

				final GedcomObject parent = context.get(parentKey);
				final GedcomObject child = new GedcomObject();
				child.type = key.substring(0, key.indexOf('['));
				child.id = Integer.parseInt(key.substring(key.indexOf('[') + 1, key.length() - 1));
				parent.children.put(key, child);
				context.put(line, child);
			}
			else if(line.contains("=")){
				final int lastDotIndex = line.lastIndexOf('.', line.indexOf('='));
				final String parentKey = line.substring(0, lastDotIndex);
				final GedcomObject parent = context.get(parentKey);
				final String[] parts = line.split("=", 2);
				final String attributeKey = parts[0].substring(lastDotIndex + 1);
				if(parent.attributes.containsKey(attributeKey)){
					if(!attributeKey.equals("CONT"))
						throw new IllegalArgumentException("error while squashing " + attributeKey + ": not managed (yet)");

					parts[1] = parent.attributes.get(attributeKey) + "\r\n" + parts[1];
				}
				parent.attributes.put(attributeKey, parts[1]);
			}
		}
		return root;
	}

	private static Map<String, List<Map<String, Object>>> transfer(final GedcomObject root){
		final Map<String, List<GedcomObject>> map = createFirstLevelMap(root);
		final List<GedcomObject> groups = map.get("FAM");
		final List<GedcomObject> repositories = map.get("REPO");
		final List<GedcomObject> notes = map.get("NOTE");
		final List<GedcomObject> persons = map.get("INDI");
		final List<GedcomObject> sources = map.get("SOUR");

		final Map<String, List<Map<String, Object>>> output = new HashMap<>();
		transferRepositories(output, repositories);
		transferNotes(output, notes);

		return output;
	}

	private static void transferRepositories(Map<String, List<Map<String, Object>>> output, List<GedcomObject> repositories){
		final List<Map<String, Object>> flefRepositories = output.computeIfAbsent("repository", k -> new ArrayList<>());
		final List<Map<String, Object>> flefPlaces = output.computeIfAbsent("place", k -> new ArrayList<>());
		final List<Map<String, Object>> flefLocalizedTexts = output.computeIfAbsent("localized_text", k -> new ArrayList<>());

		for(final GedcomObject repository : repositories){
			final Map<String, GedcomObject> children = repository.children;
			final Map<String, String> attributes = repository.attributes;

			final String addr = attributes.get("ADDR");
			final GedcomObject name = getFirstStartingWith(children, "NAME");
			final String repositoryName = name.attributes.get("DESC");
			final GedcomObject plac = getFirstStartingWith(children, "PLAC");
			if(addr != null || plac != null){
				final String address = (addr != null? addr: plac.attributes.get("DESC"));

				final Map<String, Object> flefLocalizedText = new HashMap<>();
				flefLocalizedTexts.add(flefLocalizedText);
				flefLocalizedText.put("id", name.id);
				flefLocalizedText.put("text", repositoryName);
				flefLocalizedText.put("locale", "it");
				flefLocalizedText.put("type", "original");

				final Map<String, Object> flefPlace = new HashMap<>();
				flefPlaces.add(flefPlace);
				flefPlace.put("id", plac.id);
				flefPlace.put("identifier", address);
				flefPlace.put("name_id", name.id);
			}

			final Map<String, Object> flefRepository = new HashMap<>();
			flefRepositories.add(flefRepository);
			flefRepository.put("id", repository.id);
			flefRepository.put("identifier", repositoryName);
			if(plac != null)
				flefRepository.put("place_id", plac.id);
		}
	}

	private static void transferNotes(Map<String, List<Map<String, Object>>> output, List<GedcomObject> notes){
		final List<Map<String, Object>> flefNotes = output.computeIfAbsent("note", k -> new ArrayList<>());
		final List<Map<String, Object>> flefSources = output.computeIfAbsent("source", k -> new ArrayList<>());
		final List<Map<String, Object>> flefHistoricPlaces = output.computeIfAbsent("historic_place", k -> new ArrayList<>());
		final List<Map<String, Object>> flefHistoricDates = output.computeIfAbsent("historic_date", k -> new ArrayList<>());

		for(final GedcomObject note : notes){
			final Map<String, GedcomObject> children = note.children;
			final Map<String, String> attributes = note.attributes;

			final String text = attributes.get("CONT");
			if(children.isEmpty()){
				final Map<String, Object> flefNote = new HashMap<>();
				flefNotes.add(flefNote);
				flefNote.put("id", note.id);
				flefNote.put("note", text);
				flefNote.put("reference_table", null);
				flefNote.put("reference_id", null);
			}
			else
				for(final GedcomObject child : children.values()){
					final String qualityOfData = child.attributes.get("QUAY");

					//TODO duplicate note for each source?
					final Map<String, Object> flefNote = new HashMap<>();
					flefNotes.add(flefNote);
					flefNote.put("id", note.id);
					flefNote.put("note", text);
					flefNote.put("reference_table", null);
					flefNote.put("reference_id", null);

					System.out.println(qualityOfData);
				}
		}
	}

	private static GedcomObject getFirstStartingWith(final Map<String, GedcomObject> children, final String tag){
		for(final GedcomObject child : children.values())
			if(child.type.equals(tag))
				return child;
		return null;
	}

	private static Map<String, List<GedcomObject>> createFirstLevelMap(final GedcomObject root){
		final Map<String, List<GedcomObject>> firstLevelMap = new HashMap<>();
		for(final Map.Entry<String, GedcomObject> entry : root.children.entrySet()){
			String key = entry.getKey();
			key = key.substring(0, key.indexOf('['));
			firstLevelMap.computeIfAbsent(key, k -> new ArrayList<>())
				.add(entry.getValue());
		}
		return firstLevelMap;
	}


	private static void media_note_mediaJunction_source(final String baseDirectory) throws IOException{
		Path path = Paths.get(baseDirectory + "\\ged\\TMGZ.txt");
		List<String> lines = Files.readAllLines(path);

		Map<String, Integer> mediaMap = new HashMap<>();
		List<String> media = new ArrayList<>();
		media.add("MEDIA");
		media.add("ID|IDENTIFIER|TITLE|TYPE|IMAGE_PROJECTION");
		List<String> mediaJunction = new ArrayList<>();
		mediaJunction.add("MEDIA_JUNCTION");
		mediaJunction.add("ID|MEDIA_ID|IMAGE_CROP|REFERENCE_TABLE|REFERENCE_ID");
		List<String> note = new ArrayList<>();
		note.add("NOTE");
		note.add("ID|NOTE|REFERENCE_TABLE|REFERENCE_ID");
		List<String> source = new ArrayList<>();
		source.add("SOURCE");
		source.add("ID|IDENTIFIER|SOURCE_TYPE|AUTHOR|PLACE_ID|DATE_ID|REPOSITORY_ID|LOCATION");

		String personID = null;
		String sourceID = null;
		String mediaTitle = null;
		String mediaIdentifier = null;
		String mediaCrop = null;
		String mediaDate = null;
		//TODO
		String mediaNote = null;
		String sourceIdentifier = null;
		String sourceLocation = null;
		String sourceRepository = null;
		String sourceAuthor = null;
		for(int i = 0; i < lines.size(); i++){
			String line = lines.get(i);

			if(line.startsWith("PERSON=")){
				if(personID != null && mediaIdentifier != null){
					Integer mediaID = mediaMap.get(mediaIdentifier);
					if(mediaID == null){
						mediaID = media.size() - 1;
						mediaMap.put(mediaIdentifier, mediaID);

						media.add(mediaID + "|" + mediaIdentifier + "|" + mediaTitle + "||rectangular");
					}

					mediaJunction.add((mediaJunction.size() - 1) + "|" + mediaID + "|" + (mediaCrop != null? mediaCrop: "") + "|person|" + personID);

					if(mediaDate != null){
						String noteID = "" + (mediaID + 40_000);
						note.add(noteID + "|" + mediaDate + "|media|" + mediaID);
					}

					mediaTitle = null;
					mediaIdentifier = null;
					mediaCrop = null;
					mediaDate = null;
					sourceIdentifier = null;
					sourceLocation = null;
					sourceRepository = null;
					sourceAuthor = null;
				}

				personID = line.split("=")[1];
				sourceID = null;
			}
			else if(line.startsWith("SOURCE=")){
				if(sourceID != null && mediaIdentifier != null){
					Integer mediaID = mediaMap.get(mediaIdentifier);
					if(mediaID == null){
						mediaID = media.size() - 1;
						mediaMap.put(mediaIdentifier, mediaID);

						media.add(mediaID + "|" + mediaIdentifier + "|" + mediaTitle + "||rectangular");
					}

					if(personID != null)
						mediaJunction.add((mediaJunction.size() - 1) + "|" + mediaID + "|" + (mediaCrop != null? mediaCrop: "") + "|person|" + personID);
					else{
						mediaJunction.add((mediaJunction.size() - 1) + "|" + mediaID + "|" + (mediaCrop != null? mediaCrop: "") + "|source|" + sourceID);
						source.add(sourceID + "|" + sourceIdentifier + "||" + (sourceAuthor != null? sourceAuthor: "") + "|||"
							+ (sourceRepository != null? sourceRepository: "") + "|" + (sourceLocation != null? sourceLocation: ""));
					}

					if(mediaDate != null){
						String noteID = "" + (mediaID + 40_000);
						note.add(noteID + "|" + mediaDate + "|media|" + mediaID);
					}

					mediaTitle = null;
					mediaIdentifier = null;
					mediaCrop = null;
					mediaDate = null;
					sourceIdentifier = null;
					sourceLocation = null;
					sourceRepository = null;
					sourceAuthor = null;
				}

				personID = null;
				sourceID = line.split("=")[1];
			}
			else if(line.startsWith("MEDIA.TITLE.1=")){
				if(mediaTitle != null && mediaIdentifier != null){
					Integer mediaID = mediaMap.get(mediaIdentifier);
					if(mediaID == null){
						mediaID = media.size() - 1;
						mediaMap.put(mediaIdentifier, mediaID);

						media.add(mediaID + "|" + mediaIdentifier + "|" + mediaTitle + "||rectangular");
					}

					if(personID != null)
						mediaJunction.add((mediaJunction.size() - 1) + "|" + mediaID + "|" + (mediaCrop != null? mediaCrop: "") + "|person|" + personID);
					else if(sourceID != null)
						mediaJunction.add((mediaJunction.size() - 1) + "|" + mediaID + "|" + (mediaCrop != null? mediaCrop: "") + "|source|" + sourceID);
					else
						throw new IllegalArgumentException("Something wrong happened");

					mediaIdentifier = null;
					mediaCrop = null;
					mediaDate = null;
				}

				mediaTitle = line.split("=")[1];
			}
			else if(line.startsWith("MEDIA.IDENTIFIER.1="))
				mediaIdentifier = line.split("=")[1];
			else if(line.startsWith("MEDIA.IMAGE_CROP.1="))
				mediaCrop = line.split("=")[1];
			else if(line.startsWith("MEDIA.DATE.1="))
				mediaDate = line.split("=")[1];
			else if(line.startsWith("SOURCE.IDENTIFIER="))
				sourceIdentifier = line.split("=")[1];
			else if(line.startsWith("SOURCE.LOCATION="))
				sourceLocation = line.split("=")[1];
			else if(line.startsWith("SOURCE.REPOSITORY="))
				sourceRepository = line.split("=")[1];
			else if(line.startsWith("SOURCE.AUTHOR="))
				sourceAuthor = line.split("=")[1];
		}

		Files.write(Paths.get(baseDirectory + "\\ged\\output\\media.txt"),
			media);

		Files.write(Paths.get(baseDirectory + "\\ged\\output\\mediaJunction.txt"),
			mediaJunction);

		Files.write(Paths.get(baseDirectory + "\\ged\\output\\note.txt"),
			note);

		Files.write(Paths.get(baseDirectory + "\\ged\\output\\source.txt"),
			source);
	}

	private static void repository_historicPlace_place_name(final String baseDirectory) throws IOException{
		Path path = Paths.get(baseDirectory + "\\ged\\TMGZ.txt");
		List<String> lines = Files.readAllLines(path);

		List<String> name = new ArrayList<>();
		name.add("LOCALIZED_TEXT");
		name.add("ID|TEXT|LOCALE|TYPE|TRANSCRIPTION|TRANSCRIPTION_TYPE");
		List<String> repository = new ArrayList<>();
		repository.add("REPOSITORY");
		repository.add("ID|IDENTIFIER|TYPE|PERSON_ID|PLACE_ID");
		List<String> place = new ArrayList<>();
		place.add("PLACE");
		place.add("ID|IDENTIFIER|NAME_ID|TYPE|COORDINATE|COORDINATE_TYPE|COORDINATE_CREDIBILITY|PRIMARY_PLACE_ID|IMAGE_ID|IMAGE_CROP");
		List<String> historicPlace = new ArrayList<>();
		historicPlace.add("HISTORIC_PLACE");
		historicPlace.add("ID|PLACE_ID|CERTAINTY|CREDIBILITY");

		String repositoryID = null;
		String identifier = null;
		String plac = null;
		for(int i = 0; i < lines.size(); i++){
			String line = lines.get(i);

			if(line.startsWith("REPOSITORY="))
				repositoryID = line.split("=")[1];
			else if(line.startsWith("REPOSITORY.IDENTIFIER=")){
				if(identifier != null){
					String namID = "" + (Integer.parseInt(repositoryID) + 10_000);
					String placID = "" + (Integer.parseInt(repositoryID) + 20_000);
					String histPlacID = "" + (Integer.parseInt(repositoryID) + 30_000);
					if(plac != null){
						name.add(namID + "|" + plac + "|it|original||");
						place.add(placID + "|" + plac + "|" + namID + "|||||||");
						historicPlace.add(histPlacID + "|" + placID + "|certain|3");
					}
					else
						histPlacID = "";
					repository.add(repositoryID + "|" + identifier + "|||" + histPlacID);

					repositoryID = null;
					plac = null;
				}

				identifier = line.split("=")[1];
			}
			else if(line.startsWith("REPOSITORY.PLACE="))
				plac = line.split("=")[1];
		}

		Files.write(Paths.get(baseDirectory + "\\ged\\output\\name.txt"),
			name);

		Files.write(Paths.get(baseDirectory + "\\ged\\output\\place.txt"),
			place);

		Files.write(Paths.get(baseDirectory + "\\ged\\output\\historicPlace.txt"),
			historicPlace);

		Files.write(Paths.get(baseDirectory + "\\ged\\output\\repository.txt"),
			repository);
	}

}
