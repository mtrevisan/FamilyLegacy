/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

/**
 * Category of an event along a migration route. Used to order the stops,
 * to choose the marker shape and to compute statistics.
 */
public enum MigrationEventKind{

	BIRTH("Birth"),
	RESIDENCE("Residence"),
	MARRIAGE("Marriage"),
	IMMIGRATION("Immigration"),
	EMIGRATION("Emigration"),
	DEATH("Death"),
	BURIAL("Burial"),
	OTHER("Other");


	private final String displayLabel;


	MigrationEventKind(final String displayLabel){
		this.displayLabel = displayLabel;
	}


	public String getDisplayLabel(){
		return displayLabel;
	}

	/**
	 * Maps a FLEF event type to a migration kind. Unknown types fall back
	 * to {@link #OTHER}.
	 *
	 * @param eventType the FLEF {@code EventRecord.type} value; may be
	 *                  {@code null}
	 * @return the corresponding kind, never {@code null}
	 */
	public static MigrationEventKind fromEventType(final String eventType){
		if(eventType == null || eventType.isBlank())
			return OTHER;
		return switch(eventType.toLowerCase()){
			case "birth" -> BIRTH;
			case "death" -> DEATH;
			case "burial" -> BURIAL;
			case "marriage" -> MARRIAGE;
			case "immigration" -> IMMIGRATION;
			case "emigration" -> EMIGRATION;
			case "residence" -> RESIDENCE;
			default -> OTHER;
		};
	}

	@Override
	public String toString(){
		return displayLabel;
	}

}
