/**
 * This work was created by the National Center for Ecological Analysis and Synthesis
 * at the University of California Santa Barbara (UCSB).
 *
 *   Copyright 2011 Regents of the University of California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.ucsb.nceas.ezid.test;

import java.net.URL;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.fail;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

import edu.ucsb.nceas.ezid.EZIDException;
import edu.ucsb.nceas.ezid.EZIDService;
import edu.ucsb.nceas.ezid.profile.DataCiteProfile;
import edu.ucsb.nceas.ezid.profile.DublinCoreProfile;
import edu.ucsb.nceas.ezid.profile.InternalProfile;

/**
 * Integration tests for the EZID Service.
 * @author Matthew Jones, NCEAS, UC Santa Barbara
 */
public class EZIDServiceTest  {
    private static String USERNAME = "apitest";
    private static String PASSWORD = "apitest";
    private static final String DOISHOULDER = "doi:10.5072/FK2";
    private static final String ARKSHOULDER = "ark:/99999/fk4";
    private static EZIDService ezid = null;

    protected static Log log = LogFactory.getLog(EZIDServiceTest.class);


    @Before
    public void setUp() throws EZIDException {
        ezid = new EZIDService();
        ezid.login(USERNAME, PASSWORD);
    }

    @Test
    public void loginAndLogout() {
        EZIDService ezidLocal = null;
        try {
            ezidLocal = new EZIDService();
            ezidLocal.login(USERNAME, PASSWORD);
        } catch (EZIDException e) {
            fail("Login failed: " + e.getMessage());
        }

        try {
            ezidLocal.logout();
        } catch (EZIDException e) {
            fail("Logout failed: " + e.getMessage());
        }
    }

    @Test
    public void invalidLogin() {
        try {
            EZIDService ezidLocal = new EZIDService();
            ezidLocal.login(USERNAME, "wrong_password");
            fail("Login should have failed with incorrect password.");
        } catch (EZIDException e) {
            // Good -- login should have failed -- exception expected
        }
    }

    @Test
    public void mint() {
        String testId = null;
        try {
            HashMap<String, String> metadata = generateMetadata("ToBeMinted");
            metadata.put(InternalProfile.TARGET.toString(), "http://example.com/ezidExample/");
            metadata.put(DataCiteProfile.TITLE.toString(), "Title of a test identified resource");
            testId = ezid.mintIdentifier(DOISHOULDER, metadata);
            if (testId == null || testId.isEmpty()) {
                fail("Mint returned null or empty id: " + testId);
            }
        } catch (EZIDException e) {
            fail("Mint failed: " + e.getMessage());
        }
    }

    @Test
    public void create() {
        String timestamp = generateTimeString();
        try {
            String identifier = DOISHOULDER + "/" + "TEST" + "/" + timestamp;
            HashMap<String, String> metadata = generateMetadata(identifier);
            String newId = ezid.createIdentifier(identifier, metadata);
            if (newId == null || newId.isEmpty()) {
                fail("Create returned null or empty id: " + newId);
            }
        } catch (EZIDException e) {
            fail("Create failed: " + e.getMessage());
        }
    }

    @Test
    public void delete() {
        String timestamp = generateTimeString();
        try {
            HashMap<String, String> metadata = new HashMap<String, String>();
            metadata.put(InternalProfile.STATUS.toString(), "reserved");
            String newId = ezid.createIdentifier(ARKSHOULDER + "/" + "TEST" + "/" + timestamp, metadata);
            ezid.deleteIdentifier(newId);
        } catch (EZIDException e) {
            fail("Delete failed: " + e.getMessage());
        }
    }

    @Test
    public void setAndGetMetadata() {
        String testId = null;
        String TITLEKEY = DublinCoreProfile.TITLE.toString();
        String TITLEVAL = "A Dublin Core resource title";

        try {
            HashMap<String, String> metadata = generateMetadata("ToBeMinted");
            testId = ezid.mintIdentifier(DOISHOULDER, metadata);
            metadata.put(TITLEKEY, TITLEVAL);
            ezid.setMetadata(testId, metadata);
        } catch (EZIDException e) {
            fail("SetMetadata failed: " + e.getMessage());
        }

        try {
            HashMap<String, String> metadata = ezid.getMetadata(testId);
			for (String key : metadata.keySet()) {
			    log.debug(key + ": " + metadata.get(key));
			}
            String title = metadata.get(TITLEKEY);
			log.debug("SetAndGet Title: " + title);
            if (title == null || !title.equals(TITLEVAL)) {
                fail("GetMetadata failed: Title does not match.");
            }
        } catch (EZIDException e) {
            fail("GetMetadata failed: " + e.getMessage());
        }
    }

    @Test
    public void setAndGetDataCiteXML() {
        String testId = null;

        try {
            HashMap<String, String> metadata = generateDataCiteXML("ToBeMinted");
            testId = ezid.mintIdentifier(DOISHOULDER, metadata);
            log.debug("SetAndGet: " + testId);
        } catch (EZIDException e) {
            fail("SetMetadata failed: " + e.getMessage());
        }

        try {
            HashMap<String, String> metadata = ezid.getMetadata(testId);
			for (String key : metadata.keySet()) {
			    log.debug(key + ": " + metadata.get(key));
			}
            String xml = metadata.get("datacite");
			log.debug("SetAndGet Datacite: " + xml);
            if (xml == null || !xml.contains(testId.split("/")[1])) {
                fail("GetMetadata failed: metadata does not contain submitted identifier.");
            }
        } catch (EZIDException e) {
            fail("GetMetadata failed: " + e.getMessage());
        }
    }

    /** Generate a timestamp for use in IDs. */
    public static String generateTimeString()
    {
        StringBuffer guid = new StringBuffer();

        // Create a calendar to get the date formatted properly
        String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
        SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, ids[0]);
        pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        Calendar calendar = new GregorianCalendar(pdt);
        Date trialTime = new Date();
        calendar.setTime(trialTime);
        guid.append(calendar.get(Calendar.YEAR));
        guid.append(calendar.get(Calendar.DAY_OF_YEAR));
        guid.append(calendar.get(Calendar.HOUR_OF_DAY));
        guid.append(calendar.get(Calendar.MINUTE));
        guid.append(calendar.get(Calendar.SECOND));
        guid.append(calendar.get(Calendar.MILLISECOND));
        double random = Math.random();
        guid.append(random);

        return guid.toString();
    }

    /**
     * Generate DataCite compliant metadata for use in test data insertion.
     */
    public static HashMap<String, String> generateMetadata(String identifier) {
        HashMap<String, String> metadata = new HashMap<String, String>();
        String title = "Test entry from ezid service for identifier: " + identifier;
        metadata.put("datacite.title", title);
        String creator = "Keyser Söze";
        metadata.put("datacite.creator", creator);
        String publisher = "EZID Java Library";
        metadata.put("datacite.publisher", publisher);
        String year = new Integer(Calendar.getInstance().get(Calendar.YEAR)).toString();
        metadata.put("datacite.publicationyear", year);
        return metadata;
    }

    /**
     * Generate DataCite compliant XML metadata for use in test data insertion.
     */
    public HashMap<String, String> generateDataCiteXML(String identifier) {
        HashMap<String, String> metadata = new HashMap<String, String>();
        try {
            Path tempFile = null;
            URL url = getClass().getResource("/datacite-3.1.xml");
            if (url != null) {
                tempFile = Paths.get(url.toURI());
            } else {
                fail("generateDataCiteXML failed: " + "Could not open datacite.xml resource.");
            }
            String xml = new String(Files.readAllBytes(tempFile));
            metadata.put("datacite", xml);
        } catch (URISyntaxException e) {
            fail("generateDataCiteXML failed: " + e.getMessage());
        } catch (IOException e) {
            fail("generateDataCiteXML failed: " + e.getMessage());
        }
        return metadata;
    }
}
