package org.dima.commands;

/**
 * Класс для сериализации в поток команды Register
 */
public class RegisterCommand extends MovieCommand {
    private final String username;
    private final String password;

    /**
     * Конструктор - создание нового объекта с определенными значениями
     * @param username имя пользователя
     * @param password пароль пользователя
     */
    public RegisterCommand(String username, String password) {
        super(null);
        this.username = username;
        this.password = password;
    }

    /**
     * Функция получения значения поля username
     * @return возвращает значение поля username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Функция получения значения поля password
     * @return возвращает значение поля password
     */
    public String getPassword() {
        return password;
    }
}
