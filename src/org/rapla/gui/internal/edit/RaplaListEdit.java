package org.rapla.gui.internal.edit;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.rapla.components.calendar.NavButton;
import org.rapla.components.calendar.RaplaArrowButton;
import org.rapla.components.layout.TableLayout;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.gui.toolkit.AWTColorUtil;
import org.rapla.gui.toolkit.RaplaButton;
import org.rapla.gui.toolkit.RaplaWidget;

final public class RaplaListEdit<T> implements
        RaplaWidget
{
    boolean coloredBackground = false;
    int[] oldIndex = new int[0];
    JPanel mainPanel = new JPanel();

    JPanel statusBar = new JPanel();
    JPanel identifierPanel = new JPanel();
    JLabel identifier = new JLabel();
    JLabel nothingSelectedLabel = new JLabel();
    JScrollPane scrollPane;

    NavButton prev = new NavButton('^');
    NavButton next = new NavButton('v');

    RaplaArrowButton moveUpButton = new RaplaArrowButton('^', 25);
    RaplaArrowButton moveDownButton = new RaplaArrowButton('v', 25);

    Color selectionBackground = UIManager.getColor("List.selectionBackground");
    Color background = UIManager.getColor("List.background");

    JPanel jointPanel = new JPanel() {
        private static final long serialVersionUID = 1L;

            int xa[] = new int[4];
            int ya[] = new int[4];
            public void paint(Graphics g) {
                super.paint(g);
                Dimension dim = getSize();
                int index = list.getSelectedIndex();

                Rectangle rect = list.getCellBounds(index,index);
                if (rect != null) {
                    int y = rect.y -scrollPane.getViewport().getViewPosition().y;
                    int y1= Math.min(dim.height,Math.max(0, y)  + scrollPane.getLocation().y);
                    int y2= Math.min(dim.height,Math.max(0,y + rect.height) + scrollPane.getLocation().y);
                    xa[0]=0;
                    ya[0]=y1;
                    xa[1]=dim.width;
                    ya[1]=0;
                    xa[2]=dim.width;
                    ya[2]=dim.height;
                    xa[3]=0;
                    ya[3]=y2;
                    g.setColor(selectionBackground);
                    g.fillPolygon(xa,ya,4);
                    g.setColor(background);
                    g.drawLine(xa[0],ya[0],xa[1],ya[1]);
                    g.drawLine(xa[3],ya[3],xa[2],ya[2]);
                }
            }
        };
    JPanel content = new JPanel();
    JPanel detailContainer = new JPanel();
    JPanel editPanel = new JPanel();

    JList list = new JList() {
        private static final long serialVersionUID = 1L;

            public void setModel(ListModel model) {
                super.setModel( model );
                model.addListDataListener(new ListDataListener() {
                        public void contentsChanged(ListDataEvent e) {
                            modelUpdate();
                        }
                        public void intervalAdded(ListDataEvent e) {
                        }
                        public void intervalRemoved(ListDataEvent e) {
                        }
                    });
            }
        };

    public RaplaButton createNewButton = new RaplaButton();
    public RaplaButton removeButton = new RaplaButton();

    CardLayout cardLayout = new CardLayout();
    private Listener listener = new Listener();
    private ActionListener callback;

    JPanel toolbar = new JPanel();
    
    public RaplaListEdit(I18nBundle i18n, JComponent detailContent,ActionListener callback) 
    {
        this.callback = callback;
        toolbar.setLayout( new BoxLayout( toolbar, BoxLayout.X_AXIS));
        toolbar.add(createNewButton);
        toolbar.add(removeButton);
        toolbar.add(Box.createHorizontalStrut(5));
        toolbar.add(moveUpButton);
        toolbar.add(moveDownButton);
        mainPanel.setLayout(new TableLayout(new double[][] {
            {TableLayout.PREFERRED,TableLayout.PREFERRED,TableLayout.FILL}
            ,{TableLayout.FILL}
        }));
        jointPanel.setPreferredSize(new Dimension(15,50));
        mainPanel.add(content,"0,0");
        mainPanel.add(jointPanel,"1,0");
        mainPanel.add(editPanel,"2,0");
        editPanel.setLayout(cardLayout);
        editPanel.add(nothingSelectedLabel, "0");
        editPanel.add(detailContainer, "1");
        content.setLayout(new BorderLayout());
        content.add(toolbar,BorderLayout.NORTH);
        scrollPane = new JScrollPane(list
                                     ,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
                                     ,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(310,80));
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(statusBar, BorderLayout.SOUTH);
        statusBar.setLayout( new FlowLayout(FlowLayout.LEFT));
        detailContainer.setLayout(new BorderLayout());
        editPanel.setBorder(BorderFactory.createRaisedBevelBorder());
        detailContainer.add(identifierPanel,BorderLayout.WEST);
        detailContainer.add(detailContent, BorderLayout.CENTER);
        //mainPanel.setBorder(new LineBorder(Color.black));

        identifierPanel.setLayout(new BorderLayout());
        identifierPanel.add(prev,BorderLayout.NORTH);
        identifierPanel.add(identifier,BorderLayout.CENTER);
        identifierPanel.add(next,BorderLayout.SOUTH);
        identifier.setBorder(new EmptyBorder(0,5,0,5));

        next.addActionListener(listener);
        prev.addActionListener(listener);
        removeButton.addActionListener(listener);
        createNewButton.addActionListener(listener);
        moveUpButton.addActionListener(listener);
        moveDownButton.addActionListener(listener);

        scrollPane.getViewport().addChangeListener(listener);

        //      list.setDragEnabled(true);
        list.addMouseListener(listener);

        list.addListSelectionListener(listener);

        modelUpdate();

        createNewButton.setText(i18n.getString("new"));
        createNewButton.setIcon(i18n.getIcon("icon.new"));
        removeButton.setIcon(i18n.getIcon("icon.delete"));
        removeButton.setText(i18n.getString("delete"));
        nothingSelectedLabel.setHorizontalAlignment(JLabel.CENTER);
        nothingSelectedLabel.setText(i18n.getString("nothing_selected"));
    }

    public JPanel getToolbar()
    {
    	return toolbar;
    }
    
    public JComponent getComponent() {
        return mainPanel;
    }

    public JPanel getStatusBar() {
        return statusBar;
    }

    public JList getList() {
        return list;
    }

    public void setListDimension(Dimension d) {
        scrollPane.setPreferredSize(d);
    }

    public void setMoveButtonVisible(boolean visible) {
        moveUpButton.setVisible(visible);
        moveDownButton.setVisible(visible);
    }

    public int getSelectedIndex() {
        return list.getSelectedIndex();
    }

    public void select(int index) {
        list.setSelectedIndex(index);
        if (index >=0) {
            list.ensureIndexIsVisible(index);
        }
    }

    public void setColoredBackgroundEnabled(boolean enable) {
        coloredBackground = enable;
    }

    public boolean isColoredBackgroundEnabled() {
        return coloredBackground;
    }

    private void modelUpdate() {
        removeButton.setEnabled(list.getMinSelectionIndex() >=0);
        moveUpButton.setEnabled(list.getMinSelectionIndex() > 0);
        moveDownButton.setEnabled(list.getMinSelectionIndex() >= 0 &&
                                  list.getMaxSelectionIndex() < (list.getModel().getSize() -1) );
        jointPanel.repaint();
    }


    private void editSelectedEntry() {
        Object selected = list.getSelectedValue();
        if (selected == null) {
            cardLayout.first(editPanel);
            
            callback.actionPerformed(new ActionEvent(this
                    ,ActionEvent.ACTION_PERFORMED
                    ,"select"
                    )
            );
            return;
        } else {
            cardLayout.last(editPanel);
            int index = getSelectedIndex();
            next.setEnabled((index + 1)<list.getModel().getSize());
            prev.setEnabled(index>0);
            Color color = AWTColorUtil.getAppointmentColor(0);
            if ( isColoredBackgroundEnabled() ) {
                color = AWTColorUtil.getAppointmentColor(index);
            }
            identifierPanel.setBackground(color);
            identifier.setText(String.valueOf(index + 1));
            
            callback.actionPerformed(new ActionEvent(this
                    ,ActionEvent.ACTION_PERFORMED
                    ,"select"
                    )
            );

            callback.actionPerformed(new ActionEvent(this
                                                     ,ActionEvent.ACTION_PERFORMED
                                                     ,"edit"
                                                     )
            );
        }
    }

    class Listener extends MouseAdapter implements ListSelectionListener,ActionListener,ChangeListener {
        public void actionPerformed(ActionEvent evt) {
            if (evt.getSource() == next) {
                select(Math.min(list.getModel().getSize()-1, getSelectedIndex() + 1));
            } else if (evt.getSource() == prev) {
                select(Math.max(0, getSelectedIndex()-1));
            }
            if (evt.getSource() == removeButton) {
                callback.actionPerformed(new ActionEvent(RaplaListEdit.this
                                                         ,ActionEvent.ACTION_PERFORMED
                                                         ,"remove"
                                                         )
                                       );
            } else if (evt.getSource() == createNewButton) {
                callback.actionPerformed(new ActionEvent(RaplaListEdit.this
                                                         ,ActionEvent.ACTION_PERFORMED
                                                         ,"new"
                                                         )
                                         );
            } else if (evt.getSource() == moveUpButton) {
                callback.actionPerformed(new ActionEvent(RaplaListEdit.this
                                                         ,ActionEvent.ACTION_PERFORMED
                                                         ,"moveUp"
                                                         )
                                         );
            } else if (evt.getSource() == moveDownButton) {
                callback.actionPerformed(new ActionEvent(RaplaListEdit.this
                                                         ,ActionEvent.ACTION_PERFORMED
                                                         ,"moveDown"
                                                         )
                                         );
            }
        }

        public void valueChanged(ListSelectionEvent evt) {
            //if (evt.getValueIsAdjusting())
            //return;
            int[] index = list.getSelectedIndices();
            if ( index == oldIndex)
            {
            	return;
            }
            if ( index == null || oldIndex == null || !Arrays.equals( index, oldIndex))
            {
                oldIndex = index;
                editSelectedEntry();
                modelUpdate();
            }
        }

        public void stateChanged(ChangeEvent evt) {
            if (evt.getSource() == scrollPane.getViewport()) {
                jointPanel.repaint();
            }
        }
    }
    
    @SuppressWarnings("unchecked")
	public Collection<T> getSelectedValues()
    {
    	return (Collection<T>) Arrays.asList(list.getSelectedValues());
    }

	@SuppressWarnings("unchecked")
	public T getSelectedValue() {
		return (T) list.getSelectedValue();
	}
}



