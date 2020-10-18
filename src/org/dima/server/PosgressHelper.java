package org.dima.server;

import org.dima.client.DbClient;
import org.dima.movies.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * класс отвечающий за работу с базой данных
 */
public class PosgressHelper implements AutoCloseable {
    private final static Logger logger = LoggerFactory.getLogger(PosgressHelper.class);
    private final static String SELECT_ALL_MOVIES = "SELECT * FROM MOVIES";
    private final static String SELECT_ONE_MOVIES = "SELECT * FROM MOVIES WHERE ID=?";
    private final static String[] field = {
            "id", "name", "coordinate_x", "coordinate_y", "creationdate", "oscars", "genre", "rating",
            "director_name", "birthday", "passportid", "haircolor", "location_x", "location_y", "location_name", "user_id"};
    private final static String INSERT_MOVIE = "INSERT INTO MOVIES " +
            "(NAME, COORDINATE_X, COORDINATE_Y, OSCARS, GENRE, RATING, DIRECTOR_NAME, " +
            "BIRTHDAY, PASSPORTID, HAIRCOLOR, LOCATION_X, LOCATION_Y, LOCATION_NAME, USER_ID) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final static String UPDATE_MOVIE = "UPDATE MOVIES SET " +
            "(NAME, COORDINATE_X, COORDINATE_Y, OSCARS, GENRE, RATING, DIRECTOR_NAME, " +
            "BIRTHDAY, PASSPORTID, HAIRCOLOR, LOCATION_X, LOCATION_Y, LOCATION_NAME) " +
            "= (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) WHERE id = ? AND USER_ID = ?";

    private final static String REMOVE_MOVIE = "DELETE FROM MOVIES WHERE id = ? AND USER_ID = ?";

    private final static String HOST = "localhost"; //"pg";

    private final String url;
    private final String user;
    private final String pass;

    private static PosgressHelper posgressHelper = null;
    private Connection connection = null;

    /**
     * конструктор помошника
     * @param url  определитель местонахождения ресурса
     * @param user пользователь
     * @param pass пороль пользователя
     * @throws SQLException
     */
    private PosgressHelper(String url, String user, String pass) throws SQLException {
        this.url = url;
        this.user = user;
        this.pass = pass;
        connection = getConnection();
    }

    /**
     * соединение с базой данных
     * @return соединение
     * @throws SQLException
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * шифровальщик пароля
     * @param password пороль пользователя
     * @return зашифрованный пароль
     * @throws NoSuchAlgorithmException
     */
    private static String encriptPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(password.getBytes());
        byte[] digest = md.digest();
        return new String(digest);
    }

    /**
     * проверка пользователя
     * @param username имя пользователя
     * @param password пароль пользователя
     * @return индентификатор пользователя
     * @throws MovieException
     */
    public Integer check(String username, String password) throws MovieException {
        try (Connection connection = getConnection();
             PreparedStatement pst = connection.prepareStatement("SELECT ID FROM USERS WHERE USERNAME = ? AND PASSWORD = ?")) {
            pst.setString(1, username);
            pst.setString(2, encriptPassword(password));
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt("ID");
            }

            throw new MovieException("Incorrect username or password");
        } catch (SQLException | NoSuchAlgorithmException e) {
            logger.error("Main  error", e);
            throw new MovieException("Login failed");
        }
    }

    /**
     * регистрация пользователя
     * @param username имя пользователя
     * @param password пароль пользователя
     * @return индентификатор пользователя
     * @throws MovieException
     */
    public Integer register(String username, String password) throws MovieException {
        if(username == null || password == null || username.trim().length() == 0 || password.trim().length() == 0) {
            throw new MovieException("Username and password can not be empty");
        }
        try (Connection connection = getConnection();
             PreparedStatement pst = connection.prepareStatement("INSERT INTO users (username, password) values (?, ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, username);
            pst.setString(2, encriptPassword(password));

            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt("ID");
            }
        } catch (Exception e) {
            logger.error("Register  error", e);
            throw new MovieException(e);
        }

        return null;
    }

    /**
     * создание помошника
     * @param url  определитель местонахождения ресурса
     * @param user пользователь пароль
     * @param pass пароль пользователя
     * @return объект помошника
     * @throws Exception
     */
    public static PosgressHelper getInstance(String url, String user, String pass) throws Exception {
        if (posgressHelper == null) {
            posgressHelper = new PosgressHelper(url, user, pass);
        }
        return posgressHelper;
    }

    /**
     * создание movie из полученных из базы данных данных
     * @param rs данные полученные из базы данных
     * @return movie
     * @throws SQLException
     */
    private Movie makeMovieFromResultSet(ResultSet rs) throws SQLException {
        Movie movie = new Movie();
        movie.setId(rs.getLong("ID"));
        movie.setName(rs.getString("NAME"));
        Coordinates coordinates = new Coordinates();
        coordinates.setX(rs.getDouble("COORDINATE_X"));
        coordinates.setY(rs.getInt("COORDINATE_Y"));
        movie.setCoordinates(coordinates);
        movie.setCreationDate(rs.getTimestamp("CREATIONDATE").toLocalDateTime().toLocalDate());
        movie.setOscarsCount(rs.getLong("OSCARS"));
        movie.setGenre(MovieGenre.valueOf(rs.getString("GENRE")));
        movie.setMpaaRating(MpaaRating.valueOf(rs.getString("RATING")));
        Person director = new Person();
        director.setName(rs.getString("DIRECTOR_NAME"));
        director.setBirthday(ZonedDateTime.parse(rs.getString("BIRTHDAY")));
        director.setPassportID(rs.getString("PASSPORTID"));
        director.setHairColor(Color.valueOf(rs.getString("HAIRCOLOR")));
        Location location = new Location();
        location.setX(rs.getInt("LOCATION_X"));
        location.setY(rs.getLong("LOCATION_Y"));
        location.setName(rs.getString("LOCATION_NAME"));
        director.setLocation(location);
        movie.setDirector(director);
        return movie;
    }

    /**
     * создание записи в базе данных из movie
     * @param movie movie
     * @param pst dshf;tybt
     * @throws SQLException
     */
    private void fillStatementFromMovie(Movie movie, PreparedStatement pst) throws SQLException {
        pst.setString(1, movie.getName());
        pst.setDouble(2, movie.getCoordinates().getX());
        pst.setInt(3, movie.getCoordinates().getY());
        pst.setLong(4, movie.getOscarsCount());
        pst.setString(5, movie.getGenre().toString());
        pst.setString(6, movie.getMpaaRating().toString());
        pst.setString(7, movie.getDirector().getName());
        pst.setString(8, movie.getDirector().getBirthday().toString());
        pst.setString(9, movie.getDirector().getPassportID());
        pst.setString(10, movie.getDirector().getHairColor().toString());
        pst.setDouble(11, movie.getDirector().getLocation().getX());
        pst.setDouble(12, movie.getDirector().getLocation().getY());
        pst.setString(13, movie.getDirector().getLocation().getName());
    }

    /**
     * взятие одного movie
     * @param id индетификатор movie
     * @return результат взятия
     */
    public Movie selectOneMOvie(Long id) {
        try (PreparedStatement pst = connection.prepareStatement(SELECT_ONE_MOVIES)) {
            pst.setLong(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return makeMovieFromResultSet(rs);
            }

        } catch (SQLException e) {
            logger.error("Main  error", e);
        }
        return null;
    }

    /**
     * взятие коллекции
     * @return коллекция
     */
    public List<Movie> selectMOvies() {
        List<Movie> movies = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL_MOVIES)) {

            while (rs.next()) {
                movies.add(makeMovieFromResultSet(rs));
            }

        } catch (SQLException e) {
            logger.error("Main  error", e);
        }
        return movies;
    }

    /**
     * вставка movie
     * @param movie movie
     * @param user_id индетификатор пользователя
     * @return индетификатор
     */
    public Movie insert(Movie movie, Integer user_id) {
        try (PreparedStatement pst = connection.prepareStatement(INSERT_MOVIE, PreparedStatement.RETURN_GENERATED_KEYS)) {
            fillStatementFromMovie(movie, pst);
            pst.setInt(14, user_id);

            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                return selectOneMOvie(rs.getLong("ID"));
            }
        } catch (SQLException e) {
            logger.error("Insert  error", e);
        }
        return null;
    }

    /**
     * обновление movie
     * @param id индетификатор movie
     * @param movie movie
     * @param user_id индетификатор пользователя
     * @return результат обновления
     */
    public boolean update(Long id, Movie movie, Integer user_id) {
        try (PreparedStatement pst = connection.prepareStatement(UPDATE_MOVIE)) {
            fillStatementFromMovie(movie, pst);

            pst.setLong(14, id);
            pst.setInt(15, user_id);

            return pst.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.error("Update  error", e);
        }
        return false;
    }

    /**
     * удаление
     * @param id индетификатор movie
     * @param user_id индетификатор пользователя
     * @return результат удаления
     */
    public boolean remove(Long id, Integer user_id) {
        try (PreparedStatement pst = connection.prepareStatement(REMOVE_MOVIE)) {
            pst.setLong(1, id);
            pst.setInt(2, user_id);

            return pst.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.error("Update  error", e);
        }
        return false;
    }

    /**
     * удаление помощника
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
        posgressHelper = null;
    }

}
