package org.dima.commands;

import org.dima.movies.Movie;
import org.dima.movies.User;

/**
 * Класс для сериализации в поток команды replaceifgreater
 */
public class ReplaceIfGreaterCommand extends MovieCommand {
    private final Long key;
    private final Movie movie;

    /**
     * Конструктор - создание нового объекта с определенными значениями
     * @param key ключ для сравнения
     * @param movie объект фильма
     * @param user пользователь
     */
    public ReplaceIfGreaterCommand(User user, Long key, Movie movie) {
        super(user);
        this.key = key;
        this.movie = movie;
    }

    /**
     * Функция получения значения поля key
     * @return возвращает значение поля key
     */
    public Long getKey() {
        return key;
    }

    /**
     * Функция получения значения поля movie
     * @return возвращает значение поля movie
     */
    public Movie getMovie() {
        return movie;
    }
}
