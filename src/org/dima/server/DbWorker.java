package org.dima.server;

import org.dima.commands.*;
import org.dima.movies.Movie;
import org.dima.tools.ObjectSizeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;

/**
 * Основной класс обработки команд
 */
public class DbWorker {
    public final static Logger logger = LoggerFactory.getLogger(DbWorker.class);

    SocketChannel socketChannel;

    private MoviesDB moviesDB;

    private ForkJoinPool forkJoinPool;
    /**
     * Конструктор обработчика команд
     *
     * @param channel серверный канал
     * @param db      база данных
     * @throws IOException
     */
    public DbWorker(SocketChannel channel, MoviesDB db) throws IOException {
        moviesDB = db;
        socketChannel = channel;
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        forkJoinPool = new ForkJoinPool();
    }

    /**
     * Метод обработки сообщений
     *
     * @return в случае успеха колличество принятых байт отрицательные числа в случае разрыва канала
     */
    public int read() {
        try {
            ReadThread reader = new ReadThread();
            reader.start();
            reader.join();
            return reader.numRead;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Метод вызова команды базы данных
     *
     * @param command команда
     * @return результат исполнения команды
     */
    private CommandResult call(MovieCommand command) {
        if (command instanceof RegisterCommand) {
            try {
                Integer id = moviesDB.getPosgressHelper().register(((RegisterCommand) command).getUsername(), ((RegisterCommand) command).getPassword());
                return new CommandResult();
            } catch (MovieException e) {
                return new CommandResult(CommandResult.Type.ERROR, e.getMessage());
            }
        }

        if (command.getUser() == null) {
            logger.error("User not logged in");
            return new CommandResult(CommandResult.Type.ERROR, "User not logged in");
        }

        try {
            Integer user_id = moviesDB.getPosgressHelper().check(command.getUser().getUsername(), command.getUser().getPassword());
            logger.info("Exec command " + command.getClass().getName() +
                    " from user " + command.getUser().getUsername() +
                    " with id " + user_id);
            if (command instanceof TestCommand) {
                return new CommandResult();
            } else if (command instanceof ShowCommand) {
                ArrayList<Movie> movies = new ArrayList<Movie>(moviesDB.getMovies().values());
                movies.sort(new ObjectSizeComparator());
                return new CommandResultWithObject(movies);
            } else if (command instanceof InfoCommand) {
                return new CommandResultWithObject(moviesDB.getInformation());
            } else if (command instanceof FindByIdCommand) {
                Movie movie = moviesDB.findById(((FindByIdCommand) command).getId());
                if (movie == null) {
                    return new CommandResultWithObject(CommandResult.Type.WARNING, "Not found");
                } else {
                    return new CommandResultWithObject(movie);
                }
            } else if (command instanceof ClearCommand) {
                try {
                    moviesDB.clear(user_id);
                    moviesDB.save();
                    return new CommandResult();
                } catch (Exception e) {
                    return new CommandResult(CommandResult.Type.ERROR, e.getMessage());
                }
            } else if (command instanceof InsertCommand) {
                try {
                    moviesDB.insert(((InsertCommand) command).getMovie(), user_id);
                    moviesDB.save();
                    return new CommandResult();
                } catch (Exception e) {
                    return new CommandResult(CommandResult.Type.ERROR, e.getMessage());
                }
            } else if (command instanceof UpdateCommand) {
                try {
                    moviesDB.update(((UpdateCommand) command).getId(), ((UpdateCommand) command).getMovie(), user_id);
                    moviesDB.save();
                    return new CommandResult();
                } catch (Exception e) {
                    return new CommandResult(CommandResult.Type.ERROR, e.getMessage());
                }
            } else if (command instanceof RemoveCommand) {
                try {
                    if (moviesDB.remove(((RemoveCommand) command).getKey(), user_id)) {
                        moviesDB.save();
                        return new CommandResult();
                    } else {
                        return new CommandResult(CommandResult.Type.WARNING, "There is no Record with key " + ((RemoveCommand) command).getKey());
                    }
                } catch (Exception e) {
                    return new CommandResult(CommandResult.Type.ERROR, e.getMessage());
                }
            } else if (command instanceof RemoveLowerKeyCommand) {
                try {
                    if (moviesDB.removeLowerKey(((RemoveLowerKeyCommand) command).getKey(), user_id) > 0) {
                        moviesDB.save();
                        return new CommandResult();
                    } else {
                        return new CommandResult(CommandResult.Type.WARNING, "There is no Records with key lower then " + ((RemoveLowerKeyCommand) command).getKey());
                    }
                } catch (Exception e) {
                    return new CommandResult(CommandResult.Type.ERROR, e.getMessage());
                }
            } else if (command instanceof FilterContainsNameCommand) {
                ArrayList<Movie> movies = new ArrayList<Movie>(moviesDB.filterByName(((FilterContainsNameCommand) command).getKey()));
                movies.sort(new ObjectSizeComparator());
                return new CommandResultWithObject(movies);
            } else if (command instanceof PrintFieldAscendingGenreCommand) {
                ArrayList<Movie> movies = new ArrayList<Movie>(moviesDB.filterByGenre(((PrintFieldAscendingGenreCommand) command).getGenre()));
                movies.sort(new ObjectSizeComparator());
                return new CommandResultWithObject(movies);
            } else if (command instanceof RemoveLowerCommand) {
                try {
                    if (moviesDB.removeLower(((RemoveLowerCommand) command).getMovie(), user_id) > 0) {
                        moviesDB.save();
                        return new CommandResult();
                    } else {
                        return new CommandResult(CommandResult.Type.WARNING, "There is no Records with key lower then " + ((RemoveLowerCommand) command).getMovie());
                    }
                } catch (Exception e) {
                    return new CommandResult(CommandResult.Type.ERROR, e.getMessage());
                }
            } else if (command instanceof ReplaceIfGreaterCommand) {
                try {
                    if (moviesDB.replaceIfGreater(((ReplaceIfGreaterCommand) command).getKey(), ((ReplaceIfGreaterCommand) command).getMovie(), user_id)) {
                        moviesDB.save();
                        return new CommandResult();
                    } else {
                        return new CommandResult(CommandResult.Type.WARNING, "There is no Records with key greater then " + ((ReplaceIfGreaterCommand) command).getKey());
                    }
                } catch (Exception e) {
                    return new CommandResult(CommandResult.Type.ERROR, e.getMessage());
                }

            } else if (command instanceof MaxByNameCommand) {
                Movie movie = moviesDB.maxByName();
                if (movie != null) {
                    return new CommandResultWithObject(movie);
                } else {
                    return new CommandResultWithObject(CommandResult.Type.WARNING, "Collection is empty");

                }
            } else {
                logger.error("Unknown command => " + command);
            }
            return new CommandResult(CommandResult.Type.ERROR, "Unknown command");
        } catch (MovieException e) {
            return new CommandResult(CommandResult.Type.ERROR, e.getMessage());
        }
    }

    class ReadThread extends Thread {
        private static final int BUFFER_SIZE = 1024;

        private final ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);

        private int numRead = 0;

        @Override
        public void run() {
            // Clear out our read buffer so it's ready for new data
            readBuffer.clear();

            // Attempt to read off the channel
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                while ((numRead = socketChannel.read(readBuffer)) > 0) {
                    baos.write(readBuffer.array());
                    readBuffer.clear();
                }
            } catch (IOException e) {
                logger.warn("Client Forceful shutdown");
                numRead = -2;
                return;
            }

            if (numRead == -1) {
                logger.info("Client Graceful shutdown");
                numRead = -1;
                return;
            }

            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                ObjectInputStream ois = new ObjectInputStream(bais);
                Object object = ois.readObject();
                if (!(object instanceof MovieCommand)) {
                    throw new ClassNotFoundException("Invalid command");
                }
                forkJoinPool.execute(new Executor((MovieCommand)object));
                logger.info("Receive command: " + object + " (size=" + bais.available() + ")");
            } catch (IOException | ClassNotFoundException e) {
                logger.error("Reading error", e);
            }

        }
    }

    class WriteThread extends Thread {
        private CommandResult result;

        public WriteThread(CommandResult result) {
            this.result = result;
        }

        @Override
        public void run() {
            try {
                ByteArrayOutputStream res_baos = new ByteArrayOutputStream();
                ObjectOutputStream res_oos = new ObjectOutputStream(res_baos);
                res_oos.writeObject(result);
                ByteBuffer writeBuffer = ByteBuffer.allocate(res_baos.size());
                writeBuffer.put(res_baos.toByteArray());
                writeBuffer.flip();
                int num_bytes = 0;
                while (writeBuffer.hasRemaining()) {
                    num_bytes += socketChannel.write(writeBuffer);
                }
                logger.info("Send answer: " + result.getType() + " (size=" + num_bytes + ")");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    class Executor implements Runnable {
        private MovieCommand command;

        public Executor(MovieCommand command) {
            this.command = command;
        }

        @Override
        public void run() {
            try {
                moviesDB.lock();
                CommandResult result = call((MovieCommand) command);
                if (result != null) {
                    WriteThread thread = new WriteThread(result);
                    thread.start();
                }
            } finally {
                moviesDB.unlock();
            }

        }
    }


}
