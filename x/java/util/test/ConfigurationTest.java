
package x.java.util.test;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import x.java.util.Configuration;

import org.junit.Test;

import static org.junit.Assert.*;


public class ConfigurationTest
{
    @SuppressWarnings("serial")
    public static class Config extends Configuration
    {
        public final StringProperty someString
            = new StringProperty( "test.some.string"
                                , "some string"
                                , "This is SOME string...");

        public final IntegerProperty someInt
            = new IntegerProperty("test.some.int", 17, "This is some int...");
        
        
        public final FormatProperty someFormat
            = new FormatProperty( "test.some.format"
                                , "some %d"
                                , "This is some pattern...");
    }

    protected final static Config config = new Config();
    
    public static void main(String args[])
    {
        if (args.length != 0) {
            System.err.println("I don't take arguments!");
            return;
        }
            
        try {
            config.store( System.out
                      , "some.properties\n"
                       +"The line above may contain the properties"
                       +" file name to be loaded by default.\n"
                       +"One can even subclass Configuration"
                       +" to implement such a convention!");
        } catch (IOException e_) {
            System.err.println("I couldn't print the properties!");
        }        
    }
    

    @Test public void testConfig() throws IOException
    {
        assertEquals(config.someString.get(), "some string");
        assertEquals(config.someInt.get(), 17);
        assertEquals(config.someFormat.get(17), "some 17");

        config.someInt.set(33);
        config.someFormat.set("%sthing goes");
        config.someString.set("any string");
        
        assertEquals(config.someString.get(), "any string");
        assertEquals(config.someInt.get(), 33);
        assertEquals( config.someFormat.get("some"), "something goes");
        
        File fProps_ = File.createTempFile("config", ".properties");
        FileOutputStream fos_ = new FileOutputStream(fProps_);
        config.store(fos_, "Test\nTest\nTest\n");
        fos_.close();
        
        config.someString.reset();
        config.someFormat.reset();
        config.someInt.reset();
        
        FileInputStream fis_ = new FileInputStream(fProps_);
        config.load(fis_);
        fis_.close();
        fProps_.delete();

        assertEquals(config.someString.get(), "any string");
        assertEquals(config.someInt.get(), 33);
        assertEquals( config.someFormat.get("some"), "something goes");
    }
}
