package org.dima.commands;

import org.dima.movies.User;

/**
 * Класс для сериализации в поток команды Clear
 */
public class ClearCommand extends MovieCommand  {
    /**
     * Конструктор - создание нового объекта с определенными значениями
     * @param user пользователь
     */
    public ClearCommand(User user) {
        super(user);
    }
}
