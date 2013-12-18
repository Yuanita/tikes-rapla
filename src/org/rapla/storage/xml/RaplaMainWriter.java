/*--------------------------------------------------------------------------*
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
package org.rapla.storage.xml;

import java.io.IOException;
import java.util.Date;

import org.rapla.components.util.Assert;
import org.rapla.components.util.TimeInterval;
import org.rapla.entities.Category;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.internal.PreferencesImpl;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.storage.RefEntity;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.storage.LocalCache;

/** Stores the data from the local cache in XML-format to a print-writer.*/
public class RaplaMainWriter extends RaplaXMLWriter
{
    protected final static String OUTPUT_FILE_VERSION="1.0";
	String encoding = "utf-8";
    protected LocalCache cache;

    public RaplaMainWriter(RaplaContext context, LocalCache cache) throws RaplaException {
        super(context);
        this.cache = cache;
        Assert.notNull(cache);
    }
    
    public void setWriter( Appendable writer ) {
        super.setWriter( writer );
        for ( RaplaXMLWriter xmlWriter: writerMap.values()) {
            xmlWriter.setWriter( writer );
        }
    }
    
    
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    
    public void printContent() throws IOException,RaplaException {
        printHeader( 0, null, false );

        printCategories();
        println();
        printDynamicTypes();
        println();
        ((PreferenceWriter)getWriterFor(Preferences.TYPE)).printPreferences( cache.getPreferences( null ));
        println();
        printUsers();
        println();
        printAllocatables();
        println();
        printPeriods();
        println();
        printReservations();
        println();
        
        closeElement("rapla:data");
    }
    
    public void printDynamicTypes()  throws IOException,RaplaException {
        openElement("relax:grammar");
    	DynamicTypeWriter dynamicTypeWriter = (DynamicTypeWriter)getWriterFor(DynamicType.TYPE);
        for(  DynamicType type:cache.getCollection(DynamicType.class)) {
			dynamicTypeWriter.printDynamicType(type);
            println();
        }
        printStartPattern();
        closeElement("relax:grammar");
    }
    
    private void printStartPattern() throws IOException {
        openElement("relax:start");
        openElement("relax:choice");
        for(  DynamicType type:cache.getCollection(DynamicType.class)) 
        {
            openTag("relax:ref");
            att("name",type.getElementKey());
            closeElementTag();
        }
        closeElement("relax:choice");
        closeElement("relax:start");
    }

    
    public void printCategories() throws IOException,RaplaException {
        openElement("rapla:categories");
        
        CategoryWriter categoryWriter = (CategoryWriter)getWriterFor(Category.TYPE);
		Category[] categories = cache.getSuperCategory().getCategories();
    	for (int i=0;i<categories.length;i++) {
			categoryWriter.printCategory(categories[i]);
		}
        
        closeElement("rapla:categories");
    }
    public void printUsers()  throws IOException,RaplaException {
    	openElement("rapla:users");
        UserWriter userWriter = (UserWriter)getWriterFor(User.TYPE);
        println("<!-- Users of the system -->");
        for (User user:cache.getCollection(User.class)) {
			PreferencesImpl preferences = cache.getPreferences( user);
			String password = cache.getPassword(((RefEntity<?>)user).getId());
			userWriter.printUser( user, password, preferences);
        }
        closeElement("rapla:users");
    }

    
    public void printAllocatables() throws IOException,RaplaException {
        openElement("rapla:resources");
        println("<!-- resources -->");
        // Print all resources that are not persons
        AllocatableWriter allocatableWriter = (AllocatableWriter)getWriterFor(Allocatable.TYPE);
        for (Allocatable allocatable:cache.getCollection(Allocatable.class)) {
            if ( allocatable.isPerson() )
                continue;
			allocatableWriter.printAllocatable(allocatable);
        }
        // Print all Persons
        for (Allocatable allocatable:cache.getCollection(Allocatable.class)) {
        	if ( !allocatable.isPerson() )
                continue;
            allocatableWriter.printAllocatable(allocatable);
        }
        println();
        closeElement("rapla:resources");

    }
    
    void printPeriods() throws IOException, RaplaException {
        openElement("rapla:periods");
    	PeriodWriter periodWriter = (PeriodWriter)getWriterFor(Period.TYPE);
    	for (Period period:cache.getCollection(Period.class)) {
            periodWriter.printPeriod(period);
        }
        closeElement("rapla:periods");
    }
    
    void printReservations() throws IOException, RaplaException {
        openElement("rapla:reservations");
    	ReservationWriter reservationWriter = (ReservationWriter)getWriterFor(Reservation.TYPE);
        for (Reservation reservation: cache.getCollection(Reservation.class)) {
			reservationWriter.printReservation( reservation );
        }
        closeElement("rapla:reservations");
    }


    
    private void printHeader(long repositoryVersion, TimeInterval invalidateInterval, boolean resourcesRefresh) throws IOException
    {
        println("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?><!--*- coding: " + encoding + " -*-->");
        openTag("rapla:data");
        for (int i=0;i<NAMESPACE_ARRAY.length;i++) {
            String prefix = NAMESPACE_ARRAY[i][1];
            String uri = NAMESPACE_ARRAY[i][0];
            if ( prefix == null) {
                att("xmlns", uri);
            } else {
                att("xmlns:" + prefix, uri);
            }
            println();
        }
        att("version", RaplaMainWriter.OUTPUT_FILE_VERSION);
        if ( repositoryVersion > 0)
        {
            att("repositoryVersion", String.valueOf(repositoryVersion));
        }
        if ( resourcesRefresh)
        {
            att("resourcesRefresh", "true");
        }
        if ( invalidateInterval != null)
        {
            Date startDate = invalidateInterval.getStart();
            Date endDate = invalidateInterval.getEnd(); 
			String start;
			if ( startDate == null)
			{
				startDate = new Date(0);
			}
			start = dateTimeFormat.formatDate( startDate);
			att("startDate", start);
        	if ( endDate != null)
        	{
        		String end = dateTimeFormat.formatDate( endDate);
            	att("endDate", end);
        	}
        }
        closeTag();
    }

}



