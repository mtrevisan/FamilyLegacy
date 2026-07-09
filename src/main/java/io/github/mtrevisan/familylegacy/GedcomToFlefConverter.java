package io.github.mtrevisan.familylegacy;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * Converts a GEDCOM 5.5.1 file to FLEF (Family Legacy Format) 0.0.9.
 * <p>
 * Usage: java GedcomToFlefConverter input.ged output.flef
 * <p>
 * This class parses the GEDCOM structure, builds an internal representation,
 * and then writes the equivalent FLEF records according to the provided specification.
 */
public class GedcomToFlefConverter{

	// --------------------------- Internal data structures ---------------------------

	/**
	 * Represents a single GEDCOM line after parsing.
	 * Holds level, optional cross-reference, tag, and value.
	 * Children are stored in a map from tag to list of child records.
	 */
	static class GedcomNode{
		int level;
		String xref;          // e.g. "@I1@", may be null
		String tag;           // e.g. "INDI", "NAME", "BIRT"
		String value;         // trailing text after tag, may be null
		GedcomNode parent;
		List<GedcomNode> children = new ArrayList<>();

		// Convenience accessors
		GedcomNode firstChild(String tag){
			for(GedcomNode child : children){
				if(child.tag.equals(tag)) return child;
			}
			return null;
		}

		List<GedcomNode> childrenWithTag(String tag){
			List<GedcomNode> result = new ArrayList<>();
			for(GedcomNode child : children){
				if(child.tag.equals(tag)) result.add(child);
			}
			return result;
		}

		String firstChildValue(String tag){
			GedcomNode n = firstChild(tag);
			return n != null? n.value: null;
		}

		List<String> childrenValues(String tag){
			List<String> result = new ArrayList<>();
			for(GedcomNode child : childrenWithTag(tag)){
				result.add(child.value);
			}
			return result;
		}

		boolean hasChild(String tag){
			return firstChild(tag) != null;
		}
	}

	/**
	 * A complete GEDCOM dataset: header, submission, and all top-level records.
	 * The records are stored in a map keyed by their xref (e.g., "@I1@").
	 */
	static class GedcomDatabase{
		GedcomNode header;                     // the 0 HEAD record
		GedcomNode submission;                 // 0 SUBN record, if any
		Map<String, GedcomNode> recordMap = new LinkedHashMap<>();  // xref -> node
		List<GedcomNode> topLevelRecords = new ArrayList<>();       // all 0-level records except HEAD, SUBN, TRLR
	}

	// --------------------------- Main parsing logic ---------------------------

	/**
	 * Reads a GEDCOM file and builds a GedcomDatabase.
	 * Handles line continuation (CONC, CONT) by merging text into the parent's value.
	 */
	private static GedcomDatabase parseGedcom(File inputFile) throws IOException{
		GedcomDatabase db = new GedcomDatabase();
		List<GedcomNode> stack = new ArrayList<>();  // stack of nodes by level

		try(BufferedReader reader = new BufferedReader(
			new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8))){

			String line;
			while((line = reader.readLine()) != null){
				line = line.trim();
				if(line.isEmpty()) continue;

				// Parse: level [xref] tag [value]
				String[] parts = line.split("\\s+", 3);
				if(parts.length < 2) continue; // malformed

				int level;
				try{
					level = Integer.parseInt(parts[0]);
				}
				catch(NumberFormatException e){
					continue; // skip invalid
				}

				String xref = null;
				String tag;
				String value = null;

				// Check if second token is an xref (starts with '@')
				if(parts[1].startsWith("@") && parts[1].endsWith("@")){
					xref = parts[1];
					tag = parts.length > 2? parts[2]: "";
				}
				else{
					tag = parts[1];
					value = parts.length > 2? parts[2]: "";
				}

				GedcomNode node = new GedcomNode();
				node.level = level;
				node.xref = xref;
				node.tag = tag;
				node.value = value;

				// Adjust stack to current level
				while(stack.size() > level){
					stack.remove(stack.size() - 1);
				}

				// Determine parent
				if(level == 0){
					// Top-level record
					if(tag.equals("HEAD")){
						db.header = node;
					}
					else if(tag.equals("SUBN")){
						db.submission = node;
					}
					else if(tag.equals("TRLR")){
						// end of file, ignore
					}
					else{
						// Any other 0-level record: INDI, FAM, SOUR, OBJE, NOTE, REPO, SUBM
						db.topLevelRecords.add(node);
						if(xref != null){
							db.recordMap.put(xref, node);
						}
					}
					// No parent for level 0
				}
				else{
					// Attach to the most recent node on the stack (which is the parent)
					if(!stack.isEmpty()){
						GedcomNode parent = stack.get(stack.size() - 1);
						parent.children.add(node);
						node.parent = parent;
					}
				}

				// Push this node onto the stack (if not a continuation line)
				if(!tag.equals("CONC") && !tag.equals("CONT")){
					// Replace any node at this level with the new one
					if(stack.size() > level){
						stack.set(level, node);
					}
					else{
						stack.add(node);
					}
				}
				else{
					// CONC or CONT: append to the previous sibling's value
					// The parent is already the previous node; we need to append to the last child's value
					if(!stack.isEmpty()){
						GedcomNode parent = stack.get(stack.size() - 1);
						if(!parent.children.isEmpty()){
							GedcomNode lastChild = parent.children.get(parent.children.size() - 1);
							if(tag.equals("CONC")){
								lastChild.value = (lastChild.value == null? "": lastChild.value) + value;
							}
							else if(tag.equals("CONT")){
								lastChild.value = (lastChild.value == null? "": lastChild.value) + "\n" + value;
							}
						}
					}
				}
			}
		}
		return db;
	}

	// --------------------------- FLEF output generation ---------------------------

	/**
	 * Main conversion method: takes a GEDCOM database and writes a FLEF file.
	 */
	private static void convertToFlef(GedcomDatabase db, File outputFile) throws IOException{
		try(BufferedWriter writer = new BufferedWriter(
			new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))){

			// Write header
			writeFlefHeader(writer, db);

			// Write records: individuals, families, sources, notes, places, repositories, etc.
			// We need to collect all place names, note texts, source refs, etc. to create separate records.

			// First pass: collect all place strings and create unique place records.
			Map<String, String> placeXrefMap = new LinkedHashMap<>(); // place name -> xref
			Map<String, String> noteXrefMap = new LinkedHashMap<>(); // note text (normalized) -> xref

			// Process each record to collect place/note references
			for(GedcomNode record : db.topLevelRecords){
				collectPlacesAndNotes(record, placeXrefMap, noteXrefMap);
			}

			// Write all place records
			for(Map.Entry<String, String> entry : placeXrefMap.entrySet()){
				String placeName = entry.getKey();
				String xref = entry.getValue();
				writePlaceRecord(writer, xref, placeName);
			}

			// Write all note records
			for(Map.Entry<String, String> entry : noteXrefMap.entrySet()){
				String noteText = entry.getKey();
				String xref = entry.getValue();
				writeNoteRecord(writer, xref, noteText);
			}

			// Now process each top-level record and write its FLEF counterpart
			for(GedcomNode record : db.topLevelRecords){
				String tag = record.tag;
				if(tag.equals("INDI")){
					writeIndividualRecord(writer, record, placeXrefMap, noteXrefMap);
				}
				else if(tag.equals("FAM")){
					writeFamilyRecord(writer, record, placeXrefMap, noteXrefMap);
				}
				else if(tag.equals("SOUR")){
					writeSourceRecord(writer, record, noteXrefMap);
				}
				else if(tag.equals("REPO")){
					writeRepositoryRecord(writer, record, noteXrefMap);
				}
				else if(tag.equals("OBJE")){
					// Multimedia – we'll convert to a DOCUMENT_STRUCTURE inside a source or individual,
					// but for simplicity we skip or handle as note.
					// In FLEF, multimedia are attached to sources or individuals as FILE references.
					// We'll just create a note for now.
					String note = "Multimedia object: " + record.xref + " " + record.value;
					String noteXref = getOrCreateNote(note, noteXrefMap);
					writeNoteReference(writer, "1", noteXref);
				}
				else if(tag.equals("NOTE")){
					// Already handled via note collection; skip to avoid duplication.
				}
				else if(tag.equals("SUBM")){
					// Submitter will be used in header; we can also write as a note.
				}
				else{
					// Other record types: ignore or treat as note.
				}
			}

			// Write end of file
			writer.write("0 EOF\n");
		}
	}

	// --------------------------- Helper methods for collecting references ---------------------------

	private static void collectPlacesAndNotes(GedcomNode node, Map<String, String> placeMap, Map<String, String> noteMap){
		// Recursively traverse the subtree
		if(node.tag.equals("PLAC") && node.value != null && !node.value.isEmpty()){
			placeMap.putIfAbsent(node.value, generateXref("PLACE"));
		}
		if(node.tag.equals("NOTE") && node.value != null && !node.value.isEmpty()){
			// For inline notes, store the text; for pointer notes, we need to resolve later.
			// We'll treat both the same: if it's a pointer, we'll store the referenced note's text.
			String noteText = node.value;
			if(node.xref != null){
				// This is a pointer to a note record; we'll later retrieve the text from the note record.
				// For now, we store the xref as a placeholder; we'll handle later.
				// We'll collect the note record's value separately when traversing.
			}
			else{
				noteMap.putIfAbsent(noteText, generateXref("NOTE"));
			}
		}
		// Also traverse children
		for(GedcomNode child : node.children){
			collectPlacesAndNotes(child, placeMap, noteMap);
		}
		// If this node is a NOTE record (level 0), we also need to collect its value
		if(node.tag.equals("NOTE") && node.xref != null && node.value != null){
			noteMap.putIfAbsent(node.value, generateXref("NOTE"));
		}
	}

	private static String getOrCreateNote(String text, Map<String, String> noteMap){
		return noteMap.computeIfAbsent(text, k -> generateXref("NOTE"));
	}

	private static String generateXref(String prefix){
		return "@" + prefix + (int)(Math.random() * 1000000) + "@"; // simple random; better use counter
	}

	// --------------------------- Writing FLEF structures ---------------------------

	private static void writeFlefHeader(BufferedWriter w, GedcomDatabase db) throws IOException{
		w.write("0 HEADER\n");
		w.write("1 PROTOCOL FLEF\n");
		w.write("2 NAME Family LEgacy Format\n");
		w.write("2 VERSION 0.0.9\n");

		// Source system info from GEDCOM HEADER
		GedcomNode head = db.header;
		if(head != null){
			String sourceId = head.firstChildValue("SOUR");
			if(sourceId != null){
				w.write("1 SOURCE " + sourceId + "\n");
				GedcomNode sourNode = head.firstChild("SOUR");
				if(sourNode != null){
					String prodName = sourNode.firstChildValue("NAME");
					if(prodName != null) w.write("2 NAME " + prodName + "\n");
					String prodVers = sourNode.firstChildValue("VERS");
					if(prodVers != null) w.write("2 VERSION " + prodVers + "\n");
					String corp = sourNode.firstChildValue("CORP");
					if(corp != null) w.write("2 CORPORATE " + corp + "\n");
				}
			}
			String transDate = head.firstChildValue("DATE");
			if(transDate != null){
				w.write("1 DATE " + transDate + "\n");
			}
			String copyright = head.firstChildValue("COPR");
			if(copyright != null){
				w.write("1 COPYRIGHT " + copyright + "\n");
			}
			// Submitter
			String submXref = head.firstChildValue("SUBM");
			if(submXref != null){
				GedcomNode submNode = db.recordMap.get(submXref);
				if(submNode != null){
					w.write("1 SUBMITTER\n");
					String submName = submNode.firstChildValue("NAME");
					if(submName != null){
						w.write("2 NAME " + submName + "\n");
					}
					// Address, contact, etc. omitted for brevity
				}
			}
		}
		// Note: content description
		if(head != null){
			String note = head.firstChildValue("NOTE");
			if(note != null){
				w.write("1 NOTE " + note + "\n");
			}
		}
	}

	private static void writePlaceRecord(BufferedWriter w, String xref, String placeName) throws IOException{
		w.write("0 " + xref + " PLACE\n");
		w.write("1 NAME " + placeName + "\n");
		// Could add hierarchy, coordinates, etc. if available, but we don't have them.
	}

	private static void writeNoteRecord(BufferedWriter w, String xref, String text) throws IOException{
		// Escape newlines and special chars? Not necessary for FLEF.
		w.write("0 " + xref + " NOTE\n");
		w.write("1 VALUE " + text + "\n");
		// Could add MIME, LOCALE, etc.
	}

	private static void writeNoteReference(BufferedWriter w, String indent, String noteXref) throws IOException{
		w.write(indent + " NOTE " + noteXref + "\n");
	}

	private static void writeIndividualRecord(BufferedWriter w, GedcomNode indiNode,
		Map<String, String> placeMap, Map<String, String> noteMap) throws IOException{
		String xref = indiNode.xref;
		w.write("0 " + xref + " INDIVIDUAL\n");

		// Name structure
		for(GedcomNode nameNode : indiNode.childrenWithTag("NAME")){
			w.write("1 NAME\n");
			String fullName = nameNode.value;
			// Parse parts: GIVN, SURN, NPFX, NSFX, etc.
			// For simplicity, we just set INDIVIDUAL_NAME and FAMILY_NAME.
			// Better: parse slashes.
			if(fullName != null){
				// Extract surname between slashes
				int start = fullName.indexOf('/');
				int end = fullName.indexOf('/', start + 1);
				if(start != -1 && end != -1){
					String surname = fullName.substring(start + 1, end);
					String given = fullName.substring(0, start).trim() + fullName.substring(end + 1).trim();
					if(!given.isEmpty()) w.write("2 INDIVIDUAL_NAME " + given + "\n");
					if(!surname.isEmpty()) w.write("2 FAMILY_NAME " + surname + "\n");
				}
				else{
					// No slashes: treat as given name
					w.write("2 INDIVIDUAL_NAME " + fullName + "\n");
				}
			}
			// Also handle pieces: GIVN, SURN, etc.
			String given = nameNode.firstChildValue("GIVN");
			String surname = nameNode.firstChildValue("SURN");
			if(given != null) w.write("2 INDIVIDUAL_NAME " + given + "\n");
			if(surname != null) w.write("2 FAMILY_NAME " + surname + "\n");
			// Other pieces: NPFX, NSFX, NICK
			String prefix = nameNode.firstChildValue("NPFX");
			if(prefix != null) w.write("2 TITLE " + prefix + "\n");
			String suffix = nameNode.firstChildValue("NSFX");
			if(suffix != null) w.write("2 SUFFIX " + suffix + "\n");
			String nick = nameNode.firstChildValue("NICK");
			if(nick != null) w.write("2 INDIVIDUAL_NICKNAME " + nick + "\n");
			// Notes and sources on name can be added but omitted for brevity.
		}

		// Sex
		String sex = indiNode.firstChildValue("SEX");
		if(sex != null){
			String mappedSex = mapSex(sex);
			w.write("1 SEX " + mappedSex + "\n");
		}

		// Family links
		for(GedcomNode famc : indiNode.childrenWithTag("FAMC")){
			String famXref = famc.value;
			if(famXref != null){
				w.write("1 FAMILY_CHILD " + famXref + "\n");
				// Add certainty/credibility if present
				String pedi = famc.firstChildValue("PEDI");
				if(pedi != null){
					w.write("2 CERTAINTY " + mapPedigree(pedi) + "\n");
				}
				// Notes on link
				for(GedcomNode noteChild : famc.childrenWithTag("NOTE")){
					String noteRef = getNoteRef(noteChild, noteMap);
					if(noteRef != null) w.write("2 NOTE " + noteRef + "\n");
				}
			}
		}
		for(GedcomNode fams : indiNode.childrenWithTag("FAMS")){
			String famXref = fams.value;
			if(famXref != null){
				w.write("1 FAMILY_PARTNER " + famXref + "\n");
			}
		}

		// Events
		for(GedcomNode eventNode : indiNode.children){
			String eventTag = eventNode.tag;
			if(isIndividualEvent(eventTag)){
				// Write as an EVENT record, then link.
				String eventXref = generateXref("EVENT");
				w.write("0 " + eventXref + " EVENT\n");
				w.write("1 TYPE " + mapEventType(eventTag) + "\n");
				// Write event details: date, place, cause, etc.
				writeEventDetails(w, eventNode, "1", placeMap, noteMap);
				// Link to individual
				w.write("1 EVENT " + eventXref + "\n"); // This is the link from individual to event
			}
		}

		// Notes
		for(GedcomNode noteNode : indiNode.childrenWithTag("NOTE")){
			String noteRef = getNoteRef(noteNode, noteMap);
			if(noteRef != null) w.write("1 NOTE " + noteRef + "\n");
		}

		// Sources, multimedia, etc. omitted for brevity.
	}

	private static void writeFamilyRecord(BufferedWriter w, GedcomNode famNode,
		Map<String, String> placeMap, Map<String, String> noteMap) throws IOException{
		String xref = famNode.xref;
		w.write("0 " + xref + " FAMILY\n");

		String husb = famNode.firstChildValue("HUSB");
		if(husb != null) w.write("1 PARTNER1 " + husb + "\n");
		String wife = famNode.firstChildValue("WIFE");
		if(wife != null) w.write("1 PARTNER2 " + wife + "\n");
		for(String child : famNode.childrenValues("CHIL")){
			w.write("1 CHILD " + child + "\n");
		}

		// Events: MARR, DIV, etc.
		for(GedcomNode eventNode : famNode.children){
			String eventTag = eventNode.tag;
			if(isFamilyEvent(eventTag)){
				String eventXref = generateXref("EVENT");
				w.write("0 " + eventXref + " EVENT\n");
				w.write("1 TYPE " + mapEventType(eventTag) + "\n");
				writeEventDetails(w, eventNode, "1", placeMap, noteMap);
				w.write("1 EVENT " + eventXref + "\n"); // link to family
			}
		}

		// Notes
		for(GedcomNode noteNode : famNode.childrenWithTag("NOTE")){
			String noteRef = getNoteRef(noteNode, noteMap);
			if(noteRef != null) w.write("1 NOTE " + noteRef + "\n");
		}
	}

	private static void writeSourceRecord(BufferedWriter w, GedcomNode sourNode,
		Map<String, String> noteMap) throws IOException{
		String xref = sourNode.xref;
		w.write("0 " + xref + " SOURCE\n");
		String title = sourNode.firstChildValue("TITL");
		if(title != null) w.write("1 TITLE " + title + "\n");
		String author = sourNode.firstChildValue("AUTH");
		if(author != null) w.write("1 AUTHOR " + author + "\n");
		String publisher = sourNode.firstChildValue("PUBL");
		if(publisher != null) w.write("1 PUBLISHER " + publisher + "\n");
		// Repository
		for(GedcomNode repoNode : sourNode.childrenWithTag("REPO")){
			String repoXref = repoNode.value;
			if(repoXref != null){
				w.write("1 REPOSITORY " + repoXref + "\n");
				String callNo = repoNode.firstChildValue("CALN");
				if(callNo != null) w.write("2 LOCATION " + callNo + "\n");
			}
		}
		// Notes
		for(GedcomNode noteNode : sourNode.childrenWithTag("NOTE")){
			String noteRef = getNoteRef(noteNode, noteMap);
			if(noteRef != null) w.write("1 NOTE " + noteRef + "\n");
		}
	}

	private static void writeRepositoryRecord(BufferedWriter w, GedcomNode repoNode,
		Map<String, String> noteMap) throws IOException{
		String xref = repoNode.xref;
		w.write("0 " + xref + " REPOSITORY\n");
		String name = repoNode.firstChildValue("NAME");
		if(name != null) w.write("1 NAME " + name + "\n");
		// Address and place omitted for brevity.
	}

	// --------------------------- Helper methods for event details ---------------------------

	private static void writeEventDetails(BufferedWriter w, GedcomNode eventNode, String indent,
		Map<String, String> placeMap, Map<String, String> noteMap) throws IOException{
		// Date
		GedcomNode dateNode = eventNode.firstChild("DATE");
		if(dateNode != null && dateNode.value != null){
			w.write(indent + " DATE " + dateNode.value + "\n");
		}
		// Place
		GedcomNode placeNode = eventNode.firstChild("PLAC");
		if(placeNode != null && placeNode.value != null){
			String placeName = placeNode.value;
			String placeXref = placeMap.get(placeName);
			if(placeXref != null){
				w.write(indent + " PLACE " + placeXref + "\n");
			}
			else{
				// fallback: write as literal (though FLEF expects a reference)
				w.write(indent + " PLACE @" + placeName + "@\n");
			}
		}
		// Cause
		String cause = eventNode.firstChildValue("CAUS");
		if(cause != null) w.write(indent + " CAUSE " + cause + "\n");
		// Agency
		String agency = eventNode.firstChildValue("AGNC");
		if(agency != null) w.write(indent + " AGENCY " + agency + "\n");
		// Notes
		for(GedcomNode noteNode : eventNode.childrenWithTag("NOTE")){
			String noteRef = getNoteRef(noteNode, noteMap);
			if(noteRef != null) w.write(indent + " NOTE " + noteRef + "\n");
		}
	}

	// --------------------------- Mapping functions ---------------------------

	private static String mapSex(String gedcomSex){
		if("M".equals(gedcomSex)) return "MALE";
		if("F".equals(gedcomSex)) return "FEMALE";
		return "UNKNOWN";
	}

	private static String mapPedigree(String pedi){
		if("adopted".equals(pedi)) return "adopted";
		if("birth".equals(pedi)) return "biological";
		if("foster".equals(pedi)) return "foster";
		return "challenged"; // default
	}

	private static String mapEventType(String gedcomTag){
		// Map GEDCOM event tags to FLEF TYPE strings
		switch(gedcomTag){
			case "BIRT":
				return "BIRTH";
			case "CHR":
				return "BAPTISM"; // or "CHRISTENING"
			case "DEAT":
				return "DEATH";
			case "BURI":
				return "BURIAL";
			case "CREM":
				return "CREMATION";
			case "ADOP":
				return "ADOPTION";
			case "MARR":
				return "MARRIAGE";
			case "DIV":
				return "DIVORCE";
			case "ANUL":
				return "ANNULMENT";
			case "ENGA":
				return "ENGAGEMENT";
			case "RESI":
				return "RESIDENCE";
			case "OCCU":
				return "OCCUPATION";
			case "CENS":
				return "CENSUS";
			case "PROB":
				return "PROBATE";
			case "WILL":
				return "WILL";
			case "EMIG":
				return "EMIGRATION";
			case "IMMI":
				return "IMMIGRATION";
			case "NATU":
				return "NATURALIZATION";
			case "GRAD":
				return "GRADUATION";
			case "RETI":
				return "RETIREMENT";
			case "EVEN":
				return "EVENT"; // generic
			default:
				return gedcomTag;
		}
	}

	private static boolean isIndividualEvent(String tag){
		return Arrays.asList("BIRT", "CHR", "DEAT", "BURI", "CREM", "ADOP", "BAPM", "BARM", "BASM", "BLES",
			"CHRA", "CONF", "FCOM", "ORDN", "NATU", "EMIG", "IMMI", "CENS", "PROB", "WILL", "GRAD", "RETI", "EVEN").contains(tag);
	}

	private static boolean isFamilyEvent(String tag){
		return Arrays.asList("ANUL", "CENS", "DIV", "DIVF", "ENGA", "MARB", "MARC", "MARR", "MARL", "MARS", "RESI", "EVEN").contains(tag);
	}

	private static String getNoteRef(GedcomNode noteNode, Map<String, String> noteMap){
		if(noteNode.xref != null){
			return noteNode.xref; // pointer to a note record
		}
		else if(noteNode.value != null){
			// inline note: create a note record
			return getOrCreateNote(noteNode.value, noteMap);
		}
		return null;
	}

	// --------------------------- Main method ---------------------------

	public static void main(String[] args){
		File inputGEDFile = new File("C:\\mauro\\heritage\\My Genealogy Projects\\Trevisan (Dorato)-Gallinaro-Masutti (Manfrin)-Zaros (Basso)\\Trevisan (Dorato)-Gallinaro-Masutti (Manfrin)-Zaros (Basso).ged");
		File outputFLEFFile = new File("C:\\mauro\\heritage\\My Genealogy Projects\\Trevisan (Dorato)-Gallinaro-Masutti (Manfrin)-Zaros (Basso)\\output.flef");

		try{
			System.out.println("Parsing GEDCOM...");
			GedcomDatabase db = parseGedcom(inputGEDFile);
			System.out.println("Parsed " + db.topLevelRecords.size() + " top-level records.");
			System.out.println("Converting to FLEF...");
			convertToFlef(db, outputFLEFFile);
			System.out.println("Conversion complete. Output written to " + outputFLEFFile.getAbsolutePath());
		}
		catch(IOException e){
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
}
