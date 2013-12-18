/*--------------------------------------------------------------------------*
 | Copyright (C) 2013 Christopher Kohlhaas                                  |
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
package org.rapla.facade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.rapla.components.util.DateTools;
import org.rapla.components.util.TimeInterval;
import org.rapla.components.xmlbundle.CompoundI18n;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.entities.Annotatable;
import org.rapla.entities.Category;
import org.rapla.entities.Entity;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.Named;
import org.rapla.entities.Ownable;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentFormater;
import org.rapla.entities.domain.Permission;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.ReservationStartComparator;
import org.rapla.entities.domain.internal.ReservationImpl;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ConstraintIds;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.internal.CategoryImpl;
import org.rapla.facade.internal.CalendarOptionsImpl;
import org.rapla.framework.Configuration;
import org.rapla.framework.Container;
import org.rapla.framework.DefaultConfiguration;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.framework.RaplaSynchronizationException;
import org.rapla.framework.TypedComponentRole;
import org.rapla.framework.logger.ConsoleLogger;
import org.rapla.framework.logger.Logger;

/**
    Base class for most components. Eases
    access to frequently used services, e.g. {@link I18nBundle}.
 */
public class RaplaComponent
{
	public static final TypedComponentRole<I18nBundle> RAPLA_RESOURCES = new TypedComponentRole<I18nBundle>("org.rapla.RaplaResources");
	public static final TypedComponentRole<RaplaConfiguration> PLUGIN_CONFIG= new TypedComponentRole<RaplaConfiguration>("org.rapla.plugin");
	private final ClientServiceManager serviceManager;
    private TypedComponentRole<I18nBundle> childBundleName;
    private Logger logger;
    private RaplaContext context;

    public RaplaComponent(RaplaContext context) {
        try {
            logger = context.lookup(Logger.class );
        } catch (RaplaContextException e) {
            logger = new ConsoleLogger();
        }
        this.context = context;
        this.serviceManager = new ClientServiceManager();
    }
    
    protected void setLogger(Logger logger) 
    {
    	this.logger = logger;
	}


    final public TypedComponentRole<I18nBundle> getChildBundleName() {
        return childBundleName;
    }

    final public void setChildBundleName(TypedComponentRole<I18nBundle> childBundleName) {
        this.childBundleName =  childBundleName;
    }

    final protected Container getContainer() throws RaplaContextException {
        return getContext().lookup(Container.class);
    }

    /** returns if the session user is admin */
    final public boolean isAdmin() {
        try {
            return getUser().isAdmin();
        } catch (RaplaException ex) {
        }
        return false;
    }

    /** returns if the session user is a registerer */
    final public boolean isRegisterer() {
        if (isAdmin())
        {
            return true;
        }
        try {
            Category registererGroup = getQuery().getUserGroupsCategory().getCategory(Permission.GROUP_REGISTERER_KEY);
            return getUser().belongsTo(registererGroup);
        } catch (RaplaException ex) {
        }
        return false;
    }

    final public boolean isModifyPreferencesAllowed() {
        if (isAdmin())
        {
            return true;
        }
        try {
            Category modifyPreferences = getQuery().getUserGroupsCategory().getCategory(Permission.GROUP_MODIFY_PREFERENCES_KEY);
            if ( modifyPreferences == null ) {
                return true;
            }
            return getUser().belongsTo(modifyPreferences);
        } catch (RaplaException ex) {
        }
        return false;
    }

    /** returns if the user has allocation rights for one or more resource */
    final public boolean canUserAllocateSomething(User user) throws RaplaException {
        Allocatable[] allocatables =getQuery().getAllocatables();
        if ( user.isAdmin() )
            return true;
        if (!canCreateReservation(user))
        {
        	return false;
        }
        for ( int i=0;i<allocatables.length;i++) {
            Permission[] permissions = allocatables[i].getPermissions();
            for ( int j=0;j<permissions.length;j++) {
                Permission p = permissions[j];
                if (!p.affectsUser( user ))
                {
                    continue;
                }
                if ( p.getAccessLevel() > Permission.READ)
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    final public boolean canCreateReservation(User user) {
    	boolean result = getQuery().canCreateReservations(user);
		return result;
    }
    
    
    final public boolean canCreateReservation()  {
    	try {
            User user = getUser();
            return canCreateReservation( user);
        } catch (RaplaException ex) {
            return false;
        }
    }

    protected boolean canAllocate() 
	{
		CalendarSelectionModel model = getService( CalendarSelectionModel.class);
    	
		//Date start, Date end,
		Collection<Allocatable> allocatables = getService(CalendarSelectionModel.class).getMarkedAllocatables();
		boolean canAllocate = true;
		Date start = getStartDate( model); 
		Date end = getEndDate( model, start); 
        for (Allocatable allo:allocatables)
        {
        	if (!canAllocate( start, end, allo))
        	{
        		canAllocate = false;
        	}
        }
		return canAllocate;
	}
    
    protected Date getStartDate(CalendarModel model) {
		Collection<TimeInterval> markedIntervals = model.getMarkedIntervals();
		Date startDate = null;
    	if ( markedIntervals.size() > 0)
    	{
    		TimeInterval first = markedIntervals.iterator().next();
    		startDate = first.getStart();
    	}
    	if ( startDate != null)
    	{
    		return startDate;
    	}
    	
		Date selectedDate = model.getSelectedDate();
		if ( selectedDate == null)
		{
			selectedDate = getQuery().today();
		}
		Date time = new Date (DateTools.MILLISECONDS_PER_MINUTE * getCalendarOptions().getWorktimeStartMinutes());
		startDate = getRaplaLocale().toDate(selectedDate,time);
		return startDate;
	}
	
	 protected Date getEndDate( CalendarModel model,Date startDate) {
		Collection<TimeInterval> markedIntervals = model.getMarkedIntervals();
		Date endDate = null;
    	if ( markedIntervals.size() > 0)
    	{
    		TimeInterval first = markedIntervals.iterator().next();
    		endDate = first.getEnd();
    	}
    	if ( endDate != null)
    	{
    		return endDate;
    	}
		return new Date(startDate.getTime() + DateTools.MILLISECONDS_PER_HOUR);
	}
    
    protected boolean canAllocate(Date start, Date end, Allocatable allocatables) {
        if ( allocatables == null) {
            return true;
        }
        try {
            User user = getUser();
			Date today = getQuery().today();
			return allocatables.canAllocate( user, start, end, today );
        } catch (RaplaException ex) {
            return false;
        }
    }
    
    /** returns if the current user is allowed to modify the object. */
    final public boolean canModify(Object object) {
        try {
            User user = getUser();
            return canModify(object, user);
        } catch (RaplaException ex) {
            return false;
        }
    }

    static public boolean canModify(Object object, User user) {
        if (object == null || !(object instanceof RaplaObject))
        {
            return false;
        }
        if ( user == null)
        {
            return false;
        }
        if (user.isAdmin())
            return true;
        if (object instanceof Ownable) {
            Ownable ownable = (Ownable) object;
            User owner = ownable.getOwner();
			if  ( owner != null && user.equals(owner))
            {
                return true;
            }
			if (object instanceof Allocatable) {
	            Allocatable allocatable = (Allocatable) object;
	            if (allocatable.canModify( user ))
	            {
	                return true;
	            }
	            if ( owner == null)
	            {
	            	Category[] groups = user.getGroups();
	            	for ( Category group: groups)
	            	{
	            		if (group.getKey().equals(Permission.GROUP_REGISTERER_KEY))
	            		{
	            			return true;
	            		}
	            	}
	            }
	        }
        }
        
        if (checkClassifiableModifyPermissions(object, user))
        {
            return true;
        }
        return false;
    }
    
    public boolean canRead(Appointment appointment,User user)
    {
    	Reservation  reservation = appointment.getReservation();
    	boolean canReadReservationsFromOthers = getQuery().canReadReservationsFromOthers( user);
		boolean result = canRead(reservation, user, canReadReservationsFromOthers);
		return result;
    }
    
    static public boolean canRead(Reservation reservation,User user, boolean canReadReservationsFromOthers)
    {
    	if ( user == null)
    	{
    		return true;
    	}
    	if ( canModify(reservation, user))
        {
     	   return true;
        }
    	if ( !canReadReservationsFromOthers)
    	{
    		return false;
    	}
    	if (checkClassifiablerReadPermissions(reservation, user))
    	{
    		return true;
    	}
    	return false;
    }
    
    static public boolean canRead(Allocatable allocatable, User user) {
       if ( canModify(allocatable, user))
       {
    	   return true;
       }
       if (checkClassifiablerReadPermissions(allocatable, user))
       {
           return true;
       }
       return false;
    }

    /** We check if an attribute with the permission_modify exists and look if the permission is set either globally (if boolean type is used) or for a specific user group (if category type is used)*/
	public static boolean checkClassifiableModifyPermissions(Object object,
			User user) {
		return checkClassifiablePermissions(object, user, ReservationImpl.PERMISSION_MODIFY, false);
	}
	
	public static boolean checkClassifiablerReadPermissions(
			Object object, User user) {
		return checkClassifiablePermissions(object, user, ReservationImpl.PERMISSION_READ, true);
	}
    
 	static Category dummyCategory = new CategoryImpl();
    // The dummy category is used if no permission attribute is found. Use permissionNotFoundReturns to set whether this means permssion granted or not
    private static boolean checkClassifiablePermissions(Object object, User user, String permissionKey, boolean permssionNotFoundReturnsYesCategory) {
    	Collection<Category> cat = getPermissionGroups(object, dummyCategory,permissionKey, permssionNotFoundReturnsYesCategory);
    	if ( cat == null)
    	{
    		return false;
    	}
    	if ( cat.size() == 1 && cat.iterator().next() == dummyCategory)
    	{
    		return true;
    	}
    	for (Category c: cat)
    	{
    		if (user.belongsTo( c) )
    		{
    			return true;
    		}
    	}
    	return false;
    }
    
    /** returns the group that is has the requested permission on the object
     **/
    public static Collection<Category> getPermissionGroups(Object object, Category yesCategory, String permissionKey, boolean permssionNotFoundReturnsYesCategory) 
	{
    	if (object instanceof Classifiable ) {
            final Classifiable classifiable = (Classifiable) object;
            
            Classification classification = classifiable.getClassification();
            if ( classification != null)
            {
                final DynamicType type = classification.getType();
                final Attribute attribute = type.getAttribute(permissionKey);
                if ( attribute != null)
                {
                    return getPermissionGroups(classification, yesCategory, attribute);
                }
                else
                {
                	if ( permssionNotFoundReturnsYesCategory  && yesCategory != null)
                	{
                		return Collections.singleton(yesCategory);
                	}
                	else
                	{
                		return null;
                	}
                	
                }
            }
        }
        return null;
	}

	private static Collection<Category> getPermissionGroups(Classification classification,
			Category yesCategory, final Attribute attribute) {
		final AttributeType type2 = attribute.getType();
		if (type2 == AttributeType.BOOLEAN)
		{
		    final Object value = classification.getValue( attribute);
		    if  (Boolean.TRUE.equals(value))
		    {
		    	return Collections.singleton(yesCategory);
		    }
		    else
		    {
		    	return null;
		    }
		}
		if ( type2 == AttributeType.CATEGORY)
		{
			 Collection<?> values = classification.getValues( attribute);
			 Collection<Category> cat = (Collection<Category>) values;
             if ( cat == null || cat.size() == 0)
             {
                 Category rootCat = (Category)attribute.getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
                 if ( rootCat.getCategories().length == 0)
                 {
                     cat = Collections.singleton(rootCat);
                 }
             }
		    return cat;
		}
		return null;
	}


    public CalendarOptions getCalendarOptions() {
    	User user;
    	try
    	{
    		user = getUser();
    	} 
    	catch (RaplaException ex) {
    		// Use system settings if an error occurs
    		user = null;
        }
    	return getCalendarOptions( user);
    }
    
    protected CalendarOptions getCalendarOptions(User user) {
        RaplaConfiguration conf = null;
        try {
            if ( user != null)
            {
                conf = getQuery().getPreferences( user ).getEntry(CalendarOptionsImpl.CALENDAR_OPTIONS);
            }
            if ( conf == null)
            {
                conf = getQuery().getPreferences( null ).getEntry(CalendarOptionsImpl.CALENDAR_OPTIONS);
            }
            if ( conf != null)
            {
                return new CalendarOptionsImpl( conf );
            }
        } catch (RaplaException ex) {

        }
        return getService( CalendarOptions.class);
    }

    protected User getUser() throws RaplaException {
    	return getUserModule().getUser();
    }

    protected Logger getLogger() {
        return logger;
    }

    /** lookup the service in the serviceManager under the specified key:
        serviceManager.lookup(role).
        @throws IllegalStateException if GUIComponent wasn't serviced. No service method called
        @throws UnsupportedOperationException if service not available.
     */
	protected <T> T getService(Class<T> role) {
        try {
            return context.lookup( role);
        } catch (RaplaContextException e) {
             throw serviceExcption(role, e); 
        }
    }

    protected UnsupportedOperationException serviceExcption(Object role, RaplaContextException e) {
        return new UnsupportedOperationException("Service not supported in this context: " + role, e);
    }
   
    protected <T> T getService(TypedComponentRole<T> role) {
        try {
            return context.lookup(role);
        } catch (RaplaContextException e) {
            throw serviceExcption(role, e); 
        }
    }
    
    protected RaplaContext getContext() {
        return context;
    }

    /** lookup RaplaLocale from the context */
    protected RaplaLocale getRaplaLocale() {
        if (serviceManager.raplaLocale == null)
            serviceManager.raplaLocale = getService(RaplaLocale.class);
        return serviceManager.raplaLocale;
    }


    protected Locale getLocale() {
        return getRaplaLocale().getLocale();
    }

    protected I18nBundle childBundle;
    /** lookup I18nBundle from the serviceManager */
    protected I18nBundle getI18n() {
    	TypedComponentRole<I18nBundle> childBundleName = getChildBundleName();
        if ( childBundleName != null) {
            if ( childBundle == null) {
                I18nBundle pluginI18n = getService(childBundleName );
                childBundle = new CompoundI18n(pluginI18n,getI18nDefault());
            }
            return childBundle;
        }
        return getI18nDefault();
    }

    private I18nBundle getI18nDefault() {
        if (serviceManager.i18n == null)
            serviceManager.i18n = getService(RaplaComponent.RAPLA_RESOURCES);
        return serviceManager.i18n;
    }

    /** lookup AppointmentFormater from the serviceManager */
    protected AppointmentFormater getAppointmentFormater() {
        if (serviceManager.appointmentFormater == null)
            serviceManager.appointmentFormater = getService(AppointmentFormater.class);
        return serviceManager.appointmentFormater;
    }

    /** lookup PeriodModel from the serviceManager */
    protected PeriodModel getPeriodModel() {
    	try {
    		return getQuery().getPeriodModel();
    	} catch (RaplaException ex) {
    		throw new UnsupportedOperationException("Service not supported in this context: " );
    	}
    }

    /** lookup QueryModule from the serviceManager */
    protected QueryModule getQuery() {
        return getClientFacade();
    }

    final protected ClientFacade getClientFacade() {
        if (serviceManager.facade == null)
            serviceManager.facade =  getService( ClientFacade.class );
        return serviceManager.facade;
    }

    /** lookup ModificationModule from the serviceManager */
    protected ModificationModule getModification() {
        return getClientFacade();
    }

    /** lookup UpdateModule from the serviceManager */
    protected UpdateModule getUpdateModule() {
        return getClientFacade();
    }

    /** lookup UserModule from the serviceManager */
   protected UserModule getUserModule() {
        return getClientFacade();
    }

    /** returns a translation for the object name into the selected language. If
     a translation into the selected language is not possible an english translation will be tried next.
     If theres no translation for the default language, the first available translation will be used. */
    public String getName(Object object) {
        if (object == null)
            return "";
        if (object instanceof Named) {
            String name = ((Named) object).getName(getI18n().getLocale());
            return (name != null) ? name : "";
        }
        return object.toString();
    }

    /** calls getI18n().getString(key) */
    final public String getString(String key) {
        return getI18n().getString(key);
    }


    /** calls "&lt;html>" + getI18n().getString(key) + "&lt;/html>"*/
    final public String getStringAsHTML(String key) {
        return "<html>" + getI18n().getString(key) + "</html>";
    }

    private static class ClientServiceManager  {
        I18nBundle i18n;
        ClientFacade facade;
        RaplaLocale raplaLocale;
        AppointmentFormater appointmentFormater;
    }

    final public Preferences newEditablePreferences() throws RaplaException {
        return  getModification().edit(getQuery().getPreferences());
    }
    
    /** @deprecated demand webservice in constructor instead*/
    @Deprecated 
    final public <T> T getWebservice(Class<T> a) throws RaplaException
    {
    	org.rapla.storage.dbrm.RemoteServiceCaller remote = getService( org.rapla.storage.dbrm.RemoteServiceCaller.class);
    	return remote.getRemoteMethod(a);
    }
    

    public Configuration getPluginConfig(String pluginClassName) throws EntityNotFoundException,
			RaplaException {
				RaplaConfiguration raplaConfig  = getQuery().getPreferences(null).getEntry(RaplaComponent.PLUGIN_CONFIG);
				Configuration pluginConfig = null;
				if ( raplaConfig != null) {
					pluginConfig = raplaConfig.find("class", pluginClassName);
				}
				if ( pluginConfig == null) {
					pluginConfig = new DefaultConfiguration("plugin");
				}
				return pluginConfig;
			}

	public static boolean isTemplate(RaplaObject<?> obj) 
	{
		if ( obj instanceof Appointment)
		{
			obj = ((Appointment) obj).getReservation();
		}
		if ( obj instanceof Annotatable)
		{
			String template = ((Annotatable)obj).getAnnotation( Reservation.TEMPLATE);
			return template != null;
		}
		return false;
	}

	protected List<Entity<Reservation>> copy(Collection<Reservation> toCopy, Date beginn) throws RaplaException 
	{
		List<Reservation> sortedReservations = new ArrayList<Reservation>(  toCopy);
		Collections.sort( sortedReservations, new ReservationStartComparator(getLocale()));
		List<Entity<Reservation>> copies = new ArrayList<Entity<Reservation>>();
		Date firstStart = null;
		for (Reservation reservation: sortedReservations) {
		    if ( firstStart == null )
		    {
		        firstStart = ReservationStartComparator.getStart( reservation);
		    }
		    Reservation copy = copy(reservation, beginn, firstStart);
		    copies.add( copy);
		}
		return copies;
	}
	
	public Reservation copyAppointment(Reservation reservation, Date beginn) throws RaplaException 
	{
		Date firstStart = ReservationStartComparator.getStart( reservation);
		Reservation copy = copy(reservation, beginn, firstStart);
		return copy;
	}
	
	private Reservation copy(Reservation reservation, Date destStart,Date firstStart) throws RaplaException {
		Reservation r =  getModification().clone( reservation);
		Appointment[] appointments = r.getAppointments();
	
		for ( Appointment app :appointments) {
			Repeating repeating = app.getRepeating();
		    
		    Date oldStart = app.getStart();
		    // we need to calculate an offset so that the reservations will place themself relativ to the first reservation in the list
		    long offset = DateTools.countDays( firstStart, oldStart) * DateTools.MILLISECONDS_PER_DAY;
		    Date newStart ;
		    Date destWithOffset = new Date(destStart.getTime() + offset );
		    newStart = getRaplaLocale().toDate(  destWithOffset  , oldStart );
		    app.move( newStart) ;
		    if (repeating != null)
		    {
		    	Date[] exceptions = repeating.getExceptions();
	    		repeating.clearExceptions();
	       		for (Date exc: exceptions)
	    		{
	    		 	long days = DateTools.countDays(oldStart, exc);
	        		Date newDate = DateTools.addDays(newStart, days);
	        		repeating.addException( newDate);
	    		}
		    	
		    	if ( !repeating.isFixedNumber())
		    	{
		        	Date oldEnd = repeating.getEnd();
		        	if ( oldEnd != null)
		        	{
		        		// If we don't have and endig destination, just make the repeating to the original length
		        		long days = DateTools.countDays(oldStart, oldEnd);
		        		Date end = DateTools.addDays(newStart, days);
		        		repeating.setEnd( end);
		        	}
		    	}	    
		    }
		}
		return r;
	}
	
	public static void unlock(Lock lock)
	{
		if ( lock != null)
		{
			lock.unlock();
		}
	}

	public static Lock lock(Lock lock, int seconds) throws RaplaException {
		try
		{
			if ( lock.tryLock())
			{
				return lock;
			}
			if (lock.tryLock(seconds, TimeUnit.SECONDS))
			{
				return lock;
			}
			else
			{
				throw new RaplaSynchronizationException("Someone is currently writing. Please try again! Can't acquire lock " + lock );
			}
		}
		catch (InterruptedException ex)
		{
			throw new RaplaSynchronizationException( ex);
		}
	}

	
}
