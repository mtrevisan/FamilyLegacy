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
package io.github.mtrevisan.familylegacy.flef.persistence.repositories;

import io.github.mtrevisan.familylegacy.flef.persistence.db.JPAUtil;
import io.github.mtrevisan.familylegacy.flef.persistence.db.Transactional;
import io.github.mtrevisan.familylegacy.flef.persistence.models.AbstractEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.AssertionEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.CalendarEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.CulturalNormEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.CulturalNormJunctionEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.EventEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.EventSuperTypeEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.EventTypeEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.GroupEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.GroupJunctionEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.HistoricDateEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.LocalizedPersonNameEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.LocalizedTextEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.LocalizedTextJunctionEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.MediaEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.MediaJunctionEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.ModificationEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.NoteEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.PersonEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.PersonNameEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.PlaceEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.ProjectEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.RepositoryEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.ResearchStatusEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.RestrictionEntity;
import io.github.mtrevisan.familylegacy.flef.persistence.models.SourceEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;


//https://www.infoworld.com/article/2260064/java-persistence-with-jpa-and-hibernate-part-2-persisting-data-to-a-database.html
public class Repository{

	//https://www.marcobehler.com/guides/spring-transaction-management-transactional-in-depth
	/*
        // Creazione del proxy del servizio
        Repository repo = TransactionHandler.createProxy(Repository.class);

        // Creazione di un utente
        repo.createUser(1L, "John Doe", "john.doe@example.com");

        // Recupero dell'utente
        User retrievedUser = repo.getUser(1L);
	*/

	@Transactional
	public <T extends AbstractEntity> void save(final T entity){
		try(final EntityManager entityManager = JPAUtil.getEntityManager()){
			entityManager.persist(entity);
		}
	}

	@Transactional
	public <T extends AbstractEntity> void update(final T entity){
		try(final EntityManager entityManager = JPAUtil.getEntityManager()){
			entityManager.merge(entity);
		}
	}

	public <T extends AbstractEntity> T findByID(final Class<T> entityClass, final Long id){
		try(final EntityManager entityManager = JPAUtil.getEntityManager()){
			final T entity = entityManager.find(entityClass, id);

			//recover referenced record
			if(entity != null){
				final String tableName = entity.getReferenceTable();
				final Long referenceID = entity.getReferenceID();

				if(tableName != null && referenceID != null){
					final AbstractEntity referencedEntity = findReferencedEntity(tableName, referenceID);
					entity.setReferencedEntity(referencedEntity);
				}
			}

			return entity;
		}
	}

	private AbstractEntity findReferencedEntity(final String tableName, final Long referenceID){
		try(final EntityManager entityManager = JPAUtil.getEntityManager()){
			final Class<? extends AbstractEntity> entityClass = getEntityClassFromTableName(tableName);
			return (entityClass != null? entityManager.find(entityClass, referenceID): null);
		}
	}

	private Class<? extends AbstractEntity> getEntityClassFromTableName(final String tableName){
		return switch(tableName){
			case "assertion" -> AssertionEntity.class;
			case "calendar" -> CalendarEntity.class;
			case "cultural_norm" -> CulturalNormEntity.class;
			case "cultural_norm_junction" -> CulturalNormJunctionEntity.class;
			case "event" -> EventEntity.class;
			case "event_super_type" -> EventSuperTypeEntity.class;
			case "event_type" -> EventTypeEntity.class;
			case "group" -> GroupEntity.class;
			case "group_junction" -> GroupJunctionEntity.class;
			case "historic_date" -> HistoricDateEntity.class;
			case "localized_person_name" -> LocalizedPersonNameEntity.class;
			case "localized_text" -> LocalizedTextEntity.class;
			case "localized_text_junction" -> LocalizedTextJunctionEntity.class;
			case "media" -> MediaEntity.class;
			case "media_junction" -> MediaJunctionEntity.class;
			case "modification" -> ModificationEntity.class;
			case "note" -> NoteEntity.class;
			case "person" -> PersonEntity.class;
			case "person_name" -> PersonNameEntity.class;
			case "place" -> PlaceEntity.class;
			case "project" -> ProjectEntity.class;
			case "repository" -> RepositoryEntity.class;
			case "research_status" -> ResearchStatusEntity.class;
			case "restriction" -> RestrictionEntity.class;
			case "source" -> SourceEntity.class;
			default -> throw new IllegalArgumentException("Unknown table name: " + tableName);
		};
	}

	public <T extends AbstractEntity> List<T> findAll(final Class<T> entityClass){
		final String tableName = entityClass.getAnnotation(Entity.class)
			.name();
		try(final EntityManager entityManager = JPAUtil.getEntityManager()){
			final TypedQuery<T> query = entityManager.createQuery("SELECT t FROM " + tableName + " t", entityClass);
			return query.getResultList();
		}
	}

	@Transactional
	public <T extends AbstractEntity> void delete(final T entity){
		try(final EntityManager entityManager = JPAUtil.getEntityManager()){
			if(entityManager.contains(entity))
				entityManager.remove(entity);
			else{
				final AbstractEntity managedEntity = entityManager.find(entity.getClass(), entity.getID());
				if(managedEntity != null)
					entityManager.remove(managedEntity);
			}
		}
	}

}
