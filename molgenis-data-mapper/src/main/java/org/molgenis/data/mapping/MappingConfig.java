package org.molgenis.data.mapping;

import java.util.UUID;

import org.molgenis.data.DataService;
import org.molgenis.data.Repository;
import org.molgenis.data.algorithm.AlgorithmService;
import org.molgenis.data.algorithm.AlgorithmServiceImpl;
import org.molgenis.data.meta.AttributeMappingMetaData;
import org.molgenis.data.meta.EntityMappingMetaData;
import org.molgenis.data.meta.MappingProjectMetaData;
import org.molgenis.data.meta.MappingTargetMetaData;
import org.molgenis.data.repository.impl.AttributeMappingRepositoryImpl;
import org.molgenis.data.repository.impl.EntityMappingRepositoryImpl;
import org.molgenis.data.repository.impl.MappingProjectRepositoryImpl;
import org.molgenis.data.repository.impl.MappingTargetRepositoryImpl;
import org.molgenis.security.user.MolgenisUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.IdGenerator;

@Configuration
public class MappingConfig
{
	@Autowired
	DataService dataService;

	@Autowired
	MolgenisUserService userService;

	@Bean
	public IdGenerator idGenerator()
	{
		return new IdGenerator()
		{
			@Override
			public UUID generateId()
			{
				com.eaio.uuid.UUID uuid = new com.eaio.uuid.UUID();
				return UUID.fromString(uuid.toString());
			}
		};
	}

	@Bean
	public MappingService mappingService()
	{
		return new MappingServiceImpl();
	}

	@Bean
	public AlgorithmService algorithmServiceImpl()
	{
		return new AlgorithmServiceImpl();
	}

	@Bean
	public MappingProjectRepositoryImpl mappingProjectRepository()
	{
		return new MappingProjectRepositoryImpl(mappingProjectCrudRepository(), mappingTargetRepository());
	}

	@Bean
	public MappingTargetRepositoryImpl mappingTargetRepository()
	{
		return new MappingTargetRepositoryImpl(mappingTargetCrudRepository(), entityMappingRepository());
	}

	@Bean
	public EntityMappingRepositoryImpl entityMappingRepository()
	{
		return new EntityMappingRepositoryImpl(entityMappingCrudRepository(), attributeMappingRepository());
	}

	@Bean
	public AttributeMappingRepositoryImpl attributeMappingRepository()
	{
		return new AttributeMappingRepositoryImpl(attributeMappingCrudRepository());
	}

	@Bean
	public Repository attributeMappingCrudRepository()
	{
		if (!dataService.hasRepository(AttributeMappingMetaData.ENTITY_NAME))
		{
			dataService.getMeta().addEntityMeta(AttributeMappingRepositoryImpl.META_DATA);
		}
		return dataService.getRepository(AttributeMappingMetaData.ENTITY_NAME);

	}

	@Bean
	public Repository entityMappingCrudRepository()
	{
		attributeMappingCrudRepository();
		if (!dataService.hasRepository(EntityMappingMetaData.ENTITY_NAME))
		{
			dataService.getMeta().addEntityMeta(EntityMappingRepositoryImpl.META_DATA);
		}
		return dataService.getRepository(EntityMappingMetaData.ENTITY_NAME);
	}

	@Bean
	public Repository mappingTargetCrudRepository()
	{
		entityMappingCrudRepository();
		if (!dataService.hasRepository(MappingTargetMetaData.ENTITY_NAME))
		{
			dataService.getMeta().addEntityMeta(MappingTargetRepositoryImpl.META_DATA);
		}
		return dataService.getRepository(MappingTargetMetaData.ENTITY_NAME);
	}

	@Bean
	public Repository mappingProjectCrudRepository()
	{
		mappingTargetCrudRepository();
		if (!dataService.hasRepository(MappingProjectMetaData.ENTITY_NAME))
		{
			dataService.getMeta().addEntityMeta(MappingProjectRepositoryImpl.META_DATA);
		}
		return dataService.getRepository(MappingProjectMetaData.ENTITY_NAME);
	}

}
