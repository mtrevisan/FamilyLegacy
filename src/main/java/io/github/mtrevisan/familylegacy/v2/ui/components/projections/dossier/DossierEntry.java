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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.dossier;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;


/**
 * A single row inside a dossier section.
 * <p>
 * The record carries:
 * <ul>
 *   <li>a <b>label</b>, describing what the entry is about;</li>
 *   <li>a <b>value</b>, the human-readable content;</li>
 *   <li>an optional <b>subtitle</b>, used for extra context;</li>
 *   <li>an optional <b>evidence badge</b>, a compact string like
 *       {@code "orig/prim/dir"};</li>
 *   <li>a <b>backing record</b>, opened in the edit dialog on double-click;</li>
 *   <li>a <b>kind</b>, used by the panel to choose the rendering
 *       strategy ({@link Kind#NORMAL}, {@link Kind#NAME_HEADER},
 *       {@link Kind#NAME_PART});</li>
 *   <li>an optional <b>proof status</b>, set when a
 *       {@code ConclusionRecord} resolves this assertion. Rendered as a
 *       small colored dot on the left of the row.</li>
 * </ul>
 * The record is immutable.
 */
public record DossierEntry(
	String label,
	String value,
	String subtitle,
	String evidenceBadge,
	FLEFRecord editRecord,
	Kind kind,
	ProofStatus proofStatus
){

	public enum Kind{
		NORMAL,
		NAME_HEADER,
		NAME_PART
	}


	public DossierEntry{
		if(label == null)
			label = StringUtils.EMPTY;
		if(value == null)
			value = StringUtils.EMPTY;
		if(subtitle == null)
			subtitle = StringUtils.EMPTY;
		if(evidenceBadge == null)
			evidenceBadge = StringUtils.EMPTY;
		if(kind == null)
			kind = Kind.NORMAL;
		// proofStatus is intentionally nullable: absence means "no
		// formal conclusion about this assertion".
	}

	/** Backward-compatible constructor: no kind, no proof status. */
	public DossierEntry(final String label, final String value, final String subtitle,
		final String evidenceBadge, final FLEFRecord editRecord){
		this(label, value, subtitle, evidenceBadge, editRecord, Kind.NORMAL, null);
	}

	/** Backward-compatible constructor: no proof status. */
	public DossierEntry(final String label, final String value, final String subtitle,
		final String evidenceBadge, final FLEFRecord editRecord, final Kind kind){
		this(label, value, subtitle, evidenceBadge, editRecord, kind, null);
	}


	public static DossierEntry of(final String label, final String value){
		return new DossierEntry(label, value, StringUtils.EMPTY, StringUtils.EMPTY, null, Kind.NORMAL, null);
	}

	public static DossierEntry of(final String label, final String value, final FLEFRecord editRecord){
		return new DossierEntry(label, value, StringUtils.EMPTY, StringUtils.EMPTY, editRecord, Kind.NORMAL, null);
	}

	public static DossierEntry of(final String label, final String value, final String subtitle,
		final FLEFRecord editRecord){
		return new DossierEntry(label, value, subtitle, StringUtils.EMPTY, editRecord, Kind.NORMAL, null);
	}

	/** Badge row for a name type (e.g. {@code "name"}, {@code "birth_name"}). */
	public static DossierEntry nameHeader(final String typeLabel, final FLEFRecord editRecord){
		return new DossierEntry(StringUtils.EMPTY, typeLabel, StringUtils.EMPTY, StringUtils.EMPTY, editRecord, Kind.NAME_HEADER, null);
	}

	/** Part row for a name (e.g. label={@code "given"}, value={@code "Emilio"}). */
	public static DossierEntry namePart(final String partLabel, final String partValue,
		final FLEFRecord editRecord){
		return new DossierEntry(partLabel, partValue, StringUtils.EMPTY, StringUtils.EMPTY, editRecord, Kind.NAME_PART, null);
	}


	public boolean isEditable(){
		return (editRecord != null);
	}

	public boolean hasEvidenceBadge(){
		return !evidenceBadge.isEmpty();
	}

	public boolean hasSubtitle(){
		return !subtitle.isEmpty();
	}

	public boolean isNameHeader(){
		return (kind == Kind.NAME_HEADER);
	}

	public boolean isNamePart(){
		return (kind == Kind.NAME_PART);
	}

	public boolean hasProofStatus(){
		return (proofStatus != null);
	}

}
