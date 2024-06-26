package database.api;

import database.api.ddl.commands.AlterCommand;
import database.api.ddl.commands.CreateCommand;
import database.api.ddl.commands.DropCommand;
import database.api.dml.commands.DeleteCommand;
import database.api.dml.commands.InsertCommand;
import database.api.dml.commands.SelectCommand;
import database.api.dml.commands.UpdateCommand;
import database.api.utils.commands.HelpCommand;
import database.api.tcl.commands.BeginCommand;
import database.api.tcl.commands.CommitCommand;
import database.api.tcl.commands.RollBackCommand;
import database.api.utils.OUTPUT_TYPE;
import database.api.utils.commands.OpenCommand;
import database.api.utils.commands.ShowCommand;
import database.system.core.structures.Response;
import database.system.core.structures.interfaces.Printable;
import lombok.Data;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The DatabaseAPI class provides an interface for interacting with the database.
 * It allows executing Data Definition Language (DDL) and Data Manipulation Language (DML) commands,
 * managing command history, undoing commands, and handling Transaction Control Language (TCL) operations.
 *
 * <p>This class offers a synchronized way to interact with the database through various command objects.
 * It maintains a command history to support undo operations and provides methods to start, commit,
 * and roll back transactions.</p>
 *
 * <p>Supported Commands:</p>
 * <ul>
 *     <li>DDL Commands: create, alter, drop</li>
 *     <li>DML Commands: select, insert, update, delete</li>
 *     <li>TCL Commands: begin, commit, rollback</li>
 * </ul>
 *
 * <p>Example Usage:</p>
 * <pre>{@code
 * DatabaseAPI dbApi = new DatabaseAPI();
 * dbApi.setActiveEditor(new DatabaseEditor());
 * dbApi.setCommandHistory(new CommandHistory());
 * dbApi.create("myTable", List.of("id INTEGER", "name STRING"));
 * dbApi.begin();
 * dbApi.insert("myTable", List.of("id", "name"), List.of(new Object[]{1, "John Doe"}));
 * dbApi.commit();
 * }</pre>
 */
@Data
public class DatabaseAPI implements Printable {
    private List<DatabaseEditor> editors;
    private DatabaseEditor activeEditor;
    private CommandHistory history;
    private Response lastSelectResponse;

    /**
     * Displays help information for a specific command if provided, otherwise for all commands.
     *
     * @param command an optional command name for which help is requested; if not provided, help for all commands is displayed
     * @throws IOException if an I/O error occurs during the execution
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.help(Optional.of("select"));
     * dbApi.help(Optional.empty());
     * }</pre>
     */

    final public void help(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String> command) throws IOException {
        executeCommand(new HelpCommand(this, activeEditor, command));
    }

    /**
     * Opens the specified database with the given name.
     *
     * @param databaseName The name of the database to open.
     * @param databasePath Optional path to the directory where the database is located.
     *                    If not provided, assumes the database is located in the default directory.
     * @throws IOException If there is an error while opening the database.
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.open("test_database", Optional.empty());;
     * }</pre>
     */
    final public void open(String databaseName, @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String> databasePath) throws IOException {
        executeCommand(new OpenCommand(this, activeEditor, databaseName, databasePath));
    }


    /**
     * Shows information about the database located at the specified path.
     *
     * @param databasePath Optional path to the directory where the database is located.
     *                    If not provided, assumes the database is located in the default directory.
     * @throws IOException If there is an error while showing information about the database.
     */
    final public void show(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String> databasePath) throws IOException {
        executeCommand(new ShowCommand(this, activeEditor, databasePath));
    }

    /**
     * Shows information about the tables in opened database.
     *
     * @throws IOException If there is an error while showing information about the database.
     */
    final public void show() throws IOException {
        executeCommand(new ShowCommand(this, activeEditor));
    }


    /**
     * Creates a new database by executing the CreateCommand.
     *
     * @param databaseName the name of the database to be created
     * @param databasePath an optional path where the database will be stored; if not provided, a default location will be used
     * @throws IOException if an I/O error occurs during the execution
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.create("myDatabase", Optional.of("/path/to/database"));
     * }</pre>
     */
    final synchronized public void create(String databaseName, @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String> databasePath) throws IOException {
        executeCommand(new CreateCommand(this, activeEditor, databaseName, databasePath));
    }

    /**
     * Executes a CREATE command to create a new table in the database.
     *
     * @param tableName the name of the table to create
     * @param columns   the list of column names for the new table
     * @throws IOException if an I/O error occurs during the execution
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.create("myTable", List.of("id INTEGER", "name STRING"));
     * }</pre>
     */
    final synchronized public void create(String tableName, List<String> columns) throws IOException {
        executeCommand(new CreateCommand(this, activeEditor, tableName, columns));
    }

    /**
     * Executes an ALTER command to modify an existing table in the database.
     *
     * @param tableName    the name of the table to modify
     * @param alterColumns an array of lists representing modifications to the table columns
     *                     <p>
     *                     <strong>Variations:</strong>
     *                     <ol>
     *                     <li>If one list is provided, it represents the new columns to be added to the table.</li>
     *                     <li>If two lists are provided:
     *                     <ul>
     *                     <li>The first list represents the modified columns.</li>
     *                     <li>The second list represents the new values for those columns.</li>
     *                     </ul>
     *                     </li>
     *                     <li>If three lists are provided:
     *                     <ul>
     *                     <li>The first list represents the modified columns.</li>
     *                     <li>The second list represents the new values for those columns.</li>
     *                     <li>The third list represents the columns to be deleted from the table.</li>
     *                     </ul>
     *                     </li>
     *                     </ol>
     * @throws IOException if an I/O error occurs during the execution
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.alter("myTable", List.of("newColumn INTEGER NOT NULL"));
     * dbApi.alter("myTable", null, List.of("column1 STRING UNIQUE NOT NULL", "column2 BOOLEAN DEFAULT true"));
     * dbApi.alter("myTable", null, null, List.of("column2"));
     * dbApi.alter("myTable", null, null, List.of("column2 default_column2_constraint"));
     * }</pre>
     */
    @SafeVarargs
    final synchronized public void alter(String tableName, List<String>... alterColumns) throws IOException {
        executeCommand(new AlterCommand(this, activeEditor, tableName, alterColumns));
    }

    /**
     * Executes an ALTER command to rename an existing table or database in the database.
     *
     * @param name    the current name of the table or database to be renamed
     * @param newName the new name for the table or database
     * @param isDatabase flag indicating if the rename is for a database
     * @throws IOException if an I/O error occurs during the execution
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.alter("oldTableName", "newTableName", false);
     * }</pre>
     */
    final synchronized public void alter(String name, String newName, boolean isDatabase) throws IOException {
        executeCommand(new AlterCommand(this, activeEditor, name, newName, isDatabase));
    }

    /**
     * Executes a DROP command to delete a table or database from the database.
     *
     * @param name         the name of the table or database to delete
     * @param isDatabase   the flag indicating if the drop is for a database
     * @throws IOException if an I/O error occurs during the execution
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.drop("myTable", false);
     * }</pre>
     */
    final synchronized public void drop(String name, boolean isDatabase) throws IOException {
        executeCommand(new DropCommand(this, activeEditor, name, isDatabase));
    }

    /**
     * Executes a DELETE command to remove records from a table in the database.
     *
     * @param tableName the name of the table from which records will be deleted
     * @param condition the condition to filter which records are deleted
     * @throws IOException if an I/O error occurs during the execution
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.delete("myTable", "id = 1");
     * }</pre>
     */
    final synchronized public void delete(String tableName, String condition) throws IOException {
        executeCommand(new DeleteCommand(this, activeEditor, tableName, condition));
    }

    /**
     * Executes an INSERT command to add records to a table in the database.
     *
     * @param tableName the name of the table to insert records into
     * @param columns   the list of column names for the records
     * @param values    the list of values to be inserted into the columns
     * @throws IOException if an I/O error occurs during the execution
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.insert("myTable", List.of("id", "name"), List.of(new Object[]{1, "John Doe"}));
     * }</pre>
     */
    final synchronized public void insert(String tableName, List<String> columns, List<Object[]> values) throws IOException {
        executeCommand(new InsertCommand(this, activeEditor, tableName, columns, values));
    }

    /**
     * Executes a SELECT command to retrieve all records from a table in the database.
     *
     * @param tableName the name of the table from which records will be retrieved
     * @throws IOException if an I/O error occurs during the execution
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.select("myTable");
     * }</pre>
     */
    final synchronized public void select(String tableName) throws IOException {
        executeCommand(new SelectCommand(this, activeEditor, tableName));
    }

    /**
     * Executes a SELECT command to retrieve specific columns from a table in the database.
     *
     * @param tableName the name of the table from which records will be retrieved
     * @param columns   the list of column names to select from the table
     * @throws IOException if an I/O error occurs during the execution
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.select("myTable", List.of("id", "name"));
     * }</pre>
     */
    final synchronized public void select(String tableName, List<String> columns) throws IOException {
        executeCommand(new SelectCommand(this, activeEditor, tableName, columns));
    }

    /**
     * Executes a SELECT command to retrieve specific columns from a table in the database based on a condition.
     *
     * @param tableName the name of the table from which records will be retrieved
     * @param columns   the list of column names to select from the table
     * @param condition the condition to filter which records are selected
     * @throws IOException if an I/O error occurs during the execution
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.select("myTable", List.of("id", "name"), "id = 1");
     * }</pre>
     */
    final synchronized public void select(String tableName, List<String> columns, String condition) throws IOException {
        executeCommand(new SelectCommand(this, activeEditor, tableName, columns, condition));
    }

    /**
     * Executes an UPDATE command to modify records in a table in the database.
     *
     * @param tableName the name of the table in which records will be updated
     * @param values    the list of new values to be set in the records
     * @param condition the condition to determine which records to update
     * @throws IOException if an I/O error occurs during the execution
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.update("table1", List.of("column1 = 20", "column2 = 'John'"), "id = 10");
     * }</pre>
     */
    final synchronized public void update(String tableName, List<String> values, String condition) throws IOException {
        executeCommand(new UpdateCommand(this, activeEditor, tableName, values, condition));
    }

    /**
     * Begins a new transaction by executing the BeginCommand.
     *
     * @throws IOException if an I/O error occurs during the execution
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.begin();
     * }</pre>
     */
    final synchronized public void begin() throws IOException {
        executeCommand(new BeginCommand(this, activeEditor));
    }

    /**
     * Commits the current transaction by executing the CommitCommand.
     *
     * @throws IOException if an I/O error occurs during the execution
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.commit();
     * }</pre>
     */
    final synchronized public void commit() throws IOException {
        executeCommand(new CommitCommand(this, activeEditor));
    }

    /**
     * Rolls back the current transaction by executing the RollBackCommand.
     *
     * @throws IOException if an I/O error occurs during the execution
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.rollback();
     * }</pre>
     */
    final synchronized public void rollback() throws IOException {
        executeCommand(new RollBackCommand(this, activeEditor));
    }

    /**
     * Executes the given command. If a transaction is active, the command is collected for later execution.
     * Otherwise, it is executed immediately and added to the command history if successful.
     *
     * @param command the command to execute
     * @throws IOException if an I/O error occurs during the execution
     */
    private void executeCommand(Command command) throws IOException {
        if (activeEditor.haveActiveTransactions() && !(command instanceof BeginCommand || command instanceof CommitCommand || command instanceof RollBackCommand)) {
            activeEditor.collectCommands(command);
        } else if (activeEditor.haveActiveTransactions() && (command instanceof CommitCommand || command instanceof RollBackCommand)) {
            if (command.execute()) {
                history.push(command);
            }
        } else {
            if (command.execute()) {
                history.push(command);
            }
        }
    }

    /**
     * Undoes the last executed command by popping it from the history and calling its undo method.
     *
     * @throws IOException            if an I/O error occurs during the execution
     * @throws ClassNotFoundException if the class of a serialized object could not be found while undoing
     *
     * <p>Example:</p>
     * <pre>{@code
     * dbApi.undo();
     * }</pre>
     */
    synchronized public void undo() throws IOException, ClassNotFoundException {
        Command command = history.pop();
        if (command != null) {
            command.undo();
        }
    }

    /**
     * Prints the last SELECT response using the specified output type and optional file path.
     *
     * @param outputType the output type (e.g., CONSOLE, FILE)
     * @param filePath   an optional file path where the output should be written if the output type is FILE
     */
    @Override
    public void print(OUTPUT_TYPE outputType, Optional<String> filePath) {
        if (Objects.nonNull(lastSelectResponse))
            getLastSelectResponse().print(outputType, filePath);
    }
}
