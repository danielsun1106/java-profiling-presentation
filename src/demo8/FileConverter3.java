package demo8;

import one.profiler.AsyncProfiler;
import one.profiler.Events;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The demonstration of async-profiler Java API.
 *
 * Selective profiling with resume() / stop().
 * resume() allows to continue profiling session
 * preserving data collected earlier.
 */
public class FileConverter3 {
    public static final String PATH = ".";

    public static void main(String[] args) throws IOException {
        String[] filesToProcess = new File(PATH).list((dir, name) -> name.matches("input.*\\.txt"));
        Objects.requireNonNull(filesToProcess);
        Arrays.sort(filesToProcess);

        FileConverter3 converter = new FileConverter3();
        for (String fileName : filesToProcess) {
            converter.convertFile(PATH + "/" + fileName);
        }

        AsyncProfiler.getInstance().execute("stop,file=profile3.html");
    }

    public void convertFile(String fileName) throws IOException {
        System.out.println("Processing " + fileName);

        List<String> lines = readInput(fileName);

        AsyncProfiler profiler = AsyncProfiler.getInstance();
        profiler.resume(Events.CPU, 1_000_000);

        List<Entry> entries = convertList(lines);

        profiler.stop();

        saveResult(entries, fileName.replace(".txt", ".bin"));
    }

    private List<String> readInput(String fileName) throws IOException {
        return Files.readAllLines(Paths.get(fileName));
    }

    private List<Entry> convertList(List<String> lines) {
        return lines.stream()
                .map(Entry::new)
                .distinct()
                .limit(1_000_000)
                .sorted(Comparator.comparingInt(e -> e.value))
                .collect(Collectors.toList());
    }

    private void saveResult(List<Entry> entries, String fileName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        for (Entry e : entries) {
            out.writeUTF(e.name);
            out.writeInt(e.value);
        }

        Files.write(Paths.get(fileName), baos.toByteArray(), StandardOpenOption.CREATE);
    }

    private static class Entry {
        final String name;
        final int value;

        Entry(String line) {
            int i = line.indexOf(':');
            this.name = line.substring(0, i);
            this.value = Integer.parseInt(line.substring(i + 2));
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Entry) && (((Entry) obj).name.endsWith(name));
        }
    }
}
