//package com.flower.integration.sftp;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.integration.annotation.Gateway;
//import org.springframework.integration.annotation.MessageEndpoint;
//import org.springframework.integration.annotation.MessagingGateway;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@MessageEndpoint
//@MessagingGateway
//public interface OutboundGatewayOption {
//    @Gateway(requestChannel = "sftpChannel")
//    public List<Boolean> lsGetAndRmFiles(String dir);
//
//}
