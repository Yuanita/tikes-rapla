/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Gereon Fassbender, Christopher Kohlhaas               |
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
package org.rapla.entities.domain;

import java.util.Date;

import org.rapla.components.util.TimeInterval;
import org.rapla.entities.Annotatable;
import org.rapla.entities.Entity;
import org.rapla.entities.Named;
import org.rapla.entities.Ownable;
import org.rapla.entities.RaplaType;
import org.rapla.entities.Timestamp;
import org.rapla.entities.User;
import org.rapla.entities.dynamictype.Classifiable;

/** Objects that implement allocatable can be allocated by reservations.
    @see Reservation
 */
public interface Allocatable extends Entity<Allocatable>,Named,Classifiable,Ownable,Timestamp, Annotatable {
    final RaplaType<Allocatable> TYPE = new RaplaType<Allocatable>(Allocatable.class, "resource");
    
    /** Conflicts for this allocatable should be ignored, if this flag is enabled.*/
    void setHoldBackConflicts(boolean enable);
    boolean isHoldBackConflicts();
    /** Static empty dummy Array. Mainly for using the toArray() method of the collection interface */
    Allocatable[] ALLOCATABLE_ARRAY = new Allocatable[0];
    // adds a permission. Permissions are stored in a hashset so the same permission can't be added twice
    void addPermission( Permission permission );
    boolean removePermission( Permission permission );

    /** returns if the user has the permission to allocate the resource in the given
        time. It returns <code>true</code> if for at least one permission calling
        <code>permission.covers()</code> 
        and <code>permission.affectsUser</code> yields <code>true</code>.
    */
    boolean canAllocate( User user, Date start, Date end, Date today );
   
    /** returns if the user has the permission to allocate the resource in at a time in the future without specifying the exact time  */
    boolean canAllocate(User user, Date today);
    
    /** returns the interval in which the user can allocate the resource. Returns null if the user can't allocate the resource */
    TimeInterval getAllocateInterval( User user, Date today);

    /** returns if the user has the permission to create a conflict for the resource.*/
    boolean canCreateConflicts( User user );
    /** returns if the user has the permission to modify the allocatable (and also its permission-table).*/
    boolean canModify( User user );
    
    /** returns if the user has the permission to read the information and the allocations of this resource.*/
    boolean canRead( User user );
    
    /** returns if the user has the permission to read only the information but not the allocations of this resource.*/
    boolean canReadOnlyInformation( User user );
    
    Permission[] getPermissions();
    Permission newPermission();
	boolean isPerson();
}












