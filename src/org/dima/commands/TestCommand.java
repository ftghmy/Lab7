package org.dima.commands;

import org.dima.movies.Color;
import org.dima.movies.User;

public class TestCommand  extends MovieCommand {
    private final String test;
    private final Color color;

    public TestCommand(User user, String test, Color color) {
        super(user);
        this.test = test;
        this.color = color;
    }

    public String getTest() {
        return test;
    }

    public Color getColor() {
        return color;
    }

}
