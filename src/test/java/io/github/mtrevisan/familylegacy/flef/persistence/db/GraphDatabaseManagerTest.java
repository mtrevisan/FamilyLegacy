/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.persistence.db;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class GraphDatabaseManagerTest{

	@Test
	void shouldInsertNode(){
		GraphDatabaseManager.clearDatabase();

		Assertions.assertEquals(0, GraphDatabaseManager.count("Car"));

		Map<String, Object> carRecord = new HashMap<>(3);
		carRecord.put("id", 1);
		carRecord.put("make", "tesla");
		carRecord.put("model", "model3");
		GraphDatabaseManager.insert("Car", carRecord);


		List<Map<String, Object>> res = GraphDatabaseManager.findAllBy("Car", "id", 1);
		Assertions.assertEquals(1, res.size());

		Map<String, Object> carTesla = GraphDatabaseManager.findBy("Car", "id", 1);
		Assertions.assertNotNull(carTesla);
		Assertions.assertFalse(carTesla.isEmpty());

		Assertions.assertEquals(1, GraphDatabaseManager.count("Car"));
	}

	@Test
	void shouldUpdateNode() throws StoreException{
		GraphDatabaseManager.clearDatabase();

		Map<String, Object> carRecord = new HashMap<>(3);
		carRecord.put("id", 1);
		carRecord.put("make", "tesla");
		carRecord.put("model", "model3");
		GraphDatabaseManager.insert("Car", carRecord);

		carRecord.put("model", "model4");
		GraphDatabaseManager.update("Car", "id", carRecord);


		List<Map<String, Object>> res = GraphDatabaseManager.findAllBy("Car", "id", 1);
		Assertions.assertEquals(1, res.size());

		Map<String, Object> carTesla = GraphDatabaseManager.findBy("Car", "id", 1);
		Assertions.assertNotNull(carTesla);
		Assertions.assertFalse(carTesla.isEmpty());
		Assertions.assertEquals("model4", carTesla.get("model"));
	}


	@Test
	void shouldUpsertRelationshipByName() throws StoreException{
		GraphDatabaseManager.clearDatabase();

		Map<String, Object> carRecord = new HashMap<>(3);
		carRecord.put("id", 1);
		carRecord.put("make", "tesla");
		carRecord.put("model", "model3");
		GraphDatabaseManager.insert("Car", carRecord);

		Map<String, Object> ownerRecord = new HashMap<>(3);
		ownerRecord.put("id", 2);
		ownerRecord.put("firstName", "baeldung");
		ownerRecord.put("lastName", "baeldung");
		GraphDatabaseManager.insert("Person", ownerRecord);

		GraphDatabaseManager.upsertRelationship("Person", "id", 2,
			"Car", "id", 1,
			"owner", Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.CASCADE);


		Map<String, Object> carTesla = GraphDatabaseManager.findOtherRecord("Car", "id", 1,
			"owner");
		Assertions.assertNotNull(carTesla);
		Assertions.assertFalse(carTesla.isEmpty());
	}

	@Test
	void shouldUpsertRelationshipByNameAndProperty() throws StoreException{
		GraphDatabaseManager.clearDatabase();

		Map<String, Object> carRecord = new HashMap<>(3);
		carRecord.put("id", 1);
		carRecord.put("make", "tesla");
		carRecord.put("model", "model3");
		GraphDatabaseManager.insert("Car", carRecord);

		Map<String, Object> ownerRecord = new HashMap<>(3);
		ownerRecord.put("id", 2);
		ownerRecord.put("firstName", "baeldung");
		ownerRecord.put("lastName", "baeldung");
		GraphDatabaseManager.insert("Person", ownerRecord);

		Map<String, Object> relationshipRecord = new HashMap<>(1);
		relationshipRecord.put("licenseID", 12345);
		GraphDatabaseManager.upsertRelationship("Person", "id", 2,
			"Car", "id", 1,
			"owner", relationshipRecord, GraphDatabaseManager.OnDeleteType.CASCADE);


		Map<String, Object> carTesla = GraphDatabaseManager.findOtherRecord("Car", "id", 1,
			"owner", "licenseID", 12345);
		Assertions.assertNotNull(carTesla);
		Assertions.assertFalse(carTesla.isEmpty());
	}

	@Test
	void shouldDeleteRelationship() throws StoreException{
		GraphDatabaseManager.clearDatabase();

		Map<String, Object> carRecord = new HashMap<>(3);
		carRecord.put("id", 1);
		carRecord.put("make", "tesla");
		carRecord.put("model", "model3");
		GraphDatabaseManager.insert("Car", carRecord);

		Map<String, Object> ownerRecord = new HashMap<>(3);
		ownerRecord.put("id", 2);
		ownerRecord.put("firstName", "baeldung");
		ownerRecord.put("lastName", "baeldung");
		GraphDatabaseManager.insert("Person", ownerRecord);

		GraphDatabaseManager.upsertRelationship("Person", "id", 2,
			"Car", "id", 1,
			"owner", Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.CASCADE);


		boolean deleted = GraphDatabaseManager.deleteRelationship("Person", "id", 2,
			"Car", "id", 1,
			"owner");
		Assertions.assertTrue(deleted);

		Map<String, Object> carTesla = GraphDatabaseManager.findOtherRecord("Car", "id", 1,
			"owner");
		Assertions.assertNull(carTesla);
	}

	@Test
	void shouldDeleteAll() throws StoreException{
		GraphDatabaseManager.clearDatabase();

		Map<String, Object> carRecord = new HashMap<>(3);
		carRecord.put("id", 1);
		carRecord.put("make", "tesla");
		carRecord.put("model", "model3");
		GraphDatabaseManager.insert("Car", carRecord);

		Map<String, Object> ownerRecord = new HashMap<>(3);
		ownerRecord.put("id", 2);
		ownerRecord.put("firstName", "baeldung");
		ownerRecord.put("lastName", "baeldung");
		GraphDatabaseManager.insert("Person", ownerRecord);

		GraphDatabaseManager.upsertRelationship("Person", "id", 2,
			"Car", "id", 1,
			"owner", Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.CASCADE);

		GraphDatabaseManager.delete("Car", "id", 1);

		Map<String, Object> carTesla = GraphDatabaseManager.findBy("Car", "id", 1);
		Assertions.assertNull(carTesla);

		Map<String, Object> carOwner = GraphDatabaseManager.findBy("Owner", "id", 2);
		Assertions.assertNull(carOwner);

		boolean deleted = GraphDatabaseManager.deleteRelationship("Person", "id", 2,
			"Car", "id", 1,
			"owner");
		Assertions.assertFalse(deleted);
	}


//	public static void main(String[] args) throws StoreException{
//		GraphDatabaseManager.clearDatabase();
//
//		Map<String, Object> carRecord = new HashMap<>(3);
//		carRecord.put("id", 1);
//		carRecord.put("make", "tesla");
//		carRecord.put("model", "model3");
//		GraphDatabaseManager.insert("Car", carRecord);
//
//		Map<String, Object> ownerRecord = new HashMap<>(3);
//		ownerRecord.put("id", 2);
//		ownerRecord.put("firstName", "baeldung");
//		ownerRecord.put("lastName", "baeldung");
//		GraphDatabaseManager.insert("Person", ownerRecord);
//
//		Map<String, Object> relationshipRecord = new HashMap<>(1);
//		relationshipRecord.put("licenseID", 12345);
//		GraphDatabaseManager.upsertRelationship("Person", "id", 2,
//			"Car", "id", 1,
//			"owner", relationshipRecord, GraphDatabaseManager.OnDeleteType.CASCADE);
//
//		try(final Transaction tx = getTransaction()){
//			final Result result = tx.execute(
//				"MATCH (c:Car) <-[owner]- (p:Person) "
//					+ "WHERE c.make = 'tesla'"
//					+ "RETURN p.firstName, p.lastName");
//			while(result.hasNext()){
//				final Map<String, Object> row = result.next();
//				System.out.println("First Name: " + row.get("p.firstName"));
//				System.out.println("Last Name: " + row.get("p.lastName"));
//			}
//
//			tx.commit();
//		}
//
//		try(Transaction tx = GraphDatabaseManager.getTransaction()){
//			Result nodeCountResult = tx.execute("MATCH (n) RETURN count(n) AS nodeCount");
//			Object nodeCount = nodeCountResult.stream().iterator().next().get("nodeCount");
//
//			Result res2 = tx.execute("MATCH ()-[r]-() RETURN r");
//			while(res2.hasNext()){
//				final Object row = res2.next();
//				System.out.println("First Name: " + row);
//			}
//			Result relationshipCountResult = tx.execute("MATCH ()-[r]-() RETURN count(r) AS relationshipCount");
//			Object relationshipCount = relationshipCountResult.stream().iterator().next().get("relationshipCount");
//
//			System.out.println(nodeCount + "/" + relationshipCount);
//			tx.commit();
//		}
//
//		GraphDatabaseManager.delete("Car", "id", 1);
//
//		try(Transaction tx = GraphDatabaseManager.getTransaction()){
//			Result nodeCountResult = tx.execute("MATCH (n) RETURN count(n) AS nodeCount");
//			Object nodeCount = nodeCountResult.stream().iterator().next().get("nodeCount");
//
//			Result res2 = tx.execute("MATCH ()-[r]-() RETURN r");
//			while(res2.hasNext()){
//				final Object row = res2.next();
//				System.out.println("First Name: " + row);
//			}
//			Result relationshipCountResult = tx.execute("MATCH ()-[r]-() RETURN count(r) AS relationshipCount");
//			Object relationshipCount = relationshipCountResult.stream().iterator().next().get("relationshipCount");
//
//			System.out.println(nodeCount + "/" + relationshipCount);
//			tx.commit();
//		}
//	}

}
