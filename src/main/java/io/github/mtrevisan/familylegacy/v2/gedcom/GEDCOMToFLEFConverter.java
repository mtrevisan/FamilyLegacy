package io.github.mtrevisan.familylegacy.v2.gedcom;

import io.github.mtrevisan.familylegacy.v2.gedcom.converters.FamilyConverter;
import io.github.mtrevisan.familylegacy.v2.gedcom.converters.HeaderConverter;
import io.github.mtrevisan.familylegacy.v2.gedcom.converters.IndividualConverter;
import io.github.mtrevisan.familylegacy.v2.gedcom.converters.MultimediaConverter;
import io.github.mtrevisan.familylegacy.v2.gedcom.converters.NoteConverter;
import io.github.mtrevisan.familylegacy.v2.gedcom.converters.RepositoryConverter;
import io.github.mtrevisan.familylegacy.v2.gedcom.converters.SourceConverter;
import io.github.mtrevisan.familylegacy.v2.gedcom.converters.SubmitterConverter;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.PlaceCache;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.ReferenceResolver;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Orchestrates the conversion of a GEDCOM forest into a FLEF model.
 * <p>
 * Notes are now handled as inline NoteStructure (with mandatory audit)
 * and are not stored as separate top‑level records.
 * <p>
 * The conversion happens in two passes:
 * <ol>
 *   <li>Parse all records into FLEF structures.</li>
 *   <li>Resolve family links (spouse, parent‑child relationships).</li>
 * </ol>
 */
public class GEDCOMToFLEFConverter {

	private final FLEFModel model = new FLEFModel();

	// Shared context maps
	private final Map<String, FLEFRecord> individualMap = new HashMap<>();
	private final Map<String, FLEFRecord> familyMap = new HashMap<>();
	private final Map<String, FLEFRecord> sourceMap = new HashMap<>();
	private final Map<String, FLEFRecord> repositoryMap = new HashMap<>();
	private final Map<String, FLEFRecord> multimediaMap = new HashMap<>();
	private final Map<String, FLEFRecord> submitterMap = new HashMap<>();
	private final Map<String, FLEFRecord> noteMap = new HashMap<>();

	private final PlaceCache placeCache = new PlaceCache(model);
	private final ReferenceResolver referenceResolver = new ReferenceResolver(model, individualMap, familyMap);

	/**
	 * Converts the GEDCOM forest into a FLEFModel.
	 *
	 * @param roots the list of top‑level GEDCOM nodes
	 * @return a fully converted and validated FLEF model
	 */
	public FLEFModel convert(List<GEDCOMNode> roots) {
		// ---- 1. Register all existing IDs (for IDGenerator) ----
		registerIds(roots);

		// ---- 2. Instantiate converters ----
		HeaderConverter headerConverter = new HeaderConverter(model);
		IndividualConverter individualConverter = new IndividualConverter(model, individualMap, sourceMap, multimediaMap, placeCache);
		FamilyConverter familyConverter = new FamilyConverter(model, familyMap, individualMap, sourceMap, multimediaMap, placeCache, referenceResolver);
		SourceConverter sourceConverter = new SourceConverter(model, sourceMap, repositoryMap, multimediaMap, placeCache);
		RepositoryConverter repositoryConverter = new RepositoryConverter(model, repositoryMap, multimediaMap, placeCache);
		MultimediaConverter multimediaConverter = new MultimediaConverter(model, multimediaMap, placeCache);
		SubmitterConverter submitterConverter = new SubmitterConverter(model, submitterMap, placeCache);
		NoteConverter noteConverter = new NoteConverter(model, noteMap);

		// ---- 3. First pass: parse all records ----
		for (GEDCOMNode node : roots) {
			switch (node.getTag()) {
				case "HEAD" -> headerConverter.convert(node);

				case "INDI" -> individualConverter.convert(node);
				case "FAM" -> familyConverter.collect(node);
				case "SOUR" -> sourceConverter.convert(node);
				case "REPO" -> repositoryConverter.convert(node);
				case "OBJE" -> multimediaConverter.convert(node);
				case "SUBM" -> submitterConverter.convert(node);
				case "NOTE" -> noteConverter.convert(node);
				// SUBN (submission records) are ignored.
				default -> { /* ignore unknown top‑level tags */ }
			}
		}

		// ---- 4. Second pass: resolve family links ----
		familyConverter.resolveLinks();

		// ---- 6. Add all records to the model ----
		individualMap.values().forEach(model::addRecord);
		familyMap.values().forEach(model::addRecord);
		sourceMap.values().forEach(model::addRecord);
		repositoryMap.values().forEach(model::addRecord);
		multimediaMap.values().forEach(model::addRecord);
		// Submitters are not added as top‑level records; they are included in the header.

		inlineNotes(model, noteMap);

		// ---- 7. (Optional) Deduplicate records – if needed, call Deduplicator.deduplicate(model) ----

		return model;
	}

	/**
	 * Replaces note references with embedded copies of the referenced notes.
	 *
	 * A note reference is a child with tag "note" and a value containing
	 * the ID of a note record present in noteMap:
	 *
	 *     note N123
	 *
	 * It becomes:
	 *
	 *     note {
	 *         ...
	 *     }
	 *
	 * The referenced note record is deep-copied so that modifications to the
	 * embedded note do not affect the original note record.
	 *
	 * @param model   The FLEF model.
	 * @param noteMap Map of note IDs to note records.
	 */
	public static void inlineNotes(final FLEFModel model, final Map<String, FLEFRecord> noteMap){
		for(final FLEFRecord record : model.getRecords())
			inlineNotes(record, noteMap);
	}

	private static void inlineNotes(final FLEFRecord root, final Map<String, FLEFRecord> noteMap){
		final Deque<FLEFRecord> stack = new ArrayDeque<>();
		stack.push(root);

		while(!stack.isEmpty()){
			final FLEFRecord current = stack.pop();

			final List<FLEFRecord> children = current.getChildren();

			for(int i = 0; i < children.size(); i++){
				final FLEFRecord child = children.get(i);

				if("note".equalsIgnoreCase(child.getTag())){
					String childValue = FLEFRecordHelper.getChildValuesAsString(child, "value");

					if(childValue != null){
						if(childValue.length() > 2)
							childValue = childValue.substring(1, childValue.length() - 1);
						final FLEFRecord referencedNote = noteMap.get(childValue);

						if(referencedNote != null){
							final FLEFRecord copy = FLEFRecord.createEmpty();
							referencedNote.deepCopyTo(copy);

							child.getChildren()
								.clear();
							for(FLEFRecord copyChild : copy.getChildren())
								child.addChild(copyChild);

							// Continue traversal inside the copied note.
							stack.push(copy);
							continue;
						}
					}
				}

				stack.push(child);
			}
		}
	}

	/**
	 * Registers all existing IDs (from GEDCOM cross‑references) in the IDGenerator.
	 * This ensures that newly generated IDs start after the highest existing number.
	 */
	private void registerIds(List<GEDCOMNode> roots) {
		for (GEDCOMNode node : roots) {
			registerIdsRecursive(node);
		}
	}

	private void registerIdsRecursive(GEDCOMNode node) {
		if (node.getXrefId() != null) {
			IDGenerator.registerExistingId(node.getXrefId());
		}
		for (GEDCOMNode child : node.getChildren()) {
			registerIdsRecursive(child);
		}
	}

}
