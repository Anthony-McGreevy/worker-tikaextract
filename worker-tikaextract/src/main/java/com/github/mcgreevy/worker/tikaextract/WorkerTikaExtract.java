package com.github.mcgreevy.worker.tikaextract;

import com.github.mcgreevy.worker.tikaextract.filetype.FileTypeIdentifier;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.FilePathProvider;
import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.model.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 *
 */
public class WorkerTikaExtract implements DocumentWorker
{
    private static final Logger LOG = LoggerFactory.getLogger(WorkerTikaExtract.class);
    private static final FileTypeIdentifier FILE_TYPE_IDENTIFIER = new FileTypeIdentifier();

    public WorkerTikaExtract()
    {
    }

    /**
     * This method provides an opportunity for the worker to report if it has any problems which would prevent it processing documents
     * correctly. If the worker is healthy then it should simply return without calling the health monitor.
     *
     * @param healthMonitor used to report the health of the application
     */
    @Override
    public void checkHealth(final HealthMonitor healthMonitor)
    {
    }

    /**
     * Processes a single document.
     *
     * @param document the document to be processed. Fields can be added or removed from the document.
     * @throws InterruptedException if any thread has interrupted the current thread
     * @throws DocumentWorkerTransientException if the document could not be processed
     */
    @Override
    public void processDocument(final Document document) throws InterruptedException, DocumentWorkerTransientException
    {

        final DataStore datastore = document.getApplication().getService(DataStore.class);
        final String storageRef = document.getField(
            WorkerTikaExtractConstants.CustomData.STORAGE_REFERENCE).getStringValues().stream().findFirst().get();
        try {
            final File file = getDocumentAsFile(datastore, storageRef);
            final String outputPartialReference = document.getCustomData(WorkerTikaExtractConstants.CustomData.OUTPUT_PARTIAL_REFERENCE);
            try (final FileInputStream inputstream = new FileInputStream(file)) {
                final Parser parser = new AutoDetectParser();
                final BodyContentHandler handler = new BodyContentHandler();
                final Metadata metadata = new Metadata();
                final ParseContext context = new ParseContext();
                context.set(EmbeddedDocumentExtractor.class, new EmbeddedResourceParser(context, document, datastore,
                                                                                        outputPartialReference, FILE_TYPE_IDENTIFIER));
                parser.parse(inputstream, handler, metadata, context);

                final String content = handler.toString();
                document.getField(WorkerTikaExtractConstants.Fields.CONTENT).add(content);
                for (final String name : metadata.names()) {
                    document.getField(name).add(metadata.get(name));
                }
            }
        } catch (final DataStoreException | IOException | TikaException | SAXException ex) {
            LOG.error("An error occured during process of document.", ex);
            document.addFailure(WorkerTikaExtractConstants.Failures.WORKER_TIKAEXTRACT_001, ex.toString());
        }
    }

    private File getDocumentAsFile(final DataStore datastore, final String storageRef) throws IOException, DataStoreException
    {
        return getDocumentFilePath(datastore, storageRef).toFile();
    }

    private Path getDocumentFilePath(final DataStore datastore, final String storageRef) throws IOException, DataStoreException
    {
        if (datastore instanceof FilePathProvider) {
            final Path retrievedFile = ((FilePathProvider) datastore).getFilePath(storageRef);
            LOG.debug("StorageContext using file in storage: {}", retrievedFile);
            return retrievedFile;
        } else {
            final Path outputDirectory = Files.createDirectory(Paths.get("").resolve(UUID.randomUUID().toString()).toAbsolutePath());
            LOG.debug("StorageContext created output directory: {}", outputDirectory);
            final Path retrievedFile = outputDirectory.resolve(UUID.randomUUID().toString()).toAbsolutePath();
            Files.copy(datastore.retrieve(storageRef), retrievedFile, StandardCopyOption.REPLACE_EXISTING);
            LOG.debug("StorageContext retrieved file from storage,  local file: {}", retrievedFile);
            return retrievedFile;
        }
    }
}
