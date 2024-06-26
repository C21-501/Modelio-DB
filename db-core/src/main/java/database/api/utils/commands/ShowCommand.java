package database.api.utils.commands;

import database.api.Command;
import database.api.DatabaseAPI;
import database.api.DatabaseEditor;
import database.monitor.Config;
import database.system.core.exceptions.DatabaseIOException;

import java.util.Optional;

public class ShowCommand  extends Command {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<String> databasePath = Optional.empty();
    private final boolean isDatabase;

    /**
     * Constructs a new Command instance.
     *
     * @param databaseAPI    the database API instance to interact with the database
     * @param databaseEditor the database editor instance to execute the command
     */
    public ShowCommand(DatabaseAPI databaseAPI, DatabaseEditor databaseEditor) {
        super(databaseAPI, databaseEditor);
        this.isDatabase = false;
    }

    public ShowCommand(DatabaseAPI databaseAPI, DatabaseEditor databaseEditor, @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String> databasePath) {
        super(databaseAPI, databaseEditor);
        this.databasePath = databasePath;
        isDatabase = true;
    }

    @Override
    public boolean execute() throws DatabaseIOException {
        if (isDatabase)
            databasePath.ifPresentOrElse(
                    string -> databaseEditor.getUtilManager()
                            .showAvailableDatabases(
                                    string,
                                    Config.DEFAULT_OUTPUT_TYPE,
                                    Config.CURRENT_OUTPUT_PATH
                            ),
                    () -> databaseEditor.getUtilManager()
                            .showAvailableDatabases(
                                    Config.ROOT_DATABASE_PATH,
                                    Config.DEFAULT_OUTPUT_TYPE,
                                    Config.CURRENT_OUTPUT_PATH
                            )
            );
        else
            databaseEditor.getUtilManager()
                    .showAvailableTables(
                            Config.DEFAULT_OUTPUT_TYPE,
                            Config.CURRENT_OUTPUT_PATH
                    );
        return false;
    }
}
