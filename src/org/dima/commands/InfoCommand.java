package org.dima.commands;

import org.dima.movies.User;

/**
 * Класс для сериализации в поток команды Info
 */
public class InfoCommand extends MovieCommand {
    /**
     * Конструктор - создание нового объекта с определенными значениями
     * @param user пользователь
     */
    public InfoCommand(User user) {
        super(user);
    }
}
