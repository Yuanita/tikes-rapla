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

import org.rapla.entities.Entity;
import org.rapla.entities.Named;
import org.rapla.entities.Ownable;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.framework.TypedComponentRole;

/** Preferences store user-specific Information.
    You can store arbitrary configuration objects under unique role names.
    Each role can contain 1-n configuration entries.
    @see org.rapla.entities.User
 */
public interface Preferences extends Entity<Preferences>,Ownable, Named {
    final RaplaType<Preferences> TYPE = new RaplaType<Preferences>(Preferences.class, "preferences");
    /** puts a new configuration entry to the role.*/
    /** returns if there are any preference-entries */
    boolean isEmpty();
    
    boolean hasEntry(TypedComponentRole<?> role);
    void putEntry(TypedComponentRole<Boolean> role,Boolean entry);
    void putEntry(TypedComponentRole<Integer> role,Integer entry);
    void putEntry(TypedComponentRole<String> role,String entry);
    <T extends RaplaObject> void putEntry(TypedComponentRole<T> role,T entry);
    <T extends RaplaObject> T getEntry(TypedComponentRole<T> role);
    <T extends RaplaObject> T getEntry(TypedComponentRole<T> role, T defaultEntry);
    String getEntryAsString(TypedComponentRole<String> role, String defaultValue);
    Boolean getEntryAsBoolean(TypedComponentRole<Boolean> role, boolean defaultValue);
    Integer getEntryAsInteger(TypedComponentRole<Integer> role, int defaultValue);

    /** @deprecated use the typed version instead */
    @Deprecated
    void putEntry(String role,String entry);
    /** @deprecated use the typed version instead */
    @Deprecated
    String getEntryAsString(String role, String defaultValue);
    /** @deprecated use the typed version instead */
    @Deprecated
    boolean getEntryAsBoolean(String role, boolean defaultValue);
    /** @deprecated use the typed version instead */
    @Deprecated
    int getEntryAsInteger(String role, int defaultValue);
    
}











