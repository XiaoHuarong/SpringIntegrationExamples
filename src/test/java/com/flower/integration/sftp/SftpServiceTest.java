package com.flower.integration.sftp;

import com.flower.integration.SpringIntegrationApp;
import com.jcraft.jsch.ChannelSftp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringIntegrationApp.class)
@EnableIntegration
public class SftpServiceTest {

    @Autowired
    private SftpService sftpService;

    @Autowired
    private SftpProperty sftpProperty;

    @Autowired
    private SessionFactory<ChannelSftp.LsEntry> sftpSessionFactory;

    @Before
    public void before (){
        System.out.println("00000000000000000000000000000000000000000000000000000");
    }

    @After
    public void after(){

    }

    @Test
    public void sendFileToSftp() {

    //sftpService.sendFileToSftp();
    }

    @Test
    public void testRemoveSftpRemoteFile(){
        boolean result = sftpService.removeSftpRemoteFile(
                sftpSessionFactory, sftpProperty.getSftpSendPath(),"user333.csv");

        System.out.println("=======" + result);
    }

    @Test
    public void testRenameSftpRemoteFile(){
        sftpService.renameSftpRemoteFile(sftpSessionFactory, sftpProperty.getSftpSendPath(),"user.csv",
                "user111.csv");
    }

    @Test
    public void testfileExist(){
       boolean result = sftpService.fileExist(sftpSessionFactory, sftpProperty.getSftpSendPath(),"user111.csv");
        System.out.println("++++++++++++" + result);
    }

    @Test
    public void testlsFileOfDirectory(){
        List<String> result = sftpService.lsFileOfDirectory(sftpSessionFactory,
                sftpProperty.getSftpSendPath(),"*TXT");
        System.out.println("-------------------" + result.toString());
    }

    @Test
    public void testSendSftpFile() throws Exception {
        sftpService.sendSftpFile(sftpSessionFactory,
                "G:\\jquery.txt", sftpProperty.getSftpAchievePath(), FileExistsMode.REPLACE);
    }

    @Test
    public void testSynchronizedFileToLocal(){
        sftpService.synchronizedFileToLocal(sftpProperty.getLocalTempDir());
    }
}