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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;


public class Main{

	private static final String JDBC_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
	private static final String USER = "sa";
	private static final String PASSWORD = "";


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
		final String flatGedcomFilename = "TGMZ.txt";
		final List<String> lines = flatGedcom(baseDirectory, gedcomFilename, flatGedcomFilename);
		final Map<String, List<Map<String, Object>>> tables = transfer(lines);
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
		output.replaceAll(s -> (s.endsWith("BIRT Y") || s.endsWith("DEAT Y") || s.endsWith("CREM Y") || s.endsWith("BURI Y")
			|| s.endsWith("RETI Y") || s.endsWith("EMIG Y") || s.endsWith("IMMI Y") || s.endsWith("ADOP Y") || s.endsWith("NATU Y")
			|| s.endsWith("CENS Y") || s.endsWith("GRAD Y") || s.endsWith("ORDN Y") || s.endsWith("WILL Y") || s.endsWith("ENGA Y")
			|| s.endsWith("MARR Y") || s.endsWith("MARL Y") || s.endsWith("MARB Y") || s.endsWith("MARS Y") || s.endsWith("DIV Y")
			? s.substring(0, s.length() - 2): s));

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

			if(line.contains(".MAP.")){
				String prevLine = output.get(i - 1);
				if(prevLine.endsWith(".MAP")){
					StringJoiner sj = new StringJoiner(", ");
					int j = i;
					while(j < output.size() && output.get(j).contains(".MAP.")){
						String coord = output.get(j).split(" ", 2)[1];
						char coordSign = coord.charAt(0);
						sj.add((coordSign == 'S' || coordSign == 'W'? "-": "") + coord.substring(1));

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

		output.removeIf(line -> line.contains(".CHAN ") || line.contains(".CHAN.DATE ") || line.contains(".CHAN.DATE.TIME ")
			|| line.contains(".ADDR.") || line.contains(".MAP.") || line.contains(".CONC ") || line.contains("].FORM ")
			|| line.contains("._PUBL ") || line.contains("._CUT Y") || line.endsWith(".NAME //"));

		int index = 0;
		Iterator<String> itr = output.iterator();
		while(itr.hasNext()){
			String line = itr.next();

			if(line.contains(".PLAC ")){
				String prevLine = output.get(index - 1);
				if(prevLine.contains(".ADDR ") && prevLine.endsWith(line.split(" ", 2)[1])){
					itr.remove();

					index --;
				}
			}

			index ++;
		}

		for(int i = 0; i < output.size(); i ++){
			String line = output.get(i);

			if(line.contains(".NAME ") || line.contains(".EDUC ") || line.contains(".EVEN ") || line.contains(".OCCU ")
					|| line.contains(".SEX ") || line.contains(".PLAC ") || line.contains(".NOTE ") || line.contains(".SSN ")
					|| line.contains(".DSCR ") || line.contains(".RELI ") || line.contains(".PROP ") || line.contains(".NCHI ")){
				String[] components = line.split(" ", 2);
				output.set(i, components[0]);
				output.add(i + 1, components[0] + ".DESC " + components[1]);
			}
		}
		int nameID = 1;
		int educID = 1;
		int evenID = 1;
		int occuID = 1;
		int placID = 1;
		int conclusionID = 1;
		int noteID = 10_000;
		for(int i = 0; i < output.size(); i ++){
			String line = output.get(i);

			if(line.contains(".NAME.")){
				line = line.replace(".NAME.", ".NAME[" + (nameID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".NAME")){
				line += "[" + nameID + "]";
				output.set(i, line);
				nameID ++;
			}

			if(line.contains(".EDUC.")){
				line = line.replace(".EDUC.", ".EDUC[" + (educID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".EDUC")){
				line += "[" + educID + "]";
				output.set(i, line);
				educID ++;
			}

			if(line.contains(".EVEN.")){
				boolean another = (line.indexOf(".EVEN.", line.indexOf(".EVEN") + 1) >= 0);
				line = line.replaceFirst(".EVEN.", ".EVEN[" + (evenID - 1 - (another? 1: 0)) + "].");
				line = line.replace(".EVEN.", ".EVEN[" + (evenID - 1) + "].");
				output.set(i, line);
				if(another)
					evenID ++;
			}
			else if(line.endsWith(".EVEN")){
				line += "[" + evenID + "]";
				output.set(i, line);
				evenID ++;
			}
			if(line.contains(".EVEN.")){
				line = line.replace(".EVEN.", ".EVEN[" + (evenID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".EVEN")){
				line += "[" + evenID + "]";
				output.set(i, line);
				evenID ++;
			}

			if(line.contains(".OCCU.")){
				line = line.replace(".OCCU.", ".OCCU[" + (occuID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".OCCU")){
				line += "[" + occuID + "]";
				output.set(i, line);
				occuID ++;
			}

			if(line.contains(".PLAC.")){
				line = line.replace(".PLAC.", ".PLAC[" + (placID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".PLAC")){
				line += "[" + placID + "]";
				output.set(i, line);
				placID ++;
			}

			if(line.contains(".SEX.")){
				line = line.replace(".SEX.", ".SEX[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".SEX")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".MARR.")){
				line = line.replace(".MARR.", ".MARR[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".MARR")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".MARL.")){
				line = line.replace(".MARL.", ".MARL[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".MARL")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".MARB.")){
				line = line.replace(".MARB.", ".MARB[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".MARB")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".MARS.")){
				line = line.replace(".MARS.", ".MARS[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".MARS")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".BIRT.")){
				line = line.replace(".BIRT.", ".BIRT[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".BIRT")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".DEAT.")){
				line = line.replace(".DEAT.", ".DEAT[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".DEAT")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".CREM.")){
				line = line.replace(".CREM.", ".CREM[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".CREM")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".BURI.")){
				line = line.replace(".BURI.", ".BURI[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".BURI")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".RETI.")){
				line = line.replace(".RETI.", ".RETI[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".RETI")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".RESI.")){
				line = line.replace(".RESI.", ".RESI[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".RESI")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".RELI.")){
				line = line.replace(".RELI.", ".RELI[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".RELI")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".ENGA.")){
				line = line.replace(".ENGA.", ".ENGA[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".ENGA")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".DSCR.")){
				line = line.replace(".DSCR.", ".DSCR[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".DSCR")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".SSN.")){
				line = line.replace(".SSN.", ".SSN[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".SSN")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".EMIG.")){
				line = line.replace(".EMIG.", ".EMIG[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".EMIG")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".IMMI.")){
				line = line.replace(".IMMI.", ".IMMI[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".IMMI")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".ADOP.")){
				line = line.replace(".ADOP.", ".ADOP[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".ADOP")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".PROP.")){
				line = line.replace(".PROP.", ".PROP[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".PROP")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".NATU.")){
				line = line.replace(".NATU.", ".NATU[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".NATU")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".CENS.")){
				line = line.replace(".CENS.", ".CENS[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".CENS")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".GRAD.")){
				line = line.replace(".GRAD.", ".GRAD[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".GRAD")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".ORDN.")){
				line = line.replace(".ORDN.", ".ORDN[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".ORDN")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".NCHI.")){
				line = line.replace(".NCHI.", ".NCHI[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".NCHI")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".WILL.")){
				line = line.replace(".WILL.", ".WILL[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".WILL")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".DIV.")){
				line = line.replace(".DIV.", ".DIV[" + (conclusionID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".DIV")){
				line += "[" + conclusionID + "]";
				output.set(i, line);
				conclusionID ++;
			}

			if(line.contains(".NOTE.")){
				line = line.replace(".NOTE.", ".NOTE[" + (noteID - 1) + "].");
				output.set(i, line);
			}
			else if(line.endsWith(".NOTE")){
				line += "[" + noteID + "]";
				output.set(i, line);
				noteID ++;
			}
		}

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

		Files.write(Paths.get(baseDirectory + flatGedcomFilename),
			output);

		return output;
	}

	private static Map<String, List<Map<String, Object>>> transfer(List<String> lines){
		final Map<String, List<Map<String, Object>>> tables = new HashMap<>();
		for(String line : lines){
			if(line.matches("INDI\\[\\d+]")){
				List<Map<String, Object>> persons = tables.computeIfAbsent("person", k -> new ArrayList<>());
				int personID = Integer.parseInt(line.substring(line.indexOf('[') + 1, line.indexOf(']')));
				Map<String, Object> person = new HashMap<>();
				person.put("id", personID);
				persons.add(person);
			}
			else if(line.matches("FAM\\[\\d+]")){
				List<Map<String, Object>> groups = tables.computeIfAbsent("group", k -> new ArrayList<>());
				int groupID = Integer.parseInt(line.substring(line.indexOf('[') + 1, line.indexOf(']')));
				Map<String, Object> group = new HashMap<>();
				group.put("id", groupID);
				groups.add(group);
			}
			else if(line.matches("SOUR\\[\\d+]")){
				List<Map<String, Object>> sources = tables.computeIfAbsent("source", k -> new ArrayList<>());
				int sourceID = Integer.parseInt(line.substring(line.indexOf('[') + 1, line.indexOf(']')));
				Map<String, Object> scource = new HashMap<>();
				scource.put("id", sourceID);
				sources.add(scource);
			}
			else if(line.matches("REPO\\[\\d+]")){
				List<Map<String, Object>> repositories = tables.computeIfAbsent("repository", k -> new ArrayList<>());
				int repositoryID = Integer.parseInt(line.substring(line.indexOf('[') + 1, line.indexOf(']')));
				Map<String, Object> repository = new HashMap<>();
				repository.put("id", repositoryID);
				repositories.add(repository);
			}
			else if(line.matches("NOTE\\[\\d+]")){
				List<Map<String, Object>> notes = tables.computeIfAbsent("note", k -> new ArrayList<>());
				int noteID = Integer.parseInt(line.substring(line.indexOf('[') + 1, line.indexOf(']')));
				Map<String, Object> note = new HashMap<>();
				note.put("id", noteID);
				notes.add(note);
			}
		}

		//TODO

		return tables;
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
