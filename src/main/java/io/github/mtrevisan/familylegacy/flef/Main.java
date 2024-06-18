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
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

		media_mediaJunction();
		repository_historicPlace_place_name();
	}

/*
"0 @I(\d+)@ INDI" to "PERSON=$1"

"0 @S(\d+)@ SOUR" to "SOURCE=$1"

"0 @F(\d+)@ FAM" to "GROUP=$1"

"0 @R(\d+)@ REPO" to "REPOSITORY=$1"

" LONG " to ", "

"3 MAP
4 LATI" to "COORDINATE="

"2 TITL ".."6 TITL " to "MEDIA.TITLE.1="

"(MEDIA\.TITLE\.2=[^\r\n]+[\r\n]+)2 _DATE " to "$1MEDIA.DATE.1="

"2 FILE ".."2 FILE " to "MEDIA.IDENTIFIER.1="

"1 OBJE
" to ""

"(MEDIA\.IDENTIFIER\.1=[^\r\n]+[\r\n]+)2 _CUTD " to "$1MEDIA.CROP.1="

"2 _PREF Y[\r\n]+(2 _CUTD [^\r\n]+)" to "$1\r\n2 _PREF"

"(MEDIA\.IDENTIFIER\.1=[^\r\n]+[\r\n]+)2 _CUTD " to "$1MEDIA.IMAGE_CROP.1="

"3 _CUTD " to "MEDIA.IMAGE_CROP.2="

bookmark " FORM " then remove

bookmark " _PUBL " then remove

"
3 CITY " to ", "

"(REPOSITORY=\d+
)1 NAME " to "$1REPOSITORY.IDENTIFIER="

"(SOURCE=\d+
)1 TITL " to "$1SOURCE.IDENTIFIER="

"1 NOTE busta" to "SOURCE.LOCATION=busta"
*/

	public static void media_mediaJunction() throws IOException{
		Path path = Paths.get("C:\\Users\\mauro\\Projects\\FamilyLegacy\\src\\main\\resources\\ged\\TMGZ.txt");
		List<String> lines = Files.readAllLines(path);

		List<String> media = new ArrayList<>();
		Map<String, Integer> mediaMap = new HashMap<>();
		media.add("MEDIA");
		media.add("ID|IDENTIFIER|TITLE|TYPE|IMAGE_PROJECTION");
		List<String> mediaJunction = new ArrayList<>();
		mediaJunction.add("MEDIA_JUNCTION");
		mediaJunction.add("ID|MEDIA_ID|IMAGE_CROP|REFERENCE_TABLE|REFERENCE_ID");

		String personID = null;
		String sourceID = null;
		String title = null;
		String identifier = null;
		String crop = null;
		for(int i = 0; i < lines.size(); i++){
			String line = lines.get(i);

			if(line.startsWith("PERSON=")){
				personID = line.split("=")[1];
				sourceID = null;
			}
			if(line.endsWith("SOURCE=")){
				personID = null;
				sourceID = line.split("=")[1];
			}
			if(line.startsWith("MEDIA.TITLE.1=")){
				if(title != null){
					Integer mediaID = mediaMap.get(identifier);
					if(mediaID == null){
						mediaID = media.size() - 1;
						mediaMap.put(identifier, mediaID);

						media.add(mediaID + "|" + identifier + "|" + title + "||rectangular");
					}

					if(personID != null)
						mediaJunction.add((mediaJunction.size() - 1) + "|" + mediaID + "|" + (crop != null? crop: "") + "|person|" + personID);
					else if(sourceID != null)
						mediaJunction.add((mediaJunction.size() - 1) + "|" + mediaID + "|" + (crop != null? crop: "") + "|source|" + sourceID);
					else
						throw new IllegalArgumentException("Something wrong happened");

					identifier = null;
					crop = null;
				}

				title = line.split("=")[1];
			}
			if(line.startsWith("MEDIA.IDENTIFIER.1="))
				identifier = line.split("=")[1];
			if(line.startsWith("MEDIA.IMAGE_CROP.1="))
				crop = line.split("=")[1];
		}

		Files.write(Paths.get("C:\\Users\\mauro\\Projects\\FamilyLegacy\\src\\main\\resources\\ged\\media.txt"),
			media);

		Files.write(Paths.get("C:\\Users\\mauro\\Projects\\FamilyLegacy\\src\\main\\resources\\ged\\mediaJunction.txt"),
			mediaJunction);
	}

	public static void repository_historicPlace_place_name() throws IOException{
		Path path = Paths.get("C:\\Users\\mauro\\Projects\\FamilyLegacy\\src\\main\\resources\\ged\\TMGZ.txt");
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
			if(line.startsWith("REPOSITORY.IDENTIFIER=")){
				if(identifier != null){
					String namID = "" + (Integer.parseInt(repositoryID) + 10_000);
					String placID = "" + (Integer.parseInt(repositoryID) + 20_000);
					String histPlacID = "" + (Integer.parseInt(repositoryID) + 30_000);
					if(plac != null){
						name.add(namID + "|" + plac + "||original||");
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
			if(line.startsWith("REPOSITORY.PLACE="))
				plac = line.split("=")[1];
		}

		Files.write(Paths.get("C:\\Users\\mauro\\Projects\\FamilyLegacy\\src\\main\\resources\\ged\\name.txt"),
			name);

		Files.write(Paths.get("C:\\Users\\mauro\\Projects\\FamilyLegacy\\src\\main\\resources\\ged\\place.txt"),
			place);

		Files.write(Paths.get("C:\\Users\\mauro\\Projects\\FamilyLegacy\\src\\main\\resources\\ged\\historicPlace.txt"),
			historicPlace);

		Files.write(Paths.get("C:\\Users\\mauro\\Projects\\FamilyLegacy\\src\\main\\resources\\ged\\repository.txt"),
			repository);
	}

}
