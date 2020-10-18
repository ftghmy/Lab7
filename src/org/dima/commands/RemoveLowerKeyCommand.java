package org.dima.commands;

import org.dima.movies.User;

/**
 * Класс для сериализации в поток команды RemoveLowerKey
 */
public class RemoveLowerKeyCommand extends MovieCommand {
    private final Long key;

    /**
     * Конструктор - создание нового объекта с определенными значениями
     * @param key ключ для сравнения
     * @param user пользователь
     */


    public RemoveLowerKeyCommand(User user, Long key) {
        super(user);
        this.key = key;
    }

    /**
     * Функция получения значения поля key
     * @return возвращает значение поля key
     */
    public Long getKey() {
        return key;
    }
}
