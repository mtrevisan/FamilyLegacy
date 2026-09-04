package io.github.mtrevisan.familylegacy.v2.ui.components.individual;


/**
 * Defines the types of tree modification operations.
 */
public enum IndividualOperation{
	/** Adds a child to a couple (father + mother). */
	ADD_CHILD,
	/** Adds a partner (spouse) to an individual. */
	ADD_PARTNER,
	/** Adds a parent (father or mother) to an individual. */
	ADD_PARENT
}
