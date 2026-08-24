package io.github.mtrevisan.familylegacy.v2.gedcom;

import io.github.mtrevisan.familylegacy.v2.gedcom.converters.FamilyConverter;
import io.github.mtrevisan.familylegacy.v2.gedcom.converters.HeaderConverter;
import io.github.mtrevisan.familylegacy.v2.gedcom.converters.IndividualConverter;
import io.github.mtrevisan.familylegacy.v2.gedcom.converters.MultimediaConverter;
import io.github.mtrevisan.familylegacy.v2.gedcom.converters.RepositoryConverter;
import io.github.mtrevisan.familylegacy.v2.gedcom.converters.SourceConverter;
import io.github.mtrevisan.familylegacy.v2.gedcom.converters.SubmitterConverter;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.PlaceCache;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.ReferenceResolver;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

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
	private final Map<String, String> noteMap = new HashMap<>();

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
		HeaderConverter headerConverter = new HeaderConverter(model, submitterMap);
		IndividualConverter individualConverter = new IndividualConverter(
			model, individualMap, sourceMap, multimediaMap, placeCache);
		FamilyConverter familyConverter = new FamilyConverter(
			model, familyMap, individualMap, sourceMap, multimediaMap, placeCache, referenceResolver);
		SourceConverter sourceConverter = new SourceConverter(
			model, sourceMap, repositoryMap, multimediaMap, placeCache);
		RepositoryConverter repositoryConverter = new RepositoryConverter(
			model, repositoryMap, multimediaMap, placeCache);
		MultimediaConverter multimediaConverter = new MultimediaConverter(
			model, multimediaMap, placeCache);
		SubmitterConverter submitterConverter = new SubmitterConverter(
			model, submitterMap, placeCache);

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
				// NOTE records are processed inline; they are not top‑level records.
				// SUBN (submission records) are ignored.
				default -> { /* ignore unknown top‑level tags */ }
			}
		}

		// ---- 4. Second pass: resolve family links ----
		familyConverter.resolveLinks();

		// ---- 5. Ensure header is present ----
		headerConverter.ensureHeader();

		// ---- 6. Add all records to the model ----
		individualMap.values().forEach(model::addRecord);
		familyMap.values().forEach(model::addRecord);
		sourceMap.values().forEach(model::addRecord);
		repositoryMap.values().forEach(model::addRecord);
		multimediaMap.values().forEach(model::addRecord);
		// Submitters are not added as top‑level records; they are included in the header.

		// ---- 7. (Optional) Deduplicate records – if needed, call Deduplicator.deduplicate(model) ----

		return model;
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
