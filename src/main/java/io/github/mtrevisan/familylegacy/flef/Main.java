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
import java.net.URISyntaxException;
import java.net.URL;
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

	private static final Set<String> SUFFIXES_Y = new HashSet<>(Arrays.asList("BIRT Y", "CHR Y", "DEAT Y", "BURI Y", "CREM Y", "ADOP Y",
		"BAPM Y", "BARM Y", "BASM Y", "BLES Y", "CHRA Y", "CONF Y", "FCOM Y", "ORDN Y", "NATU Y", "EMIG Y", "IMMI Y", "CENS Y", "PROB Y",
		"WILL Y", "GRAD Y", "RETI Y", "ANUL Y", "DIV Y", "DIVF Y", "ENGA Y", "MARB Y", "MARC Y", "MARR Y", "MARL Y", "MARS Y", "RESI Y"));

	private static final String[] IDENTIFIERS = {"SEX", "MARR", "MARL", "MARB", "MARS", "BIRT", "DEAT", "CREM", "BURI", "RETI", "RESI",
		"RELI", "ENGA", "DSCR", "SSN", "EMIG", "IMMI", "ADOP", "PROP", "NATU", "CENS", "GRAD", "ORDN", "NCHI", "WILL", "DIV"};

	private static class GedcomObject{
		String type;
		int id;
		final Map<String, GedcomObject> children = new HashMap<>();
		final Map<String, String> attributes = new HashMap<>();
	}


	public static void main(final String[] args) throws URISyntaxException, SQLGrammarException, SQLDataException, GedcomGrammarException,
			GedcomDataException, SQLException, IOException{
//		final GedcomFileParser gedcomParser = new GedcomFileParser();
//		gedcomParser.load("/gedg/gedcom_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged");

//		final SQLFileParser sqlParser = new SQLFileParser();
//		final String grammarFile = "src/main/resources/gedg/treebard/FLeF.sql";
//		sqlParser.load(grammarFile, "src/main/resources/gedg/treebard/FLeF.data");

//		final DatabaseManager dbManager = new DatabaseManager(JDBC_URL, USER, PASSWORD);
//		dbManager.initialize(grammarFile);

		final String gedcomFilename = "ged\\TGMZ.ged";
		String flatGedcomFilename = "ged\\TGMZ.txt";
		boolean flat = false;

		final List<String> lines;
		if(flat)
			lines = flatGedcom(gedcomFilename, flatGedcomFilename);
		else
			lines = readFile(flatGedcomFilename);
		final GedcomObject root = extractor(lines);
		final Map<String, List<Map<String, Object>>> tables = transfer(root, lines);
	}

	private static List<String> flatGedcom(final String gedcomFilename, final String flatGedcomFilename) throws URISyntaxException,
			IOException{
		final List<String> lines = readFile(gedcomFilename);

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
					dateLine = dateLine.substring(dateLine.indexOf(' ') + 1);

					timeLine = output.get(i + 2);
					if(timeLine.contains(".CHAN.DATE.TIME "))
						timeLine = timeLine.substring(timeLine.indexOf(' ') + 1);
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
		int noteID = 100;
		int textID = 5_000;
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

		if(flatGedcomFilename != null){
			final URL resourceOutput = Main.class.getClassLoader().getResource(flatGedcomFilename);
			Files.write(Paths.get(resourceOutput.toURI()), output);
		}

		return output;
	}

	private static List<String> readFile(final String gedcomFilename) throws URISyntaxException, IOException{
		final URL resourceInput = Main.class.getClassLoader().getResource(gedcomFilename);
		final Path path = Paths.get(resourceInput.toURI());

		return Files.readAllLines(path);
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
				child.type = extractType(key);
				child.id = extractID(key);
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

	private static Map<String, List<Map<String, Object>>> transfer(final GedcomObject root, final List<String> lines){
		final Map<String, List<GedcomObject>> map = createFirstLevelMap(root);
		final List<GedcomObject> groups = map.get("FAM");
		final List<GedcomObject> repositories = map.get("REPO");
		final List<GedcomObject> notes = map.get("NOTE");
		final List<GedcomObject> sources = map.get("SOUR");
		final List<GedcomObject> persons = map.get("INDI");

		final Map<String, List<Map<String, Object>>> output = new HashMap<>();
		transferProject(output);
		transferCalendar(output);
		transferCulturalNorm(output);
		transferGroups(output, groups);
		transferRepositories(output, repositories);
		transferNotes(output, notes, lines);
		transferSources(output, sources);
		transferPersons(output, persons);

		return output;
	}

	private static void transferProject(final Map<String, List<Map<String, Object>>> output){
		final List<Map<String, Object>> flefProjects = output.computeIfAbsent("project", k -> new ArrayList<>());

		final Map<String, Object> flefProject = new HashMap<>();
		flefProjects.add(flefProject);
		flefProject.put("id", 1);
		flefProject.put("protocol_name", "Family LEgacy Format");
		flefProject.put("protocol_version", "0.0.10");
		flefProject.put("copyright", "(c) 2024 Mauro Trevisan");
	}

	private static void transferCalendar(final Map<String, List<Map<String, Object>>> output){
		final List<Map<String, Object>> flefCalendars = output.computeIfAbsent("calendar", k -> new ArrayList<>());

		Map<String, Object> flefCalendar = new HashMap<>();
		flefCalendars.add(flefCalendar);
		flefCalendar.put("id", 1);
		flefCalendar.put("type", "gregorian");

		flefCalendar = new HashMap<>();
		flefCalendars.add(flefCalendar);
		flefCalendar.put("id", 2);
		flefCalendar.put("type", "julian");

		flefCalendar = new HashMap<>();
		flefCalendars.add(flefCalendar);
		flefCalendar.put("id", 3);
		flefCalendar.put("type", "venetan");
	}

	private static void transferCulturalNorm(final Map<String, List<Map<String, Object>>> output){
		final List<Map<String, Object>> flefCulturalNorms = output.computeIfAbsent("cultural_norm", k -> new ArrayList<>());
		final List<Map<String, Object>> flefHistoricDates = output.computeIfAbsent("historic_date", k -> new ArrayList<>());
		final List<Map<String, Object>> flefHistoricPlaces = output.computeIfAbsent("historic_place", k -> new ArrayList<>());
		final List<Map<String, Object>> flefPlaces = output.computeIfAbsent("place", k -> new ArrayList<>());
		final List<Map<String, Object>> flefLocalizedTexts = output.computeIfAbsent("localized_text", k -> new ArrayList<>());

		final Map<String, Object> flefLocalizedText = new HashMap<>();
		flefLocalizedTexts.add(flefLocalizedText);
		flefLocalizedText.put("id", 10_000);
		flefLocalizedText.put("text", "Regno Lombardo-Veneto");
		flefLocalizedText.put("locale", "it");
		flefLocalizedText.put("type", "original");

		Map<String, Object> flefPlace = new HashMap<>();
		flefPlaces.add(flefPlace);
		final int placeID = flefPlaces.size();
		flefPlace.put("id", placeID);
		flefPlace.put("identifier", "Regno Lombardo-Veneto");
		flefPlace.put("name_id", 10_000);
		flefPlace.put("type", "reign");

		Map<String, Object> flefHistoricPlace = new HashMap<>();
		flefHistoricPlaces.add(flefHistoricPlace);
		final int historicPlaceID = flefHistoricPlaces.size();
		flefHistoricPlace.put("id", historicPlaceID);
		flefHistoricPlace.put("place_id", placeID);
		flefHistoricPlace.put("certainty", "certain");
		flefHistoricPlace.put("credibility", 3);

		Map<String, Object> flefHistoricDate = new HashMap<>();
		flefHistoricDates.add(flefHistoricDate);
		final int dateStartID = flefHistoricDates.size();
		flefHistoricDate.put("id", dateStartID);
		flefHistoricDate.put("date", "31 JAN 1807");
		flefHistoricDate.put("calendar_id", 1);
		flefHistoricDate = new HashMap<>();
		flefHistoricDates.add(flefHistoricDate);
		final int dateEndID = flefHistoricDates.size();
		flefHistoricDate.put("id", dateStartID);
		flefHistoricDate.put("date", "19 FEB 1811");
		flefHistoricDate.put("calendar_id", 1);

		Map<String, Object> flefCulturalNorm = new HashMap<>();
		flefCulturalNorms.add(flefCulturalNorm);
		flefCulturalNorm.put("id", 1);
		flefCulturalNorm.put("identifier", "nomi latini");
		flefCulturalNorm.put("description", "per i nomi in latino si deve usare il nominativo (quello che generalmente finisce in *-us* per il maschile e *-a* per il femminile)");
		flefCulturalNorm.put("certainty", "certain");
		flefCulturalNorm.put("credibility", 3);

		flefCulturalNorm = new HashMap<>();
		flefCulturalNorms.add(flefCulturalNorm);
		flefCulturalNorm.put("id", 2);
		flefCulturalNorm.put("identifier", "napoleonic code age of majority in men");
		flefCulturalNorm.put("description", "23 anni minore, 29 anni maggiore");
		flefCulturalNorm.put("place_id", historicPlaceID);
		flefCulturalNorm.put("date_start_id", dateStartID);
		flefCulturalNorm.put("date_end_id", dateEndID);
		flefCulturalNorm.put("certainty", "certain");
		flefCulturalNorm.put("credibility", 3);

		flefCulturalNorm = new HashMap<>();
		flefCulturalNorms.add(flefCulturalNorm);
		flefCulturalNorm.put("id", 3);
		flefCulturalNorm.put("identifier", "napoleonic code respectful act for men");
		flefCulturalNorm.put("description", "fino ai 30");
		flefCulturalNorm.put("place_id", historicPlaceID);
		flefCulturalNorm.put("date_start_id", dateStartID);
		flefCulturalNorm.put("date_end_id", dateEndID);
		flefCulturalNorm.put("certainty", "certain");
		flefCulturalNorm.put("credibility", 3);

		flefCulturalNorm = new HashMap<>();
		flefCulturalNorms.add(flefCulturalNorm);
		flefCulturalNorm.put("id", 4);
		flefCulturalNorm.put("identifier", "napoleonic code age of majority in women");
		flefCulturalNorm.put("description", "22 anni minore");
		flefCulturalNorm.put("place_id", historicPlaceID);
		flefCulturalNorm.put("date_start_id", dateStartID);
		flefCulturalNorm.put("date_end_id", dateEndID);
		flefCulturalNorm.put("certainty", "certain");
		flefCulturalNorm.put("credibility", 3);

		flefCulturalNorm = new HashMap<>();
		flefCulturalNorms.add(flefCulturalNorm);
		flefCulturalNorm.put("id", 5);
		flefCulturalNorm.put("identifier", "napoleonic code respectful act for women");
		flefCulturalNorm.put("description", "fino ai 25");
		flefCulturalNorm.put("place_id", historicPlaceID);
		flefCulturalNorm.put("date_start_id", dateStartID);
		flefCulturalNorm.put("date_end_id", dateEndID);
		flefCulturalNorm.put("certainty", "certain");
		flefCulturalNorm.put("credibility", 3);
	}

	private static void transferGroups(final Map<String, List<Map<String, Object>>> output, final List<GedcomObject> groups){
		final List<Map<String, Object>> flefGroups = output.computeIfAbsent("group", k -> new ArrayList<>());
		final List<Map<String, Object>> flefGroupJunctions = output.computeIfAbsent("group_junction", k -> new ArrayList<>());
		final List<Map<String, Object>> flefNotes = output.computeIfAbsent("note", k -> new ArrayList<>());
		final List<Map<String, Object>> flefAssertions = output.computeIfAbsent("assertion", k -> new ArrayList<>());
		final List<Map<String, Object>> flefCitations = output.computeIfAbsent("citation", k -> new ArrayList<>());
		final List<Map<String, Object>> flefHistoricDates = output.computeIfAbsent("historic_date", k -> new ArrayList<>());

		for(final GedcomObject group : groups){
			final Map<String, GedcomObject> children = group.children;

			final GedcomObject husband = getFirstStartingWith(children, "HUSB");
			final GedcomObject wife = getFirstStartingWith(children, "WIFE");
			final List<GedcomObject> marriages = getAllStartingWith(children, "MARR");
			for(final GedcomObject marriage : marriages){
				final Map<String, GedcomObject> marriageChildren = marriage.children;
				final String marriageDate = marriage.attributes.get("DATE");
				final List<GedcomObject> marriageNotes = getAllStartingWith(marriageChildren, "NOTE");
				final List<GedcomObject> marriageSources = getAllStartingWith(marriageChildren, "SOUR");
				final GedcomObject marriagePlace = getFirstStartingWith(marriageChildren, "PLAC");
				for(final GedcomObject note : marriageNotes){
//					final String desc = note.attributes.get("DESC");
//					final String time = (desc.startsWith("ore ")? desc: null);
//					if(time != null){
//						//TODO add to marriage date
//						System.out.println();
//					}
//					else{
//						//add note
//						final GedcomObject noteSource = getFirstStartingWith(note.children, "SOUR");
//						if(noteSource != null){
//							final GedcomObject noteSourceEvent = getFirstStartingWith(noteSource.children, "EVEN");
//							if(noteSourceEvent != null){
//								final String noteSourceEventDesc = noteSourceEvent.attributes.get("DESC");
//								if(noteSourceEventDesc == null){
//									System.out.println();
//								}
//								else if(noteSourceEventDesc.equals("MARR")){
//									final Map<String, Object> flefNote = new HashMap<>();
//									flefNotes.add(flefNote);
//									flefNote.put("id", note.id);
//									flefNote.put("note", desc);
//									flefNote.put("reference_table", "source");
//									flefNote.put("reference_id", noteSource.id);
//								}
//								else
//									System.out.println();
//							}
//							else{
//								final Map<String, Object> flefNote = new HashMap<>();
//								flefNotes.add(flefNote);
//								flefNote.put("id", note.id);
//								flefNote.put("note", desc);
//								flefNote.put("reference_table", "source");
//								flefNote.put("reference_id", noteSource.id);
//							}
//						}
//						else if(!marriageSources.isEmpty()){
//							if(marriageSources.size() == 1){
//								final Map<String, Object> flefNote = new HashMap<>();
//								flefNotes.add(flefNote);
//								flefNote.put("id", note.id);
//								flefNote.put("note", desc);
//								flefNote.put("reference_table", "source");
//								flefNote.put("reference_id", marriageSources.getFirst().id);
//							}
//							else
//								System.out.println();
//						}
//						else
//							System.out.println();
//					}
				}
				for(final GedcomObject marriageSource : marriageSources){
					if(marriagePlace != null){
						//create citation
						final Map<String, Object> flefCitation = new HashMap<>();
						flefCitations.add(flefCitation);
						final int citationID = flefCitations.size();
						flefCitation.put("id", citationID);
						flefCitation.put("source_id", marriageSource.id);
						flefCitation.put("location", null);
						flefCitation.put("extract_id", null);
						flefCitation.put("extract_type", "transcript");

						//create assertion connected to a place
						final Map<String, Object> flefAssertion = new HashMap<>();
						flefAssertions.add(flefAssertion);
						flefAssertion.put("id", flefAssertions.size());
						flefAssertion.put("citation_id", citationID);
						flefAssertion.put("reference_table", "historic_place");
						flefAssertion.put("reference_id", marriagePlace.id);
						flefAssertion.put("role", null);
						flefAssertion.put("certainty", null);
						flefAssertion.put("credibility", 3);
					}

					if(marriageDate != null){
						//create citation
						final Map<String, Object> flefCitation = new HashMap<>();
						flefCitations.add(flefCitation);
						final int citationID = flefCitations.size();
						flefCitation.put("id", citationID);
						flefCitation.put("source_id", marriageSource.id);
						flefCitation.put("location", null);
						flefCitation.put("extract_id", null);
						flefCitation.put("extract_type", "transcript");

						//TODO
//						final String marriageDesc = marriage.attributes.get("DESC");
//						final String time = (marriageDesc.startsWith("ore ")? marriageDesc: null);
						final Map<String, Object> flefHistoricDate = new HashMap<>();
						flefHistoricDates.add(flefHistoricDate);
						final int marriageDateID = flefHistoricDates.size();
//						flefHistoricDate.put("id", marriageDateID);
//						flefHistoricDate.put("date", "31 JAN 1807");

						//create assertion connected to a date
						final Map<String, Object> flefAssertion = new HashMap<>();
						flefAssertions.add(flefAssertion);
						flefAssertion.put("id", flefAssertions.size());
						flefAssertion.put("citation_id", citationID);
						flefAssertion.put("reference_table", "historic_date");
						flefAssertion.put("reference_id", marriageDateID);
						flefAssertion.put("role", null);
						flefAssertion.put("certainty", null);
						flefAssertion.put("credibility", 3);
					}

//					System.out.println();
				}

				final boolean verifiedMarriage = !marriageSources.isEmpty();

				if(husband != null || wife != null){
					final Map<String, Object> flefGroup = new HashMap<>();
					flefGroups.add(flefGroup);
					flefGroup.put("id", marriage.id);
					flefGroup.put("type", "family");

					if(husband != null){
						final Map<String, Object> flefGroupJunction = new HashMap<>();
						flefGroupJunctions.add(flefGroupJunction);
						flefGroupJunction.put("id", flefGroupJunctions.size());
						flefGroupJunction.put("group_id", marriage.id);
						flefGroupJunction.put("reference_table", "person");
						flefGroupJunction.put("reference_id", husband.id);
						flefGroupJunction.put("role", "partner");
						if(verifiedMarriage){
							flefGroupJunction.put("certainty", "certain");
							flefGroupJunction.put("credibility", 3);
						}
					}

					if(wife != null){
						final Map<String, Object> flefGroupJunction = new HashMap<>();
						flefGroupJunctions.add(flefGroupJunction);
						flefGroupJunction.put("id", flefGroupJunctions.size());
						flefGroupJunction.put("group_id", marriage.id);
						flefGroupJunction.put("reference_table", "person");
						flefGroupJunction.put("reference_id", wife.id);
						flefGroupJunction.put("role", "partner");
						if(verifiedMarriage){
							flefGroupJunction.put("certainty", "certain");
							flefGroupJunction.put("credibility", 3);
						}
					}
				}
			}
		}
	}

	private static void transferRepositories(final Map<String, List<Map<String, Object>>> output, final List<GedcomObject> repositories){
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

	private static void transferNotes(final Map<String, List<Map<String, Object>>> output, final List<GedcomObject> notes,
		final List<String> lines){
		final List<Map<String, Object>> flefNotes = output.computeIfAbsent("note", k -> new ArrayList<>());

		for(final GedcomObject note : notes){
			final String noteID = ".NOTE[" + note.id + "]";
			final List<Integer> personIDs = new ArrayList<>();
			for(final String line : lines)
				if(line.contains(noteID))
					personIDs.add(extractID(line));

			final Map<String, String> attributes = note.attributes;

			final String text = attributes.get("CONT");
			for(final Integer personID : personIDs){
				final Map<String, Object> flefNote = new HashMap<>();
				flefNotes.add(flefNote);
				flefNote.put("id", note.id);
				flefNote.put("note", text);
				flefNote.put("reference_table", "person");
				flefNote.put("reference_id", personID);
			}
		}
	}

	private static void transferSources(final Map<String, List<Map<String, Object>>> output, final List<GedcomObject> sources){
		final List<Map<String, Object>> flefSources = output.computeIfAbsent("source", k -> new ArrayList<>());
		final List<Map<String, Object>> flefNotes = output.computeIfAbsent("note", k -> new ArrayList<>());
		final List<Map<String, Object>> flefCitations = output.computeIfAbsent("citation", k -> new ArrayList<>());
		final List<Map<String, Object>> flefLocalizedTexts = output.computeIfAbsent("localized_text", k -> new ArrayList<>());
		final List<Map<String, Object>> flefMedias = output.computeIfAbsent("media", k -> new ArrayList<>());
		final List<Map<String, Object>> flefHistoricDates = output.computeIfAbsent("historic_date", k -> new ArrayList<>());

		for(final GedcomObject source : sources){
			final Map<String, GedcomObject> children = source.children;
			final Map<String, String> attributes = source.attributes;

			final Integer repoID = extractID(attributes.get("REPO"));
			final String auth = attributes.get("AUTH");
			final String titl = attributes.get("TITL");
			final Map<String, Object> flefSource = new HashMap<>();
			flefSources.add(flefSource);
			flefSource.put("id", source.id);
			flefSource.put("identifier", titl);
			//FIXME fix null
			flefSource.put("source_type", null);
			flefSource.put("author", auth);
			//FIXME fix null
			flefSource.put("place_id", null);
			//FIXME fix null
			flefSource.put("date_id", null);
			flefSource.put("repository_id", repoID);
			//FIXME fix null
			flefSource.put("location", null);

			final List<GedcomObject> medias = getAllStartingWith(children, "OBJE");
			for(final GedcomObject media : medias){
				final Map<String, String> mediaAttributes = media.attributes;

				final String title = mediaAttributes.get("TITL");
				final String date = mediaAttributes.get("_DATE");
				final String identifier = mediaAttributes.get("FILE");

				final int dateID = flefHistoricDates.size() + 1;
				if(date != null){
					final Map<String, Object> flefHistoricDate = new HashMap<>();
					flefHistoricDates.add(flefHistoricDate);
					flefHistoricDate.put("id", dateID);
					flefHistoricDate.put("date", date);
				}
				final Map<String, Object> flefMedia = new HashMap<>();
				flefMedias.add(flefMedia);
				flefMedia.put("id", media.id);
				flefMedia.put("identifier", identifier);
				flefMedia.put("title", title);
				final boolean isImage = (identifier.endsWith(".jpg") || identifier.endsWith(".psd") || identifier.endsWith(".png")
					|| identifier.endsWith(".bmp") || identifier.endsWith(".gif") || identifier.endsWith(".tif"));
				flefMedia.put("type", (isImage
					? "photo":
					(identifier.endsWith(".mp4")
						? "video"
						: "TODO see identifier")));
				if(isImage)
					flefMedia.put("image_projection", "rectangular");
				if(date != null)
					flefMedia.put("date_id", dateID);
			}

			final List<GedcomObject> notes = getAllStartingWith(children, "NOTE");
			if(notes.size() > 1)
				for(final GedcomObject note : notes){
					final Map<String, String> noteAttributes = note.attributes;

					final String text = noteAttributes.get("DESC");
					final Map<String, Object> flefNote = new HashMap<>();
					flefNotes.add(flefNote);
					flefNote.put("id", note.id);
					flefNote.put("note", text);
					flefNote.put("reference_table", "source");
					flefNote.put("reference_id", source.id);
				}

			final GedcomObject citation = getFirstStartingWith(children, "TEXT");
			if(citation != null){
				final String flefLocation = (notes.size() == 1? notes.getFirst().attributes.get("DESC"): "TODO see notes");
				final Map<String, Object> flefCitation = new HashMap<>();
				flefCitations.add(flefCitation);
				flefCitation.put("id", citation.id);
				flefCitation.put("source_id", source.id);
				flefCitation.put("location", flefLocation);
				flefCitation.put("extract_id", citation.id);
				flefCitation.put("extract_type", "transcript");

				String flefText = citation.attributes.get("DESC");
				String body = citation.attributes.get("CONT");
				if(body != null)
					flefText += "\\r\\n" + body;
				final Map<String, Object> flefLocalizedText = new HashMap<>();
				flefLocalizedTexts.add(flefLocalizedText);
				flefLocalizedText.put("id", citation.id);
				flefLocalizedText.put("text", flefText);
				flefLocalizedText.put("locale", "TODO see text");
				flefLocalizedText.put("type", "original");
			}
		}
	}

	//TODO assertions, place, historic_place, localized_text_junction, media_junction, person, person_name, event
	private static void transferPersons(final Map<String, List<Map<String, Object>>> output, final List<GedcomObject> persons){
		final List<Map<String, Object>> flefSources = output.computeIfAbsent("person", k -> new ArrayList<>());

		for(final GedcomObject person : persons){
			final Map<String, GedcomObject> children = person.children;
			final Map<String, String> attributes = person.attributes;

			//TODO
		}
	}


	private static String extractType(final String text){
		return (text != null
			? text.substring(0, text.indexOf('['))
			: null);
	}

	private static Integer extractID(final String text){
		return (text != null
			? Integer.valueOf(text.substring(text.indexOf('[') + 1, text.indexOf(']')))
			: null);
	}

	private static GedcomObject getFirstStartingWith(final Map<String, GedcomObject> children, final String tag){
		for(final GedcomObject child : children.values())
			if(child.type.equals(tag))
				return child;
		return null;
	}

	private static List<GedcomObject> getAllStartingWith(final Map<String, GedcomObject> children, final String tag){
		final List<GedcomObject> output = new ArrayList<>();
		for(final GedcomObject child : children.values())
			if(child.type.equals(tag))
				output.add(child);
		return output;
	}

	private static Map<String, List<GedcomObject>> createFirstLevelMap(final GedcomObject root){
		final Map<String, List<GedcomObject>> firstLevelMap = new HashMap<>();
		for(final Map.Entry<String, GedcomObject> entry : root.children.entrySet()){
			final String key = extractType(entry.getKey());
			firstLevelMap.computeIfAbsent(key, k -> new ArrayList<>())
				.add(entry.getValue());
		}
		return firstLevelMap;
	}

}
