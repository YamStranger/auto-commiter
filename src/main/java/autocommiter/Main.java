package autocommiter;

import command.line.ArgumentsParser;
import date.Dates;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * User: YamStranger
 * Date: 6/12/15
 * Time: 4:08 PM
 */
public class Main {
    public static void main(String... args) throws Exception {
        Map<String, String> values = new ArgumentsParser(args).arguments();
        if (values.containsKey("?") || values.containsKey("help")) {
            printHelp();
            return;
        }
        String token = values.get("token");
        String remote = values.get("remote");
        String folder = values.get("folder");
        String commits = values.get("commits");
        if (token == null || remote == null || folder == null || commits == null) {
            printHelp();
            return;
        }
        Path current = Paths.get("");
        JGitCommitter jGitCommitter = new JGitCommitter(token, remote, current.resolve(folder));
        Dates period = new Dates();
        Dates checking = new Dates();
        Integer amount = Integer.valueOf(commits);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int count = random.nextInt(amount / 2, amount);
        System.out.println("new period started, will be committed " + count + " in config " + amount);
        int wait = (24 * 60 * 60) / (7 * amount);
        while (!Thread.currentThread().isInterrupted()) {
            int skip = 0;
            if (new Dates().difference(checking, Calendar.SECOND) >= wait && count > 0) {
                //interval
                if (skip-- <= 0 && random.nextBoolean()) {
                    skip = random.nextInt(1, 3);
                    System.out.println("new file changes preparing " + new Dates());
                    Path data = jGitCommitter.create("data.bin");
                    try (BufferedWriter writer = Files.newBufferedWriter(data,
                            Charset.forName("UTF-8"));) {
                        int number = random.nextInt(100, 5000);
                        for (int i = 0; i < number; ++i) {
                            UUID uuid = UUID.randomUUID();
                            writer.write(uuid.toString());
                            writer.write(" ");
                            if (i % 3 == 0) {
                                writer.newLine();
                            }
                        }
                        writer.flush();
                        jGitCommitter.synchronize();
                        count--;
                        System.out.println("commit done " + new Dates() + " left " + count);
                    } catch (IOException e) {
                        System.out.println("some exception, sorry");
                        e.printStackTrace();
                    }
                }
                checking = new Dates();
            }
            if (new Dates().difference(period, Calendar.MINUTE) >= 24 * 60) {
                period = new Dates();
                int newAmount = random.nextInt(amount / 2, amount);
                System.out.println("new period started, will be committed " + newAmount + " in config " + amount);
                count = newAmount;
            }
            Thread.sleep(1000);
        }

    }

    private static void printHelp() {
        System.out.println("Here is solution for automits to repo");
        System.out.println("Please specify params:");
        System.out.println("-token=tokenExample");
        System.out.println("-remote=https://github.com/bla.git");
        System.out.println("-folder=repository");
        System.out.println("-commits=2");
    }
}
