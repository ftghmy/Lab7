package org.dima.commands;

import org.dima.movies.Movie;
import org.dima.movies.User;

/**
 * Класс для сериализации в поток команды update
 */
public class UpdateCommand extends MovieCommand {
    private final Movie movie;
    private final Long id;

    /**
     * Конструктор - создание нового объекта с определенными значениями
     * @param id номер фильма в коллекции
     * @param movie объект фильма
     * @param user пользователь
     */
    public UpdateCommand(User user, Long id, Movie movie) {
        super(user);
        this.movie = movie;
        this.id = id;
    }

    /**
     * Функция получения значения поля id
     * @return возвращает значение поля id
     */
    public Long getId() {
        return id;
    }

    /**
     * Функция получения значения поля movie
     * @return возвращает значение поля movie
     */
    public Movie getMovie() {
        return movie;
    }

}
