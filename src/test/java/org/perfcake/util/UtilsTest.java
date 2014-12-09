/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
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
 * -----------------------------------------------------------------------/
 */
package org.perfcake.util;

import org.perfcake.PerfCakeConst;
import org.perfcake.util.properties.DefaultPropertyGetter;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilsTest {

   private static final String PROPERTY_LOCATION = "property.location";
   private static final String TEST_VALUE = "test.value";
   private static final String TEST_KEY = "test.key";
   private static final String DEFAULT_VALUE = "default.value";
   private static final String TEST_KEY2 = "test.key2";

   @Test
   public void camelCaseToEnum() {
      Assert.assertEquals(Utils.camelCaseToEnum("camelCaseStringsWithACRONYMS"), "CAMEL_CASE_STRINGS_WITH_ACRONYMS");
   }

   @Test
   public void testTimeToHMS() {

      long test1 = (12 * 3600 + 12 * 60 + 12) * 1000;
      Assert.assertEquals(Utils.timeToHMS(test1), "12:12:12");
      long test2 = (121 * 3600 + 12 * 60 + 12) * 1000;
      Assert.assertEquals(Utils.timeToHMS(test2), "121:12:12");
      long test3 = (1 * 3600 + 12 * 60 + 12) * 1000;
      Assert.assertEquals(Utils.timeToHMS(test3), "1:12:12");
   }

   @Test
   public void testGetProperty() {
      Assert.assertNull(Utils.getProperty(TEST_KEY));
      Assert.assertEquals(Utils.getProperty(TEST_KEY, DEFAULT_VALUE), DEFAULT_VALUE);

      System.setProperty(TEST_KEY, TEST_VALUE);

      Assert.assertEquals(Utils.getProperty(TEST_KEY, DEFAULT_VALUE), TEST_VALUE);

      Assert.assertEquals(Utils.getProperty("props." + TEST_KEY), TEST_VALUE);
      Assert.assertEquals(Utils.getProperty("env.JAVA_HOME"), System.getenv("JAVA_HOME"));
      Assert.assertEquals(Utils.getProperty("env." + TEST_KEY, "non"), "non");
      Assert.assertEquals(Utils.getProperty("props.JAVA_HOME", "non"), "non");

      Map<String, String> env = System.getenv();
      if (!env.isEmpty()) {
         Entry<String, String> first = env.entrySet().iterator().next();
         Assert.assertEquals(Utils.getProperty(first.getKey()), first.getValue());
      }
   }

   @Test
   public void testFilterProperties() throws IOException {
      String unfiltered = "text with ${test.key2} property";
      System.setProperty(TEST_KEY2, TEST_VALUE);

      String filtered = Utils.filterProperties(unfiltered);

      Assert.assertEquals(filtered, "text with test.value property");

      final String propertyPattern = "[^\\\\](#\\{([^#\\{:]+)(:[^#\\{:]*)?})";
      final String filteredString = "Sound system in #{bar} test";
      final Matcher matcher = Pattern.compile(propertyPattern).matcher(filteredString);
      Assert.assertTrue(matcher.find());
      final Properties testProperties = new Properties();
      testProperties.setProperty("bar", "Blue Oyster");

      filtered = Utils.filterProperties(filteredString, matcher, new DefaultPropertyGetter(testProperties));
      Assert.assertEquals(filtered, "Sound system in Blue Oyster test");
   }

   @Test
   public void testLocationToUrl() throws MalformedURLException {
      URL url1 = Utils.locationToUrl("foo", PROPERTY_LOCATION, "bar", ".bak");
      Assert.assertEquals(url1.getProtocol(), "file");
      Assert.assertEquals(url1.toExternalForm(), "file://bar/foo.bak");

      System.setProperty(PROPERTY_LOCATION, "barbar");
      URL url2 = Utils.locationToUrl("http://foo", PROPERTY_LOCATION, "bar", ".bak");
      Assert.assertEquals(url2.getProtocol(), "http");
      Assert.assertEquals(url2.toExternalForm(), "http://foo");
   }

   @Test
   public void testLocationToUrlWithCheck() throws Exception {
      System.setProperty(PerfCakeConst.SCENARIOS_DIR_PROPERTY, getClass().getResource("/scenarios").getPath());
      System.setProperty(PerfCakeConst.MESSAGES_DIR_PROPERTY, getClass().getResource("/messages").getPath());

      URL url = Utils.locationToUrlWithCheck("message1", PerfCakeConst.MESSAGES_DIR_PROPERTY, "", ".txt", ".xml");
      Assert.assertTrue(url.getPath().endsWith("/messages/message1.xml"));
      url = Utils.locationToUrlWithCheck("subdir/subfile", PerfCakeConst.MESSAGES_DIR_PROPERTY, "", ".txt", ".xml");
      Assert.assertTrue(url.getPath().endsWith("/messages/subdir/subfile.txt"));
      url = Utils.locationToUrlWithCheck("message1.xml", "wrong.and.non.existing.property", getClass().getResource("/messages").getPath());
      Assert.assertTrue(url.getPath().endsWith("/messages/message1.xml"));
      url = Utils.locationToUrlWithCheck("file://message1.xml", PerfCakeConst.MESSAGES_DIR_PROPERTY, "", ".never.used");
      Assert.assertTrue(url.getPath().endsWith("/messages/message1.xml"));
      url = Utils.locationToUrlWithCheck("file://src/test/resources/messages/message1.xml", "wrong.and.non.existing.property", "bad.value");
      Assert.assertTrue(url.getPath().endsWith("/messages/message1.xml"));
      url = Utils.locationToUrlWithCheck("non.existing.file.name", "wrong.and.non.existing.property", "bad.value");
      Assert.assertEquals(url.toString(), "file://non.existing.file.name"); // this is not a valid location so the path field of URL doesn't get filled

      System.clearProperty(PerfCakeConst.SCENARIOS_DIR_PROPERTY);
      System.clearProperty(PerfCakeConst.MESSAGES_DIR_PROPERTY);
   }

   @Test
   public void testNonNullValue() {
      String s1 = null, s2 = null, s3 = null;

      Assert.assertNull(Utils.getFirstNotNull(s1, s2, s3));
      Assert.assertEquals(Utils.getFirstNotNull(s1, "Hello", s2), "Hello");
      Assert.assertEquals(Utils.getFirstNotNull("World", s1, s2), "World");
      Assert.assertEquals(Utils.getFirstNotNull(s1, "Hello", "world", s2), "Hello");
      Assert.assertNull(Utils.getFirstNotNull());
   }

   @Test
   public void testSetNotNullProperty() {
      Properties p = new Properties();
      Utils.setFirstNotNullProperty(p, "p1", null, null, null);
      Utils.setFirstNotNullProperty(p, "p2", null, "Hello", null);
      Utils.setFirstNotNullProperty(p, "p3", "World", null, null);
      Utils.setFirstNotNullProperty(p, "p4", null, "Hello", "world", null);
      Utils.setFirstNotNullProperty(p, "p5");

      Assert.assertNull(p.getProperty("p1"));
      Assert.assertEquals(p.getProperty("p2"), "Hello");
      Assert.assertEquals(p.getProperty("p3"), "World");
      Assert.assertEquals(p.getProperty("p4"), "Hello");
      Assert.assertNull(p.getProperty("p5"));
   }
}
