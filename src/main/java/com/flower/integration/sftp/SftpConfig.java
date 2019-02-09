package com.flower.integration.sftp;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.*;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.FileInfo;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.gateway.SftpOutboundGateway;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
import org.springframework.integration.sftp.outbound.SftpMessageHandler;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * Sftp configuration.
 *
 * @Autor Song H.J.
 * @Date 2019-01-18
 */
@Configuration
@DependsOn("sftpProperty")
public class SftpConfig {

    @Resource(name = "sftpProperty")
    private SftpProperty sftpProperty;

    private static Logger log = LoggerFactory.getLogger(SftpConfig.class);


    @Value("${sftp.host}")
    private String sftpHost;

    @Value("${sftp.port:23}")
    private int sftpPort;

    @Value("${sftp.user}")
    private String sftpUser;

    @Value("${sftp.privateKey:#{null}}")
    private org.springframework.core.io.Resource sftpPrivateKey;

    @Value("${sftp.privateKeyPassphrase:}")
    private String sftpPrivateKeyPassphrase;

    @Value("${sftp.password}")
    private String sftpPassword;

   /* @Bean
    public SessionFactory<LsEntry> sftpSessionFactory() {
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(false);
        factory.setHost(sftpProperty.getHost());
        factory.setPort(sftpProperty.getPort());
        factory.setUser(sftpProperty.getUser());
        Properties jschProps = new Properties();
        //!important 必须配置PreferredAuthentications，否则程序控制台会询问user name 和 password。
        jschProps.put("StrictHostKeyChecking", "no");
        jschProps.put("PreferredAuthentications",
                "password,gssapi-with-mic,publickey,keyboard-interactive");

        factory.setSessionConfig(jschProps);

      //  if (sftpPassword != null) {
            factory.setPassword(sftpProperty.getPassword());
//        } else {
//            factory.setPrivateKey(sftpPrivateKey);
//            factory.setPrivateKeyPassphrase(sftpPrivateKeyPassphrase);
//        }

        factory.setAllowUnknownKeys(true);
        //        //设置缓存的属性，缓存的size(), waitTimeout().
        CachingSessionFactory<LsEntry> cachingSessionFactory =
                new CachingSessionFactory<LsEntry>(factory);
        cachingSessionFactory.setPoolSize(10);
//        cachingSessionFactory.setSessionWaitTimeout(1000);

        return cachingSessionFactory;
//        return new CachingSessionFactory<LsEntry>(factory);
    }*/

    /**
     * 创建 spring-integration-sftp session
     * 避免使用jsch原生的创建session的方式
     *
     * @return SessionFactory<LsEntry>
     */
    @Bean
    public SessionFactory<LsEntry> sftpSessionFactory(){
        System.out.println("######################################################");
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setUser(sftpProperty.getUser());
        factory.setHost(sftpProperty.getHost());
        factory.setPort(sftpProperty.getPort());
        factory.setPassword(sftpProperty.getPassword());

        Properties jschProps = new Properties();
        //!important 必须配置PreferredAuthentications，否则程序控制台会询问user name 和 password。
        jschProps.put("StrictHostKeyChecking", "no");
        jschProps.put("PreferredAuthentications",
                "password,gssapi-with-mic,publickey,keyboard-interactive");

        factory.setSessionConfig(jschProps);
        factory.setAllowUnknownKeys(true);

        //设置缓存的属性，缓存的size(), waitTimeout().
        CachingSessionFactory<LsEntry> cachingSessionFactory =
                new CachingSessionFactory<LsEntry>(factory);
//        cachingSessionFactory.setPoolSize(2000);


        return  cachingSessionFactory;
    }

    /**
     * 配置Outbound Channel Adapter.
     *
     * 实质上就是一个MessageHandler，接收Message Channel发送的信息流.
     * @return MessageHandler
     */
    @ServiceActivator(inputChannel = "fileInChannel")
    @Bean
    public SftpMessageHandler sftpMessageHandler(){
        SftpMessageHandler sftpMsgHandler = new SftpMessageHandler(sftpSessionFactory());

        sftpMsgHandler.setRemoteDirectoryExpression(
                new LiteralExpression(sftpProperty.getSftpAchievePath()));
        sftpMsgHandler.setAutoCreateDirectory(true);
        sftpMsgHandler.setCharset("UFT-8");
        return sftpMsgHandler;
    }


    /**
     * 配置 Inbound Channel Adapter
     *
     * 监控sftp服务器文件的状态。一旦由符合条件的文件生成，就将其同步到本地服务器。
     * 需要条件：inboundFileChannel的bean；轮询的机制；文件同步bean,SftpInboundFileSynchronizer；
     */
    @Bean
    @InboundChannelAdapter(value = "inboundFileChannel",
            poller = @Poller(cron = "0 1/10 * * * *", maxMessagesPerPoll = "1"))
    public MessageSource<File> fileMessageSource() {
        System.out.println("=========================================================");

        //创建sftpInboundFileSynchronizer，并绑定到message source.
        SftpInboundFileSynchronizingMessageSource source =
                new SftpInboundFileSynchronizingMessageSource(sftpInboundFileSynchronizer());

        //自动创建本地文件夹
        source.setAutoCreateLocalDirectory(true);
        source.setLocalDirectory(new File(sftpProperty.getLocalTempDir()));

        //设置文件过滤器
        source.setLocalFilter(new AcceptOnceFileListFilter<File>());

        return source;

    }

    /**
     * 为Inbound-channel-adapter提供bean
     */
    @Bean
    public DirectChannel inboundFileChannel() {
        return new DirectChannel();
    }

    /**
     * SftpInboundFileSynchronizer,
     *
     *  同步sftp文件至本地服务器.
     *      <1> 可以放在service中获取bean使用.toLocal方法；
     *      <2> 也可以使用inbound-channel-adapter中，做监控文件服务器的动态。
     *
     * @return SftpInboundFileSynchronizer
     */
    @Bean(name = "synFileChannel")
    public SftpInboundFileSynchronizer sftpInboundFileSynchronizer (){

        SftpInboundFileSynchronizer fileSynchronize =
                new SftpInboundFileSynchronizer(sftpSessionFactory());
        fileSynchronize.setDeleteRemoteFiles(true);
        fileSynchronize.setPreserveTimestamp(true);

        //!important
        fileSynchronize.setRemoteDirectory(sftpProperty.getSftpSendPath());
        fileSynchronize.setFilter(new SftpSimplePatternFileListFilter("*.*"));
        //fileSynchronize.setLocalFilenameGeneratorExpression( );
        fileSynchronize.setPreserveTimestamp(true);
        return fileSynchronize;
    }

    ///////////////////////////////////////////////////////////////////////

    /**
     * 配置 SFTP Outbound Gateway
     *
     * @return MessageHandler
     */
    @Bean
    @ServiceActivator(inputChannel = "sftpChannel")
    public MessageHandler handler() {


        SftpOutboundGateway sftpOutboundGateway = new SftpOutboundGateway(sftpSessionFactory(),"ls","payload");
//        MessageChannel message = sftpOutboundGateway.getOutputChannel();

        sftpOutboundGateway.setLocalDirectory(new File("E:\\sftp_tmp_dir"));
        sftpOutboundGateway.setAutoCreateLocalDirectory(true);  // TODO dynanic path
        return sftpOutboundGateway;
    }

    @Bean
    @ServiceActivator(inputChannel = "sftpChannel2")
    public MessageHandler handler2() {
        SftpOutboundGateway sftpOutboundGateway = new SftpOutboundGateway(sftpSessionFactory(),"ls","payload");
        sftpOutboundGateway.setOptions("-dirs");
        sftpOutboundGateway.setLocalDirectory(new File("E:\\sftp_tmp_dir"));
        sftpOutboundGateway.setAutoCreateLocalDirectory(true);  // TODO dynanic path

        return sftpOutboundGateway;
    }

    @Bean
    @ServiceActivator(inputChannel = "sftpChannel3")
    public MessageHandler handler3() {
        System.out.println("=========================         3         ================================");

        SftpOutboundGateway sftpOutboundGateway = new SftpOutboundGateway(sftpSessionFactory(),"mget","payload");
        sftpOutboundGateway.setOptions("-R");
        sftpOutboundGateway.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
        sftpOutboundGateway.setLocalDirectory(new File("E:\\sftp_tmp_dir"));
        sftpOutboundGateway.setAutoCreateLocalDirectory(true);  // TODO dynanic path

        return sftpOutboundGateway;
    }

    @Autowired
    private BeanFactory beanFactory;

//outbound gateway，put命令需要借助与sftpRemoteFileTemplate。
    //看源码，可以发现outbound gateway 有多种构造函数；
    @Bean
    @ServiceActivator(inputChannel = "sftpChannel4")
    public MessageHandler handler4(){
        SftpRemoteFileTemplate  sftpRemoteFileTemplate = new SftpRemoteFileTemplate(sftpSessionFactory());
        sftpRemoteFileTemplate.setRemoteDirectoryExpression(new LiteralExpression("/send"));

        SftpOutboundGateway sftpOutboundGateway = new SftpOutboundGateway(sftpRemoteFileTemplate,"put","payload");
//        sftpOutboundGateway.setLocalDirectoryExpressionString("/get/");
        sftpOutboundGateway.setBeanFactory(beanFactory);
        return sftpOutboundGateway;
    }


    @Bean
    @ServiceActivator(inputChannel = "sftpChannel5")
    public MessageHandler handler5(){
        SftpRemoteFileTemplate  sftpRemoteFileTemplate = new SftpRemoteFileTemplate(sftpSessionFactory());
        sftpRemoteFileTemplate.setRemoteDirectoryExpression(new LiteralExpression("/send"));


        SftpOutboundGateway sftpOutboundGateway = new SftpOutboundGateway(sftpRemoteFileTemplate,"mput","payload");
//        sftpOutboundGateway.setLocalDirectoryExpressionString("/get/");
//        sftpOutboundGateway.setOptions("-R");
        sftpOutboundGateway.setMputFilter(new FileListFilter<File>() {
            @Override
            public List<File> filterFiles(File[] files) {
                return null;
            }
        });
        sftpOutboundGateway.setBeanFactory(beanFactory);
        return sftpOutboundGateway;
    }

    @Bean
    @ServiceActivator(inputChannel = "sftpChannel6")
    public MessageHandler handler6(){

        SftpOutboundGateway sftpOutboundGateway = new SftpOutboundGateway(sftpSessionFactory(),"rm","payload");
        sftpOutboundGateway.setBeanFactory(beanFactory);
        return sftpOutboundGateway;
    }

    @Bean
    @ServiceActivator(inputChannel = "sftpChannel7")
    public MessageHandler handler7(){

//        SftpRemoteFileTemplate  sftpRemoteFileTemplate = new SftpRemoteFileTemplate(sftpSessionFactory());
//        sftpRemoteFileTemplate.setRemoteDirectoryExpression(new LiteralExpression("/send"));


        SftpOutboundGateway sftpOutboundGateway = new SftpOutboundGateway(sftpSessionFactory(),"mv","'send/22.TXT'");
//        sftpOutboundGateway.setRenameExpression(new LiteralExpression("/send1"));
//        sftpOutboundGateway.setChmod(777);
//        sftpOutboundGateway.setRenameExpressionString("send1");

        sftpOutboundGateway.setRenameExpression(new LiteralExpression("send1/22.TXT"));
//        sftpOutboundGateway.setAutoCreateLocalDirectory(true);
        sftpOutboundGateway.setBeanFactory(beanFactory);
        return sftpOutboundGateway;
    }


    @MessagingGateway
    public interface UploadGateway {

        @Gateway(requestChannel = "sftpChannel")
        List<FileInfo> listFileInfo(String dir);

        @Gateway(requestChannel = "sftpChannel2")
        List<FileInfo> listFileName(String dir);

        @Gateway(requestChannel = "sftpChannel3")
        List<File> listFile(String dir);

        @Gateway(requestChannel = "sftpChannel4")
        String putFile(File source);

        @Gateway(requestChannel = "sftpChannel5")
        List<String> mputFile(File directory);

        @Gateway(requestChannel = "sftpChannel6")
        boolean removeFile(String file);

        @Gateway(requestChannel = "sftpChannel7")
        boolean moveFile(String file);

    }

}

