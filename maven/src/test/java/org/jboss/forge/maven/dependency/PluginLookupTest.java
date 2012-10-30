/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.maven.dependency;

import java.util.Set;

import org.jboss.forge.maven.container.MavenContainer;
import org.jboss.forge.maven.container.MavenDependencyResolver;
import org.jboss.forge.maven.dependency.filter.PackagingDependencyFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 *
 */
public class PluginLookupTest
{
   private DependencyResolver resolver;

   @Before
   public void setUp()
   {
      resolver = new MavenDependencyResolver(new MavenContainer());
   }

   @Test
   public void testResolveNonJarArtifact() throws Exception
   {

      Dependency dep = DependencyBuilder.create("org.jboss.forge:forge-example-plugin:2.0.0-SNAPSHOT")
               .setPackagingType("far");
      DependencyQueryBuilder query = DependencyQueryBuilder.create(dep).setFilter(new PackagingDependencyFilter("far"));
      Set<Dependency> artifacts = resolver.resolveDependencies(query);
      Assert.assertFalse(artifacts.isEmpty());
      Assert.assertEquals(1, artifacts.size());
      Dependency dependency = artifacts.iterator().next();
      Assert.assertEquals("far", dependency.getPackagingType());
      Assert.assertNotNull(dependency.getScopeType());
      Assert.assertTrue(dependency.isOptional());
   }
}