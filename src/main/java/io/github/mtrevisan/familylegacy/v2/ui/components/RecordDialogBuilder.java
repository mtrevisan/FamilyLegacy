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
		String title
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
			final String tag, final String title){
		configs.put(key, new EntityReferenceConfig(tag, title));

		return this;
	}

	/**
	 * Builds the {@link RecordDialogComponents} instance.
	 */
	public RecordDialogComponents build(){
		return new RecordDialogComponents(this);
	}

}
