package org.dima.server;

import org.dima.movies.Movie;
import org.dima.movies.MovieGenre;
import org.dima.movies.MoviesDbInformation;
import org.dima.movies.XmlParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Этот класс реализует взаимодействие с коллекцией
 */
public class MoviesDB {
    final static Logger logger = LoggerFactory.getLogger(DbWorker.class);
    final public static Locale defaultLocale = new Locale("ru");
    private final ReentrantLock locker;

    private MoviesDbInformation information;
    private PosgressHelper posgressHelper;

    public void lock(){
        locker.lock();
    }

    public void unlock(){
        locker.unlock();
    }

    private final static String DBNAME = "studs";
    private final static String USERNAME = "s285690";
    private final static String PASSWORD = "oxo736";

    public MoviesDB(String host) throws Exception {
        String url = "jdbc:postgresql://" + host + ":5432/" + DBNAME;
        posgressHelper = PosgressHelper.getInstance(url, USERNAME, PASSWORD);
        this.movies = new LinkedHashMap<Long, Movie>();
        this.source = null;
        locker = new ReentrantLock();
        posgressHelper.selectMOvies().forEach(movie -> {
            movies.put(movie.getId(), movie);
        });
    }

    public PosgressHelper getPosgressHelper() {
        return posgressHelper;
    }

    /**
     * Конструктор класса Базы Данных
     *
     * @param source Файл источника данных XML
     * @throws XmlParseException Неверный формат данных
     */
    public MoviesDB(Path source) throws XmlParseException {
        Locale.setDefault(defaultLocale);
        this.source = source;
        this.movies = new LinkedHashMap<Long, Movie>();
        parseXmlFile(source);
        locker = new ReentrantLock();
        //TODO replace read from xml file to pg db
        //runTests();
    }

    /**
     * Парсер Базы данных из XML
     *
     * @param source Файл источника данных XML
     * @throws XmlParseException Неверный формат данных
     */
    private void parseXmlFile(Path source) throws XmlParseException {
        try (Scanner scanner = new Scanner(source)) {
            scanner.findWithinHorizon("<MOVIES>", 0);
            scanner.useDelimiter("</MOVIES>");
            while (scanner.hasNext()) {
                if (scanner.findWithinHorizon("<MOVIE>", 0) == null) {
                    break;
                }
                scanner.useDelimiter("</MOVIE>");
                if (scanner.hasNext()) {
                    Movie movie = Movie.fromXml(scanner);
                    insert(movie, null);
                }
            }
        } catch (IllegalArgumentException | DateTimeParseException | IOException e) {
            throw new XmlParseException(e.getMessage());
        }
    }

    //TODO replece print to xml by pg db

    /**
     * Запись данных в XML
     *
     * @param source Файл получателя XML
     * @throws Exception ошибка записи в файл
     */
    public void printToXmlFile(Path source) throws Exception {
        try (PrintWriter writer = new PrintWriter(source.toFile())) {
            writer.println("<MOVIES>");
            for (Movie movie : movies.values()) {
                writer.println("\t<MOVIE>");
                movie.toXml(writer, "\t\t");
                writer.println("\t</MOVIE>");
            }
            writer.println("</MOVIES>");
        }
    }

    public void save() throws Exception {
        if (getSource() != null) {
            printToXmlFile(getSource());
        }
    }

    public void clear(Integer user_id) throws Exception {
        movies.keySet().stream().collect(Collectors.toList()).forEach(item -> remove(item, user_id));
    }

    /**
     * получить объект структуры данных связанных с фильмами
     *
     * @return список фильмов
     */
    public LinkedHashMap<Long, Movie> getMovies() {
        return movies;
    }

    /**
     * получить информацию о списке фильмов
     *
     * @return возвращает объект information
     */
    public MoviesDbInformation getInformation() {
        return new MoviesDbInformation(
                movies.getClass().toString(),
                LocalDateTime.now(),
                movies.size(),
                movies.size() > 0 ? Collections.max(movies.keySet()) : new Long(0)
        );
    }

    /**
     * получить файл источника данных
     *
     * @return возвращает соответствующий файл
     */
    public Path getSource() {
        return source;
    }

    /**
     * втавить фильм
     *
     * @param movie фильм
     * @param user_id индетификатор пользователя
     */
    public void insert(Movie movie, Integer user_id) throws IllegalArgumentException {
        movie.setId(getInformation().getMax_id() + 1);
        movie.setCreationDate(LocalDate.now());
        if (movie.validate()) {
            //TODO new_movie = insert(movie)
            Movie new_movie = posgressHelper.insert(movie, user_id);
            movies.put(new_movie.getId(), new_movie);
        } else {
            logger.debug("Movie is invalid");
            throw new IllegalArgumentException("Movie is invalid");
        }
    }

    /**
     * найти по индентификатору
     *
     * @param id индентификатор
     * @return фильм
     */
    public Movie findById(Long id) {
        return movies.get(id);
    }


    /**
     * обновить элемент коллекции по индентификатору
     *
     * @param id    индентификатор
     * @param movie новый элемент
     * @param user_id индетификатор пользователя
     * @return результат обновления
     */
    public boolean update(Long id, Movie movie, Integer user_id) throws IllegalArgumentException {
        if (movie != null && movie.validate()) {
            if (posgressHelper.update(id, movie, user_id)) {
                movies.put(id, movie);
                return true;
            }
            return false;
        } else {
            logger.debug("Movie is invalid");
            throw new IllegalArgumentException("Movie is invalid");
        }
    }

    /**
     * удалить элемент коллекции
     *
     * @param key значение ключа
     * @param user_id индетификатор пользователя
     * @return True-если заменил,False-если не заменил
     */
    public boolean remove(Long key, Integer user_id) {
        //TODO remove(id)
        if (key != null && posgressHelper.remove(key, user_id)) {
            movies.remove(key);
            return true;
        }
        return false;
    }

    /**
     * вывести любой объект из коллекции, значение поля name которого является максимальным
     *
     * @return индентификатор фильма
     */
    public Movie maxByName() {
        Optional<Movie> movie = movies.values().stream()
                .max(new Comparator<Movie>() {
                    @Override
                    public int compare(Movie o1, Movie o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
       return movie.orElse(null);
    }

    /**
     * удалить из коллекции все элементы, ключ которых меньше, чем заданный
     *
     * @param key заданный ключ
     * @param user_id индетификатор пользователя
     * @return колличество удаленных элементов
     */
    public int removeLowerKey(Long key, Integer user_id) {
        movies.keySet().stream()
                .filter(item -> item.compareTo(key) < 0)
                .collect(Collectors.toList())
                .forEach(item -> remove(item, user_id));
        return 1;
    }

    /**
     * вывести элементы, значение поля name которых содержит заданную подстроку
     *
     * @param key заданная подстрока
     * @return отфильтрованный список
     */
    public List<Movie> filterByName(String key) {
        return movies.values().stream()
                .filter(item -> item.getName().toUpperCase().contains(key.toUpperCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * вывести значения поля genre всех элементов в порядке возрастания
     *
     * @param genre жанр
     * @return отфильтрованный список
     */
    public List<Movie> filterByGenre(MovieGenre genre) {
        return movies.values().stream()
                .filter(item -> item.getGenre() == genre)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * удалить из коллекции все элементы, меньшие, чем заданный
     *
     * @param movie заданный элемент
     * @param user_id индетификатор пользователя
     * @return колличество удаленных
     */
    public int removeLower(Movie movie, Integer user_id) {
        movies.keySet().stream()
                .filter(item -> movies.get(item).compareTo(movie) < 0)
                .collect(Collectors.toList())
                .forEach(item -> remove(item, user_id));
        return 1;
    }

    /**
     * заменить значение по ключу, если новое значение больше старого
     *
     * @param key   заданный ключ
     * @param movie заданное значение
     * @param user_id индетификатор пользователя
     * @return True-если заменил,False-если не заменил
     */
    public boolean replaceIfGreater(Long key, Movie movie, Integer user_id) {
        if (key == null) {
            return false;
        }
        if (movies.get(key) != null &&  movies.get(key).compareTo(movie) < 0) {
            return update(key, movie, user_id);
        }
        return false;
    }

    private final Path source;
    private final LinkedHashMap<Long, Movie> movies;


   /* private Movie test;
    void runTests() {
        try {
            test = new Movie();
            test.setId(getInformation().max_id + 1);
            test.setName("AAA");
            test.setGenre(MovieGenre.COMEDY);
            test.setCreationDate(LocalDate.now());
            Person director = new Person();
            director.setBirthday(ZonedDateTime.now());
            director.setHairColor(Color.BROWN);
            director.setName("AAAA");
            director.setPassportID("237528735623856");
            Location location = new Location();
            location.setName("AAAAAA");
            location.setX(123);
            location.setY(456);
            director.setLocation(location);
            test.setDirector(director);
            test.setMpaaRating(MpaaRating.G);
            test.setOscarsCount(2);
            Coordinates coordinates = new Coordinates();
            coordinates.setX(123);
            coordinates.setY(567);
            test.setCoordinates(coordinates);

            PrintStream std =  System.out;
            System.setOut(new PrintStream("test.out"));

            testInsert();
            testFindById();
            testFindByName();
            testUpdate();
            testRemove();
            testInsert();
            testMaxByName();
            testRemoveLowerKey();
            testInsert();
            testFilterByName();
            testFilterByGenre();
            testRemoveLower();
            testInsert();
            testReplaceIfGreater();


            System.setOut(std);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    void testInsert() {
        System.err.print("Test insert ...");

        insert(new Movie());
        insert(test);

        System.err.println("OK");
    }

    void testFindById() {
        System.err.print("Test find by id ...");
        Movie movie = findById(getInformation().max_id);
        movie.getId();
        movie = findById(getInformation().max_id + 1);
        if(movie != null) {
            throw new NullPointerException();
        }
        System.err.println("OK");
    }

    void testFindByName() {
        System.err.print("Test find by name ...");
        Long id = findByName("AAA");
        id.toString();

        id = findByName("54274");
        if(id != null) {
            throw new NullPointerException();
        }
        System.err.println("OK");
    }

    void testUpdate() {
        System.err.print("Test update ...");

        update(new Long(213), new Movie());
        update(test.getId(), test);

        System.err.println("OK");
    }

    void testRemove() {
        System.err.print("Test remove ...");
        remove("54274");
        remove("AAA");
        System.err.println("OK");
    }

    void testMaxByName() {
        System.err.print("Test max by name ...");
        maxByName();
        System.err.println("OK");
    }

    void testRemoveLowerKey() {
        System.err.print("Test remove lower key ...");
        removeLowerKey("54274");
        removeLowerKey("AAA");
        System.err.println("OK");
    }

    void testFilterByName() {
        System.err.print("Test filter by name ...");
        findByName("54274");
        findByName("AAA");
        System.err.println("OK");
    }

    void testFilterByGenre() {
        System.err.print("Test filter by genre ...");
        filterByGenre(MovieGenre.COMEDY);
        System.err.println("OK");
    }

    void testRemoveLower() {
        System.err.print("Test remove lower ...");
        Movie movie = new Movie();
        movie.setId(getInformation().max_id + 1);
        movie.setName("BBB");
        removeLower(new Movie());
        removeLower(movie);
        System.err.println("OK");
    }

    void testReplaceIfGreater() {
        System.err.print("Test replace If Greater ...");

        replaceIfGreater("AA", new Movie());
        replaceIfGreater("AA", test);
        replaceIfGreater(null, test);

        System.err.println("OK");
    }

*/


}
