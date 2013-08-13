package org.jboss.forge.addon.validator;

import javax.validation.constraints.NotNull;

import org.jboss.forge.furnace.services.Exported;

@Exported
public class ManagedExportedObject
{
   public String sayHello(@NotNull String name)
   {
      return "Hello," + name;
   }
}
