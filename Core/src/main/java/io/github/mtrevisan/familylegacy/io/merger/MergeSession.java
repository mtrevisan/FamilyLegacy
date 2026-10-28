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
package io.github.mtrevisan.familylegacy.io.merger;

import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * State of one merge or deduplication: the records being built, where they come from, and the decisions taken so far.
 * <p>
 * The session works on copies: the input models are never modified.
 */
final class MergeSession implements RecordStore{

	private static final String TAG_DATE = "date";


	final GrammarSupport grammar;
	final ReferenceRewriter references;
	final RecordSignature signature;
	final FieldMerger fields;
	/** The date and time of the merge, for the audit entries and the header. */
	final String now;

	private final FLEFGrammar flefGrammar;
	/** {@code true} when merging an incoming model into a base model, {@code false} when deduplicating one model. */
	private final boolean mergeMode;

	/** The records being built, in file order: base records first. */
	private final Map<String, FLEFRecord> records = new LinkedHashMap<>();
	private final Map<String, Integer> fileOrder = new HashMap<>();
	/** Current ids of the records that come from the incoming model. */
	private final Set<String> incoming = new HashSet<>();
	/** Current id to the id the record has in its input model, for the records that were renamed. */
	private final Map<String, String> inputIdOf = new HashMap<>();
	/** Absorbed id (current) to the id of the record that absorbed it. */
	private final Map<String, String> absorbedInto = new LinkedHashMap<>();
	private final Set<String> deleted = new LinkedHashSet<>();
	private final IdAllocator ids = new IdAllocator();

	private final List<MergeReport.Decision> decisions = new ArrayList<>();
	private final List<String> warnings = new ArrayList<>();

	private FLEFRecord header;


	MergeSession(final FLEFGrammar flefGrammar, final GrammarSupport grammar, final String now, final boolean mergeMode){
		this.flefGrammar = flefGrammar;
		this.grammar = grammar;
		this.now = now;
		this.mergeMode = mergeMode;

		references = new ReferenceRewriter(grammar);
		signature = new RecordSignature(grammar);
		fields = new FieldMerger(grammar, signature);
	}


	/**
	 * Loads the base model and, when merging, the incoming one, renaming the incoming records whose id is already used by the base
	 * (ids are local to a file: equal ids in two files say nothing about the records).
	 */
	void load(final FLEFModel base, final FLEFModel incomingModel){
		header = (base.getHeader() != null? FieldMerger.copy(base.getHeader()): null);
		for(final FLEFRecord record : base.getRecords()){
			add(FieldMerger.copy(record));
			ids.reserve(record.getId());
		}
		if(incomingModel == null)
			return;

		final List<FLEFRecord> copies = new ArrayList<>(incomingModel.getRecordCount());
		for(final FLEFRecord record : incomingModel.getRecords())
			copies.add(FieldMerger.copy(record));
		final Map<String, String> renames = new HashMap<>();
		for(final FLEFRecord copy : copies)
			if(ids.isTaken(copy.getId()))
				renames.put(copy.getId(), null);
			else
				ids.reserve(copy.getId());
		for(final Map.Entry<String, String> rename : renames.entrySet())
			rename.setValue(ids.allocate(rename.getKey()));
		references.rewrite(copies, renames);

		for(final FLEFRecord copy : copies){
			final String inputId = copy.getId();
			final String newId = renames.getOrDefault(inputId, inputId);
			copy.setId(newId);
			add(copy);
			incoming.add(newId);
			inputIdOf.put(newId, inputId);
		}

		mergeHeaders(incomingModel.getHeader());
	}

	private void add(final FLEFRecord record){
		fileOrder.put(record.getId(), fileOrder.size());
		records.put(record.getId(), record);
	}

	/**
	 * The merged file keeps the header of the base, gains the submitter contacts of the incoming file, and is dated now.
	 */
	private void mergeHeaders(final FLEFRecord incomingHeader){
		if(incomingHeader == null)
			return;
		if(header == null){
			header = FieldMerger.copy(incomingHeader);
			return;
		}

		final StructType headerType = (flefGrammar.getFileDefinition() != null
			&& grammar.resolve(flefGrammar.getFileDefinition().headerField().type()) instanceof StructType struct? struct: null);
		if(headerType != null)
			// differences in the singular fields (protocol, source, date) are expected between two files: they are not conflicts
			fields.mergeStructure(header, incomingHeader, headerType, "header", new ArrayList<>());

		final FLEFRecord date = FieldMerger.first(header, TAG_DATE);
		if(date != null)
			date.setValue(now);
	}


	boolean isMergeMode(){
		return mergeMode;
	}

	@Override
	public Collection<FLEFRecord> records(){
		return records.values();
	}

	List<FLEFRecord> records(final String kind){
		final List<FLEFRecord> result = new ArrayList<>();
		for(final FLEFRecord record : records.values())
			if(kind.equals(record.getTag()))
				result.add(record);
		return result;
	}

	@Override
	public FLEFRecord record(final String id){
		return records.get(id);
	}

	boolean isIncoming(final String id){
		return incoming.contains(id);
	}

	int fileOrder(final String id){
		return fileOrder.getOrDefault(id, Integer.MAX_VALUE);
	}

	/** The id a record has in its input model. */
	String inputId(final String id){
		return inputIdOf.getOrDefault(id, id);
	}

	/** How a record is named in audit comments and in the report. */
	String label(final String id){
		return (isIncoming(id)? "record " + inputId(id) + " of the merged file": "record " + id);
	}

	/** The current id of a record, following the merges it was absorbed by; {@code null} if it was deleted. */
	String resolve(String id){
		while(absorbedInto.containsKey(id))
			id = absorbedInto.get(id);
		return (deleted.contains(id)? null: id);
	}

	String newId(final String template){
		return ids.allocate(template);
	}

	void addRecord(final FLEFRecord record){
		add(record);
	}


	/**
	 * Completes the merges whose fields were already merged into the survivors: redirects every reference to the absorbed records
	 * and removes them.
	 *
	 * @param merges absorbed id to survivor id
	 */
	@Override
	public int absorb(final Map<String, String> merges){
		if(merges.isEmpty())
			return 0;

		for(final String absorbedId : merges.keySet())
			records.remove(absorbedId);
		absorbedInto.putAll(merges);
		return references.rewrite(records.values(), merges);
	}

	@Override
	public void delete(final String id){
		if(records.remove(id) != null)
			deleted.add(id);
	}

	boolean isDeleted(final String id){
		return deleted.contains(id);
	}


	void decide(final MergeReport.Decision decision){
		decisions.add(decision);
	}

	List<MergeReport.Decision> decisions(){
		return decisions;
	}

	void warn(final String warning){
		warnings.add(warning);
	}


	MergeReport report(){
		final FLEFModel model = new FLEFModel();
		if(header != null)
			model.setHeader(header);
		for(final FLEFRecord record : records.values())
			model.addRecord(record);

		final Map<String, String> idMapping = new LinkedHashMap<>();
		if(mergeMode){
			for(final String id : fileOrder.keySet().stream().filter(incoming::contains)
					.sorted((a, b) -> Integer.compare(fileOrder(a), fileOrder(b))).toList()){
				final String current = resolve(id);
				if(current != null)
					idMapping.put(inputId(id), current);
			}
		}
		else
			for(final String absorbedId : absorbedInto.keySet()){
				final String current = resolve(absorbedId);
				if(current != null)
					idMapping.put(absorbedId, current);
			}

		return new MergeReport(model, decisions, idMapping, warnings);
	}

}
