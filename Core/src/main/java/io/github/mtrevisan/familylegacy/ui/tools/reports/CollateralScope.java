package io.github.mtrevisan.familylegacy.ui.tools.reports;

/**
 * How far beyond the direct line a multi-person report should reach.
 *
 * <p>The interpretation of each value depends on the direction of the
 * report:</p>
 *
 * <table>
 *   <caption>Per-direction meaning</caption>
 *   <tr><th>Value</th><th>Ancestors report</th><th>Descendants report</th></tr>
 *   <tr>
 *     <td>{@code NONE}</td>
 *     <td>Direct ancestors only.</td>
 *     <td>Direct descendants only.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code SPOUSES}</td>
 *     <td>Ancestors plus their spouses (usually redundant, but useful when
 *         step-parents are documented).</td>
 *     <td>Descendants plus their spouses.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code SIBLINGS}</td>
 *     <td>Ancestors plus their siblings (uncles / aunts) and the siblings'
 *         spouses.</td>
 *     <td>Descendants plus their spouses, plus children of those spouses
 *         from other unions (step-children).</td>
 *   </tr>
 *   <tr>
 *     <td>{@code COUSINS}</td>
 *     <td>Ancestors plus siblings, plus the descendants of those siblings
 *         (first cousins, second cousins, ...).</td>
 *     <td>Descendants plus spouses, plus step-children, plus descendants of
 *         step-children (extended family).</td>
 *   </tr>
 *   <tr>
 *     <td>{@code EXTENDED}</td>
 *     <td colspan="2">Everyone connected to the direct line by any documented
 *         relationship, including {@code associate}.</td>
 *   </tr>
 * </table>
 *
 * <p>The value is ignored by single-person reports, where the concept of a
 * collateral line does not apply.</p>
 */
public enum CollateralScope{
	NONE,
	SPOUSES,
	SIBLINGS,
	COUSINS,
	EXTENDED
}
