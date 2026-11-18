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
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.util.ArrayList;
import java.util.List;


/**
 * Deletes a record of a live model following the deletion rule of the protocol: entries of repeated fields that reference the deleted
 * record are removed, optional single fields are cleared, records whose required single field referenced it are deleted (and so on,
 * in cascade), and a ConclusionRecord whose RESOLVES becomes empty or whose PREFERRED was removed is deleted.
 * <p>
 * For example, deleting a SourceRecord deletes its ExtractRecords (their {@code source} is required), and the references to those
 * extracts in the {@code extract} of the citations are removed together with the citations of the source.
 */
public final class RecordDeleter{

	private final ReferenceRewriter references;
	private final ProtocolRules rules;


	public RecordDeleter(final FLEFGrammar grammar){
		final GrammarSupport support = new GrammarSupport(grammar);
		references = new ReferenceRewriter(support);
		final RecordSignature signature = new RecordSignature(support);
		final FieldMerger fields = new FieldMerger(support, signature);
		rules = new ProtocolRules(support, references, fields);
	}


	/**
	 * Deletes the record and, in cascade, what depends on it.
	 *
	 * @param model	The model (changed in place).
	 * @param id	The id of the record to delete.
	 * @return	The ids of the deleted records, the given one first; empty if there is no such record.
	 */
	public List<String> delete(final FLEFModel model, final String id){
		final List<FLEFRecord> deleted = rules.delete(RecordStore.of(model, references), id);
		final List<String> ids = new ArrayList<>(deleted.size());
		for(final FLEFRecord record : deleted)
			ids.add(record.getId());
		return ids;
	}

}
