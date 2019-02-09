package com.flower.integration.sftp;

import com.jcraft.jsch.ChannelSftp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service("sftpService")
public class SftpService {


    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource(name = "fileInChannel")
    protected MessageChannel messageChannel;

    @Autowired
    private SftpProperty sftpProperty;

    @Autowired
    private SessionFactory<ChannelSftp.LsEntry> sftpSessionFactory;

    /**
     * 发送文件到SFTP, 借用MessageChannel
     *
     * @param localFilePath file local path.
     */
    public void sendFileToSftp(String localFilePath) {

        Path filePath = Paths.get(localFilePath);
        if (filePath.toFile().exists()) {
            Message<File> fileMessage = MessageBuilder.withPayload(filePath.toFile()).build();
            boolean result = messageChannel.send(fileMessage);
            String resultMsg = result ? "Success" : "Failure";
            log.info("File send to sftp {}, File: {}.", resultMsg, filePath.getFileName());
        } else {
            log.warn("No found file. {}", filePath.getFileName());
        }
    }

    /**
     * 删除sftp文件
     *
     * @param sessionFactory  sftp server.
     * @param remoteDirectory file directory.
     * @param fileName        file
     * @return return true is remove success,or false.
     */
    public boolean removeSftpRemoteFile(SessionFactory<ChannelSftp.LsEntry> sessionFactory, String remoteDirectory, String fileName) {
        SftpRemoteFileTemplate sftpRemoteFileTemplate = new SftpRemoteFileTemplate(sessionFactory);

        boolean direCheck = remoteDirectory.endsWith(sftpRemoteFileTemplate.getRemoteFileSeparator());
        if (!direCheck) {
            remoteDirectory += sftpRemoteFileTemplate.getRemoteFileSeparator();
        }
        boolean fileExist = sftpRemoteFileTemplate.exists(remoteDirectory + fileName);
        if (fileExist) {
            return sftpRemoteFileTemplate.remove(remoteDirectory + fileName);
        } else {
            log.warn("No found file in the directory, {}.", remoteDirectory);
            return false;
        }
    }

    /**
     * sftp文件重命名
     *
     * @param sessionFactory  sftp server
     * @param remoteDirectory file directory path.
     * @param sourceFileName  source file name
     * @param targetFileName  rename target name
     */
    public void renameSftpRemoteFile(SessionFactory<ChannelSftp.LsEntry> sessionFactory, String remoteDirectory,
                                     String sourceFileName, String targetFileName) {
        SftpRemoteFileTemplate fileTemplate = new SftpRemoteFileTemplate(sessionFactory);

        boolean direCheck = remoteDirectory.endsWith(fileTemplate.getRemoteFileSeparator());
        if (!direCheck) {
            remoteDirectory += fileTemplate.getRemoteFileSeparator();
        }
        boolean fileExist = fileTemplate.exists(remoteDirectory + sourceFileName);
        if (fileExist) {
            fileTemplate.rename(remoteDirectory + sourceFileName, remoteDirectory + targetFileName);
        } else {
            log.warn("No found file in the directory, {}.", remoteDirectory);
        }
    }

    /**
     * sftp文件是否存在
     *
     * @param sessionFactory sftp server
     * @param directory      file directory
     * @param fileName       file name
     * @return true if file exist, or false.
     */
    public boolean fileExist(SessionFactory<ChannelSftp.LsEntry> sessionFactory, String directory, String fileName) {
        SftpRemoteFileTemplate fileTemplate = new SftpRemoteFileTemplate(sessionFactory);
        boolean fileNameCheck = directory.endsWith(fileTemplate.getRemoteFileSeparator());
        if (!fileNameCheck) {
            directory += fileTemplate.getRemoteFileSeparator();
        }

        return fileTemplate.exists(directory + fileName);
    }


    /**
     * sftp检索文件
     *
     * @param sessionFactory sftp server
     * @param directory      file directory
     * @param fileNameFilter file name filter
     * @return file name list match filter
     */
    public List<String> lsFileOfDirectory(SessionFactory<ChannelSftp.LsEntry> sessionFactory,
                                          String directory, String fileNameFilter) {
        SftpRemoteFileTemplate fileTemplate = new SftpRemoteFileTemplate(sessionFactory);

        if (!directory.endsWith(fileTemplate.getRemoteFileSeparator())) {
            directory += fileTemplate.getRemoteFileSeparator();
        }
        ChannelSftp.LsEntry[] files = fileTemplate.list(directory + fileNameFilter);
        List<String> fileNames = new ArrayList<>();
        for (ChannelSftp.LsEntry lsEntry : files) {
            boolean isDir = lsEntry.getAttrs().isDir();
            if (!isDir) {
                fileNames.add(lsEntry.getFilename());
            }
        }
        return fileNames;
    }

    @Autowired
    private BeanFactory beanFactory;

    /**
     * 本地发送文件至sftp服务器
     *
     * @param sessionFactory sftp server
     * @param filePath file local path
     * @param targetPath target directory
     * @param mode FileExistsModel
     *             NULL：默认，替换文件；
     *             APPEND：若文件存在，追加内容；
     *             REPLACE：替换文件；
     *             APPEND_NO_FLUSH：
     *             FAIL：
     *             IGNORE：
     */
    public void sendSftpFile(SessionFactory<ChannelSftp.LsEntry> sessionFactory,
                             String filePath, String targetPath, FileExistsMode mode){
        SftpRemoteFileTemplate fileTemplate = new SftpRemoteFileTemplate(sessionFactory);
        try {
            //设置远程sftp服务器配置
            fileTemplate.setRemoteDirectoryExpression(new LiteralExpression(targetPath));
            fileTemplate.setAutoCreateDirectory(true);
            fileTemplate.setCharset("UTF-8");
            fileTemplate.setBeanFactory(beanFactory);
            fileTemplate.afterPropertiesSet();
        } catch (Exception e){
            log.warn(e.getMessage());
        }

        Path file = Paths.get(filePath);
        if (file.toFile().exists()){
            Message<File> message = MessageBuilder.withPayload(file.toFile()).build();
            if (null == mode){
                fileTemplate.send(message);
            } else {
                //fileTemplate.setFileNameGenerator(new DefaultFileNameGenerator());
                if (fileTemplate.isUseTemporaryFileName()){
                    fileTemplate.setUseTemporaryFileName(false);
                }
                fileTemplate.send(message, mode);
            }
        }
    }


    @Resource(name = "synFileChannel")
    private SftpInboundFileSynchronizer sftpInboundFileSynchronizer;

    public void synchronizedFileToLocal(String localDir){
        File dir = Paths.get(localDir).toFile();
        sftpInboundFileSynchronizer.synchronizeToLocalDirectory(dir);
    }

}




