package org.dima.commands;

import org.dima.movies.User;

/**
 * Класс для сериализации в поток команды show
 */
public class ShowCommand extends MovieCommand {
    /**
     * Конструктор - создание нового объекта с определенными значениями
     * @param user пользователь
     */
    public ShowCommand(User user) {
        super(user);
    }
}
