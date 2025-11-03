package it.indexerfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class FirstIndexer {

    public static void main(String[] args) throws IOException {

    	Path indexPath = Paths.get("./Index");
        Path documentsPath = Paths.get("./Data");
        
        Directory directory = FSDirectory.open(indexPath);

        Analyzer defaultAnalyzer = new ItalianAnalyzer();
        Map<String, Analyzer> perField = new HashMap<>();
        perField.put("filename", new KeywordAnalyzer());
        PerFieldAnalyzerWrapper perFieldAnalyzer =
                new PerFieldAnalyzerWrapper(defaultAnalyzer, perField);

        IndexWriterConfig config = new IndexWriterConfig(perFieldAnalyzer);
        config.setCodec(new SimpleTextCodec());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        
        long startTime = System.currentTimeMillis();

        AtomicInteger fileCount = new AtomicInteger(0);
        
        try (IndexWriter writer = new IndexWriter(directory, config)) {

            Files.walk(documentsPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .forEach(p -> {
                        try {
                            Document doc = new Document();

                            doc.add(new TextField("filename",
                                    p.getFileName().toString(), Field.Store.YES));

                            String content = new String(Files.readAllBytes(p));
                            doc.add(new TextField("content", content, Field.Store.YES));

                            writer.addDocument(doc);
                            fileCount.incrementAndGet();
                            System.out.println("Indicizzato: " + p.getFileName());

                        } catch (IOException e) {
                            System.err.println("Errore su file " + p + ": " + e.getMessage());
                        }
                    });

            writer.commit();
        }
        long endTime = System.currentTimeMillis();
        long elapsedMillis = endTime - startTime;
        double elapsedSeconds = elapsedMillis / 1000.0;
        System.out.println("\nIndicizzazione completata!");
        System.out.println("File indicizzati: " + fileCount.get());
        System.out.println(" Tempo totale: " + elapsedSeconds + " secondi (" + elapsedMillis + " ms)");

        directory.close();
    }
}
