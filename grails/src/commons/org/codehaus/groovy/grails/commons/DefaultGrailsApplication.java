/*
 * Copyright 2004-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package org.codehaus.groovy.grails.commons;

import groovy.lang.GroovyClassLoader;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.grails.exceptions.MoreThanOneActiveDataSourceException;
import org.springframework.core.io.Resource;

/**
 * 
 * 
 * @author Steven Devijver
 * @since Jul 2, 2005
 */
public class DefaultGrailsApplication implements GrailsApplication {

	private GroovyClassLoader cl = null;
	private GrailsControllerClass[] controllerClasses = null;
	private GrailsPageFlowClass[] pageFlows = null;
	private GrailsDomainClass[] domainClasses = null;
	private GrailsDataSource dataSource = null;
	private GrailsServiceClass[] services = null;
	
	private Map controllerMap = null;
	private Map domainMap = null;
	private Map pageFlowMap = null;
	private Map serviceMap = null;
	
	
	
	
	public DefaultGrailsApplication(Resource[] resources) throws IOException, ClassNotFoundException {
		super();
		
		this.cl = new GroovyClassLoader();
		for (int i = 0; resources != null && i < resources.length; i++) {
			try {
				cl.parseClass(resources[i].getFile());
			} catch (CompilationFailedException e) {
				throw new org.codehaus.groovy.grails.exceptions.CompilationFailedException("Compilation error in file [" + resources[i].getFilename() + "]: " + e.getMessage(), e);
			}
		}
		// get all the classes that were loaded
		Class[] classes = cl.getLoadedClasses();
		// first load the domain classes
		this.domainMap = new HashMap();
		for (int i = 0; i < classes.length; i++) {
			// check that it is a domain class
			if(GrailsClassUtils.isDomainClass(classes[i])) {				
				GrailsDomainClass grailsDomainClass = new DefaultGrailsDomainClass(classes[i]);				
				this.domainMap.put(grailsDomainClass.getName().substring(0, 1).toLowerCase() + grailsDomainClass.getName().substring(1), grailsDomainClass);						
			}
		}		
		
		this.controllerMap = new HashMap();
		this.pageFlowMap = new HashMap();
		this.serviceMap = new HashMap();
		for (int i = 0; i < classes.length; i++) {
			if (Modifier.isAbstract(classes[i].getModifiers())) {
				continue;
			}
			if (GrailsClassUtils.isControllerClass(classes[i])  /* && not ends with FromController */) {
				GrailsControllerClass grailsControllerClass = new DefaultGrailsControllerClass(classes[i]);
				if (grailsControllerClass.getAvailable()) {
					this.controllerMap.put(grailsControllerClass.getFullName(), grailsControllerClass);
				}
			} else if (GrailsClassUtils.isPageFlowClass(classes[i])) {
				GrailsPageFlowClass grailsPageFlowClass = new DefaultGrailsPageFlowClass(classes[i]);
				if (grailsPageFlowClass.getAvailable()) {
					this.pageFlowMap.put(grailsPageFlowClass.getFullName(), grailsPageFlowClass);
				}
			} else if (GrailsClassUtils.isDataSource(classes[i])) {
				GrailsDataSource tmpDataSource = new DefaultGrailsDataSource(classes[i]);
				if (tmpDataSource.getAvailable()) {
					if (dataSource == null) {
						dataSource = tmpDataSource;
					} else {
						throw new MoreThanOneActiveDataSourceException("More than one active data source is configured!");
					}
				}
			} else if (GrailsClassUtils.isService(classes[i])) {
				GrailsServiceClass grailsServiceClass = new DefaultGrailsServiceClass(classes[i]);
				serviceMap.put(grailsServiceClass.getFullName(), grailsServiceClass);
			}
		}
		
		this.controllerClasses = ((GrailsControllerClass[])controllerMap.values().toArray(new GrailsControllerClass[controllerMap.size()]));
		this.pageFlows = ((GrailsPageFlowClass[])pageFlowMap.values().toArray(new GrailsPageFlowClass[pageFlowMap.size()]));
		this.domainClasses = ((GrailsDomainClass[])this.domainMap.values().toArray(new GrailsDomainClass[domainMap.size()]));
		this.services = ((GrailsServiceClass[])this.serviceMap.values().toArray(new GrailsServiceClass[serviceMap.size()]));
	}

	public GrailsControllerClass[] getControllers() {
		return this.controllerClasses;
	}

	public GrailsControllerClass getController(String name) {
		return (GrailsControllerClass)this.controllerMap.get(name);
	}
	
	public GrailsControllerClass getControllerByURI(String uri) {
		for (int i = 0; i < controllerClasses.length; i++) {
			if (controllerClasses[i].mapsToURI(uri)) {
				return controllerClasses[i];
			}
		}
		return null;
	}

	public GrailsPageFlowClass getPageFlow(String fullname) {
		return (GrailsPageFlowClass)this.pageFlowMap.get(fullname);
	}
	
	public GrailsPageFlowClass[] getPageFlows() {
		return this.pageFlows;
	}
	
	public GroovyClassLoader getClassLoader() {
		return this.cl;
	}

	public GrailsDomainClass[] getGrailsDomainClasses() {
		return this.domainClasses;
	}

	public GrailsDomainClass getGrailsDomainClass(String name) {
			return (GrailsDomainClass)this.domainMap.get(name);
	}
	

	public GrailsDataSource getGrailsDataSource() {
		return this.dataSource;
	}
	
	public GrailsServiceClass[] getGrailsServiceClasses() {
		return this.services;
	}
	
	public GrailsServiceClass getGrailsServiceClass(String name) {
		return (GrailsServiceClass)this.serviceMap.get(name);
	}
}
