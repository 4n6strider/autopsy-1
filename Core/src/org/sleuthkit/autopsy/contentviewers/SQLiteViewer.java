/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sleuthkit.autopsy.contentviewers;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import javax.swing.JComboBox;
import javax.swing.SwingWorker;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.centralrepository.datamodel.EamOrganization;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.autopsy.datamodel.ContentUtils;
import org.sleuthkit.datamodel.AbstractFile;


public class SQLiteViewer extends javax.swing.JPanel implements FileTypeViewer {

    public static final String[] SUPPORTED_MIMETYPES = new String[]{"application/x-sqlite3"};
    private static final Logger LOGGER = Logger.getLogger(FileViewer.class.getName());
    private Connection connection = null;
     
    private String tmpDBPathName = null;
    private File tmpDBFile = null;
                
    // TBD: Change the value to be a Array of ColDefs
    Map<String, String> dbTablesMap = new TreeMap<>();
    
    /**
     * Creates new form SQLiteViewer
     */
    public SQLiteViewer() {
        initComponents();
        
        customizeComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jHdrPanel = new javax.swing.JPanel();
        tablesDropdownList = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        numEntriesField = new javax.swing.JTextField();
        jTableDataPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        tablesDropdownList.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        tablesDropdownList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tablesDropdownListActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(SQLiteViewer.class, "SQLiteViewer.jLabel1.text")); // NOI18N

        numEntriesField.setEditable(false);
        numEntriesField.setText(org.openide.util.NbBundle.getMessage(SQLiteViewer.class, "SQLiteViewer.numEntriesField.text")); // NOI18N
        numEntriesField.setBorder(null);
        numEntriesField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numEntriesFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jHdrPanelLayout = new javax.swing.GroupLayout(jHdrPanel);
        jHdrPanel.setLayout(jHdrPanelLayout);
        jHdrPanelLayout.setHorizontalGroup(
            jHdrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jHdrPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(tablesDropdownList, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(23, 23, 23)
                .addComponent(numEntriesField, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(130, Short.MAX_VALUE))
        );
        jHdrPanelLayout.setVerticalGroup(
            jHdrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jHdrPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jHdrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tablesDropdownList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(numEntriesField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout jTableDataPanelLayout = new javax.swing.GroupLayout(jTableDataPanel);
        jTableDataPanel.setLayout(jTableDataPanelLayout);
        jTableDataPanelLayout.setHorizontalGroup(
            jTableDataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jTableDataPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addGap(15, 15, 15))
        );
        jTableDataPanelLayout.setVerticalGroup(
            jTableDataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jTableDataPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jHdrPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jTableDataPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jHdrPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jTableDataPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void numEntriesFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_numEntriesFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_numEntriesFieldActionPerformed

    private void tablesDropdownListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tablesDropdownListActionPerformed
        JComboBox<String> cb = (JComboBox<String>) evt.getSource();
        String tableName = (String) cb.getSelectedItem();
        if (null == tableName) {
            return;
        }
        
        readTable(tableName);
    }//GEN-LAST:event_tablesDropdownListActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jHdrPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JPanel jTableDataPanel;
    private javax.swing.JTextField numEntriesField;
    private javax.swing.JComboBox<String> tablesDropdownList;
    // End of variables declaration//GEN-END:variables

    @Override
    public List<String> getSupportedMIMETypes() {
         return Arrays.asList(SUPPORTED_MIMETYPES);
    }

    @Override
    public void setFile(AbstractFile file) {
        processSQLiteFile( file);
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void resetComponent() {
        
        dbTablesMap.clear();
        
        tablesDropdownList.setEnabled(true);
        tablesDropdownList.removeAllItems();
        numEntriesField.setText("");
        
        // close DB connection to file
        if (null != connection) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Failed to close DB connection to file.", ex); //NON-NLS
            }
        }
        
        // delete last temp file
        if (null != tmpDBFile) {
            tmpDBFile.delete();
            tmpDBFile = null;
        }
    }
    
    private void customizeComponents() {
        
        // add a actionListener to jTablesComboBox
    }
    
    /**
     * Process the given SQLite DB file
     *
     * @param sqliteFile - 
     *
     * @return none
     */
    private void processSQLiteFile(AbstractFile sqliteFile) {

        tablesDropdownList.removeAllItems();

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                
                try {
                    tmpDBPathName = Case.getCurrentCase().getTempDirectory() + File.separator + sqliteFile.getName() + "-" + sqliteFile.getId();
                    tmpDBFile = new File(tmpDBPathName);
                
                    // Copy the file to temp folder
                    ContentUtils.writeToFile(sqliteFile, tmpDBFile);
                    System.out.println("RAMAN: SQLite file copied to: " + tmpDBPathName);
        
                    // Open copy using JDBC
                    Class.forName("org.sqlite.JDBC"); //NON-NLS //load JDBC driver 
                    connection = DriverManager.getConnection("jdbc:sqlite:" + tmpDBPathName); //NON-NLS

                    // Read all table names and schema
                    return getTables();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Failed to copy DB file.", ex); //NON-NLS
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Failed to Open DB.", ex); //NON-NLS
                } catch (ClassNotFoundException ex) {
                    LOGGER.log(Level.SEVERE, "Failed to initialize JDBC Sqlite.", ex); //NON-NLS
                }
                return false;
            }

            @Override
            protected void done() {
               super.done();
                try {
                    boolean status = get();
                    if ( (status == true) && (dbTablesMap.size() > 0) ) {
                        dbTablesMap.keySet().forEach((tableName) -> {
                            tablesDropdownList.addItem(tableName);
                        });
                    }
                    else {
                        // Populate error message
                         tablesDropdownList.addItem("No tables found");
                         tablesDropdownList.setEnabled(false);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    LOGGER.log(Level.SEVERE, "Unexpected exception while opening DB file", ex); //NON-NLS
                }
            }
        }.execute();

    }
    
    /**
     * Gets the table names and their schema from loaded SQLite db file
     *
     * @return true if success, false otherwise
     */
    private boolean getTables() {
        
        try {
            Statement statement = connection.createStatement();
            
            ResultSet resultSet = statement.executeQuery(
                        "SELECT name, sql FROM sqlite_master "
                                + " WHERE type= 'table' "
                                + " ORDER BY name;"); //NON-NLS

                while (resultSet.next()) {
                    String tableName = resultSet.getString("name"); //NON-NLS
                    String tableSQL = resultSet.getString("sql"); //NON-NLS
                   
                    dbTablesMap.put(tableName, tableSQL);
                    String query = "PRAGMA table_info(" + tableName + ")"; //NON-NLS
                    ResultSet rs2;
                    try {
                        Statement statement2 = connection.createStatement();
                        rs2 = statement2.executeQuery(query);
                        while (rs2.next()) {
                            
                            System.out.println("RAMAN: Col Name = " + rs2.getString("name") );
                            System.out.println("RAMAN: Col Type = " + rs2.getString("type") );
        
                            // RAMAN TBD: parse and save the table schema
                        }
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Error while trying to get columns from sqlite db." + connection, ex); //NON-NLS
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error getting table names from the DB", e); //NON-NLS
            }
        return true;
    }
    
    private void readTable(String tableName) {
        
        System.out.println("RAMAN: selected table = " + tableName);
         
        // TBD: need to handle cancelling if one is already in progress
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                
                try {
                    Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery(
                        "SELECT COUNT(*) as count FROM " + tableName ); //NON-NLS

                    System.out.println("Row count = " + resultSet.getInt("count") );
                    // Read all table names and schema
                    return resultSet.getInt("count");
                }catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Failed to Open DB.", ex); //NON-NLS
                }
                //NON-NLS
                return 0;
            }

            @Override
            protected void done() {
               super.done();
                try {
                    int numRows = get();
                    numEntriesField.setText( numRows + " entries");
                } catch (InterruptedException | ExecutionException ex) {
                    LOGGER.log(Level.SEVERE, "Unexpected exception while reading table.", ex); //NON-NLS
                }
            }
        }.execute();
        
    }
            
    enum SQLStorageClass {
        NULL,
        INTEGER, 
        REAL,
        TEXT,
        BLOB
    };
    
    private class SQLColDef {
        
        private final String colName;
        private final SQLStorageClass storageClass;
        
        SQLColDef(String colName, SQLStorageClass sc ) {
            this.colName = colName;
            this.storageClass = sc;
        }
        
    }
}
