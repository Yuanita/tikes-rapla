/*--------------------------------------------------------------------------*
| Copyright (C) 2006 Christopher Kohlhaas                                  |
|                                                                          |
| This program is free software; you can redistribute it and/or modify     |
| it under the terms of the GNU General Public License as published by the |
| Free Software Foundation. A copy of the license has been included with   |
| these distribution in the COPYING file, if not go to www.fsf.org         |
|                                                                          |
| As a special exception, you are granted the permissions to link this     |
| program with every library, which license fulfills the Open Source       |
| Definition as published by the Open Source Initiative (OSI).             |
*--------------------------------------------------------------------------*/
package org.rapla.entities.configuration;

import java.io.Serializable;

import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.framework.Configuration;
import org.rapla.framework.DefaultConfiguration;

/**
 * This class adds just the get Type method to the DefaultConfiguration so that the config can be stored in a preference object
 * @author ckohlhaas
 * @version 1.00.00
 * @since 2.03.00
 */
public class RaplaConfiguration extends DefaultConfiguration implements RaplaObject, Serializable{
   // Don't forget to increase the serialVersionUID when you change the fields
   private static final long serialVersionUID = 1;

   public static final RaplaType<RaplaConfiguration> TYPE = new RaplaType<RaplaConfiguration>(RaplaConfiguration.class, "config");

   
   /** Creates a RaplaConfinguration with one element of the specified name 
* @param name the element name
* @param content The content of the element. Can be null.
*/
   public RaplaConfiguration( String name, String content) {
	   super(name, content);
   }
   
   public RaplaConfiguration(String localName) {
	   super(localName);
   }

   public RaplaConfiguration(Configuration configuration) {
	   super( configuration);
   }

   public RaplaType getRaplaType() {
	   return TYPE;
   }
    
   public RaplaConfiguration replace(  Configuration oldChild, Configuration newChild) 
   {
	   return (RaplaConfiguration) super.replace( oldChild, newChild);
   }
          
   @Override
   protected RaplaConfiguration newConfiguration(String localName) {
	   return new RaplaConfiguration( localName);
   }
    
   

    
    
}
