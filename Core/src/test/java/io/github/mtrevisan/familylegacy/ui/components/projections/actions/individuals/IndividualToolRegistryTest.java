package io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class IndividualToolRegistryTest{

	@Test
	void everyConnectHasItsCounterpartInTheRelationshipTools(){
		final var tools = IndividualToolRegistry.relationshipTools();

		Assertions.assertTrue(tools.stream().anyMatch(ConnectPartnerTool.class::isInstance));
		Assertions.assertTrue(tools.stream().anyMatch(ConnectParentTool.class::isInstance));
		Assertions.assertTrue(tools.stream().anyMatch(ConnectChildTool.class::isInstance));
	}

	@Test
	void deleteIsAloneAndLastInTheMenu(){
		Assertions.assertEquals(1, IndividualToolRegistry.deleteTools().size());
		Assertions.assertInstanceOf(DeleteIndividualTool.class, IndividualToolRegistry.deleteTools().getFirst());
		for(final var group : java.util.List.of(IndividualToolRegistry.primaryTools(), IndividualToolRegistry.relationshipTools(),
				IndividualToolRegistry.secondaryTools(), IndividualToolRegistry.tertiaryTools()))
			Assertions.assertTrue(group.stream().noneMatch(DeleteIndividualTool.class::isInstance));

		Assertions.assertEquals(1, io.github.mtrevisan.familylegacy.ui.components.projections.actions.groups.GroupToolRegistry.deleteTools()
			.size());
	}

}
