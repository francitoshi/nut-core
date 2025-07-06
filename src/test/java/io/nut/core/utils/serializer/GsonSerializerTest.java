/*
 *  GsonSerializerTest.java
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

import io.nut.base.crypto.EncryptedMapWrapper;
import io.nut.base.crypto.Kripto;
import io.nut.base.serializer.StringSerializer;
import java.io.File;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mapdb.DB;
import org.mapdb.DBMaker;

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

    @Test
    public void testAes() throws InvalidKeySpecException
    {
        GsonSerializer<User> instance = new GsonSerializer<>(User.class);
        Kripto kripto = Kripto.getInstance().setMinDeriveRounds(8);
        
        char[] passphrase = "passphrase".toCharArray();

        Address dir1 = new Address("street1", 1);
        Address dir2 = new Address("street2", 1);
        User user1 = new User("name1", 11, true, dir1, "password1");
        User user2 = new User("name2", 22, true, dir2, "password2");
        
        File file = new File("encrypted.db");
        System.out.println("\n=== write ===");
        try(DB db = DBMaker.fileDB(file).make())
        {
            file.deleteOnExit();
            Map<String, String> map = (Map<String, String>) db.hashMap("userdata").createOrOpen();

            EncryptedMapWrapper<String, User> wrapper = new EncryptedMapWrapper<>(kripto, map, passphrase, "salt", 10, 256, new StringSerializer(), instance);

        
            wrapper.put("user1", user1);
            wrapper.put("user2", user2);
        }        
        
        System.out.println("\n=== read ===");
        try(DB db = DBMaker.fileDB(file).make())
        {
            file.deleteOnExit();
            Map<String, String> map = (Map<String, String>) db.hashMap("userdata").createOrOpen();

            EncryptedMapWrapper<String, User> wrapper = new EncryptedMapWrapper<>(kripto, map, passphrase, "salt", 10, 256, new StringSerializer(), instance);

            User user11 = wrapper.get("user1");
            User user22 = wrapper.get("user2");
            
            assertEquals(user1, user11);
            assertEquals(user2, user22);
            
            assertEquals(user1.address, user11.address);
            assertEquals(user2.address, user22.address);
            
        }        
        
    }

}
