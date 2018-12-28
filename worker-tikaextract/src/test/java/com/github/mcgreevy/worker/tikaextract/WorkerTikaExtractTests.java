/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.mcgreevy.worker.tikaextract;

import com.github.mcgreevy.worker.tikaextract.WorkerTikaExtract;
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
