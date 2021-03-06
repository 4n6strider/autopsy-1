/*
 * Autopsy Forensic Browser
 *
 * Copyright 2018-2018 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.keywordsearch;

import com.google.common.io.CharSource;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.datamodel.Content;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.TskCoreException;

/**
 * Dedicated SqliteTextExtractor to solve the problems associated with Tika's
 * Sqlite parser.
 *
 * Tika problems: 1) Tika fails to open virtual tables 2) Tika fails to open
 * tables with spaces in table name 3) Tika fails to include the table names in
 * output (except for the first table it parses)
 */
class SqliteTextExtractor extends ContentTextExtractor {

    private static final String SQLITE_MIMETYPE = "application/x-sqlite3";
    private static final Logger logger = Logger.getLogger(SqliteTextExtractor.class.getName());
    private static final CharSequence EMPTY_CHARACTER_SEQUENCE = "";

    @Override
    boolean isContentTypeSpecific() {
        return true;
    }

    @Override
    public boolean isDisabled() {
        return false;
    }

    @Override
    public void logWarning(String msg, Exception exception) {
        logger.log(Level.WARNING, msg, exception); //NON-NLS
    }

    /**
     * Supports only the sqlite mimetypes
     *
     * @param file           Content file
     * @param detectedFormat Mimetype of content file
     *
     * @return true if x-sqlite3
     */
    @Override
    boolean isSupported(Content file, String detectedFormat) {
        return SQLITE_MIMETYPE.equals(detectedFormat);
    }

    /**
     * Returns an input stream that will read from a sqlite database.
     *
     * @param source Content file
     *
     * @return An InputStream that reads from a Sqlite database.
     *
     * @throws
     * org.sleuthkit.autopsy.keywordsearch.TextExtractor.TextExtractorException
     */
    @Override
    public Reader getReader(Content source) throws TextExtractorException {
        try {
            //Firewall for any content that is not an AbstractFile
            if (!AbstractFile.class.isInstance(source)) {
                return CharSource.wrap(EMPTY_CHARACTER_SEQUENCE).openStream();
            }
            return new SQLiteTableReader((AbstractFile) source);
        } catch (NoCurrentCaseException | IOException | TskCoreException
                | ClassNotFoundException | SQLException ex) {
            throw new TextExtractorException(
                    String.format("Encountered an issue while trying to initialize " //NON-NLS
                            + "a sqlite table steamer for abstract file with id: [%s], name: " //NON-NLS
                            + "[%s].", source.getId(), source.getName()), ex); //NON-NLS
        }
    }

    /**
     * Lazily loads tables from the database during reading to conserve memory.
     */
    private class SQLiteTableReader extends Reader {

        private final Iterator<String> tableIterator;
        private final Connection connection;
        private Reader currentTableReader;
        private final AbstractFile source;

        /**
         * Creates a reader that streams each table into memory and wraps a
         * reader around it. Designed to save memory for large databases.
         *
         * @param file Sqlite database file
         *
         * @throws NoCurrentCaseException Current case has closed
         * @throws IOException            Exception copying abstract file over
         *                                to local temp directory
         * @throws TskCoreException       Exception using file manager to find
         *                                meta files
         * @throws ClassNotFoundException Could not find sqlite JDBC class
         * @throws SQLException           Could not establish jdbc connection
         */
        public SQLiteTableReader(AbstractFile file) throws NoCurrentCaseException,
                IOException, TskCoreException, ClassNotFoundException, SQLException {
            source = file;

            String localDiskPath = SqliteUtil.writeAbstractFileToLocalDisk(file);
            SqliteUtil.findAndCopySQLiteMetaFile(file);
            Class.forName("org.sqlite.JDBC"); //NON-NLS  
            connection = DriverManager.getConnection("jdbc:sqlite:" + localDiskPath); //NON-NLS
            tableIterator = getTables().iterator();
        }

        /**
         * Gets the table names from the SQLite database file.
         *
         * @return Collection of table names from the database schema
         */
        private Collection<String> getTables() throws SQLException {
            Collection<String> tableNames = new LinkedList<>();
            try (Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery(
                            "SELECT name FROM sqlite_master "
                            + " WHERE type= 'table' ")) {
                while (resultSet.next()) {
                    tableNames.add(resultSet.getString("name")); //NON-NLS
                }
            }
            return tableNames;
        }

        /**
         * Reads from a database table and loads the contents into a table
         * builder so that its properly formatted during indexing.
         *
         * @param tableName Database table to be read
         */
        private String getTableAsString(String tableName) {
            TableBuilder table = new TableBuilder();
            table.addTableName(tableName);
            String quotedTableName = "\"" + tableName + "\"";

            try (Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery(
                            "SELECT * FROM " + quotedTableName)) { //NON-NLS
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = resultSet.getMetaData().getColumnCount();
                Collection<String> row = new LinkedList<>();

                //Add column names once from metadata
                for (int i = 1; i <= columnCount; i++) {
                    row.add(metaData.getColumnName(i));
                }

                table.addHeader(row);
                while (resultSet.next()) {
                    row = new LinkedList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        Object result = resultSet.getObject(i);
                        String type = metaData.getColumnTypeName(i);
                        if (isValuableResult(result, type)) {
                            row.add(resultSet.getObject(i).toString());
                        }
                    }
                    table.addRow(row);
                }
                table.addCell("\n");
            } catch (SQLException ex) {
                logger.log(Level.WARNING, String.format(
                        "Error attempting to read file table: [%s]" //NON-NLS
                        + " for file: [%s] (id=%d).", tableName, //NON-NLS
                        source.getName(), source.getId()), ex);
            }

            return table.toString();
        }

        /**
         * Determines if the result from the result set is worth adding to the
         * row. Ignores nulls and blobs for the time being.
         *
         * @param result Object result retrieved from resultSet
         * @param type   Type of objet retrieved from resultSet
         *
         * @return boolean where true means valuable, false implies it can be
         *         skipped.
         */
        private boolean isValuableResult(Object result, String type) {
            //Ignore nulls and blobs
            return result != null && type.compareToIgnoreCase("blob") != 0;
        }

        /**
         * Loads a database file into the character buffer. The underlying
         * implementation here only loads one table at a time to conserve
         * memory.
         *
         * @param cbuf Buffer to copy database content characters into
         * @param off  offset to begin loading in buffer
         * @param len  length of the buffer
         *
         * @return The number of characters read from the reader
         *
         * @throws IOException If there is an error with the CharSource wrapping
         */
        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            if (currentTableReader == null) {
                String tableResults = getNextTable();
                if (tableResults == null) {
                    return -1;
                }
                currentTableReader = CharSource.wrap(tableResults).openStream();
            }

            int charactersRead = currentTableReader.read(cbuf, off, len);
            while (charactersRead == -1) {
                String tableResults = getNextTable();
                if (tableResults == null) {
                    return -1;
                }
                currentTableReader = CharSource.wrap(tableResults).openStream();
                charactersRead = currentTableReader.read(cbuf, off, len);
            }

            return charactersRead;
        }

        /**
         * Grab the next table name from the collection of all table names, once
         * we no longer have a table to process, return null which will be
         * understood to mean the end of parsing.
         *
         * @return Current table contents or null meaning there are not more
         *         tables to process
         */
        private String getNextTable() {
            if (tableIterator.hasNext()) {
                return getTableAsString(tableIterator.next());
            } else {
                return null;
            }
        }

        /**
         * Close the underlying connection to the database.
         *
         * @throws IOException Not applicable, we can just catch the
         *                     SQLException
         */
        @Override
        public void close() throws IOException {
            try {
                connection.close();
            } catch (SQLException ex) {
                //Non-essential exception, user has no need for the connection 
                //object at this stage so closing details are not important
                logger.log(Level.WARNING, "Could not close JDBC connection", ex);
            }
        }

    }

    /**
     * Formats input so that it reads as a table in the console or in a text
     * viewer
     */
    private class TableBuilder {

        private final Integer DEFAULT_CAPACITY = 32000;
        private final StringBuilder table = new StringBuilder(DEFAULT_CAPACITY);

        private static final String TAB = "\t";
        private static final String NEW_LINE = "\n";
        private static final String SPACE = " ";

        /**
         * Add the section to the top left corner of the table. This is where
         * the name of the table should go
         *
         * @param tableName Table name
         */
        public void addTableName(String tableName) {
            table.append(tableName)
                 .append(NEW_LINE)
                 .append(NEW_LINE);
        }

        /**
         * Adds a formatted header row to the underlying StringBuilder
         *
         * @param vals
         */
        public void addHeader(Collection<String> vals) {
            addRow(vals);
        }

        /**
         * Adds a formatted row to the underlying StringBuilder
         *
         * @param vals
         */
        public void addRow(Collection<String> vals) {
            table.append(TAB);
            vals.forEach((val) -> {
                table.append(val);
                table.append(SPACE);
            });
            table.append(NEW_LINE);
        }

        public void addCell(String cell) {
            table.append(cell);
        }

        /**
         * Returns a string version of the table, with all of the escape
         * sequences necessary to print nicely in the console output.
         *
         * @return Formated table contents
         */
        @Override
        public String toString() {
            return table.toString();
        }
    }
}
