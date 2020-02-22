package org.squonk.fragnet.chem;

import org.squonk.fragnet.search.model.v2.SimpleSmilesMol;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ChemUtils {

    public static Stream<SimpleSmilesMol> readSmilesData(String data) throws IOException {
        Stream<String> stream = Pattern.compile("\\r?\\n").splitAsStream( data );
        return readLines(stream);
    }

    public static Stream<SimpleSmilesMol> readSmilesFile(String file) throws IOException {
        Path path = FileSystems.getDefault().getPath(file);
        Stream<String> stream = Files.lines(path);
        return readLines(stream);
    }

    public static Stream<SimpleSmilesMol> readLines(Stream<String> lines) throws IOException {
        AtomicInteger count = new AtomicInteger(0);
        Stream<SimpleSmilesMol> mols = lines.sequential().map((l) -> {
            count.incrementAndGet();
            l = l.trim();
            String[] tokens = l.split("\\s+");
            if (tokens.length > 1) {
                return new SimpleSmilesMol(tokens[0], tokens[1]);
            } else {
                return new SimpleSmilesMol(l, ""+count);
            }
        });
        return mols;
    }

}
