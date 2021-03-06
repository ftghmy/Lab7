package org.dima.commands;

import org.dima.movies.Movie;
import org.dima.movies.User;

/**
 * Класс для сериализации в поток команды Insert
 */
public class InsertCommand extends MovieCommand {
    private final Movie movie;

    /**
     * Конструктор - создание нового объекта с определенными значениями
     * @param movie объект фильма
     * @param user пользователь
     */
    public InsertCommand(User user, Movie movie) {
        super(user);
        this.movie = movie;
    }

    /**
     * Функция получения значения поля movie
     * @return возвращает значение поля movie
     */
    public Movie getMovie() {
        return movie;
    }


}
