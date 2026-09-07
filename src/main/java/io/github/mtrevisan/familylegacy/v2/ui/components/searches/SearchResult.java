package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;


/**
 * Represents a search result: a matching record and an optional context (e.g., the event that caused the match).
 */
public class SearchResult{

	private final FLEFRecord record;
	private final FLEFRecord context; // e.g., the matching event for individuals

	public SearchResult(final FLEFRecord record){
		this(record, null);
	}

	public SearchResult(final FLEFRecord record, final FLEFRecord context){
		this.record = record;
		this.context = context;
	}

	public FLEFRecord getRecord(){
		return record;
	}

	public FLEFRecord getContext(){
		return context;
	}

}
