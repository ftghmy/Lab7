package org.dima.commands;

import org.dima.movies.User;

import java.io.Serializable;

/**
 * Базовый класс для всех команд
 */
public abstract class MovieCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    private User user;

    private MovieCommand() {
        user = null;
    }

    public MovieCommand(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
