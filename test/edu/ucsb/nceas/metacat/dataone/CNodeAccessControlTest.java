/**
 *  '$RCSfile$'
 *  Copyright: 2010 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *  Purpose: To test the Access Controls in metacat by JUnit
 *
 *   '$Author$'
 *     '$Date$'
 * '$Revision$'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.ucsb.nceas.metacat.dataone;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.dataone.client.D1Node;
import org.dataone.client.NodeLocator;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.util.AuthUtils;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.ObjectFormatList;
import org.dataone.service.types.v2.OptionList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;

import edu.ucsb.nceas.utilities.IOUtil;
import junit.framework.Test;
import junit.framework.TestSuite;

public class CNodeAccessControlTest extends D1NodeServiceTest {
   
    public static final String TEXT = "data";
    public static final String ALGORITHM = "MD5";
    private static final Session nullSession = null;
    private static final Session publicSession = MNodeAccessControlTest.getPublicUser();
    private static Session KNBadmin = null;
    private static Session PISCOManager = null;
    
    /**
     * Constructor
     * @param name
     */
    public CNodeAccessControlTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new CNodeAccessControlTest("initialize"));
        suite.addTest(new CNodeAccessControlTest("testMethodsWithoutSession"));
        suite.addTest(new CNodeAccessControlTest("testMethodsWithSession"));
        return suite;
    }
    
    /**
     * Establish a testing framework by initializing appropriate objects
     */
    public void setUp() throws Exception {
        //Use the default CN
        D1Client.setNodeLocator(null);
    }
    
    /**
     * Run an initial test that always passes to check that the test harness is
     * working.
     */
    public void initialize() {
        printTestHeader("initialize");
        try {
            /*Session session =getCNSession();
            System.out.println("==================the cn session is "+session.getSubject().getValue());
            Session userSession = getOneKnbDataAdminsMemberSession();
            Set<Subject> subjects = AuthUtils.authorizedClientSubjects(userSession);
            for (Subject subject: subjects) {
                System.out.println("the knb data admin user has this subject "+subject.getValue());
            }
             userSession = getOnePISCODataManagersMemberSession();
             subjects = AuthUtils.authorizedClientSubjects(userSession);
            for (Subject subject: subjects) {
                System.out.println("the pisco data manager user has this subject "+subject.getValue());
            }*/
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(1 == 1);
    }
    
    /**
     * Test those methods which don't need sessions.
     * @throws Exception
     */
    public void testMethodsWithoutSession() throws Exception {
        //testGetCapacity();
        testListObjects();
        testListViews();
        testListFormats();
    }
    
    /**
     * Test those methods which need sessions.
     * @throws Exception
     */
    public void testMethodsWithSession() throws Exception {
        KNBadmin = MNodeAccessControlTest.getOneKnbDataAdminsMemberSession();
        PISCOManager = MNodeAccessControlTest.getOnePISCODataManagersMemberSession();
        //rights holder on the system metadata is a user.
        Subject rightsHolder = getAnotherSession().getSubject();
        testMethodsWithGivenHightsHolder(rightsHolder, rightsHolder);
        //rights holder on the system metadata is a group
        Subject rightsHolder2 = MNodeAccessControlTest.getOneEssDiveUserMemberSubject();
        Subject rightsHolderGroupOnSys = MNodeAccessControlTest.getEssDiveUserGroupSubject();
        testMethodsWithGivenHightsHolder(rightsHolder2, rightsHolderGroupOnSys);
    }
    
    /**
     * Real methods to test the access control - 
     * @param rightsHolder
     * @throws Exception
     */
    private void testMethodsWithGivenHightsHolder(Subject rightsHolder, Subject rightsHolderOnSys) throws Exception {
        Session rightsHolderSession = new Session();
        rightsHolderSession.setSubject(rightsHolder);
        Session submitter = getTestSession();
       
        //1. Test generating identifiers (it only checks if session is null)
        String scheme = "unknow";
        String fragment = "test-access"+System.currentTimeMillis();
        testGenerateIdentifier(nullSession, scheme, fragment, false);
        Identifier id1 = testGenerateIdentifier(publicSession, scheme, fragment, true);
        
        //2 Test the create method (it only checks if session is null)
        InputStream object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        SystemMetadata sysmeta = createSystemMetadata(id1, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        sysmeta.setAccessPolicy(new AccessPolicy());//no access policy
        testCreate(nullSession, id1, sysmeta, object, false);
        testCreate(submitter, id1, sysmeta, object, true);
       
        //3 The object id1 doesn't have any access policy, it can be read by rights holder, cn and mn.
        testGetAPI(getCNSession(), id1, sysmeta.getChecksum(), true);//cn can read it
        testGetAPI(getMNSession(), id1, sysmeta.getChecksum(), true);//mn can read it
        testGetAPI(rightsHolderSession, id1, sysmeta.getChecksum(), true);//rightsholder can read it
        testGetAPI(submitter, id1,sysmeta.getChecksum(),false); //submitter can't read it
        testGetAPI(publicSession, id1,sysmeta.getChecksum(),false); //public can't read it
        testGetAPI(KNBadmin, id1,sysmeta.getChecksum(),false); //knb can't read it
        testGetAPI(PISCOManager, id1,sysmeta.getChecksum(),false); //pisco can't read it
        testGetAPI(nullSession, id1,sysmeta.getChecksum(),false); //nullSession can't read it
        testIsAuthorized(getCNSession(), id1, Permission.CHANGE_PERMISSION, true);//cn can read it
        testIsAuthorized(getMNSession(), id1, Permission.CHANGE_PERMISSION, true);//mn can read it
        testIsAuthorized(rightsHolderSession, id1, Permission.CHANGE_PERMISSION, true);//rightsholder can read it
        testIsAuthorized(submitter, id1,Permission.READ,false); //submitter can't read it
        testIsAuthorized(publicSession, id1, Permission.READ,false); //public can't read it
        testIsAuthorized(KNBadmin, id1,Permission.READ,false); //knb can't read it
        testIsAuthorized(PISCOManager, id1,Permission.READ,false); //pisco can't read it
        testIsAuthorized(nullSession, id1,Permission.READ,false); //nullSession can't read it
       
        //4 Test update the system metadata with new access rule (knb group can read it)
        AccessPolicy policy = new AccessPolicy();
        AccessRule rule = new AccessRule();
        rule.addPermission(Permission.READ);
        rule.addSubject(MNodeAccessControlTest.getKnbDataAdminsGroupSubject());
        policy.addAllow(rule);
        sysmeta.setAccessPolicy(policy);
        testUpdateSystemmetadata(nullSession, id1, sysmeta, false);
        testUpdateSystemmetadata(publicSession, id1, sysmeta, false);
        testUpdateSystemmetadata(submitter, id1, sysmeta, false);
        testUpdateSystemmetadata(PISCOManager, id1, sysmeta, false);
        testUpdateSystemmetadata(KNBadmin, id1, sysmeta, false);
        testUpdateSystemmetadata(rightsHolderSession, id1, sysmeta, true);
        testUpdateSystemmetadata(getCNSession(), id1, sysmeta, true);
        testUpdateSystemmetadata(getMNSession(), id1, sysmeta, true);
        
        testIsAuthorized(nullSession, id1, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(publicSession, id1, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(submitter, id1, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(PISCOManager, id1, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(KNBadmin, id1, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(rightsHolderSession, id1, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(getCNSession(), id1, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(getMNSession(), id1, Permission.CHANGE_PERMISSION, true);
        
        //read it with the access rule - knb group add read it
        testGetAPI(getCNSession(), id1, sysmeta.getChecksum(), true);//cn can read it
        testGetAPI(getMNSession(), id1, sysmeta.getChecksum(), true);//mn can read it
        testGetAPI(rightsHolderSession, id1, sysmeta.getChecksum(), true);//rightsholder can read it
        testGetAPI(submitter, id1,sysmeta.getChecksum(),false); //submitter can't read it
        testGetAPI(publicSession, id1,sysmeta.getChecksum(),false); //public can't read it
        testGetAPI(KNBadmin, id1,sysmeta.getChecksum(),true); //knb can read it
        testGetAPI(PISCOManager, id1,sysmeta.getChecksum(),false); //pisco can't read it
        testGetAPI(nullSession, id1,sysmeta.getChecksum(),false); //nullSession can't read it
        testIsAuthorized(submitter, id1,Permission.READ,false); 
        testIsAuthorized(publicSession, id1, Permission.READ,false); 
        testIsAuthorized(KNBadmin, id1,Permission.READ,true); 
        testIsAuthorized(PISCOManager, id1,Permission.READ,false); 
        testIsAuthorized(nullSession, id1,Permission.READ,false); 
        
        //5.Test get api when knb group has the write permission and submitter has read permission
        //set up
        policy = new AccessPolicy();
        rule = new AccessRule();
        rule.addPermission(Permission.WRITE);
        rule.addSubject(MNodeAccessControlTest.getKnbDataAdminsGroupSubject());
        policy.addAllow(rule);
        AccessRule rule2= new AccessRule();
        rule2.addPermission(Permission.READ);
        rule2.addSubject(submitter.getSubject());
        policy.addAllow(rule2);
        sysmeta.setAccessPolicy(policy);
        testUpdateSystemmetadata(rightsHolderSession, id1, sysmeta, true);
        //read
        testGetAPI(getCNSession(), id1, sysmeta.getChecksum(), true);//cn can read it
        testGetAPI(getMNSession(), id1, sysmeta.getChecksum(), true);//mn can read it
        testGetAPI(rightsHolderSession, id1, sysmeta.getChecksum(), true);//rightsholder can read it
        testGetAPI(submitter, id1,sysmeta.getChecksum(),true); //submitter can read it
        testGetAPI(publicSession, id1,sysmeta.getChecksum(),false); //public can't read it
        testGetAPI(KNBadmin, id1,sysmeta.getChecksum(),true); //knb can read it
        testGetAPI(PISCOManager, id1,sysmeta.getChecksum(),false); //pisco can't read it
        testGetAPI(nullSession, id1,sysmeta.getChecksum(),false); //nullSession can't read it
        testIsAuthorized(submitter, id1,Permission.READ,true); 
        testIsAuthorized(publicSession, id1, Permission.READ,false); 
        testIsAuthorized(KNBadmin, id1,Permission.WRITE,true); 
        testIsAuthorized(PISCOManager, id1,Permission.READ,false); 
        testIsAuthorized(nullSession, id1,Permission.READ,false); 
        
        //6. Test get api when the public and submitter has the read permission and the knb-admin group has write permission
        //set up
        AccessRule rule3= new AccessRule();
        rule3.addPermission(Permission.READ);
        rule3.addSubject(publicSession.getSubject());
        policy.addAllow(rule3);
        sysmeta.setAccessPolicy(policy);
        testUpdateSystemmetadata(KNBadmin, id1, sysmeta, false);
        testUpdateSystemmetadata(rightsHolderSession, id1, sysmeta, true);
        //read
        testGetAPI(getCNSession(), id1, sysmeta.getChecksum(), true);//cn can read it
        testGetAPI(getMNSession(), id1, sysmeta.getChecksum(), true);//mn can read it
        testGetAPI(rightsHolderSession, id1, sysmeta.getChecksum(), true);//rightsholder can read it
        testGetAPI(submitter, id1,sysmeta.getChecksum(),true); //submitter can read it
        testGetAPI(publicSession, id1,sysmeta.getChecksum(),true); //public can read it
        testGetAPI(KNBadmin, id1,sysmeta.getChecksum(),true); //knb can read it
        testGetAPI(PISCOManager, id1,sysmeta.getChecksum(),true); //pisco can read it
        testGetAPI(nullSession, id1,sysmeta.getChecksum(),true); //nullSession can read it
        testIsAuthorized(submitter, id1,Permission.READ,true); 
        testIsAuthorized(publicSession, id1, Permission.READ,true); 
        testIsAuthorized(KNBadmin, id1,Permission.READ,true); 
        testIsAuthorized(PISCOManager, id1,Permission.READ,true); 
        testIsAuthorized(nullSession, id1,Permission.READ,true); 
        
        //7. Test the updateSystemMetadata (needs change permission) (the public and submitter has the read permission and the knb-admin group has write permission)
        //add a new policy that pisco group and submitter has the change permission, and third user has the read permission
        AccessRule rule4= new AccessRule();
        rule4.addPermission(Permission.CHANGE_PERMISSION);
        rule4.addSubject(MNodeAccessControlTest.getPISCODataManagersGroupSubject());
        policy.addAllow(rule4);
        AccessRule rule5= new AccessRule();
        rule5.addPermission(Permission.CHANGE_PERMISSION);
        rule5.addSubject(submitter.getSubject());
        policy.addAllow(rule5);
        AccessRule rule6= new AccessRule();
        rule6.addPermission(Permission.READ);
        rule6.addSubject(MNodeAccessControlTest.getThirdUser().getSubject());
        policy.addAllow(rule6);
        sysmeta.setAccessPolicy(policy);
        testUpdateSystemmetadata(nullSession, id1, sysmeta, false);
        testUpdateSystemmetadata(publicSession, id1, sysmeta, false);
        testUpdateSystemmetadata(submitter, id1, sysmeta, false);
        testUpdateSystemmetadata(PISCOManager, id1, sysmeta, false);
        testUpdateSystemmetadata(KNBadmin, id1, sysmeta, false);
        testUpdateSystemmetadata(rightsHolderSession, id1, sysmeta, true);
        testUpdateSystemmetadata(getCNSession(), id1, sysmeta, true);
        testUpdateSystemmetadata(getMNSession(), id1, sysmeta, true);
        //now pisco member session and submitter can update systememetadata since they have change permssion
        testUpdateSystemmetadata(nullSession, id1, sysmeta, false);
        testUpdateSystemmetadata(publicSession, id1, sysmeta, false);
        testUpdateSystemmetadata(submitter, id1, sysmeta, true);
        testUpdateSystemmetadata(PISCOManager, id1, sysmeta, true);
        testUpdateSystemmetadata(KNBadmin, id1, sysmeta, false);
        testUpdateSystemmetadata(rightsHolderSession, id1, sysmeta, true);
        testUpdateSystemmetadata(getCNSession(), id1, sysmeta, true);
        testUpdateSystemmetadata(getMNSession(), id1, sysmeta, true);
        testIsAuthorized(submitter, id1,Permission.CHANGE_PERMISSION,true); 
        testIsAuthorized(MNodeAccessControlTest.getThirdUser(), id1,Permission.READ,true); 
        testIsAuthorized(publicSession, id1, Permission.READ,true); 
        testIsAuthorized(KNBadmin, id1,Permission.WRITE,true); 
        testIsAuthorized(PISCOManager, id1,Permission.CHANGE_PERMISSION,true); 
        testIsAuthorized(nullSession, id1,Permission.READ,true); 
        
        //8. Test update (needs the write permission). Now the access policy for id1 is: 
        //the public and the third user has the read permission and the knb-admin group has write permission, and submitter and pisco group has the change permission.
        testIsAuthorized(nullSession, id1,Permission.WRITE,false);
        testIsAuthorized(publicSession, id1, Permission.WRITE,false); 
        testIsAuthorized(submitter, id1,Permission.WRITE,true); 
        testIsAuthorized(MNodeAccessControlTest.getThirdUser(), id1,Permission.WRITE,false); 
        testIsAuthorized(KNBadmin, id1,Permission.WRITE,true); 
        testIsAuthorized(PISCOManager, id1,Permission.WRITE,true);
        testIsAuthorized(rightsHolderSession, id1,Permission.WRITE,true);
        testIsAuthorized(getMNSession(), id1,Permission.WRITE,true); 
        testIsAuthorized(getCNSession(), id1,Permission.WRITE,true); 
        
       
        Thread.sleep(100);
        Identifier id7 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        testCreate(getCNSession(), id7, sysmeta, object, false);
        
        
        //9 test Publish (needs write permission)
        //Now the access policy for id1- id7 is: the public and the third user has the read permission and the knb-admin group has write permission, and submitter and pisco group has the change permission.
       
        
        //10 test syncFailed (needs mn+cn)
               
        //11 test system metadata change (needs cn)
       
        //12 test archive (needs change permission)
        Thread.sleep(100);
        Identifier id12 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = createSystemMetadata(id12, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        sysmeta.setAccessPolicy(new AccessPolicy());//no access policy
        testCreate(submitter, id12, sysmeta, object, true);
        testArchive(nullSession, id12, false);
        testArchive(publicSession, id12, false);
        testArchive(MNodeAccessControlTest.getThirdUser(), id12, false);
        testArchive(submitter, id12, false);
        testArchive(KNBadmin, id12, false);
        testArchive(PISCOManager, id12, false);
        testArchive(rightsHolderSession, id12, true);
        Thread.sleep(100);
        Identifier id13 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = createSystemMetadata(id13, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        AccessPolicy policy2 = new AccessPolicy();
        AccessRule rule7 = new AccessRule();
        rule7.addPermission(Permission.CHANGE_PERMISSION);
        rule7.addSubject(MNodeAccessControlTest.getPISCODataManagersGroupSubject());
        policy2.addAllow(rule7);
        rule6 = new AccessRule();
        rule6.addPermission(Permission.WRITE);
        rule6.addSubject(MNodeAccessControlTest.getKnbDataAdminsGroupSubject());
        policy2.addAllow(rule6);
        sysmeta.setAccessPolicy(policy2);
        testCreate(submitter, id13, sysmeta, object, true);
        testIsAuthorized(nullSession, id13, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(publicSession, id13, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(submitter, id13, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(PISCOManager, id13, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(KNBadmin, id13, Permission.CHANGE_PERMISSION, false);
        testIsAuthorized(rightsHolderSession, id13, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(getCNSession(), id13, Permission.CHANGE_PERMISSION, true);
        testIsAuthorized(getMNSession(), id13, Permission.CHANGE_PERMISSION, true);
        testArchive(nullSession, id13, false);
        testArchive(publicSession, id13, false);
        testArchive(MNodeAccessControlTest.getThirdUser(), id13, false);
        testArchive(submitter, id13, false);
        testArchive(KNBadmin, id13, false);
        testArchive(PISCOManager, id13, true);
        Thread.sleep(100);
        Identifier id14 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = createSystemMetadata(id14, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        policy2 = new AccessPolicy();
        rule7 = new AccessRule();
        rule7.addPermission(Permission.WRITE);
        rule7.addSubject(MNodeAccessControlTest.getPISCODataManagersGroupSubject());
        policy2.addAllow(rule7);
        rule6 = new AccessRule();
        rule6.addPermission(Permission.WRITE);
        rule6.addSubject(MNodeAccessControlTest.getKnbDataAdminsGroupSubject());
        policy2.addAllow(rule6);
        sysmeta.setAccessPolicy(policy2);
        testCreate(submitter, id14, sysmeta, object, true);
        testArchive(nullSession, id14, false);
        testArchive(publicSession, id14, false);
        testArchive(MNodeAccessControlTest.getThirdUser(), id14, false);
        testArchive(submitter, id14, false);
        testArchive(KNBadmin, id14, false);
        testArchive(PISCOManager, id14, false);
        testArchive(getMNSession(), id14, true);
        Thread.sleep(100);
        Identifier id15 = testGenerateIdentifier(submitter, scheme, "test-access"+System.currentTimeMillis(), true);
        object = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        sysmeta = createSystemMetadata(id15, submitter.getSubject(), object);
        sysmeta.setRightsHolder(rightsHolderOnSys);
        policy2 = new AccessPolicy();
        rule7 = new AccessRule();
        rule7.addPermission(Permission.WRITE);
        rule7.addSubject(MNodeAccessControlTest.getPISCODataManagersGroupSubject());
        policy2.addAllow(rule7);
        rule6 = new AccessRule();
        rule6.addPermission(Permission.WRITE);
        rule6.addSubject(MNodeAccessControlTest.getKnbDataAdminsGroupSubject());
        policy2.addAllow(rule6);
        sysmeta.setAccessPolicy(policy2);
        testCreate(submitter, id15, sysmeta, object, true);
        testArchive(nullSession, id15, false);
        testArchive(publicSession, id15, false);
        testArchive(MNodeAccessControlTest.getThirdUser(), id15, false);
        testArchive(submitter, id15, false);
        testArchive(KNBadmin, id15, false);
        testArchive(PISCOManager, id15, false);
        testArchive(getCNSession(), id15, true);
        
        //13 test getLogRecord (needs MN+CN)
        testGetLogRecords(nullSession, false);
        testGetLogRecords(publicSession, false);
        testGetLogRecords(MNodeAccessControlTest.getThirdUser(), false);
        testGetLogRecords(submitter, false);
        testGetLogRecords(KNBadmin, false);
        testGetLogRecords(PISCOManager, false);
        testGetLogRecords(rightsHolderSession, false);
        testGetLogRecords(getMNSession(), true);
        testGetLogRecords(getCNSession(), true);
        
        //14 test delete (needs mn+cn)
        testDelete(nullSession, id15, false);
        testDelete(publicSession, id15, false);
        testDelete(MNodeAccessControlTest.getThirdUser(), id15, false);
        testDelete(submitter, id15, false);
        testDelete(KNBadmin, id15, false);
        testDelete(PISCOManager, id15, false);
        testDelete(rightsHolderSession, id15, false);
        testDelete(getCNSession(), id15, true);
        testDelete(getMNSession(), id1, true);
        //testDelete(getMNSession(), id2, true);
        
    }
    
   
    /**
     * A generic test method to determine if the given session can call the isAuthorized method to result the expectation.  
     * @param session the session will call the isAuthorized method
     * @param pid the identifier of the object will be applied
     * @param permission the permission will be checked
     * @param expectedResult the expected for authorization. True will be successful.
     * @throws Exception
     */
    private void testIsAuthorized(Session session, Identifier pid, Permission permission, boolean expectedResult) throws Exception {
        if(expectedResult) {
            boolean result = CNodeService.getInstance(request).isAuthorized(session, pid, permission);
            assertTrue(result == expectedResult);
        } else {
            try {
                boolean result = CNodeService.getInstance(request).isAuthorized(session, pid, permission);
                fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
            } catch (NotAuthorized e) {
                
            }
        }
        
    }
    
    /**
     *A generic test method to determine if the given session can call the isNodeAuthorized method to result the expectation.  
     * @param session
     * @param pid
     * @param targetNodeSubject
     * @param expectedResult
     * @throws Exception
     */
    private void testIsNodeAuthorized(Session session, Identifier pid, Subject targetNodeSubject, boolean expectedResult) throws Exception {
        if(expectedResult) {
            boolean result = CNodeService.getInstance(request).isNodeAuthorized(session, targetNodeSubject, pid);
            assertTrue(result == expectedResult);
        } else {
            try {
                boolean result = CNodeService.getInstance(request).isNodeAuthorized(session, targetNodeSubject, pid);
                fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
            } catch (NotAuthorized e) {
                
            }
        }
        
    }
    
    /**
     * A generic test method to determine if the given session can call the create method to result the expectation.  
     * @param session the session will call the create method
     * @param pid the identifier will be used at the create method
     * @param sysmeta the system metadata object will be used at the create method
     * @param expectedResult the expected result for authorization. True will be successful.
     */
    private Identifier testCreate (Session session, Identifier pid, SystemMetadata sysmeta, InputStream object, boolean expectedResult) throws Exception{
        if(expectedResult) {
            Identifier id = CNodeService.getInstance(request).create(session, pid, object, sysmeta);
            assertTrue(id.equals(pid));
        } else {
            try {
                pid = CNodeService.getInstance(request).create(session, pid, object, sysmeta);
                fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
            } catch (InvalidToken e) {
                
            }
        }
        return pid;
    }
    
    /**
     * A generic test method to determine if the given session can call the delete method to result the expectation.  
     * @param session the session will call the delete method
     * @param id the identifier will be used to call the delete method
     * @param expectedResult the expected result for authorization. True will be successful.
     */
     private void testDelete(Session session, Identifier pid, boolean expectedResult) throws Exception{
         if(expectedResult) {
             Identifier id = CNodeService.getInstance(request).delete(session, pid);
             assertTrue(id.equals(pid));
         } else {
             try {
                 pid = CNodeService.getInstance(request).delete(session, pid);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
    
     /**
      * A generic test method to determine if the given session can call the registerSystemMetadata method to result the expectation. 
      * @param session
      * @param pid
      * @param sysmeta
      * @param expectedResult
      * @throws Exception
      */
     private void testRegisterSystemmetadata(Session session, Identifier pid, SystemMetadata sysmeta, boolean expectedResult) throws Exception {
         if(expectedResult) {
             Identifier id = CNodeService.getInstance(request).registerSystemMetadata(session, pid, sysmeta);
             assertTrue(id.getValue().equals(pid.getValue()));
         } else {
             try {
                 Identifier id = CNodeService.getInstance(request).registerSystemMetadata(session, pid, sysmeta);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the updateSystemMetadata method to result the expectation. 
      * @param session
      * @param pid
      * @param newSysmeta
      * @param expectedResult
      * @throws Exception
      */
     private void testUpdateSystemmetadata(Session session, Identifier pid, SystemMetadata newSysmeta, boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean result = CNodeService.getInstance(request).updateSystemMetadata(session, pid, newSysmeta);
             assertTrue(result == expectedResult);
         } else {
             try {
                 boolean result = CNodeService.getInstance(request).updateSystemMetadata(session, pid, newSysmeta);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      *  A generic test method to determine if the given session can call the archive method to result the expectation.
      * @param session
      * @param pid
      * @param expectedResult
      * @throws Exception
      */
     private void testArchive(Session session, Identifier pid ,boolean expectedResult) throws Exception {
         if(expectedResult) {
             Identifier id = CNodeService.getInstance(request).archive(session, pid);
             assertTrue(id.equals(pid));
         } else {
             try {
                 Identifier id = CNodeService.getInstance(request).archive(session, pid);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     
     /**
      * A generic test method to determine if the given session can call the deleteReplicationMetadata method  to result the expectation.
      * @param session
      * @param pid
      * @param nodeId
      * @param serialVersion
      * @param expectedResult
      * @throws Exception
      */
     private void testDeleteReplicationMetadata(Session session, Identifier pid, NodeReference nodeId, long serialVersion,  boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean success = CNodeService.getInstance(request).deleteReplicationMetadata(session, pid, nodeId, serialVersion);
             assertTrue(success);
         } else {
             try {
                 boolean success = CNodeService.getInstance(request).deleteReplicationMetadata(session, pid, nodeId, serialVersion);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the setReplicationStatus method  to result the expectation. 
      * @param session
      * @param pid
      * @param targetNode
      * @param expectedResult
      * @throws Exception
      */
     private void testSetReplicationStatus(Session session, Identifier pid, NodeReference targetNode, boolean expectedResult) throws Exception {
         ReplicationStatus status = ReplicationStatus.COMPLETED;
         BaseException failure = null;
         if(expectedResult) {
             boolean success = CNodeService.getInstance(request).setReplicationStatus(session, pid, targetNode, status, failure);
             assertTrue(success);
         } else {
             try {
                 boolean success = CNodeService.getInstance(request).setReplicationStatus(session, pid, targetNode, status, failure);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the testUpdateReplicaMetadata method  to result the expectation. 
      * @param session
      * @param pid
      * @param replica
      * @param serialVersion
      * @param expectedResult
      * @throws Exception
      */
     private void testUpdateReplicaMetadata(Session session, Identifier pid, Replica replica, Long serialVersion, boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean success = CNodeService.getInstance(request).updateReplicationMetadata(session, pid, replica, serialVersion);
             assertTrue(success);
         } else {
             try {
                 boolean success = CNodeService.getInstance(request).updateReplicationMetadata(session, pid, replica, serialVersion);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the setObsoletedBy method  to result the expectation.  
      * @param session
      * @param pid
      * @param obsoletedByPid
      * @param serialVersion
      * @param expectedResult
      * @throws Exception
      */
     private void testSetObsoletedBy(Session session, Identifier pid, Identifier obsoletedByPid, Long serialVersion,  boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean success = CNodeService.getInstance(request).setObsoletedBy(session, pid, obsoletedByPid, serialVersion);
             assertTrue(success);
         } else {
             try {
                 boolean success = CNodeService.getInstance(request).setObsoletedBy(session, pid, obsoletedByPid, serialVersion);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the setRightsHolder method  to result the expectation.  
      * @param session
      * @param pid
      * @param userId
      * @param serialVersion
      * @param expectedResult
      * @throws Exception
      */
     private void testSetRightsHolder(Session session, Identifier pid, Subject userId, Long serialVersion,  boolean expectedResult) throws Exception {
         if(expectedResult) {
             Identifier id = CNodeService.getInstance(request).setRightsHolder(session, pid, userId, serialVersion);
             assertTrue(id.getValue().equals(pid.getValue()));
         } else {
             try {
                 Identifier id = CNodeService.getInstance(request).setRightsHolder(session, pid, userId, serialVersion);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      *A generic test method to determine if the given session can call the setReplicationPolicy method  to result the expectation.
      * @param session
      * @param pid
      * @param policy
      * @param serialVersion
      * @param expectedResult
      * @throws Exception
      */
     private void testSetReplicationPolicy(Session session, Identifier pid, ReplicationPolicy policy, long serialVersion,  boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean success = CNodeService.getInstance(request).setReplicationPolicy(session, pid, policy, serialVersion);
             assertTrue(success);
         } else {
             try {
                 boolean success = CNodeService.getInstance(request).setReplicationPolicy(session, pid, policy, serialVersion);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the testSetAccessPolicy method  to result the expectation.  
      * @param session
      * @param pid
      * @param accessPolicy
      * @param serialVersion
      * @param expectedResult
      * @throws Exception
      */
     private void testSetAccessPolicy(Session session, Identifier pid, AccessPolicy accessPolicy, Long serialVersion,  boolean expectedResult) throws Exception {
         if(expectedResult) {
             boolean success = CNodeService.getInstance(request).setAccessPolicy(session, pid, accessPolicy, serialVersion);
             assertTrue(success);
         } else {
             try {
                 boolean success = CNodeService.getInstance(request).setAccessPolicy(session, pid, accessPolicy, serialVersion);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the setObsoletedBy method  to result the expectation.  
      * @param session
      * @param formatId
      * @param format
      * @param expectedResult
      * @throws Exception
      */
     private void testAddFormat(Session session, ObjectFormatIdentifier formatId,  ObjectFormat format,  boolean expectedResult) throws Exception {
         if(expectedResult) {
             ObjectFormatIdentifier formatIdentifier = CNodeService.getInstance(request).addFormat(session, formatId, format);
             assertTrue(formatIdentifier.getValue().equals(formatId.getValue()));
         } else {
             try {
                 ObjectFormatIdentifier formatIdentifier = CNodeService.getInstance(request).addFormat(session, formatId, format);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     
     
 
     /**
      * A generic test method to determine if the given session can call the getLogRecords method to result the expectation. 
      * @param session
      * @param expectedResult
      * @throws Exception
      */
     private void testGetLogRecords(Session session,boolean expectedResult) throws Exception {
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
         Date fromDate = sdf.parse("1971-01-01");
         Date toDate = new Date();
         String event = null;
         String pidFilter = null;
         int start = 0;
         int count = 1;
         if(expectedResult) {
             Log log = CNodeService.getInstance(request).getLogRecords(session, fromDate, toDate, event, pidFilter, start, count);
             assertTrue(log.getTotal() > 0);
         } else {
             try {
                 CNodeService.getInstance(request).getLogRecords(session, fromDate, toDate, event, pidFilter, start, count);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the getLogRecords method to result the expectation. 
      * @param session
      * @param expectedResult
      * @throws Exception
      */
     private Identifier testGenerateIdentifier(Session session,String scheme, String fragment, boolean expectedResult) throws Exception {
         Identifier id  = null;
         if(expectedResult) {
             id= CNodeService.getInstance(request).generateIdentifier(session, scheme, fragment);
             assertTrue(id.getValue() != null && !id.getValue().trim().equals(""));
         } else {
             try {
                 CNodeService.getInstance(request).generateIdentifier(session, scheme, fragment);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (InvalidToken e) {
                 
             }
         }
         return id;
     }
     
     /**
      * Test the get api methods (describe, getSystemmetadata, get, view, getPackage, getChecksum)
      * @param session
      * @param id
      * @param expectedSum
      * @param expectedResult
      * @throws Exception
      */
     private void testGetAPI(Session session, Identifier id, Checksum expectedSum, boolean expectedResult) throws Exception {
         testDescribe(session, id, expectedResult);
         testGetSystemmetadata(session, id, expectedResult);
         testGet(session, id, expectedResult);
         testView(session, id, "metacatui", expectedResult);
         testGetChecksum(session, id, expectedSum, expectedResult);
     }
     
     /**
      * A generic test method to determine if the given session can call the describe method to result the expection.  
      * @param session the session will call the describe method
      * @param id the identifier will be used to call the describe method
      * @param expectedResult the expected result for authorization. True will be successful.
      */
     private void testDescribe(Session session, Identifier id, boolean expectedResult) throws Exception {
         if(expectedResult) {
             DescribeResponse reponse =CNodeService.getInstance(request).describe(session,id);
             ObjectFormatIdentifier format = reponse.getDataONE_ObjectFormatIdentifier();
             assertTrue(format.getValue().equals("application/octet-stream"));
         } else {
             try {
                 DescribeResponse reponse =CNodeService.getInstance(request).describe(session,id);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the getSystemMetadata method to result the expectation.  
      * @param session the session will call the getSystemMetadata method
      * @param id the identifier will be used to call the getSystemMetadata method
      * @param expectedResult the expected result for authorization. True will be successful.
      */
     private void testGetSystemmetadata(Session session, Identifier id, boolean expectedResult) throws Exception {
         if(expectedResult) {
             SystemMetadata sysmeta =CNodeService.getInstance(request).getSystemMetadata(session,id);
             assertTrue(sysmeta.getIdentifier().equals(id));
         } else {
             try {
                 SystemMetadata sysmeta =CNodeService.getInstance(request).getSystemMetadata(session,id);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the get method to result the expectation.  
      * @param session the session will call the get method
      * @param id the identifier will be used to call the get method
      * @param expectedResult the expected result for authorization. True will be successful.
      */
     private void testGet(Session session, Identifier id, boolean expectedResult) throws Exception {
         if(expectedResult) {
             InputStream out =CNodeService.getInstance(request).get(session,id);
             assertTrue(IOUtil.getInputStreamAsString(out).equals(TEXT));
             out.close();
         } else {
             try {
                 InputStream out =CNodeService.getInstance(request).get(session,id);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
   
     
     /**
      * A generic test method to determine if the given session can call the getChecksum method to result the expectation. 
      * @param session
      * @param pid
      * @param expectedValue
      * @param expectedResult
      * @throws Exception
      */
     private void testGetChecksum(Session session, Identifier pid, Checksum expectedValue, boolean expectedResult) throws Exception {
         if(expectedResult) {
            Checksum checksum= CNodeService.getInstance(request).getChecksum(session, pid);
            //System.out.println("$$$$$$$$$$$$$$$$$$$$$The chechsum from MN is "+checksum.getValue());
            //System.out.println("The exprected chechsum is "+expectedValue.getValue());
            assertTrue(checksum.getValue().equals(expectedValue.getValue()));
         } else {
             try {
                 Checksum checksum= CNodeService.getInstance(request).getChecksum(session, pid);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     
     /**
      * A generic test method to determine if the given session can call the view method to result the expectation. 
      * @param session
      * @param pid
      * @param theme
      * @param expectedResult
      * @throws Exception
      */
     private void testView(Session session, Identifier pid, String theme, boolean expectedResult) throws Exception {
         if(expectedResult) {
             InputStream input = CNodeService.getInstance(request).view(session, theme, pid);
             assertTrue(IOUtil.getInputStreamAsString(input).equals(TEXT));
             input.close();
         } else {
             try {
                 InputStream input = CNodeService.getInstance(request).view(session, theme, pid);
                 fail("we should get here since the previous statement should thrown an NotAuthorized exception.");
             } catch (NotAuthorized e) {
                 
             }
         }
     }
     


     /**
      * Just test we can get the node capacity since there is not session requirement
      * @throws Exception
      */
     private void testGetCapacity() throws Exception {
         Node node = CNodeService.getInstance(request).getCapabilities();
         assertTrue(node.getName().equals(Settings.getConfiguration().getString("dataone.nodeName")));
     }
     
     
     /**
      * Test the listObjects method. It doesn't need any authorization. 
      * So the public user and the node subject should get the same total.
      * @throws Exception
      */
     private void testListObjects() throws Exception {
         Session publicSession =MNodeAccessControlTest.getPublicUser();
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
         Date startTime = sdf.parse("1971-01-01");
         Date endTime = new Date();
         ObjectFormatIdentifier objectFormatId = null;
         Identifier identifier = null;
         NodeReference nodeId = null;
         int start = 0;
         Integer count = 1;
         ObjectList publicList = CNodeService.getInstance(request).listObjects(publicSession, startTime, endTime, objectFormatId, identifier, nodeId, start, count);
         int publicSize = publicList.getTotal();
         Session nodeSession = getMNSession();
         ObjectList privateList = CNodeService.getInstance(request).listObjects(nodeSession, startTime, endTime, objectFormatId, identifier, nodeId, start, count);
         int privateSize = privateList.getTotal();
         assertTrue(publicSize == privateSize);
     }
     
     /**
      * Test the listViews method. It doesn't need any authorization. 
      * @throws Exception
      */
     private void testListViews() throws Exception {
         OptionList publicList = CNodeService.getInstance(request).listViews();
         assertTrue(publicList.sizeOptionList() > 0);
     }
     
     /**
      * Test the listObjects method. It doesn't need any authorization. 
      * @throws Exception
      */
     private void testListFormats() throws Exception {
         ObjectFormatList list =CNodeService.getInstance(request).listFormats();
         assertTrue(list.getTotal() > 0);
     }
    

}
