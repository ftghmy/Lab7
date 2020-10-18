package org.dima.client;

import org.dima.commands.*;
import org.dima.movies.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Scanner;

/**
 * Класс клиентского приложения
 */
public class DbClient implements AutoCloseable {

    private final SocketChannel channel;

    /**
     * Конструктор открывающий TCP соединение
     * @param host адрес хоста
     * @param port порт
     * @throws IOException
     */
    public DbClient(String host, int port) throws IOException {
        InetSocketAddress addr = new InetSocketAddress(host, port);
        channel = SocketChannel.open(addr);
        System.out.println(channel.socket().getLocalPort());
    }

    /**
     * Закрытие TCP соединения
     * @throws IOException
     */
    public void close() throws IOException {
        channel.close();
    }

    /**
     * Метод обработки команд
     * @param command команда
     * @return  результат исполнения
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public CommandResult exec(MovieCommand command) throws IOException, ClassNotFoundException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(command);
            channel.socket().getOutputStream().write(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(channel.socket().getInputStream());
            Object result = ois.readObject();
            return (CommandResult) result;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * метод запуска соединения
     * @param args первый имя хоста второй порт
     */
    public static void main(String[] args)  {

        if (args.length < 2) {
            System.err.println("Usage: Lab6Client host port");
            System.exit(-1);
        }

        String host = args[0];
        int port = 0;
        try {
            port = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.err.println("Usage: Lab6Client host port");
            System.err.println("Port must be integer");
            System.exit(-1);
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("Connecting to " + args[0] + ":" + args[1]);
        boolean need_exit = false;
        while (!need_exit) {
            try (DbClient client = new DbClient(host, port)) {
                System.out.println("Connected to " + host + ":" + port);
                testCommands(client);
                org.dima.client.QueryProcessor processor = new org.dima.client.QueryProcessor(client);
                while (!need_exit) {
                    System.out.print("movie>> ");
                    need_exit = processor.process(scanner);
                }
            } catch (IOException e) {
                System.err.println("Ошибка подключения к серверу " + host + ":" + port + " => " + e.getMessage());
                String answer = "";
                while (true) {
                    System.err.print("Повторить подключение [Y/n]: ");
                    answer = scanner.nextLine();
                    if (answer.trim().equals("y") || answer.trim().equals("Y")) {
                        break;
                    }
                    if (answer.trim().equals("N") || answer.trim().equals("n")) {
                        need_exit = true;
                        break;
                    }
                }
            }
        }
    }


    public static Movie createMovie() {
        Movie test = new Movie();
        test.setId((long) (2 + 1));
        test.setName("000");
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
        return test;
    }



    private static final long DELAY = 0;
    private static void testCommands(DbClient client) {
        System.out.println("Run tests");
        try {
            User user = new User("dima", "dima");
            System.out.println("Test at line " +  Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + client.exec(new TestCommand(user, "8723648  r2786r8273 r27r6 82", Color.YELLOW)).getType());
            Thread.sleep(DELAY);
            System.out.println("Test at line " +  Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + client.exec(new ShowCommand(user)).getType());
            Thread.sleep(DELAY);

            Movie movie = createMovie();
            System.out.println("Test at line " +  Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + client.exec(new InsertCommand(user, movie)).getType());
            Thread.sleep(DELAY);
            System.out.println("Test at line " +  Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + client.exec(new UpdateCommand(user, movie.getId(), movie)).getType());
            Thread.sleep(DELAY);

            System.out.println("Test at line " +  Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + client.exec(new RemoveLowerKeyCommand(user, 0L)).getType());
            Thread.sleep(DELAY);
            System.out.println("Test at line " +  Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + client.exec(new FilterContainsNameCommand(user, movie.getName().substring(0, 1))).getType());
            Thread.sleep(DELAY);
            System.out.println("Test at line " +  Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + client.exec(new PrintFieldAscendingGenreCommand(user, MovieGenre.COMEDY)).getType());
            Thread.sleep(DELAY);
            System.out.println("Test at line " +  Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + client.exec(new RemoveLowerCommand(user, movie)).getType());
            Thread.sleep(DELAY);
            System.out.println("Test at line " +  Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + client.exec(new ReplaceIfGreaterCommand(user, 0L, movie)).getType());
            Thread.sleep(DELAY);
            System.out.println("Test at line " +  Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + client.exec(new MaxByNameCommand(user)).getType());
            Thread.sleep(DELAY);
            System.out.println("Test at line " +  Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + client.exec(new RemoveCommand(user, 0L)).getType());
            Thread.sleep(DELAY);
            System.out.println("Test at line " +  Thread.currentThread().getStackTrace()[1].getLineNumber() + ": " + client.exec(new ClearCommand(user)).getType());
            Thread.sleep(DELAY);
            user = null;
        } catch (IOException | InterruptedException | ClassNotFoundException ex) {
            System.out.println("Tests failed");
            ex.printStackTrace();
            System.exit(-1);
        }
        System.out.println("Tests complete.");
    }
}
