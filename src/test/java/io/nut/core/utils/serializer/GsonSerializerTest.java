/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.core.utils.serializer;

import java.util.Objects;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class GsonSerializerTest
{

    static class Address
    {
        public final String street;
        public final int number;
        public Address(String street, int number)
        {
            this.street = street;
            this.number = number;
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.street);
            hash = 53 * hash + this.number;
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            final Address other = (Address) obj;
            if (this.number != other.number)
            {
                return false;
            }
            return Objects.equals(this.street, other.street);
        }
        
    }

    static class User
    {

        public final String name;
        public final int years;
        public final boolean active;
        public final Address address; // nested object, that must be also serialized
        public final String password;

        public User(String name, int years, boolean active, Address address, String password)
        {
            this.name = name;
            this.years = years;
            this.active = active;
            this.address = address;
            this.password = password;
        }

        @Override
        public int hashCode()
        {
            int hash = 5;
            hash = 83 * hash + Objects.hashCode(this.name);
            hash = 83 * hash + this.years;
            hash = 83 * hash + (this.active ? 1 : 0);
            hash = 83 * hash + Objects.hashCode(this.address);
            hash = 83 * hash + Objects.hashCode(this.password);
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            final User other = (User) obj;
            if (this.years != other.years)
            {
                return false;
            }
            if (this.active != other.active)
            {
                return false;
            }
            if (!Objects.equals(this.name, other.name))
            {
                return false;
            }
            if (!Objects.equals(this.password, other.password))
            {
                return false;
            }
            return Objects.equals(this.address, other.address);
        }
        
    }

    @Test
    public void testToBytes()
    {
        GsonSerializer<User> instance = new GsonSerializer<>(User.class);

        Address dir1 = new Address("street1", 1);
        Address dir2 = new Address("street2", 1);
        
        User usu1 = new User("nombre1", 11, true, dir1, "password1");
        User usu2 = new User("nombre2", 22, true, dir2, "password2");
        
        byte[] bytes1 = instance.toBytes(usu1);
        byte[] bytes2 = instance.toBytes(usu2);
        
        User usu11 = instance.fromBytes(bytes1);
        User usu22 = instance.fromBytes(bytes2);

        assertEquals(usu1, usu11);
        assertEquals(usu2, usu22);
        assertEquals(dir1, usu11.address);
        assertEquals(dir2, usu22.address);

    }

}
