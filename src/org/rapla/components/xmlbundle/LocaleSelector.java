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
package org.rapla.components.xmlbundle;

import java.util.Locale;


/** If you want to change the locales during runtime put a LocaleSelector
    in the base-context. Instances of I18nBundle will then register them-self
    as {@link LocaleChangeListener LocaleChangeListeners}. Change the locale
    with {@link #setLocale} and all bundles will try to load the appropriate resources.
 */
public interface LocaleSelector {

    void addLocaleChangeListener(LocaleChangeListener listener);

    void removeLocaleChangeListener(LocaleChangeListener listener);

    void setLocale(Locale locale);

    Locale getLocale();

    void setLanguage(String language);

    void setCountry(String country);

    String getLanguage();

}
