package org.jboss.forge.addon.validator;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import javax.xml.bind.JAXBException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class MethodValidatorTest
{
   @Deployment
   @Dependencies({ @AddonDependency(name = "org.jboss.forge.addon:validator"),
            @AddonDependency(name = "org.jboss.forge.furnace.container:cdi") })
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap
               .create(ForgeArchive.class)
               .addClass(ManagedExportedObject.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("org.jboss.forge.addon:validator"),
                        AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi"));

      return archive;
   }

   @Inject
   private ManagedExportedObject exportedObject;

   @Test
   public void testInjection() throws Exception
   {
      Assert.assertNotNull(exportedObject);
   }

   @Test(expected = ConstraintViolationException.class)
   public void testValidationFail() throws Exception
   {
      exportedObject.sayHello(null);
   }

   @Test
   public void testValidationPass() throws Exception
   {
      exportedObject.sayHello("Forge");
   }

}
