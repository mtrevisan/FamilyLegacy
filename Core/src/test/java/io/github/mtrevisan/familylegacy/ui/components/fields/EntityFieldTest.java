package io.github.mtrevisan.familylegacy.ui.components.fields;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceCitationHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class EntityFieldTest{

	private static final String TAG_SUBJECT = "subject";

	private static EntityField oneOfField(){
		return EntityField.createForRecordFromOneofReference(TAG_SUBJECT, null, new FLEFModel());
	}

	private static FLEFRecord individual(){
		return FLEFRecord.createMainRecord("I1", "individual");
	}

	private static FLEFRecord group(){
		return FLEFRecord.createMainRecord("G1", "group");
	}


	@Test
	void theFieldAcceptsTheRecordsOfItsTypes(){
		final EntityField field = oneOfField().withHandlerTypes(IndividualHandler.class, GroupHandler.class);

		final FLEFRecord individual = individual();
		field.setEntity(individual);
		Assertions.assertSame(individual, field.getEntity());

		final FLEFRecord group = group();
		field.setEntity(group);
		Assertions.assertSame(group, field.getEntity());
	}

	@Test
	void theTypesCanBeRestrictedAfterwards(){
		final EntityField field = oneOfField().withHandlerTypes(IndividualHandler.class, GroupHandler.class);
		final FLEFRecord group = group();
		field.setEntity(group);

		//the target of a relationship of a group can only be a group
		Assertions.assertSame(field, field.restrictHandlerTypes(GroupHandler.class));

		final FLEFRecord individual = individual();
		field.setEntity(individual);
		Assertions.assertSame(group, field.getEntity(), "an individual is not accepted any more");
		final FLEFRecord anotherGroup = FLEFRecord.createMainRecord("G2", "group");
		field.setEntity(anotherGroup);
		Assertions.assertSame(anotherGroup, field.getEntity());

		//and widened again
		field.restrictHandlerTypes(IndividualHandler.class, GroupHandler.class);
		field.setEntity(individual);
		Assertions.assertSame(individual, field.getEntity());
	}

	@Test
	void restrictingTheTypesDoesNotSetTheFieldUpAgain(){
		final EntityField field = oneOfField().withHandlerTypes(IndividualHandler.class, GroupHandler.class);
		final int mouseListeners = field.getMouseListeners().length;
		final int propertyListeners = field.getPropertyChangeListeners(EntityField.PROPERTY_ENTITY_CHANGED).length;
		final int keyBindings = field.getInputMap().allKeys() != null? field.getInputMap().allKeys().length: 0;

		for(int i = 0; i < 3; i ++)
			field.restrictHandlerTypes(GroupHandler.class);

		//the behavior and the listeners are installed once
		Assertions.assertEquals(mouseListeners, field.getMouseListeners().length);
		Assertions.assertEquals(propertyListeners, field.getPropertyChangeListeners(EntityField.PROPERTY_ENTITY_CHANGED).length);
		Assertions.assertEquals(keyBindings, field.getInputMap().allKeys() != null? field.getInputMap().allKeys().length: 0);
	}

	@Test
	void theTypesAreAssignedOnlyOnce(){
		final EntityField field = oneOfField().withHandlerTypes(IndividualHandler.class);

		//the assertion (on in the tests) still protects the field from being set up twice
		Assertions.assertThrows(AssertionError.class, () -> field.withHandlerTypes(GroupHandler.class));
	}

	@Test
	void theTypesCannotBeRestrictedBeforeTheyAreAssigned(){
		Assertions.assertThrows(IllegalStateException.class, () -> oneOfField().restrictHandlerTypes(GroupHandler.class));
	}

	@Test
	void aFieldThatWrapsACitationCannotBeRestricted(){
		final EntityField field = EntityField.createForStructureWithReference("source", null, new FLEFModel(),
			SourceCitationHandler.class);

		Assertions.assertThrows(IllegalStateException.class, () -> field.restrictHandlerTypes(SourceCitationHandler.class));
	}

}
