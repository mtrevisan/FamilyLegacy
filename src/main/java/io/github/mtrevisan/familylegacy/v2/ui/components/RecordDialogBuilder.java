package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;

import java.util.EnumMap;
import java.util.Map;


/**
 * Builder for {@link RecordDialogComponents}.
 * Provides sensible defaults but allows overriding every configurable parameter.
 */
public final class RecordDialogBuilder{

	/**
	 * Configuration for panels that reference an entity.
	 */
	public record EntityReferenceConfig(
		String tag,
		String title,
		Class<? extends RecordTypeHandler<?>> handlerClass,
		Class<? extends RecordTypeHandler<?>> handlerType
	){}


	// Required
	final BaseRecordDialog owner;
	final FLEFModel model;
	final FLEFRecord record;

	final Map<PanelKey, EntityReferenceConfig> configs = new EnumMap<>(PanelKey.class);


	public RecordDialogBuilder(final BaseRecordDialog owner, final FLEFModel model, final FLEFRecord record){
		this.owner = owner;
		this.model = model;
		this.record = record;
	}


	public <T extends Class<? extends RecordTypeHandler<?>>> RecordDialogBuilder withComponent(final PanelKey key,
			final String tag, final String title, final T handlerClass, final T handlerType){
		configs.put(key, new EntityReferenceConfig(tag, title, handlerClass, handlerType));

		return this;
	}

	/**
	 * Builds the {@link RecordDialogComponents} instance.
	 */
	public RecordDialogComponents build(){
		return new RecordDialogComponents(this);
	}

}
