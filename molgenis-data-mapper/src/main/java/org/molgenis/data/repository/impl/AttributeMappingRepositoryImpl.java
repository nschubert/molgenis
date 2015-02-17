package org.molgenis.data.repository.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.Repository;
import org.molgenis.data.mapping.model.AttributeMapping;
import org.molgenis.data.meta.AttributeMappingMetaData;
import org.molgenis.data.meta.EntityMappingMetaData;
import org.molgenis.data.repository.AttributeMappingRepository;
import org.molgenis.data.support.MapEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.IdGenerator;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class AttributeMappingRepositoryImpl implements AttributeMappingRepository
{
	public static final EntityMetaData META_DATA = new AttributeMappingMetaData();

	@Autowired
	private IdGenerator idGenerator;

	private final Repository repository;

	public AttributeMappingRepositoryImpl(Repository repository)
	{
		this.repository = repository;
	}

	@Override
	public List<Entity> upsert(Collection<AttributeMapping> attributeMappings)
	{
		List<Entity> result = new ArrayList<Entity>();
		for (AttributeMapping attributeMapping : attributeMappings)
		{
			result.add(upsert(attributeMapping));
		}
		return result;
	}

	private Entity upsert(AttributeMapping attributeMapping)
	{
		Entity result;
		if (attributeMapping.getIdentifier() == null)
		{
			attributeMapping.setIdentifier(idGenerator.generateId().toString());
			result = toAttributeMappingEntity(attributeMapping);
			repository.add(result);
		}
		else
		{
			result = toAttributeMappingEntity(attributeMapping);
			repository.update(result);
		}
		return result;
	}

	@Override
	public List<AttributeMapping> getAttributeMappings(List<Entity> attributeMappingEntities,
			EntityMetaData sourceEntityMetaData, EntityMetaData targetEntityMetaData)
	{
		return Lists.transform(attributeMappingEntities, new Function<Entity, AttributeMapping>()
		{
			@Override
			public AttributeMapping apply(Entity attributeMappingEntity)
			{
				return toAttributeMapping(attributeMappingEntity, sourceEntityMetaData, targetEntityMetaData);
			}
		});

	}

	private AttributeMapping toAttributeMapping(Entity attributeMappingEntity, EntityMetaData sourceEntityMetaData,
			EntityMetaData targetEntityMetaData)
	{
		String identifier = attributeMappingEntity.getString(EntityMappingMetaData.IDENTIFIER);
		String targetAtributeName = attributeMappingEntity.getString(AttributeMappingMetaData.TARGETATTRIBUTEMETADATA);
		AttributeMetaData targetAttributeMetaData = targetEntityMetaData.getAttribute(targetAtributeName);
		String algorithm = attributeMappingEntity.getString(AttributeMappingMetaData.ALGORITHM);

		return new AttributeMapping(identifier, targetAttributeMetaData, algorithm);
	}

	private Entity toAttributeMappingEntity(AttributeMapping attributeMapping)
	{
		Entity attributeMappingEntity = new MapEntity(META_DATA);
		attributeMappingEntity.set(AttributeMappingMetaData.IDENTIFIER, attributeMapping.getIdentifier());
		attributeMappingEntity.set(AttributeMappingMetaData.TARGETATTRIBUTEMETADATA, attributeMapping
				.getTargetAttributeMetaData() != null ? attributeMapping.getTargetAttributeMetaData().getName() : null);
		attributeMappingEntity.set(AttributeMappingMetaData.ALGORITHM, attributeMapping.getAlgorithm());
		return attributeMappingEntity;
	}
}
