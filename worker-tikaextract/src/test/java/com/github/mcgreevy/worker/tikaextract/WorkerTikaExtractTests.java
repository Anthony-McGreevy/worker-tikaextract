package com.github.mcgreevy.worker.tikaextract;

import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import static com.hpe.caf.worker.document.testing.hamcrest.DocumentMatchers.containsStringFieldValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * Integration test for worker-tikaextract, running the testing framework.
 */
public class WorkerTikaExtractTests
{
    @Test
    public void exampleTest() throws Exception
    {
        final Document document = DocumentBuilder.configure()
            .withFields()
            .addFieldValues("REFERENCE", "/mnt/fs/docs/hr policy.doc", "/mnt/fs/docs/strategy.doc")
            .addFieldValue("REFERENCED_VALUE", "VGhpcyBpcyBhIHRlc3QgdmFsdWU", DocumentWorkerFieldEncoding.base64)
            .documentBuilder()
            .withCustomData()
            .add("ADDITIONAL_INFO", "this is some additional info")
            .documentBuilder()
            .build();

        final WorkerTikaExtract sut = new WorkerTikaExtract();

        sut.processDocument(document);

        assertThat(document, containsStringFieldValue("UNIQUE_ID", "1001"));
        assertThat(document, containsStringFieldValue("UNIQUE_ID", "45"));
    }
}
