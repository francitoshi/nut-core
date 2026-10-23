/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.core.serializer;

import io.nut.base.commons.serializer.Serializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class GsonSerializer<T> implements Serializer<T>
{
    private final Gson gson;
    private final Class<T> clazz;

    public GsonSerializer(Class<T> clazz)
    {
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
        this.clazz = clazz;
    }

    @Override
    public byte[] serialize(T t)
    {
        if (t == null)
        {
            return null;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8))
        {
            gson.toJson(t, writer);
            writer.flush();
            return baos.toByteArray();
        }
        catch (Exception ex)
        {
            throw new RuntimeException("Error serializing object to bytes", ex);
        }
    }

    @Override
    public T deserialize(byte[] bytes)
    {
        if (bytes == null || clazz == null)
        {
            return null;
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes); InputStreamReader reader = new InputStreamReader(bais, StandardCharsets.UTF_8))
        {
            return this.gson.fromJson(reader, this.clazz);
        }
        catch (Exception ex)
        {
            throw new RuntimeException("Error deserializing bytes to object", ex);
        }
    }
}
