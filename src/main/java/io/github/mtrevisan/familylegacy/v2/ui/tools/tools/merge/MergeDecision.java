package io.github.mtrevisan.familylegacy.v2.ui.tools.tools.merge;


/**
 * What to do with a field that exists on both the source and the target
 * record and whose instances are not identical.
 */
public enum MergeDecision{

	/** The target's instances win; the source's are discarded. */
	KEEP_TARGET("Keep target"),

	/** The source's instances replace the target's. */
	KEEP_SOURCE("Keep source"),

	/**
	 * The target's instances are kept and the source's are added after
	 * them. Only meaningful for fields that can appear multiple times
	 * (notes, sources, names, events, ...). For single-valued fields it
	 * produces a schema-invalid record.
	 */
	KEEP_BOTH("Keep both");


	private final String label;


	MergeDecision(final String label){
		this.label = label;
	}


	public String label(){
		return label;
	}

}
