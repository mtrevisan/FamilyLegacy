/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.importer.gedcom.converters;

import io.github.mtrevisan.familylegacy.importer.gedcom.Deduplicator;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOM7Normalizer;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.IDGenerator;
import io.github.mtrevisan.familylegacy.importer.gedcom.PlaceCache;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Orchestrates the conversion of a GEDCOM forest into a FLEF model.
 * <p>
 * Notes are handled as inline NoteStructure (with mandatory audit)
 * and are not stored as separate top‑level records.
 * <p>
 * GEDCOM 5.5, 5.5.1, and 7 are supported: a GEDCOM 7 forest is first rewritten into the syntax of GEDCOM 5.5.1 (see
 * {@link GEDCOM7Normalizer}), and the structures that only GEDCOM 7 has are read by the converters.
 * <p>
 * The conversion happens in passes:
 * <ol>
 *   <li>Index the records and register their IDs.</li>
 *   <li>Convert submitters and the header.</li>
 *   <li>Convert the records that other records refer to: multimedia, repositories, sources (in this order, as a source refers
 *   to repositories and multimedia).</li>
 *   <li>Convert individuals and collect families.</li>
 *   <li>Resolve family links (spouse, parent‑child relationships, family events) and the sources of multimedia.</li>
 * </ol>
 */
public class GEDCOMToFLEFConverter {

	@FunctionalInterface
	public interface ProgressListener{
		/**
		 * @param percent 0-100, or a negative value for an indeterminate step
		 * @param message short description of the current step
		 */
		void onProgress(int percent, String message);
	}


	/** Records referred to by other records, converted first. */
	private static final List<String> REFERENCED_RECORD_TAGS = List.of("OBJE", "REPO", "SOUR");
	/** Records that refer to other records. */
	private static final Set<String> REFERRING_RECORD_TAGS = Set.of("INDI", "FAM");


	private final FLEFModel model = new FLEFModel();

	// Shared context maps
	private final Map<String, FLEFRecord> individualMap = new HashMap<>();
	private final Map<String, FLEFRecord> familyMap = new HashMap<>();
	private final Map<String, FLEFRecord> sourceMap = new HashMap<>();
	private final Map<String, FLEFRecord> repositoryMap = new HashMap<>();
	private final Map<String, FLEFRecord> multimediaMap = new HashMap<>();
	private final Map<String, FLEFRecord> submitterMap = new HashMap<>();
	private final Map<String, GEDCOMNode> noteRawMap = new HashMap<>();
	private final Map<String, GEDCOMNode> recordsById = new HashMap<>();
	private final Map<String, GEDCOMNode> childFamilyLinks = new HashMap<>();

	private final PlaceCache placeCache = new PlaceCache(model);


	/** Convenience overload that runs the conversion without progress reporting. */
	public FLEFModel convert(final List<GEDCOMNode> roots){
		return convert(roots, (percent, message) -> {});
	}

	/**
	 * Converts the GEDCOM forest into a FLEFModel.
	 *
	 * @param roots the list of top‑level GEDCOM nodes
	 * @return a fully converted and validated FLEF model
	 */
	public FLEFModel convert(final List<GEDCOMNode> roots, final ProgressListener listener){
		// ---- 0. GEDCOM 7 is read through the syntax of GEDCOM 5.5.1 ----
		if(GEDCOM7Normalizer.isGEDCOM7(roots))
			GEDCOM7Normalizer.normalize(roots);

		// ---- 1. Register all existing IDs (for IDGenerator) ----
		listener.onProgress(0, "Registering identifiers…");
		Deduplicator.reset();
		IDGenerator.reset();
		registerIds(roots);

		// index the records
		for(GEDCOMNode node : roots){
			final String xref = node.getXrefId();
			if(xref == null)
				continue;

			recordsById.putIfAbsent(xref, node);
			if("NOTE".equals(node.getTag()))
				noteRawMap.put(xref, node);
		}

		// ---- 2. Instantiate converters ----
		HeaderConverter headerConverter = new HeaderConverter(model, noteRawMap);
		SubmitterConverter submitterConverter = new SubmitterConverter(submitterMap);
		MultimediaConverter multimediaConverter = new MultimediaConverter(model, multimediaMap, noteRawMap);
		RepositoryConverter repositoryConverter = new RepositoryConverter(model, repositoryMap, noteRawMap, placeCache);
		SourceConverter sourceConverter = new SourceConverter(model, noteRawMap, sourceMap, placeCache);
		IndividualConverter individualConverter = new IndividualConverter(model, individualMap, noteRawMap, recordsById,
			childFamilyLinks, placeCache);
		FamilyConverter familyConverter = new FamilyConverter(model, familyMap, individualMap, noteRawMap, recordsById,
			childFamilyLinks, placeCache);

		listener.onProgress(10, "Parsing records…");

		final int total = roots.size();
		int done = 0;

		// ---- 3. Header and the records referred to by other records ----
		for(GEDCOMNode node : roots)
			if("SUBM".equals(node.getTag()))
				submitterConverter.convert(node);
		for(GEDCOMNode node : roots)
			if("HEAD".equals(node.getTag()))
				headerConverter.convert(node, submitterMap);
		for(final String tag : REFERENCED_RECORD_TAGS)
			for(GEDCOMNode node : roots){
				if(!tag.equals(node.getTag()))
					continue;

				switch(tag){
					case "OBJE" -> multimediaConverter.convert(node);
					case "REPO" -> repositoryConverter.convert(node);
					default -> sourceConverter.convert(node);
				}

				done ++;
				listener.onProgress(10 + (int)(done * 60. / total), "Parsing " + tag + "…");
			}

		// ---- 4. Records that refer to other records ----
		// NOTE records are inlined where referred; SUBN (submission records) are ignored.
		for(GEDCOMNode node : roots){
			if(!REFERRING_RECORD_TAGS.contains(node.getTag()))
				continue;

			switch(node.getTag()){
				case "INDI" -> individualConverter.convert(node);
				default -> familyConverter.collect(node);
			}

			done ++;
			listener.onProgress(10 + (int)(done * 60. / total), "Parsing " + node.getTag() + "…");
		}

		listener.onProgress(75, "Resolving family links…");

		// ---- 5. Second pass: resolve family links and the sources of the multimedia ----
		familyConverter.resolveLinks();
		multimediaConverter.linkSources();

		// ---- 6. Add the records not yet in the model ----
		listener.onProgress(95, "Building model…");
		addMissingRecords(individualMap);
		addMissingRecords(familyMap);
		addMissingRecords(sourceMap);
		addMissingRecords(repositoryMap);
		addMissingRecords(multimediaMap);
		// Submitters are not added as top‑level records; they are included in the header.

		listener.onProgress(100, "Done");

		return model;
	}

	private void addMissingRecords(final Map<String, FLEFRecord> records){
		for(final FLEFRecord record : records.values())
			if(!model.hasRecord(record.getId()))
				model.addRecord(record);
	}

	/**
	 * Registers all existing IDs (from GEDCOM cross‑references) in the IDGenerator.
	 * This ensures that newly generated IDs start after the highest existing number.
	 */
	private void registerIds(List<GEDCOMNode> roots){
		for(GEDCOMNode node : roots){
			registerIdsRecursive(node);
		}
	}

	private void registerIdsRecursive(GEDCOMNode node){
		if(node.getXrefId() != null){
			IDGenerator.registerExistingId(node.getXrefId());
		}
		for(GEDCOMNode child : node.getChildren()){
			registerIdsRecursive(child);
		}
	}

}
