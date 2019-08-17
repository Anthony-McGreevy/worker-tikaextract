package com.github.mcgreevy.worker.tikaextract.filetype;

import com.github.mcgreevy.worker.tikaextract.WorkerTikaExtractConstants;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;

public final class FileTypeIdentifier
{
    private static final List<String> NON_FAMILY_TYPES = new ArrayList<>();

    static {
        final String nonFamilyFileTypes = System.getenv(WorkerTikaExtractConstants.EnvironmentBasedConfig.NON_FAMILY_TYPES);
        if (!Strings.isNullOrEmpty(nonFamilyFileTypes)) {
            for (final String type : nonFamilyFileTypes.split(",")) {
                NON_FAMILY_TYPES.add(type);
            }
        }
    }

    public static boolean isFamilyType(final String mimeType)
    {
        return !NON_FAMILY_TYPES.contains(mimeType);
    }
}
