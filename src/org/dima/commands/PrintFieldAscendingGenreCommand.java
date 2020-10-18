package org.dima.commands;

import org.dima.movies.MovieGenre;
import org.dima.movies.User;

/**
 * Класс для сериализации в поток команды PrintFieldAscendingGenre
 */
public class PrintFieldAscendingGenreCommand extends MovieCommand {
    private final MovieGenre genre;

    /**
     * Конструктор - создание нового объекта с определенными значениями
     * @param genre жанр для сравнения
     * @param user пользователь
     */
    public PrintFieldAscendingGenreCommand(User user, MovieGenre genre) {
        super(user);
        this.genre = genre;
    }

    /**
     * Функция получения значения поля genre
     * @return возвращает значение поля genre
     */
    public MovieGenre getGenre() {
        return genre;
    }
}
