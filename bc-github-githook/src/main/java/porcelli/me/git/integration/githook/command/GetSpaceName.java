package porcelli.me.git.integration.githook.command;

import java.nio.file.Path;

public class GetSpaceName implements Command {

    public String execute(final Path currentPath) {
        return currentPath.getName(currentPath.getNameCount() - 2).toString();
    }
}
