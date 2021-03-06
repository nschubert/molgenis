package org.molgenis.data.meta;

import org.molgenis.data.Fetch;
import org.molgenis.data.meta.model.Package;

import static org.molgenis.data.meta.model.EntityTypeMetadata.*;
import static org.molgenis.data.meta.model.Package.PACKAGE_SEPARATOR;
import static org.molgenis.data.system.model.RootSystemPackage.PACKAGE_SYSTEM;

public class MetaUtils
{
	public static Fetch getEntityTypeFetch()
	{
		// TODO simplify fetch creation (in this case *all* attributes and expand xref/mrefs)
		return new Fetch().field(ID).field(NAME).field(PACKAGE).field(LABEL).field(DESCRIPTION)
				.field(ATTRIBUTES).field(IS_ABSTRACT).field(EXTENDS).field(TAGS).field(BACKEND);
	}

	/**
	 * Returns whether the given package is a system package, i.e. it is the root system package or a descendent of the
	 * root system package.
	 *
	 * @param package_ package
	 * @return whether package is a system package
	 */
	public static boolean isSystemPackage(Package package_)
	{
		return package_.getFullyQualifiedName().equals(PACKAGE_SYSTEM) || (package_.getRootPackage() != null && package_
				.getRootPackage().getFullyQualifiedName().equals(PACKAGE_SYSTEM));
	}

	public static String getFullyQualyfiedName(String name, Package aPackage)
	{
		return aPackage == null ? name : aPackage.getFullyQualifiedName() + PACKAGE_SEPARATOR + name;
	}
}
