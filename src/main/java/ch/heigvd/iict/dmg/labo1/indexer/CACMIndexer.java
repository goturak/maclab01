package ch.heigvd.iict.dmg.labo1.indexer;

import ch.heigvd.iict.dmg.labo1.parsers.ParserListener;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class CACMIndexer implements ParserListener {

    private Directory dir = null;
    private IndexWriter indexWriter = null;

    private Analyzer analyzer = null;
    private Similarity similarity = null;

    public CACMIndexer(Analyzer analyzer, Similarity similarity) {
        this.analyzer = analyzer;
        this.similarity = similarity;
    }

    public void openIndex() {
        // 1.2. create an index writer config
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(OpenMode.CREATE); // create and replace existing index
        iwc.setUseCompoundFile(false); // not pack newly written segments in a compound file:
        //keep all segments of index separately on disk
        if (similarity != null)
            iwc.setSimilarity(similarity);
        // 1.3. create index writer
        Path path = FileSystems.getDefault().getPath("index");
        try {
            this.dir = FSDirectory.open(path);
            this.indexWriter = new IndexWriter(dir, iwc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewDocument(Long id, String authors, String title, String summary) {
        Document doc = new Document();


        doc.add(new LongPoint("id", id));
        if (!authors.isEmpty()) {
            String[] authorArray = authors.split(";");
            for (String s : authorArray) {
                doc.add(new StringField("authors", s, Field.Store.YES));
            }
        }
        doc.add(new TextField("title", title, Field.Store.YES));

        if (summary != null) {
            FieldType summaryFieldType= new FieldType();
            summaryFieldType.setStoreTermVectors(true);
            summaryFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
            doc.add(new Field("summary", summary,summaryFieldType));
        }
        try {
            this.indexWriter.addDocument(doc);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void finalizeIndex() {
        if (this.indexWriter != null)
            try {
                this.indexWriter.close();
            } catch (IOException e) { /* BEST EFFORT */ }
        if (this.dir != null)
            try {
                this.dir.close();
            } catch (IOException e) { /* BEST EFFORT */ }
    }

}
