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
package io.github.mtrevisan.familylegacy.flef.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.cfg.Environment;

import java.util.HashMap;
import java.util.Map;


public class JPAUtil{

	private static EntityManagerFactory entityManagerFactory;


	public static EntityManager getEntityManager(){
		if(entityManagerFactory == null){
			final Map<String, Object> settings = new HashMap<>();
			//H2 driver and URL
			settings.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
			settings.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
			settings.put("jakarta.persistence.jdbc.user", "sa");
			settings.put("jakarta.persistence.jdbc.password", "");
			//Hibernate properties
			settings.put(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
			settings.put(Environment.HBM2DDL_AUTO, "update");
			settings.put(Environment.SHOW_SQL, "true");
			settings.put(Environment.FORMAT_SQL, "true");

			entityManagerFactory = Persistence.createEntityManagerFactory("FamilyLegacy");
		}
		return entityManagerFactory.createEntityManager();
	}

	public static void closeEntityManagerFactory(){
		if(entityManagerFactory != null && entityManagerFactory.isOpen())
			entityManagerFactory.close();
	}

	//	final Map<String, Object> settings = new HashMap<>();
	//	//H2 driver and URL
	//	settings.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
	//	settings.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
	//	settings.put("jakarta.persistence.jdbc.user", "sa");
	//	settings.put("jakarta.persistence.jdbc.password", "");
	//	//Hibernate properties
	//	settings.put(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
	//	settings.put(Environment.HBM2DDL_AUTO, "update");  // Crea/aggiorna schema al bisogno
	//	settings.put(Environment.SHOW_SQL, "true");        // Mostra le query SQL in console
	//	settings.put(Environment.FORMAT_SQL, "true");      // Format SQL
	//	try(
	//			final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("FamilyLegacy");
	//			final EntityManager entityManager = entityManagerFactory.createEntityManager();
	//		){
	//		entityManager.getTransaction().begin();
	//
	//		//TODO
	//		...
	//
	//		entityManager.persist(entity);
	//
	//		entityManager.getTransaction().commit();
	//	}

}
