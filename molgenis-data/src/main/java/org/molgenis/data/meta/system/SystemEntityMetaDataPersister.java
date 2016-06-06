package org.molgenis.data.meta.system;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.molgenis.data.meta.EntityMetaDataMetaData.ENTITY_META_DATA;
import static org.molgenis.data.meta.RootSystemPackage.PACKAGE_SYSTEM;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.molgenis.data.DataService;
import org.molgenis.data.RepositoryCollection;
import org.molgenis.data.meta.AttributeMetaData;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.meta.EntityMetaData;
import org.molgenis.data.meta.EntityMetaDataMetaData;
import org.molgenis.data.meta.Package;
import org.molgenis.data.meta.PackageMetaData;
import org.molgenis.data.meta.TagMetaData;
import org.molgenis.util.DependencyResolver;
import org.molgenis.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Persists {@link org.molgenis.data.meta.SystemEntityMetaData} in the meta data {@link org.molgenis.data.RepositoryCollection}.
 */
@Component
public class SystemEntityMetaDataPersister
{
	private final DataService dataService;
	private final SystemEntityMetaDataRegistry systemEntityMetaRegistry;

	@Autowired
	private TagMetaData tagMeta;

	@Autowired
	private AttributeMetaDataMetaData attributeMetaMeta;

	@Autowired
	private PackageMetaData packageMeta;

	@Autowired
	private EntityMetaDataMetaData entityMetaMeta;

	@Autowired
	public SystemEntityMetaDataPersister(DataService dataService, SystemEntityMetaDataRegistry systemEntityMetaRegistry)
	{
		this.dataService = requireNonNull(dataService);
		this.systemEntityMetaRegistry = requireNonNull(systemEntityMetaRegistry);
	}

	public void persist(ContextRefreshedEvent event)
	{
		RepositoryCollection repositoryCollection = dataService.getMeta().getDefaultBackend();

		// create meta entity tables
		// TODO make generic with dependency resolving, use MetaDataService.isMetaEntityMetaData
		if (!repositoryCollection.hasRepository(tagMeta))
		{
			repositoryCollection.createRepository(tagMeta);
		}
		if (!repositoryCollection.hasRepository(attributeMetaMeta))
		{
			repositoryCollection.createRepository(attributeMetaMeta);
		}
		if (!repositoryCollection.hasRepository(packageMeta))
		{
			repositoryCollection.createRepository(packageMeta);
		}
		if (!repositoryCollection.hasRepository(entityMetaMeta))
		{
			repositoryCollection.createRepository(entityMetaMeta);
		}

		// add default meta entities
		ApplicationContext ctx = event.getApplicationContext();
		Map<String, Package> packageMap = ctx.getBeansOfType(Package.class);
		DependencyResolver.resolve(packageMap.values().stream()).filter(this::isNotPersisted).forEach(this::persist);

		// persist entity meta data
		Set<EntityMetaData> metaEntityMetaSet = systemEntityMetaRegistry.getSystemEntityMetaDatas().collect(toSet());
		DependencyResolver.resolve(metaEntityMetaSet).stream().forEach(this::persist);

		// remove entity meta data
		removeNonExistingSystemEntities();
	}

	private static void populateAutoAttributeValues(EntityMetaData existingEntityMeta, EntityMetaData entityMeta)
	{
		// inject existing auto-generated identifiers in system entity meta data
		Map<String, String> attrMap = stream(existingEntityMeta.getAllAttributes().spliterator(), false)
				.collect(toMap(AttributeMetaData::getName, AttributeMetaData::getIdentifier));
		entityMeta.getAllAttributes().forEach(attr -> {
			String attrIdentifier = attrMap.get(attr.getName());
			if (attrIdentifier != null)
			{
				attr.setIdentifier(attrIdentifier);
			}
			else
			{
				// new attribute in java class
			}
		});
	}

	private void persist(EntityMetaData entityMeta)
	{
		EntityMetaData existingEntityMeta = dataService
				.findOneById(ENTITY_META_DATA, entityMeta.getName(), EntityMetaData.class);
		if (existingEntityMeta == null)
		{
			dataService.getMeta().addEntityMeta(entityMeta);
		}
		else
		{
			populateAutoAttributeValues(existingEntityMeta, entityMeta);

			if (!EntityUtils.equals(entityMeta, existingEntityMeta))
			{
				dataService.getMeta().updateEntityMeta(entityMeta);
			}
		}
	}

	private boolean isNotPersisted(Package package_)
	{
		return dataService.findOneById(packageMeta.getName(), package_.getIdValue()) == null;
	}

	private void persist(Package package_)
	{
		dataService.add(packageMeta.getName(), package_);
	}

	private void removeNonExistingSystemEntities()
	{
		Stream<EntityMetaData> removedEntityMetas = dataService.findAll(ENTITY_META_DATA, EntityMetaData.class)
				.filter(this::isSystemEntity).filter(this::isNotExists);
		dataService.delete(ENTITY_META_DATA, removedEntityMetas); // FIXME dependency resolving?
	}

	private boolean isSystemEntity(EntityMetaData entityMeta)
	{
		Package package_ = entityMeta.getPackage();
		if (package_ == null)
		{
			return false;
		}
		if (package_.getName().equals(PACKAGE_SYSTEM))
		{
			return true;
		}
		Package rootPackage = package_.getRootPackage();
		if (rootPackage != null && rootPackage.getName().equals(PACKAGE_SYSTEM))
		{
			return true;
		}
		return false;
	}

	private boolean isNotExists(EntityMetaData entityMeta)
	{
		return !systemEntityMetaRegistry.hasSystemEntityMetaData(entityMeta.getName());
	}
}
