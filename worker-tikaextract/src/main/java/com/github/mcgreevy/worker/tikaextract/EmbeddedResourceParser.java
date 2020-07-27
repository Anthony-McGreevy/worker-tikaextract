package com.github.mcgreevy.worker.tikaextract;

import com.github.mcgreevy.worker.tikaextract.filetype.FileTypeIdentifier;
import com.google.common.base.Strings;
import com.hpe.caf.api.*;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.worker.document.*;
import com.hpe.caf.worker.document.model.*;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import java.io.*;
import java.util.*;
import org.apache.tika.exception.*;
import org.apache.tika.io.*;
import org.apache.tika.parser.*;
import org.apache.tika.sax.*;
import org.apache.tika.extractor.ParsingEmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import java.nio.charset.StandardCharsets;

import static org.apache.tika.sax.XHTMLContentHandler.XHTML;

public final class EmbeddedResourceParser extends ParsingEmbeddedDocumentExtractor
{
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(WorkerTikaExtract.class);
    private static final Parser DELEGATING_PARSER = new DelegatingParser();
    private static final Codec JSON_CODEC = new JsonCodec();
    private final Document document;
    private final DataStore datastore;
    private final String outputPartialReference;
    private final ParseContext context;
    private final FileTypeIdentifier fileTypeIdentifier;
    private int subfileCount = 0;

    public EmbeddedResourceParser(final ParseContext context, final Document document, final DataStore datastore,
                                  final String outputPartialReference, final FileTypeIdentifier fileTypeIdentifier)
    {
        super(context);
        this.context = context;
        this.document = document;
        this.datastore = datastore;
        this.outputPartialReference = outputPartialReference;
        this.fileTypeIdentifier = fileTypeIdentifier;
    }

    @Override
    public void parseEmbedded(final InputStream stream, final ContentHandler handler, final Metadata metadata, final boolean outputHtml)
        throws SAXException, IOException
    {
        if (outputHtml) {
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute("", "class", "class", "CDATA", "package-entry");
            handler.startElement(XHTML, "div", "div", attributes);
        }

        final String name = metadata.get(Metadata.RESOURCE_NAME_KEY);
        if (name != null && name.length() > 0 && outputHtml) {
            handler.startElement(XHTML, "h1", "h1", new AttributesImpl());
            char[] chars = name.toCharArray();
            handler.characters(chars, 0, chars.length);
            handler.endElement(XHTML, "h1", "h1");
        }

        // Use the delegate parser to parse this entry
        try (final TemporaryResources tmp = new TemporaryResources()) {
            final TikaInputStream newStream = TikaInputStream.get(new CloseShieldInputStream(stream), tmp);
            if (stream instanceof TikaInputStream) {
                final Object container = ((TikaInputStream) stream).getOpenContainer();
                if (container != null) {
                    newStream.setOpenContainer(container);
                }
            }
            final String storageRef = datastore.store(stream, outputPartialReference);
            if (fileTypeIdentifier.isFamilyType(metadata.get("Content-Type"))) {
                final EmbeddedContentHandler subfileHandler = new EmbeddedContentHandler(new BodyContentHandler());
                final Metadata subfileMetadata = new Metadata();
                DELEGATING_PARSER.parse(newStream, subfileHandler, subfileMetadata, context);

                final String content = subfileHandler.toString();
                final Subdocument subdocument = document.getSubdocuments().add(storageRef);
                addFieldToDocument(subdocument, WorkerTikaExtractConstants.CustomData.STORAGE_REFERENCE, storageRef);
                if (!Strings.isNullOrEmpty(content)) {
                    addFieldToDocument(subdocument, WorkerTikaExtractConstants.Fields.CONTENT, subfileHandler.toString());
                }
                for (final String key : subfileMetadata.names()) {
                    addFieldToDocument(subdocument, key, subfileMetadata.get(key));
                }
                final Field numberOfSubdocuments = document.getField(WorkerTikaExtractConstants.Fields.NUMBER_OF_SUBDOCUMENTS);
                if (Strings.isNullOrEmpty(numberOfSubdocuments.getStringValues().stream().findFirst().orElse(""))) {
                    addFieldToDocument(WorkerTikaExtractConstants.Fields.NUMBER_OF_SUBDOCUMENTS, "1");
                } else {
                    final int numberOfSubdocs = Integer.parseInt(numberOfSubdocuments.getStringValues().stream().findFirst().get()) + 1;
                    numberOfSubdocuments.clear();
                    addFieldToDocument(WorkerTikaExtractConstants.Fields.NUMBER_OF_SUBDOCUMENTS,
                                       Integer.toString(numberOfSubdocs));
                }
            } else {
                final Document subDocument = DocumentBuilder.configure().withReference(storageRef).build();
                addFieldToDocument(subDocument, WorkerTikaExtractConstants.CustomData.STORAGE_REFERENCE, storageRef);
                final WorkerResponse response = createWorkerResponse(document, subDocument);
                document.getTask().getService(WorkerTaskData.class).addResponse(response, false);
            }

        } catch (final EncryptedDocumentException ex) {
            LOG.error("A Encrypted Document Exception occurred during subfile metadata extraction.", ex);
        } catch (final TikaException ex) {
            LOG.error("A Tika Exception occurred during subfile metadata extraction.", ex);
        } catch (final DataStoreException ex) {
            LOG.error("A Data Store Exception occured saving the a subfile to the datastore", ex);
        } catch (final WorkerException ex) {
            LOG.error("A Worker Exception occured creating subfile to send back to workflow worker.", ex);
        } catch (final RuntimeException ex) {
            LOG.error("A Runtime Exception occured during dispatch of subdocument for processing.", ex);
        }

        if (outputHtml) {
            handler.endElement(XHTML, "div", "div");
        }
    }

    private void addFieldToDocument(final String fieldName, final String fieldValue)
    {
        addFieldToDocument(document, fieldName, fieldValue);
    }

    private void addFieldToDocument(final Document subdocument, final String fieldName, final String fieldValue)
    {
        if (Strings.isNullOrEmpty(fieldValue)) {
            return;
        }
        final byte[] fieldValueBytes = fieldValue.getBytes(StandardCharsets.UTF_8);
        final boolean shouldStore = fieldValueBytes.length > 5120;

        if (!shouldStore) {
            subdocument.getField(fieldName).add(fieldValue);
        } else {
            try (final InputStream in = new ByteArrayInputStream(fieldValueBytes)) {
                subdocument.getField(fieldName).addReference(datastore.store(in, outputPartialReference));
            } catch (final IOException | DataStoreException ex) {
                LOG.error("An error occurred storing the contents of the field value to the data store.", ex);
            }
        }
    }

    private WorkerResponse createWorkerResponse(final Document document, final Document subfile)

    {
        try {
            final byte[] taskData = JSON_CODEC.serialise(createTaskData(document, subfile));
            return new WorkerResponse(System.getenv("NON_FAMILY_OUTPUT_QUEUE_NAME"), TaskStatus.NEW_TASK,
                                      taskData, getValueOrDefault("NON_FAMILY_OUTPUT_WORKER_NAME", "WorkerWorflow"),
                                      Integer.parseInt(getValueOrDefault("NON_FAMILY_OUTPUT_WORKER_VERSION", "2")), null);

        } catch (final CodecException ex) {
            LOG.debug("An error occured dispatching subfile for processing: \n Subfile: " + subfile.getReference(), ex);
            throw new RuntimeException(ex);
        }
    }

    private DocumentWorkerDocumentTask createTaskData(final Document document, final Document subfile)
    {
        final String projectId = document.getCustomData(WorkerTikaExtractConstants.CustomData.PROJECT_ID);
        final String workflowId = document.getCustomData(WorkerTikaExtractConstants.CustomData.WORKFLOW_ID);
        final String tenantId = document.getCustomData(WorkerTikaExtractConstants.CustomData.TENANT_ID);
        final DocumentWorkerDocumentTask taskData = new DocumentWorkerDocumentTask();
        taskData.document = convertToDocumentWorkerDocument(subfile, document.getReference());
        taskData.changeLog = new ArrayList<>();
        taskData.customData = new HashMap<>();
        taskData.customData.put("tenantId", tenantId);
        taskData.customData.put("outputPartialReference", outputPartialReference);
        taskData.customData.put("workflowId", String.valueOf(workflowId));
        taskData.customData.put("projectId", projectId);
        return taskData;
    }

    private DocumentWorkerDocument convertToDocumentWorkerDocument(final Document document, final String parentRefernce)
    {
        final DocumentWorkerDocument newDocument = new DocumentWorkerDocument();
        newDocument.fields = new HashMap<>();
        final DocumentWorkerFieldValue parentReference = new DocumentWorkerFieldValue();
        parentReference.data = parentRefernce + "." + subfileCount;
        newDocument.fields.put("PARENT_FILE_REFERENCE", Arrays.asList(parentReference));
        newDocument.failures = new ArrayList<>();
        newDocument.subdocuments = new ArrayList<>();
        newDocument.reference = document.getReference();
        subfileCount++;
        return newDocument;
    }

    private static String getValueOrDefault(final String key, final String defaultValue)
    {
        return System.getProperty(key, System.getenv(key) != null ? System.getenv(key) : defaultValue);
    }
}
