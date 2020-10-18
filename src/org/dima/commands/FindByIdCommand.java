package org.dima.commands;

import org.dima.movies.User;

/**
 * Класс для сериализации в поток команды FindById
 */
public class FindByIdCommand extends MovieCommand {
    private final Long id;



    /**
     * Конструктор - создание нового объекта с определенными значениями
     * @param id индентификатор для сравнения
     * @param user пользователь
     */
    public FindByIdCommand(User user, Long id) {
        super(user);
        this.id = id;
    }

    /**
     * Функция получения значения поля id
     * @return возвращает значение поля id
     */
    public Long getId() {
        return id;
    }
}
