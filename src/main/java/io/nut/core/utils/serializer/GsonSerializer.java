/*
 *  GsonSerializer.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.core.utils.serializer;

import io.nut.base.serializer.Serializer;
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
    public byte[] toBytes(T t)
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
    public T fromBytes(byte[] bytes)
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
