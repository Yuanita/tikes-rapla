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
package org.rapla.gui.internal.edit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.rapla.framework.RaplaContext;
import org.rapla.gui.internal.common.NamedListCellRenderer;

public class ListField<T> extends AbstractEditField implements ActionListener,FocusListener, MultiEditField, SetGetField<T>, SetGetCollectionField<T> {
	JPanel panel;
	JComboBox field;
	protected String nothingSelected;
	Vector<Object> list;
	boolean multipleValues = false; // indicator, shows if multiple different
									// values are shown in this field
	final String multipleValuesOutput = TextField.getOutputForMultipleValues();
	boolean includeNothingSelected;

	
	public ListField(RaplaContext sm, String fieldName, Collection<T> v)
	{
		this(sm, fieldName, false);
		setVector(v);
	}

	public ListField(RaplaContext sm, String fieldName, boolean includeNothingSelected)
	{
		super(sm);
		this.includeNothingSelected = includeNothingSelected;
		setFieldName(fieldName);
		panel = new JPanel();
		panel.setOpaque(false);
		field = new JComboBox();
		field.addActionListener(this);
		panel.setLayout(new BorderLayout());
		panel.add(field, BorderLayout.WEST);
		field.setRenderer(new NamedListCellRenderer(getI18n().getLocale()));
		nothingSelected = getString("nothing_selected");
		field.addFocusListener(this);
	}

	public void setVector(Collection<T> v) {
		this.list = new Vector<Object>(v);
		if ( includeNothingSelected)
		{
		    list.add( nothingSelected);
		}
		field.setModel(new DefaultComboBoxModel(list));
	}

	public void setRenderer(ListCellRenderer renderer) {
		field.setRenderer(renderer);
	}

	public Collection<T> getValues() {
		Object value = field.getSelectedItem();
		if (list.contains(nothingSelected) && nothingSelected.equals(value)) {
			return Collections.emptyList();
		} else {
			@SuppressWarnings("unchecked")
            T casted = (T) value;
            return Collections.singletonList( casted);
		}
	}
	
	 public T getValue() 
	 {
	     Collection<T> values = getValues();
	     if ( values.size() == 0)
	     {
	         return null;
	     }
	     else
	     {
	         T first = values.iterator().next();
	         return first;
	     }
	 }

	 public void setValue(T object) 
	 {
	        List<T> list;
	        if ( object == null)
	        {
	            list = Collections.emptyList();
	        }
	        else
	        {
	            list = Collections.singletonList(object);
	        }
	        setValues(list);
	 }

	 
	public void setValues(Collection<T> value) {
		if (list.contains(nothingSelected) && (value == null || value.size() == 0) ) {
			field.setSelectedItem(nothingSelected);
		} else {
		    if ( value != null && value.size() > 0)
		    {
		        T first = value.iterator().next();
                field.setSelectedItem(first);
		    }
		    else
		    {
		        field.setSelectedItem(null);
		    }
		}
	}

	public JComponent getComponent() {
		return panel;
	}

	public void actionPerformed(ActionEvent evt) {
		// checks if a new common value has been set
		if (multipleValues && field.getSelectedItem() != multipleValuesOutput) {
			// delete place holder for multiple different values
			multipleValues = false;
			field.removeItem(multipleValuesOutput);
		}
		fireContentChanged();
	}

	public void focusGained(FocusEvent evt) {

		Component focusedComponent = evt.getComponent();
		Component parent = focusedComponent.getParent();
		if (parent instanceof JPanel) {
			((JPanel) parent).scrollRectToVisible(focusedComponent
					.getBounds(null));
		}

	}

	public void focusLost(FocusEvent evt) {

	}

	// implementation of interface MultiEditField
	public boolean hasMultipleValues() {
		return multipleValues;
	}

	// implementation of interface MultiEditField
	public void setFieldForMultipleValues() {
		multipleValues = true;
		// place holder for multiple different values
		field.addItem(multipleValuesOutput);
		field.setSelectedItem(multipleValuesOutput);
	}
}
