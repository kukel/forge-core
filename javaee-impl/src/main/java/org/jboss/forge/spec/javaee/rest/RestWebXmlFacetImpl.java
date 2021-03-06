/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.spec.javaee.rest;

import javax.inject.Inject;

import org.jboss.forge.env.Configuration;
import org.jboss.forge.env.ConfigurationFactory;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.spec.javaee.RestFacet;
import org.jboss.forge.spec.javaee.RestWebXmlFacet;
import org.jboss.forge.spec.javaee.ServletFacet;
import org.jboss.shrinkwrap.descriptor.impl.spec.servlet.web.WebAppDescriptorImpl;
import org.jboss.shrinkwrap.descriptor.spi.node.Node;

/**
 * @Author Paul Bakker - paul.bakker@luminis.eu
 */
@Alias("forge.spec.jaxrs.webxml")
public class RestWebXmlFacetImpl extends BaseFacet implements RestWebXmlFacet
{
   public static final String JAXRS_SERVLET = "javax.ws.rs.core.Application";

   @Inject
   private ConfigurationFactory configurationFactory;
   
   // Do not refer this field directly. Use the getProjectConfiguration() method instead.
   private Configuration configuration;

   @Override
   public boolean install()
   {
      if (!installedInWebXML())
      {
         // TODO this needs to be fixed in desciptors (allow creation of servlet mapping)
         ServletFacet servlet = project.getFacet(ServletFacet.class);
         WebAppDescriptorImpl web = (WebAppDescriptorImpl) servlet.getConfig();
         Node node = web.getRootNode();
         Node servletClass = node.getSingle("servlet-mapping/servlet-name=" + JAXRS_SERVLET);
         if (servletClass == null)
         {
            Node mapping = node.createChild("servlet-mapping");
            mapping.createChild("servlet-name").text(JAXRS_SERVLET);
            String urlPattern = getProjectConfiguration().getString(RestFacet.ROOTPATH);
            if (urlPattern.endsWith("/"))
            {
               urlPattern = urlPattern.substring(0, urlPattern.length() - 1);
            }
            mapping.createChild("url-pattern").text(urlPattern + "/*");
         }

         servlet.saveConfig(web);
      }

      return true;
   }

   @Override
   public boolean isInstalled()
   {
      return installedInWebXML();
   }

   private boolean installedInWebXML()
   {
      return getServletPath() != null;
   }

   @Override
   public String getServletPath()
   {
      if (project.hasFacet(ServletFacet.class))
      {
         ServletFacet servlet = project.getFacet(ServletFacet.class);
         WebAppDescriptorImpl web = (WebAppDescriptorImpl) servlet.getConfig();

         Node node = web.getRootNode();
         Node servletClass = node.getSingle("servlet-mapping/servlet-name=" + JAXRS_SERVLET);
         if (servletClass != null)
         {
            Node url = servletClass.getParent().getSingle("url-pattern");
            if (url != null)
            {
               return url.getText();
            }
         }
      }
      return null;
   }

   @Override
   public void setApplicationPath(final String path)
   {
      getProjectConfiguration().setProperty(RestFacet.ROOTPATH, path);
      ServletFacet servlet = project.getFacet(ServletFacet.class);
      WebAppDescriptorImpl web = (WebAppDescriptorImpl) servlet.getConfig();

      Node node = web.getRootNode();
      Node servletClass = node.getSingle("servlet-mapping/servlet-name=" + JAXRS_SERVLET);

      if (servletClass != null)
      {
         servletClass.getParent().getOrCreate("url-pattern").text(path);
      }

      servlet.saveConfig(web);
   }
   
   /**
    * Important: Use this method always to obtain the configuration. Do not invoke this inside a constructor since the
    * returned {@link Configuration} instance would not be the project scoped one.
    * 
    * @return The project scoped {@link Configuration} instance
    */
   private Configuration getProjectConfiguration()
   {
      if (this.configuration == null)
      {
         this.configuration = configurationFactory.getProjectConfig(project);
      }
      return this.configuration;
   }
}
