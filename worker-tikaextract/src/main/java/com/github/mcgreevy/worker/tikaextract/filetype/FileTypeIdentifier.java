package com.github.mcgreevy.worker.tikaextract.filetype;

import com.github.mcgreevy.worker.tikaextract.WorkerTikaExtractConstants;
import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class FileTypeIdentifier
{
    private static final List<String> FAMILY_FILE_TYPES = new ArrayList<>();

    static {
        final String nonFamilyFileTypesJson = System.getenv(WorkerTikaExtractConstants.EnvironmentBasedConfig.FAMILY_TYPES);
        if (!Strings.isNullOrEmpty(nonFamilyFileTypesJson)) {
            final Gson gson = new Gson();
            final Type listType = new TypeToken<List<String>>(){}.getType();
            final List<String> nonFamilyMimeTypes = gson.fromJson(nonFamilyFileTypesJson, listType);
            FAMILY_FILE_TYPES.addAll(nonFamilyMimeTypes);
        }
    }

    public static boolean isFamilyType(final String mimeType)
    {
        return FAMILY_FILE_TYPES.contains(mimeType);
    }
}
