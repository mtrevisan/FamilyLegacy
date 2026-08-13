package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;

import java.awt.Dialog;


@FunctionalInterface
public interface CreateNewFunction{

	BaseRecordDialog apply(Dialog dialog, FLEFModel model);

}
