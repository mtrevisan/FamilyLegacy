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
import io.github.mtrevisan.familylegacy.flef.helpers.parsers.CalendarParserBuilder;
import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordAuthor;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCoordinate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCoordinateSystem;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCopyright;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCreationDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordDescription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordExtract;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordExtractLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordExtractType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordLocation;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordNote;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordPhotoProjection;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordProtocolName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordProtocolVersion;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordRole;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordTitle;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordType;


public final class Main{

	private static final String JDBC_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
	private static final String USER = "sa";
	private static final String PASSWORD = "";

	private static final List<String> REMOVAL_TAGS = Arrays.asList("SUBN", ".CHAN ", ".CHAN.DATE ", ".CHAN.DATE.TIME ", ".ADDR.", ".CONC ",
		".FORM ", "._PUBL ", "._CUT Y", ".NAME //");

	private static final List<String> SUFFIXES = Arrays.asList(".NAME ", ".EDUC ", ".EVEN ", ".OCCU ", ".SEX ", ".PLAC ", ".NOTE ", ".SSN ",
		".DSCR ", ".RELI ", ".PROP ", ".NCHI ", ".TEXT ");

	private static final Collection<String> SUFFIXES_Y = new HashSet<>(Arrays.asList("BIRT Y", "CHR Y", "DEAT Y", "BURI Y", "CREM Y",
		"ADOP Y", "BAPM Y", "BARM Y", "BASM Y", "BLES Y", "CHRA Y", "CONF Y", "FCOM Y", "ORDN Y", "NATU Y", "EMIG Y", "IMMI Y", "CENS Y",
		"PROB Y", "WILL Y", "GRAD Y", "RETI Y", "ANUL Y", "DIV Y", "DIVF Y", "ENGA Y", "MARB Y", "MARC Y", "MARR Y", "MARL Y", "MARS Y",
		"RESI Y"));

	private static final String[] IDENTIFIERS = {"SEX", "MARR", "MARL", "MARB", "MARS", "BIRT", "DEAT", "CREM", "BURI", "RETI", "RESI",
		"RELI", "ENGA", "DSCR", "SSN", "EMIG", "IMMI", "ADOP", "PROP", "NATU", "CENS", "GRAD", "ORDN", "NCHI", "WILL", "DIV"};
	private static final Pattern PATTERN = Pattern.compile("@@");
	private static final Pattern REGEX = Pattern.compile(".*\\[\\d+]$");
	private static final Pattern REGEXP = Pattern.compile("SOUR\\[\\d+]\\.REPO\\[\\d+]");
	private static final Pattern PATTERN1 = Pattern.compile("\\s+@[A-Z](\\d+)@");
	private static final Pattern PATTERN2 = Pattern.compile("[^\\d]");
	private static final Pattern PATTERN3 = Pattern.compile("\\d\\s+[A-Z]+\\s+@[A-Z]\\d+@");
	private static final Pattern PATTERN4 = Pattern.compile("[A-Z]+\\s+@[A-Z]\\d+@");

	private Main(){
	}

	private static class GedcomObject{
		String type;
		int id;
		final Map<String, GedcomObject> children = new HashMap<>();
		final Map<String, String> attributes = new HashMap<>();
	}



	public static void main(final String[] args) throws URISyntaxException, GedcomGrammarException, GedcomDataException, SQLException,
			IOException{
//		final GedcomFileParser gedcomParser = new GedcomFileParser();
//		gedcomParser.load("/gedg/gedcom_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged");

//		final SQLFileParser sqlParser = new SQLFileParser();
//		final String grammarFile = "src/main/resources/gedg/treebard/FLeF.sql";
//		sqlParser.load(grammarFile, "src/main/resources/gedg/treebard/FLeF.data");

//		final DatabaseManager dbManager = new DatabaseManager(JDBC_URL, USER, PASSWORD);
//		dbManager.initialize(grammarFile);

		final String gedcomFilename = "ged/TGMZ.ged";
		final String flatGedcomFilename = "ged/TGMZ.txt";
		final boolean flat = false;

		final List<String> lines;
		if(flat){
			final List<String> gedcomLines = readFile(gedcomFilename);
			lines = flatGedcom(gedcomLines, flatGedcomFilename);
		}
		else
			lines = readFile(flatGedcomFilename);
		final GedcomObject root = extractor(lines);
		final Map<String, List<Map<String, Object>>> tables = transfer(root, lines);
	}

	private static List<String> flatGedcom(final List<String> lines, final String flatGedcomFilename) throws URISyntaxException,
			IOException{
		lines.replaceAll(s -> PATTERN.matcher(s).replaceAll("@"));

		final List<String> output = new ArrayList<>();

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
				final String[] lineComponents = line.split(" ", 4);
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
					if(PATTERN4.matcher(line).matches()){
						final String number = PATTERN2.matcher(line).replaceAll("");
						line = PATTERN1.matcher(line).replaceAll("[" + number + "]");
					}
					if(line.startsWith("CONC ")){
						final int prevIndex = output.size() - 1;
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
					if(PATTERN3.matcher(line).matches()){
						final String number = PATTERN2.matcher(line.substring(2)).replaceAll("");
						line = PATTERN1.matcher(line).replaceAll("[" + number + "]");
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
			final String line = output.get(i);
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
				final String prevLine = output.get(i - 1);
				if(prevLine.endsWith(".ADDR")){
					final StringJoiner sj = new StringJoiner(", ");
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
				final StringBuilder prevLine = new StringBuilder(output.get(i - 1));
				int j = i;
				while(j < output.size() && output.get(j).contains(".CONC ")){
					prevLine.append(output.get(j).split(" ", 2)[1]);

					j ++;
				}
				output.set(i - 1, prevLine.toString());
			}
		}

		output.removeIf(line -> REMOVAL_TAGS.stream().anyMatch(line::contains));

		for(int i = 0; i < output.size(); i ++){
			final String line = output.get(i);

			if(SUFFIXES.stream().anyMatch(line::contains)){
				final String[] components = line.split(" ", 2);
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
			String replaced = s;
			if(REGEXP.matcher(s).matches()){
				final int cutIndex = s.lastIndexOf('[');
				final String pre = s.substring(0, cutIndex);
				final String repoID = s.substring(cutIndex + 1, s.length() - 1);
				replaced = pre + "=REPO[" + repoID + "]";
			}
			return replaced;
		});

		if(flatGedcomFilename != null){
			final Path resourceFolder = Paths.get("src/main/resources/");
			final Path filePath = resourceFolder.resolve(flatGedcomFilename);
			Files.write(filePath, output);
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

	private static GedcomObject extractor(final Iterable<String> lines){
		final GedcomObject root = new GedcomObject();
		final Map<String, GedcomObject> context = new HashMap<>();
		context.put(StringUtils.EMPTY, root);

		for(String line : lines){
			line = line.trim();

			if(!line.contains("=") && REGEX.matcher(line).matches()){
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
					if(!"CONT".equals(attributeKey))
						throw new IllegalArgumentException("error while squashing " + attributeKey + ": not managed (yet)");

					parts[1] = parent.attributes.get(attributeKey) + "\r\n" + parts[1];
				}
				parent.attributes.put(attributeKey, parts[1]);
			}
		}
		return root;
	}

	private static Map<String, List<Map<String, Object>>> transfer(final GedcomObject root, final Iterable<String> lines){
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
//		insertRecordID(flefProject, 1);
		insertRecordProtocolName(flefProject, EntityManager.PROTOCOL_NAME_DEFAULT);
		insertRecordProtocolVersion(flefProject, EntityManager.PROTOCOL_VERSION_DEFAULT);
		insertRecordCopyright(flefProject, "(c) 2024 Mauro Trevisan");
		insertRecordCreationDate(flefProject, EntityManager.now());
	}

	private static void transferCalendar(final Map<String, List<Map<String, Object>>> output){
		final List<Map<String, Object>> flefCalendars = output.computeIfAbsent("calendar", k -> new ArrayList<>());

		Map<String, Object> flefCalendar = new HashMap<>();
		flefCalendars.add(flefCalendar);
//		insertRecordID(flefCalendar, 1);
		insertRecordType(flefCalendar, CalendarParserBuilder.CALENDAR_GREGORIAN);

		flefCalendar = new HashMap<>();
		flefCalendars.add(flefCalendar);
//		insertRecordID(flefCalendar, 2);
		insertRecordType(flefCalendar, CalendarParserBuilder.CALENDAR_JULIAN);

		flefCalendar = new HashMap<>();
		flefCalendars.add(flefCalendar);
//		insertRecordID(flefCalendar, 3);
		insertRecordType(flefCalendar, CalendarParserBuilder.CALENDAR_VENETAN);

		flefCalendar = new HashMap<>();
		flefCalendars.add(flefCalendar);
//		insertRecordID(flefCalendar, 4);
		insertRecordType(flefCalendar, CalendarParserBuilder.CALENDAR_FRENCH_REPUBLICAN);

		flefCalendar = new HashMap<>();
		flefCalendars.add(flefCalendar);
//		insertRecordID(flefCalendar, 5);
		insertRecordType(flefCalendar, CalendarParserBuilder.CALENDAR_HEBREW);
	}

	private static void transferCulturalNorm(final Map<String, List<Map<String, Object>>> output){
		final List<Map<String, Object>> flefCulturalNorms = output.computeIfAbsent("cultural_norm", k -> new ArrayList<>());
		final List<Map<String, Object>> flefHistoricDates = output.computeIfAbsent("historic_date", k -> new ArrayList<>());
		final List<Map<String, Object>> flefPlaces = output.computeIfAbsent("place", k -> new ArrayList<>());
		final List<Map<String, Object>> flefNotes = output.computeIfAbsent("note", k -> new ArrayList<>());

		final Map<String, Object> flefPlace = new HashMap<>();
		flefPlaces.add(flefPlace);
		final int placeID = flefPlaces.size();
//		insertRecordID(flefPlace, placeID);
		insertRecordIdentifier(flefPlace, "Regno Lombardo-Veneto");
		insertRecordName(flefPlace, "Regno Lombardo-Veneto");
		insertRecordLocale(flefPlace, "it-IT");
		insertRecordType(flefPlace, "reign");

		Map<String, Object> flefHistoricDate = new HashMap<>();
		flefHistoricDates.add(flefHistoricDate);
		final int dateStartID = flefHistoricDates.size();
//		insertRecordID(flefHistoricDate, dateStartID);
		insertRecordDate(flefHistoricDate, "31 JAN 1807");
		flefHistoricDate = new HashMap<>();
		flefHistoricDates.add(flefHistoricDate);
		final int dateEndID = flefHistoricDates.size();
//		insertRecordID(flefHistoricDate, dateEndID);
		insertRecordDate(flefHistoricDate, "19 FEB 1811");

		Map<String, Object> flefCulturalNorm = new HashMap<>();
		flefCulturalNorms.add(flefCulturalNorm);
//		insertRecordID(flefCulturalNorm, 1);
		insertRecordIdentifier(flefCulturalNorm, "nomi latini");
		insertRecordDescription(flefCulturalNorm, "per i nomi in latino si deve usare il nominativo (quello che generalmente finisce in *-us* per il maschile e *-a* per il femminile)");
		insertRecordCertainty(flefCulturalNorm, "certain");
		insertRecordCredibility(flefCulturalNorm, "direct and primary evidence used, or by dominance of the evidence");

		flefCulturalNorm = new HashMap<>();
		flefCulturalNorms.add(flefCulturalNorm);
//		insertRecordID(flefCulturalNorm, 2);
		insertRecordIdentifier(flefCulturalNorm, "napoleonic code age of majority in men");
		insertRecordDescription(flefCulturalNorm, "23 anni minore, 29 anni maggiore");
//		insertRecordPlaceID(flefCulturalNorm, placeID);
//		insertRecordDateStartID(flefCulturalNorm, dateStartID);
//		insertRecordDateEndID(flefCulturalNorm, dateEndID);
		insertRecordCertainty(flefCulturalNorm, "certain");
		insertRecordCredibility(flefCulturalNorm, "direct and primary evidence used, or by dominance of the evidence");

		flefCulturalNorm = new HashMap<>();
		flefCulturalNorms.add(flefCulturalNorm);
//		insertRecordID(flefCulturalNorm, 3);
		insertRecordIdentifier(flefCulturalNorm, "napoleonic code respectful act for men");
		insertRecordDescription(flefCulturalNorm, "fino ai 30");
//		insertRecordPlaceID(flefCulturalNorm, placeID);
//		insertRecordDateStartID(flefCulturalNorm, dateStartID);
//		insertRecordDateEndID(flefCulturalNorm, dateEndID);
		insertRecordCertainty(flefCulturalNorm, "certain");
		insertRecordCredibility(flefCulturalNorm, "direct and primary evidence used, or by dominance of the evidence");

		flefCulturalNorm = new HashMap<>();
		flefCulturalNorms.add(flefCulturalNorm);
//		insertRecordID(flefCulturalNorm, 4);
		insertRecordIdentifier(flefCulturalNorm, "napoleonic code age of majority in women");
		insertRecordDescription(flefCulturalNorm, "22 anni minore");
//		insertRecordPlaceID(flefCulturalNorm, placeID);
//		insertRecordDateStartID(flefCulturalNorm, dateStartID);
//		insertRecordDateEndID(flefCulturalNorm, dateEndID);
		insertRecordCertainty(flefCulturalNorm, "certain");
		insertRecordCredibility(flefCulturalNorm, "direct and primary evidence used, or by dominance of the evidence");

		flefCulturalNorm = new HashMap<>();
		flefCulturalNorms.add(flefCulturalNorm);
//		insertRecordID(flefCulturalNorm, 5);
		insertRecordIdentifier(flefCulturalNorm, "napoleonic code respectful act for women");
		insertRecordDescription(flefCulturalNorm, "fino ai 25");
//		insertRecordPlaceID(flefCulturalNorm, placeID);
//		insertRecordDateStartID(flefCulturalNorm, dateStartID);
//		insertRecordDateEndID(flefCulturalNorm, dateEndID);
		insertRecordCertainty(flefCulturalNorm, "certain");
		insertRecordCredibility(flefCulturalNorm, "direct and primary evidence used, or by dominance of the evidence");

		flefHistoricDates.add(flefHistoricDate);
		final int concilioDateStartID = flefHistoricDates.size();
//		insertRecordID(flefHistoricDate, concilioDateStartID);
		insertRecordDate(flefHistoricDate, "11 NOV 1215");

		flefHistoricDates.add(flefHistoricDate);
		final int codiceDirittoCanonicoStartID = flefHistoricDates.size();
//		insertRecordID(flefHistoricDate, codiceDirittoCanonicoStartID);
		insertRecordDate(flefHistoricDate, "1917");

		flefCulturalNorm = new HashMap<>();
		flefCulturalNorms.add(flefCulturalNorm);
//		insertRecordID(flefCulturalNorm, 6);
		insertRecordIdentifier(flefCulturalNorm, "impedimento di diritto ecclesiastico");
		insertRecordDescription(flefCulturalNorm, "il Concilio Lateranense IV nel 1215 ridusse l’impedimento al quarto grado della linea collaterale");
//		insertRecordDateStartID(flefCulturalNorm, concilioDateStartID);
//		insertRecordDateEndID(flefCulturalNorm, codiceDirittoCanonicoStartID);
		insertRecordCertainty(flefCulturalNorm, "certain");
		insertRecordCredibility(flefCulturalNorm, "direct and primary evidence used, or by dominance of the evidence");

		flefCulturalNorm = new HashMap<>();
		flefCulturalNorms.add(flefCulturalNorm);
//		insertRecordID(flefCulturalNorm, 7);
		insertRecordIdentifier(flefCulturalNorm, "impedimento di diritto ecclesiastico");
		insertRecordDescription(flefCulturalNorm, "can. 1076 - In linea retta di consanguineità legittima o naturale è sempre nullo il Matrimonio. In linea collaterale fino al terzo, moltiplicandosi l’impedimento con lo stipite. Nel dubbio di consanguineità in linea retta o del primo collaterale non si permetterà il Matrimonio.\\r\\ncan. 1077 - L’affinità in linea retta irrita il Matrimonio in tutti i gradi; nella collaterale fino al secondo compreso. Si moltiplica con la consanguineità da cui procede, con la ripetizione successiva del Matrimonio con un consanguineo del defunto.");
//		insertRecordDateStartID(flefCulturalNorm, codiceDirittoCanonicoStartID);
		insertRecordCertainty(flefCulturalNorm, "certain");
		insertRecordCredibility(flefCulturalNorm, "direct and primary evidence used, or by dominance of the evidence");

		//https://www.voxcanonica.com/2022/12/05/limpedimento-di-affinita-can-1092/
		//https://dadun.unav.edu/bitstream/10171/6811/1/85-08.Pellegrino.pdf
		//https://www.studiolegalecotini.it/matrimonio-canonico-impedimenti.htm
		//È vietato, pertanto, il matrimonio tra gli ascendenti e i discendenti in linea retta, sia legittimi che naturali (es.: tra padre e figlia, tra madre e figlio, tra nonno e nipote, ecc.); mentre in linea collaterale è vietato solo fino al quarto grado incluso (es.: 1° tra fratello e sorella, 2° tra zio e nipote, 3° tra zio e pronipote, 3° tra primi cugini).
		// il grado di consanguineità va computato qui in rapporto all'unico principio da cui entrambi derivano. Su questo però la computazione della legge ecclesiastica differisce da quella civile: poiché quella civile somma i gradi di discendenza dal ceppo comune di entrambe le parti; quella ecclesiastica invece conta quelli di una parte soltanto, cioè di quella in cui si riscontrano più numerosi gradi. Perciò secondo il computo della legge civile fratello e sorella, o due fratelli, sono consanguinei in secondo grado. Invece secondo il computo ecclesiastico due fratelli sono consanguinei in primo grado: perché nessuno dei due dista dalla radice comune più di un grado. Invece il figlio di un fratello dista dal fratello di suo padre in secondo grado: perché tanti sono i gradi che li dividono dalla radice comune. Perciò secondo il computo ecclesiastico quanti sono i gradi che separano una persona da un ascendente comune, tanta è la distanza che la separa da qualsiasi discendente, e mai può essere minore; poiché "la causa è sempre superiore all'effetto". Sebbene, quindi, gli altri che discendono da un capostipite comune siano consanguinei di una data persona in forza di codesto capostipite, non possono essere vicini a chi ne discende in altro modo, più di quanto codesto discendente è vicino al capostipite stesso. Talora invece la distanza maggiore nella parentela può essere da parte degli altri; i quali forse distano dal capostipite comune più dell'interessato; ma la consanguineità va sempre computata dalla maggiore distanza.
		/*Can. 108 - §1. La consanguineità si computa per linee e per gradi.
		§2. Nella linea retta tanti sono i gradi quante le generazioni, ossia quante le persone, tolto il capostipite.
		§3. Nella linea obliqua tanti sono i gradi quante le persone in tutte e due le linee insieme, tolto il capostipite.*/
		//Metodo per calcolare i gradi di parentela: in linea retta si cominciano a contare le generazioni dal soggetto del quale si vuole conoscere il grado di parentela fino al capostipite, escludendo quest ultimo. In linea collaterale si risale da uno dei parenti fino allo stipite comune e da questo si discende all’altro parente, senza tener conto dello stipite
		final Map<String, Object> flefNote = new HashMap<>();
		flefNotes.add(flefNote);
//		insertRecordID(flefNote, 20_000);
		insertRecordNote(flefNote, "Codice di Diritto Canonico 1917\\r\\nhttps://www.ecclesiadei.it/wp-content/uploads/2021/01/Codice-Diritto-Canonico-1917.pdf\\r\\npag. 102");
flefNote.put("reference_table", "cultural_norm");
flefNote.put("reference_id", 7);

		//età matrimonio
		//http://www.appuntigiurisprudenza.it/diritto-ecclesiastico/gli-impedimenti-al-matrimonio-canonico-nello-specifico.html
	}

	private static void transferGroups(final Map<String, List<Map<String, Object>>> output, final Iterable<GedcomObject> groups){
		final List<Map<String, Object>> flefGroups = output.computeIfAbsent("group", k -> new ArrayList<>());
		final List<Map<String, Object>> flefGroupJunctions = output.computeIfAbsent("group_junction", k -> new ArrayList<>());
		final List<Map<String, Object>> flefNotes = output.computeIfAbsent("note", k -> new ArrayList<>());
		final List<Map<String, Object>> flefAssertions = output.computeIfAbsent(EntityManager.NODE_ASSERTION, k -> new ArrayList<>());
		final List<Map<String, Object>> flefCitations = output.computeIfAbsent("citation", k -> new ArrayList<>());
		final List<Map<String, Object>> flefHistoricDates = output.computeIfAbsent("historic_date", k -> new ArrayList<>());
		final List<Map<String, Object>> flefEvents = output.computeIfAbsent("event", k -> new ArrayList<>());
		final List<Map<String, Object>> flefPlaces = output.computeIfAbsent("place", k -> new ArrayList<>());
		final List<Map<String, Object>> flefCulturalNormJunctions = output.computeIfAbsent("cultural_norm_junction", k -> new ArrayList<>());

		for(final GedcomObject group : groups){
			final Map<String, GedcomObject> children = group.children;

			final List<GedcomObject> marriageChildrenPerson = getAllStartingWith(children, "CHIL");
			final GedcomObject marriageChildrenCount = getFirstStartingWith(children, "NCHI");
			if(marriageChildrenCount != null){
				final String count = extractDesc(marriageChildrenCount.attributes);
				if(count != null && Integer.parseInt(count) != marriageChildrenPerson.size()){
					final int childrenCount = Integer.parseInt(count) - marriageChildrenPerson.size();
					if(childrenCount < 0)
						throw new IllegalArgumentException("Cannot have a negative count children");
					else
						System.out.println("TODO: Add " + childrenCount + " more children to family " + group.id);
				}
			}

			final GedcomObject husband = getFirstStartingWith(children, "HUSB");
			final GedcomObject wife = getFirstStartingWith(children, "WIFE");
			final List<GedcomObject> marriages = getAllStartingWith(children, "MARR");
			for(final GedcomObject marriage : marriages){
				final Map<String, GedcomObject> marriageChildren = marriage.children;
				String marriageDate = marriage.attributes.get("DATE");
				final List<GedcomObject> marriageNotes = getAllStartingWith(marriageChildren, "NOTE");
				for(final GedcomObject note : marriageNotes){
					final String desc = extractDesc(note.attributes);
					if(desc != null && desc.startsWith("ore ")){
						//add to marriage date
						marriageDate = (marriageDate != null? marriageDate + " ": "") + desc.substring("ore ".length());
						break;
					}
				}
				final List<GedcomObject> marriageSources = getAllStartingWith(marriageChildren, "SOUR");
				final GedcomObject marriagePlace = getFirstStartingWith(marriageChildren, "PLAC");

				//add date
				Integer marriageDateID = null;
				if(marriageDate != null){
					//create date
					final Map<String, Object> flefHistoricDate = new HashMap<>();
					flefHistoricDates.add(flefHistoricDate);
					marriageDateID = flefHistoricDates.size();
//					insertRecordID(flefHistoricDate, marriageDateID);
					insertRecordDate(flefHistoricDate, marriageDate);
				}

				//add place
				if(marriagePlace != null){
					final String marriagePlaceName = extractDesc(marriagePlace.attributes);
					final GedcomObject marriagePlaceMap = getFirstStartingWith(marriagePlace.children, "MAP");
					String coordinate = null;
					if(marriagePlaceMap != null){
						final String lat = marriagePlaceMap.attributes.get("LATI");
						final String lon = marriagePlaceMap.attributes.get("LONG");
						if(lat != null && lon != null)
							coordinate = (lat.replace("N", "").replace('S', '-') + ", "
								+ lon.replace("E", "").replace('W', '-'));
					}

					final Map<String, Object> flefPlace = new HashMap<>();
					flefPlaces.add(flefPlace);
//					insertRecordID(flefPlace, marriagePlace.id);
					if(marriagePlaceName != null)
						insertRecordIdentifier(flefPlace, marriagePlaceName);
					insertRecordName(flefPlace, marriagePlaceName);
					if(coordinate != null){
						insertRecordCoordinate(flefPlace, coordinate);
						insertRecordCoordinateSystem(flefPlace, "WGS84");
					}


					final GedcomObject marriagePlaceSource = getFirstStartingWith(marriagePlace.children, "SOUR");
					if(marriagePlaceSource != null){
						final String marriagePlaceSourceLocation = marriagePlaceSource.attributes.get("PAGE");
						final String marriagePlaceSourceCredibility = marriagePlaceSource.attributes.get("QUAY");

						//create source
						final GedcomObject data = getFirstStartingWith(marriagePlaceSource.children, "DATA");
						//FIXME
						final String date = (data != null? data.attributes.get("DATE"): null);
						final GedcomObject even = getFirstStartingWith(marriagePlaceSource.children, "EVEN");
						//FIXME
						final String evenType = (even != null? extractDesc(even.attributes): null);
						final String evenRole = (even != null? even.attributes.get("ROLE"): null);

						//create citation
						final Map<String, Object> flefCitation = new HashMap<>();
						flefCitations.add(flefCitation);
						final int citationID = flefCitations.size();
//						insertRecordID(flefCitation, citationID);
//						insertRecordSourceID(flefCitation, marriagePlaceSource.id);
						if(marriagePlaceSourceLocation != null)
							insertRecordLocation(flefCitation, marriagePlaceSourceLocation);

						//create assertion connected to a date
						if(marriageDateID != null){
							final Map<String, Object> flefAssertionDate = new HashMap<>();
							flefAssertions.add(flefAssertionDate);
//							insertRecordID(flefAssertionDate, flefAssertions.size());
//							insertRecordCitationID(flefAssertionDate, citationID);
							flefAssertionDate.put("reference_table", "historic_date");
							flefAssertionDate.put("reference_id", marriageDateID);
							if(evenRole != null)
								insertRecordRole(flefAssertionDate, evenRole);
							if(marriagePlaceSourceCredibility != null){
								insertRecordCertainty(flefAssertionDate, "certain");
								insertRecordCredibility(flefAssertionDate, marriagePlaceSourceCredibility);
							}
						}

						//create assertion connected to a place
						final Map<String, Object> flefAssertionPlace = new HashMap<>();
						flefAssertions.add(flefAssertionPlace);
//						insertRecordID(flefAssertionPlace, flefAssertions.size());
//						insertRecordCitationID(flefAssertionPlace, citationID);
						flefAssertionPlace.put("reference_table", "place");
						flefAssertionPlace.put("reference_id", marriagePlace.id);
						if(evenRole != null)
							insertRecordRole(flefAssertionPlace, evenRole);
						if(marriagePlaceSourceCredibility != null){
							insertRecordCertainty(flefAssertionPlace, "certain");
							insertRecordCredibility(flefAssertionPlace, marriagePlaceSourceCredibility);
						}
					}
				}

				for(final GedcomObject note : marriageNotes){
					final String desc = extractDesc(note.attributes);
					if(desc != null && !desc.startsWith("ore ")){
						//add event
						final Map<String, Object> flefEvent = new HashMap<>();
						flefEvents.add(flefEvent);
						final int eventID = flefEvents.size();
//						insertRecordID(flefEvent, eventID);
						insertRecordType(flefEvent, "marriage");
//						if(marriagePlace != null)
//							insertRecordPlaceID(flefEvent, marriagePlace.id);
//						if(marriageDateID != null)
//							insertRecordDateID(flefEvent, marriageDateID);
						flefEvent.put("reference_table", "group");
						flefEvent.put("reference_id", marriage.id);

						//add note
						final Map<String, Object> flefNote = new HashMap<>();
						flefNotes.add(flefNote);
//						insertRecordID(flefNote, note.id);
						insertRecordNote(flefNote, desc);
						flefNote.put("reference_table", "event");
						flefNote.put("reference_id", eventID);
						if("dispensati dall'impedimento dirimente di consanguineità in 3° grado eguale linea collaterale".equals(desc)){
							final Map<String, Object> flefCulturalNormJunction = new HashMap<>();
							flefCulturalNormJunctions.add(flefCulturalNormJunction);
//							insertRecordID(flefCulturalNormJunction, flefCulturalNormJunctions.size());
//							insertRecordCulturalNormID(flefCulturalNormJunction, 7);
							flefCulturalNormJunction.put("reference_table", "note");
							flefCulturalNormJunction.put("reference_id", note.id);
							insertRecordCertainty(flefCulturalNormJunction, "certain");
							insertRecordCredibility(flefCulturalNormJunction, "direct and primary evidence used, or by dominance of the evidence");
						}
						else if(desc.contains("dispensa")){
							final Map<String, Object> flefCulturalNormJunction = new HashMap<>();
							flefCulturalNormJunctions.add(flefCulturalNormJunction);
//							insertRecordID(flefCulturalNormJunction, flefCulturalNormJunctions.size());
//							insertRecordCulturalNormID(flefCulturalNormJunction, 6);
							flefCulturalNormJunction.put("reference_table", "note");
							flefCulturalNormJunction.put("reference_id", note.id);
							insertRecordCertainty(flefCulturalNormJunction, "certain");
							insertRecordCredibility(flefCulturalNormJunction, "direct and primary evidence used, or by dominance of the evidence");
						}
					}
				}

				for(final GedcomObject marriageSource : marriageSources){
					final GedcomObject marriageSourceEven = getFirstStartingWith(marriageSource.children, "EVEN");
					final String evenRole = (marriageSourceEven != null? marriageSourceEven.attributes.get("ROLE"): null);

					if(marriagePlace != null){
						//create citation
						final Map<String, Object> flefCitation = new HashMap<>();
						flefCitations.add(flefCitation);
						final int citationID = flefCitations.size();
//						insertRecordID(flefCitation, citationID);
//						insertRecordSourceID(flefCitation, marriageSource.id);

						//create assertion connected to a place
						final Map<String, Object> flefAssertion = new HashMap<>();
						flefAssertions.add(flefAssertion);
//						insertRecordID(flefAssertion, flefAssertions.size());
//						insertRecordCitationID(flefAssertion, citationID);
						flefAssertion.put("reference_table", "place");
						flefAssertion.put("reference_id", marriagePlace.id);
						if(evenRole != null)
							insertRecordRole(flefAssertion, evenRole);
						insertRecordCertainty(flefAssertion, "certain");
						insertRecordCredibility(flefAssertion, "direct and primary evidence used, or by dominance of the evidence");
					}

					if(marriageDate != null){
						//create citation
						final Map<String, Object> flefCitation = new HashMap<>();
						flefCitations.add(flefCitation);
						final int citationID = flefCitations.size();
//						insertRecordID(flefCitation, citationID);
//						insertRecordSourceID(flefCitation, marriageSource.id);

						//create assertion connected to a date
						final Map<String, Object> flefAssertion = new HashMap<>();
						flefAssertions.add(flefAssertion);
//						insertRecordID(flefAssertion, flefAssertions.size());
//						insertRecordCitationID(flefAssertion, citationID);
						flefAssertion.put("reference_table", "historic_date");
						flefAssertion.put("reference_id", marriageDateID);
						if(evenRole != null)
							insertRecordRole(flefAssertion, evenRole);
						insertRecordCertainty(flefAssertion, "certain");
						insertRecordCredibility(flefAssertion, "direct and primary evidence used, or by dominance of the evidence");
					}
				}

				final boolean verifiedMarriage = !marriageSources.isEmpty();

				if(husband != null || wife != null){
					final Map<String, Object> flefGroup = new HashMap<>();
					flefGroups.add(flefGroup);
//					insertRecordID(flefGroup, marriage.id);
					insertRecordType(flefGroup, "family");

					if(husband != null){
						final Map<String, Object> flefGroupJunction = new HashMap<>();
						flefGroupJunctions.add(flefGroupJunction);
//						insertRecordID(flefGroupJunction, flefGroupJunctions.size());
//						insertRecordGroupID(flefGroupJunction, marriage.id);
						flefGroupJunction.put("reference_table", "person");
						flefGroupJunction.put("reference_id", husband.id);
						insertRecordRole(flefGroupJunction, "partner");
						if(verifiedMarriage){
							insertRecordCertainty(flefGroupJunction, "certain");
							insertRecordCredibility(flefGroupJunction, "direct and primary evidence used, or by dominance of the evidence");
						}
					}

					if(wife != null){
						final Map<String, Object> flefGroupJunction = new HashMap<>();
						flefGroupJunctions.add(flefGroupJunction);
//						insertRecordID(flefGroupJunction, flefGroupJunctions.size());
//						insertRecordGroupID(flefGroupJunction, marriage.id);
						flefGroupJunction.put("reference_table", "person");
						flefGroupJunction.put("reference_id", wife.id);
						insertRecordRole(flefGroupJunction, "partner");
						if(verifiedMarriage){
							insertRecordCertainty(flefGroupJunction, "certain");
							insertRecordCredibility(flefGroupJunction, "direct and primary evidence used, or by dominance of the evidence");
						}
					}
				}
			}
		}
	}

	private static String extractDesc(final Map<String, String> attributes){
		final String desc = attributes.get("DESC");
		final String cont = attributes.get("CONT");
		return (desc != null? desc + (cont != null? "\\r\\n" + cont: ""): null);
	}

	private static void transferRepositories(final Map<String, List<Map<String, Object>>> output, final Iterable<GedcomObject> repositories){
		final List<Map<String, Object>> flefRepositories = output.computeIfAbsent("repository", k -> new ArrayList<>());
		final List<Map<String, Object>> flefPlaces = output.computeIfAbsent("place", k -> new ArrayList<>());

		for(final GedcomObject repository : repositories){
			final Map<String, GedcomObject> children = repository.children;
			final Map<String, String> attributes = repository.attributes;

			final String addr = attributes.get("ADDR");
			final GedcomObject name = getFirstStartingWith(children, "NAME");
			final String repositoryName = (name != null? extractDesc(name.attributes): null);
			final GedcomObject plac = getFirstStartingWith(children, "PLAC");
			if(plac != null){
				final String address = (addr != null? addr: extractDesc(plac.attributes));

				final Map<String, Object> flefPlace = new HashMap<>();
				flefPlaces.add(flefPlace);
//				insertRecordID(flefPlace, plac.id);
				insertRecordIdentifier(flefPlace, address);
				if(repositoryName != null)
					insertRecordName(flefPlace, repositoryName);
			}

			final Map<String, Object> flefRepository = new HashMap<>();
			flefRepositories.add(flefRepository);
//			insertRecordID(flefRepository, repository.id);
			insertRecordIdentifier(flefRepository, repositoryName);
//			if(plac != null)
//				insertRecordPlaceID(flefRepository, plac.id);
		}
	}

	private static void transferNotes(final Map<String, List<Map<String, Object>>> output, final Iterable<GedcomObject> notes,
			final Iterable<String> lines){
		final List<Map<String, Object>> flefNotes = output.computeIfAbsent("note", k -> new ArrayList<>());

		for(final GedcomObject note : notes){
			final String noteID = ".NOTE[" + note.id + "]";
			final Collection<Integer> personIDs = new ArrayList<>();
			for(final String line : lines)
				if(line.contains(noteID))
					personIDs.add(extractID(line));

			final Map<String, String> attributes = note.attributes;

			final String text = attributes.get("CONT");
			for(final Integer personID : personIDs){
				final Map<String, Object> flefNote = new HashMap<>();
				flefNotes.add(flefNote);
//				insertRecordID(flefNote, note.id);
				insertRecordNote(flefNote, text);
				flefNote.put("reference_table", "person");
				flefNote.put("reference_id", personID);
			}
		}
	}

	private static void transferSources(final Map<String, List<Map<String, Object>>> output, final Iterable<GedcomObject> sources){
		final List<Map<String, Object>> flefSources = output.computeIfAbsent("source", k -> new ArrayList<>());
		final List<Map<String, Object>> flefNotes = output.computeIfAbsent("note", k -> new ArrayList<>());
		final List<Map<String, Object>> flefCitations = output.computeIfAbsent("citation", k -> new ArrayList<>());
		final List<Map<String, Object>> flefMedias = output.computeIfAbsent("media", k -> new ArrayList<>());
		final List<Map<String, Object>> flefMediaJunctions = output.computeIfAbsent("media_junction", k -> new ArrayList<>());
		final List<Map<String, Object>> flefHistoricDates = output.computeIfAbsent("historic_date", k -> new ArrayList<>());

		for(final GedcomObject source : sources){
			final Map<String, GedcomObject> children = source.children;
			final Map<String, String> attributes = source.attributes;

			String sourceLocation = null;
			final List<GedcomObject> sourceNotes = getAllStartingWith(children, "NOTE");
			for(final GedcomObject sourceNote : sourceNotes){
				final String desc = sourceNote.attributes.get("DESC");
				if(desc != null && desc.contains("busta"))
					sourceLocation = desc;
				else{
					final Map<String, Object> flefNote = new HashMap<>();
					flefNotes.add(flefNote);
//					insertRecordID(flefNote, sourceNote.id);
					insertRecordNote(flefNote, desc);
					flefNote.put("reference_table", "source");
					flefNote.put("reference_id", source.id);
				}
			}

			final Integer repoID = extractID(attributes.get("REPO"));
			final String auth = attributes.get("AUTH");
			final String titl = attributes.get("TITL");
			final Map<String, Object> flefSource = new HashMap<>();
			flefSources.add(flefSource);
//			insertRecordID(flefSource, source.id);
//			insertRecordRepositoryID(flefSource, repoID);
			insertRecordIdentifier(flefSource, titl);
			insertRecordAuthor(flefSource, auth);
			//TODO see media for date ID
//			insertRecordDateID(flefSource, -1);
			if(sourceLocation != null)
				insertRecordLocation(flefSource, sourceLocation);

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
//					insertRecordID(flefHistoricDate, dateID);
					insertRecordDate(flefHistoricDate, date);
				}
				final Map<String, Object> flefMedia = new HashMap<>();
				flefMedias.add(flefMedia);
//				insertRecordID(flefMedia, media.id);
				insertRecordIdentifier(flefMedia, identifier);
				insertRecordTitle(flefMedia, title);
				final boolean isImage = (identifier.endsWith(".jpg") || identifier.endsWith(".psd") || identifier.endsWith(".png")
					|| identifier.endsWith(".bmp") || identifier.endsWith(".gif") || identifier.endsWith(".tif"));
				insertRecordType(flefMedia, (isImage
					? "photo":
					(identifier.endsWith(".mp4")
						? "video"
						: "TODO see identifier")));
				if(isImage)
					insertRecordPhotoProjection(flefMedia, "rectangular");
//				if(date != null)
//					insertRecordDateID(flefMedia, dateID);

				final Map<String, Object> flefMediaJunction = new HashMap<>();
				flefMediaJunctions.add(flefMediaJunction);
//				insertRecordID(flefMediaJunction, media.id);
//				insertRecordMediaID(flefMediaJunction, media.id);
				flefMediaJunction.put("reference_table", "source");
				flefMediaJunction.put("reference_id", source.id);
			}

			final List<GedcomObject> notes = getAllStartingWith(children, "NOTE");
			if(notes.size() > 1)
				for(final GedcomObject note : notes){
					final Map<String, String> noteAttributes = note.attributes;

					final String text = extractDesc(noteAttributes);
					final Map<String, Object> flefNote = new HashMap<>();
					flefNotes.add(flefNote);
//					insertRecordID(flefNote, note.id);
					insertRecordNote(flefNote, text);
					flefNote.put("reference_table", "source");
					flefNote.put("reference_id", source.id);
				}

			final GedcomObject citation = getFirstStartingWith(children, "TEXT");
			if(citation != null){
				final String flefLocation = (notes.size() == 1? extractDesc(notes.getFirst().attributes): "TODO see notes");
				final String flefText = extractDesc(citation.attributes);
				final Map<String, Object> flefCitation = new HashMap<>();
				flefCitations.add(flefCitation);
//				insertRecordID(flefCitation, citation.id);
//				insertRecordSourceID(flefCitation, source.id);
				insertRecordLocation(flefCitation, flefLocation);
				insertRecordExtract(flefCitation, flefText);
				insertRecordExtractLocale(flefCitation, "TODO see text");
				insertRecordExtractType(flefCitation, "transcript");
			}
		}
	}

	private static void transferPersons(final Map<String, List<Map<String, Object>>> output, final Iterable<GedcomObject> persons){
		final List<Map<String, Object>> flefPersons = output.computeIfAbsent("person", k -> new ArrayList<>());
		final List<Map<String, Object>> flefPersonNames = output.computeIfAbsent("person_name", k -> new ArrayList<>());
		final List<Map<String, Object>> flefLocalizedTexts = output.computeIfAbsent("localized_text", k -> new ArrayList<>());
		final List<Map<String, Object>> flefLocalizedTextJunctions = output.computeIfAbsent("localized_text_junction", k -> new ArrayList<>());
		final List<Map<String, Object>> flefMedias = output.computeIfAbsent("media", k -> new ArrayList<>());
		final List<Map<String, Object>> flefMediaJunctions = output.computeIfAbsent("media_junction", k -> new ArrayList<>());

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
