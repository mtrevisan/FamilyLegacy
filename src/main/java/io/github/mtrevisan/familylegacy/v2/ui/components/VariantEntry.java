package io.github.mtrevisan.familylegacy.v2.ui.components;

import javax.swing.*;
import java.awt.*;


/**
 * Represents a TEXT_VALUE_VARIANT (PHONETIC or TRANSCRIPTION).
 */
public class VariantEntry{

	private final String type;       // "PHONETIC" or "TRANSCRIPTION"
	private final String system;
	private final String transcriptionType;
	private final String value;

	public VariantEntry(String type, String system, String transcriptionType, String value){
		this.type = type;
		this.system = system;
		this.transcriptionType = transcriptionType;
		this.value = value;
	}

	public String getType(){
		return type;
	}

	public String getSystem(){
		return system;
	}

	public String getTranscriptionType(){
		return transcriptionType;
	}

	public String getValue(){
		return value;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		if("PHONETIC".equals(type)){
			sb.append("Phonetic [").append(system).append("]: ").append(value);
		}
		else{
			sb.append("Transcription [").append(system).append("]");
			if(transcriptionType != null && !transcriptionType.isEmpty()){
				sb.append(" (").append(transcriptionType).append(")");
			}
			sb.append(": ").append(value);
		}
		return sb.toString();
	}
}
