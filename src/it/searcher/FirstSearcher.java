package it.searcher;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class FirstSearcher {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {

            Path indexPath = Paths.get("./Index");
            Directory dir = FSDirectory.open(indexPath);
            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            StoredFields storedFields = searcher.storedFields();

            Analyzer defaultAnalyzer = new ItalianAnalyzer();
            Map<String, Analyzer> perField = new HashMap<>();
            perField.put("filename", new KeywordAnalyzer());
            PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, perField);

            System.out.println("MOTORE DI RICERCA");
            System.out.println("Scrivi 'e' o 'E' per uscire.\n");

            while (true) {
                System.out.print("Inserisci parole da cercare (es: sale pomodoro ricetta): ");
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("e")) {
                    System.out.println("Uscita dal programma.");
                    break;
                }

                if (input.isEmpty()) {
                    System.out.println("Nessuna query inserita.\n");
                    continue;
                }

                try {

                    List<String> terms = new ArrayList<>();
                    Matcher m = Pattern.compile("\"([^\"]+)\"|(\\S+)").matcher(input);
                    while (m.find()) {
                        if (m.group(1) != null)
                            terms.add(m.group(1)); // frase tra virgolette
                        else
                            terms.add(m.group(2)); // parola singola
                    }

                    MultiFieldQueryParser parser = new MultiFieldQueryParser(
                            new String[]{"content", "filename"}, analyzer);

                    parser.setDefaultOperator(QueryParser.Operator.OR);

                    StringBuilder queryBuilder = new StringBuilder();
                    for (String term : terms) {
                        term = term.toLowerCase().trim();
                        if (term.length() > 2) {
                            queryBuilder.append("(")
                                    .append(QueryParser.escape(term))
                                    .append("* OR ")
                                    .append(QueryParser.escape(term))
                                    .append("~1) ");
                        } else {
                            queryBuilder.append(QueryParser.escape(term)).append(" ");
                        }
                    }

                    String finalQuery = queryBuilder.toString().trim();
                    Query query = parser.parse(finalQuery);

                    TopDocs hits = searcher.search(query, 20);

                    if (hits.scoreDocs.length == 0) {
                        System.out.println("Nessun risultato trovato.\n");
                    } else {
                        for (ScoreDoc scoreDoc : hits.scoreDocs) {
                            Document doc = storedFields.document(scoreDoc.doc);
                            float score = scoreDoc.score;
                            System.out.printf("- %-30s (score: %.3f)%n", doc.get("filename"), score);
                        }
                        System.out.println();
                    }

                } catch (ParseException e) {
                    System.out.println("Errore di sintassi nella query: " + e.getMessage() + "\n");
                } catch (Exception e) {
                    System.out.println("Errore durante la ricerca: " + e.getMessage() + "\n");
                    e.printStackTrace();
                }
            }

            reader.close();
            dir.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
