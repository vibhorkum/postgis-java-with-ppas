/*
 * Test.java
 * 
 * PostGIS extension for PostgreSQL JDBC driver - example and test classes
 * 
 * (C) 2004 Paul Ramsey, pramsey@refractions.net
 * 
 * (C) 2005 Markus Schaber, markus.schaber@logix-tt.com
 *
 * (C) 2015 Phillip Ross, phillip.w.g.ross@gmail.com
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA or visit the web at
 * http://www.gnu.org.
 * 
 */

package org.postgis;


import com.edb.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.sql.*;


public class BoxesTest {

    private static final Logger logger = LoggerFactory.getLogger(BoxesTest.class);

    /** Our test candidates: */
    public static final String[] BOXEN3D = new String[]{
        "BOX3D(1 2 3,4 5 6)", // 3d variant
        "BOX3D(1 2,4 5)"// 2d variant
    };

    public static final String[] BOXEN2D = new String[]{"BOX(1 2,3 4)"};

    private boolean testWithDatabase = false;

    private Connection connection = null;


    @Test
    public void testBoxes() throws Exception {
        for (String aBOXEN3D : BOXEN3D) {
            PGbox3d candidate = new PGbox3d(aBOXEN3D);
            if (testWithDatabase) {
                test(aBOXEN3D, candidate, false);
            }
        }
        for (String aBOXEN2D : BOXEN2D) {
            PGbox2d candidate = new PGbox2d(aBOXEN2D);
            if (testWithDatabase) {
                test(aBOXEN2D, candidate, true);
            }
        }
    }


    public void test(String orig, PGobject candidate, boolean newPostgisOnly) throws Exception {
        logger.debug("Original: {}", orig);
        String redone = candidate.toString();
        logger.debug("Parsed: {}", redone);
        Assert.assertEquals(orig, redone, "Recreated Text Rep not equal");

        // Simulate the way postgresql-jdbc uses to create PGobjects
        PGobject recreated = candidate.getClass().newInstance();
        recreated.setValue(redone);

        String reparsed = recreated.toString();
        logger.debug("Re-Parsed: " + reparsed);
        Assert.assertEquals(recreated, candidate, "Recreated boxen are not equal");
        Assert.assertEquals(reparsed, orig, "2nd generation text reps are not equal");

        logger.debug("testing on connection: {}", connection.getCatalog());
        Statement statement = connection.createStatement();
        if (newPostgisOnly && AutoRegistrationTest.getPostgisMajor(statement) < 1) {
            logger.debug("PostGIS version is too old, not testing box2d");
        } else {
            PGobject sqlGeom = viaSQL(candidate, statement);
            logger.debug("SQLin    : " + sqlGeom.toString());
            Assert.assertEquals(candidate, sqlGeom, "Geometries after SQL are not equal");
            PGobject sqlreGeom = viaSQL(recreated, statement);
            logger.debug("SQLout  :  " + sqlreGeom.toString());
            Assert.assertEquals(candidate, sqlreGeom, "reparsed Geometries after SQL are not equal");
        }
        statement.close();
    }


    /** Pass a geometry representation through the SQL server */
    private static PGobject viaSQL(PGobject obj, Statement stat) throws SQLException {
        ResultSet rs = stat.executeQuery("SELECT '" + obj.toString() + "'::" + obj.getType());
        rs.next();
        return (PGobject) rs.getObject(1);
    }


    @BeforeClass
    @Parameters({"testWithDatabaseSystemProperty", "jdbcUrlSystemProperty", "jdbcUsernameSystemProperty", "jdbcPasswordSystemProperty"})
    public void initJdbcConnection(String testWithDatabaseSystemProperty,
                                   String jdbcUrlSystemProperty,
                                   String jdbcUsernameSystemProperty,
                                   String jdbcPasswordSystemProperty) throws Exception {
        logger.debug("testWithDatabaseSystemProperty: {}", testWithDatabaseSystemProperty);
        logger.debug("jdbcUrlSystemProperty: {}", jdbcUrlSystemProperty);
        logger.debug("jdbcUsernameSystemProperty: {}", jdbcUsernameSystemProperty);
        logger.debug("jdbcPasswordSystemProperty: {}", jdbcPasswordSystemProperty);

        testWithDatabase = Boolean.parseBoolean(System.getProperty(testWithDatabaseSystemProperty));
        String jdbcUrl = System.getProperty(jdbcUrlSystemProperty);
        String jdbcUsername = System.getProperty(jdbcUsernameSystemProperty);
        String jdbcPassword = System.getProperty(jdbcPasswordSystemProperty);

        logger.debug("testWithDatabase: {}", testWithDatabase);
        logger.debug("jdbcUrl: {}", jdbcUrl);
        logger.debug("jdbcUsername: {}", jdbcUsername);
        logger.debug("jdbcPassword: {}", jdbcPassword);

        if (testWithDatabase) {
            Class.forName("org.postgis.DriverWrapper");
            connection = DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
        } else {
            logger.info("testWithDatabase value was false.  Database tests will be skipped.");
        }
    }


}