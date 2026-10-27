/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.core.serializer;

import io.github.francitoshi.nut.base.commons.serializer.Serializer;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.nio.charset.StandardCharsets;

public class MoshiSerializer<T> implements Serializer<T>
{
    private final JsonAdapter<T> adapter;
    private final Class<T> clazz;

    public MoshiSerializer(Class<T> clazz)
    {
        this.clazz = clazz;
        this.adapter = clazz == null ? null : new Moshi.Builder().build().adapter(clazz);
    }

    @Override
    public byte[] serialize(T t)
    {
        if (t == null)
        {
            return null;
        }
        try
        {
            return adapter.toJson(t).getBytes(StandardCharsets.UTF_8);
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
        try
        {
            return adapter.fromJson(new String(bytes, StandardCharsets.UTF_8));
        }
        catch (Exception ex)
        {
            throw new RuntimeException("Error deserializing bytes to object", ex);
        }
    }
}
