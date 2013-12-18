/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org .       |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/

package org.rapla.storage.xml;

import java.util.Date;

import org.rapla.components.util.Assert;
import org.rapla.components.util.xml.RaplaSAXAttributes;
import org.rapla.entities.Annotatable;
import org.rapla.entities.Category;
import org.rapla.entities.IllegalAnnotationException;
import org.rapla.entities.User;
import org.rapla.entities.domain.Permission;
import org.rapla.entities.domain.internal.AllocatableImpl;
import org.rapla.entities.domain.internal.PermissionImpl;
import org.rapla.entities.storage.internal.ReferenceHandler;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

public class AllocatableReader extends RaplaXMLReader
{
    private DynAttReader dynAttHandler;
    private AllocatableImpl allocatable;
	private String annotationKey;
	private Annotatable currentAnnotatable;

    public AllocatableReader( RaplaContext context ) throws RaplaException
    {
        super( context );
        dynAttHandler = new DynAttReader( context );
        addChildHandler( dynAttHandler );
    }

    @Override
    public void processElement(
        String namespaceURI,
        String localName,
        RaplaSAXAttributes atts ) throws RaplaSAXParseException
    {
        if (namespaceURI.equals( DYNATT_NS ))
        {
            dynAttHandler.setClassifiable( allocatable );
            delegateElement(
                dynAttHandler,
                namespaceURI,
                localName,
                atts );
            return;
        }

        if (!namespaceURI.equals( RAPLA_NS ))
            return;

        String holdBackString = getString( atts, "holdbackconflicts", "false" );
        boolean holdBackConflicts = Boolean.valueOf( holdBackString ).booleanValue();
        if (localName.equals( "resource" ) || localName.equals( "person" ))
        {
            String createdAt = atts.getValue(  "created-at");
            String lastChanged = atts.getValue( "last-changed");

            Date createTime = null;
            Date changeTime = createTime;
            if (createdAt != null)
                createTime = parseTimestamp( createdAt);
            if (lastChanged != null)
                changeTime = parseTimestamp( lastChanged);

            allocatable = new AllocatableImpl(createTime, changeTime);
            currentAnnotatable = allocatable;
            setId( allocatable, atts );
            allocatable.setHoldBackConflicts( holdBackConflicts );
            setVersionIfThere( allocatable, atts);
            setLastChangedBy(allocatable, atts);
            setOwner(allocatable, atts);
        }

        
        if (localName.equals( "permission" ))
        {
            PermissionImpl permission = new PermissionImpl();

            // process user
            String userString = atts.getValue( "user" );
            ReferenceHandler referenceHandler = permission.getReferenceHandler();
            if (userString != null)
            {
                referenceHandler.putId("user", getId(User.TYPE, userString));
            }

            // process group
            String groupId = atts.getValue( "groupidref" );
            if (groupId != null)
            {
            	referenceHandler.putId("group", getId(Category.TYPE, groupId));
            }
            else
            {
                String groupName = atts.getValue( "group" );
                if (groupName != null)
                {
                    Category group= getGroup( groupName);
                    permission.setGroup( group);
                }
            }

            String startDate = getString( atts, "start-date", null );
            if (startDate != null)
            {
                permission.setStart( parseDate( startDate, false ) );
            }

            String endDate = getString( atts, "end-date", null );
            if (endDate != null)
            {
                permission.setEnd( parseDate( endDate, false ) );
            }

            String minAdvance = getString( atts, "min-advance", null );
            if (minAdvance != null)
            {
                permission.setMinAdvance( parseLong( minAdvance ).intValue() );
            }

            String maxAdvance = getString( atts, "max-advance", null );
            if (maxAdvance != null)
            {
                permission.setMaxAdvance( parseLong( maxAdvance ).intValue() );
            }

            String accessLevel = getString(
                atts,
                "access",
                Permission.ACCESS_LEVEL_NAMEMAP.get( Permission.ALLOCATE_CONFLICTS ) );
            Integer matchingLevel = Permission.ACCESS_LEVEL_NAMEMAP.findAccessLevel( accessLevel );
            if (matchingLevel  == null)
            {
                throw createSAXParseException( "Unknown access level '" + accessLevel + "'" );
            }
            permission.setAccessLevel( matchingLevel );
            allocatable.addPermission( permission );
        }
        
        if (localName.equals( "annotation" ) && namespaceURI.equals( RAPLA_NS ))
        {
            annotationKey = atts.getValue( "key" );
            Assert.notNull( annotationKey, "key attribute cannot be null" );
            startContent();
        }
        
    }

    @Override
    public void processEnd( String namespaceURI, String localName )
    {
        if (!namespaceURI.equals( RAPLA_NS ))
            return;

        if (localName.equals( "resource" ) || localName.equals( "person" ))
        {
            if (allocatable.getPermissions().length == 0)
                allocatable.addPermission( new PermissionImpl() );
            add( allocatable );
        }
        
        if (localName.equals( "annotation" ) && namespaceURI.equals( RAPLA_NS ))
        {
            try
            {
                String annotationValue = readContent().trim();
				currentAnnotatable.setAnnotation( annotationKey, annotationValue );
            }
            catch (IllegalAnnotationException ex)
            {
            }
        }
    }
}
