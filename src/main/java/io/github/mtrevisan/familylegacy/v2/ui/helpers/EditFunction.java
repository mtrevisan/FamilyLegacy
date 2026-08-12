package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import javax.swing.JDialog;
import java.awt.Dialog;


@FunctionalInterface
public interface EditFunction{

	JDialog apply(Dialog dialog, FLEFModel model, FLEFRecord record);

}
