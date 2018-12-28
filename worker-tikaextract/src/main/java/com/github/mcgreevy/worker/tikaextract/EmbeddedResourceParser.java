/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.mcgreevy.worker.tikaextract;

import com.github.mcgreevy.worker.tikaextract.filetype.FileTypeIdentifier;
import com.github.mcgreevy.worker.tikaextract.response.MessageDispatchException;
import com.github.mcgreevy.worker.tikaextract.response.SubfileDispatcher;
import com.google.common.base.Strings;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Field;
import com.hpe.caf.worker.document.model.Subdocument;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.ParsingEmbeddedDocumentExtractor;
import org.apache.tika.io.CloseShieldInputStream;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.DelegatingParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.EmbeddedContentHandler;
import static org.apache.tika.sax.XHTMLContentHandler.XHTML;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class EmbeddedResourceParser extends ParsingEmbeddedDocumentExtractor
{
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(WorkerTikaExtract.class);
    private static final Parser DELEGATING_PARSER = new DelegatingParser();
    private final Document document;
    private final DataStore datastore;
    private final String outputPartialReference;
    private final ParseContext context;
    private final FileTypeIdentifier fileTypeIdentifier;

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
    public void parseEmbedded(
        InputStream stream, ContentHandler handler, Metadata metadata, boolean outputHtml)
        throws SAXException, IOException
    {
        if (outputHtml) {
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute("", "class", "class", "CDATA", "package-entry");
            handler.startElement(XHTML, "div", "div", attributes);
        }

        String name = metadata.get(Metadata.RESOURCE_NAME_KEY);
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
                SubfileDispatcher.sendMessage(document, subDocument);
            }

        } catch (final EncryptedDocumentException ex) {
            LOG.error("An error occurred during subfile metadata extraction.", ex);
        } catch (final TikaException ex) {
            LOG.error("An error occurred during subfile metadata extraction.", ex);
        } catch (final DataStoreException ex) {
            LOG.error("An error occured saving the a subfile to the datastore", ex);
        } catch (final WorkerException ex) {
            LOG.error("An error occured creating subfile to send back to workflow worker.", ex);
        } catch (final MessageDispatchException ex) {
            LOG.error("An error occured during dispatch of subdocument for processing.", ex);
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
}
