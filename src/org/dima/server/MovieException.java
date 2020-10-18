package org.dima.server;

/**
 * исключение выбрасываемое при обработке коллекции
 */
public class MovieException extends Exception {
    public MovieException(String msg) {
        super(msg);
    }

    /**
     * конструктор исключения
     * @param ex текст исключения
     */
    public MovieException(Exception ex) {
        super(ex);
    }
}
