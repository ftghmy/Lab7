package org.dima.commands;

import org.dima.movies.User;

/**
 * Класс для сериализации в поток команды MaxByName
 */
public class MaxByNameCommand extends MovieCommand {
    /**
     *
     * @param user пользователь
     */
    public MaxByNameCommand(User user) {
        super(user);
    }
}
