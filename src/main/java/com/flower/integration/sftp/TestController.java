package com.flower.integration.sftp;


import com.jcraft.jsch.ChannelSftp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.file.remote.FileInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;


@RestController
public class TestController {


    @Autowired
    private SftpService sftpService;

    @Autowired
    private SftpConfig.UploadGateway uploadGateway;

    @GetMapping("/sftp")
    public void testSftpSpringBatch() {

        List<FileInfo> fileList = uploadGateway.listFileInfo("/send");

        for (FileInfo file : fileList) {
            String fileName = file.getFilename();
            String filePath = file.getRemoteDirectory();
            ChannelSftp.LsEntry fileInfo = (ChannelSftp.LsEntry) file.getFileInfo();
            boolean isDir = file.isDirectory();
            boolean isLink = file.isLink();
            long modifyTime = file.getModified();
            System.out.println("=============================  " + fileName);
            System.out.println("==================  " + filePath);
            System.out.println("==================  " + fileInfo.getFilename());
            System.out.println("==================  " + isDir);
            System.out.println("==================  " + isLink);
            System.out.println("==================  " + modifyTime);
        }
    }

    @GetMapping("/sftp2")
    public void testSftpSpringBatch2() {

        List<FileInfo> fileNameList = uploadGateway.listFileName("/send");

        for (FileInfo fileName : fileNameList) {

            System.out.println("=============================  " + fileName);
        }
    }


    @GetMapping("/sftp3")
    public void testSftpSpringBatch3() throws InterruptedException {

        List<File> fileNameList = uploadGateway.listFile("/send");

        for (File fileName : fileNameList) {
            System.out.println("=============================  " + fileName);
        }
    }

    @GetMapping("/sftp4")
    public void testSftpSpringBatch4() throws InterruptedException {

        String result = uploadGateway.putFile(new File("G:\\Redis.pdf"));

        System.out.println("=============================  " + result);
    }

    @GetMapping("/sftp5")
    public void testSftpSpringBatch5() throws InterruptedException {

        List<String> result = uploadGateway.mputFile(new File("G:\\js"));


        for (String fileName : result) {
            System.out.println("=============================  " + fileName);
        }
    }

    @GetMapping("/sftp6")
    public void testSftpSpringBatch6() throws InterruptedException {

        boolean result = uploadGateway.removeFile("/send/2.txt");



            System.out.println("=============================  " + result);

    }

    @GetMapping("/sftp7")
    public void testSftpSpringBatch7() throws InterruptedException {

        boolean result = uploadGateway.moveFile("/22.TXT");



        System.out.println("=============================  " + result);

    }
}
