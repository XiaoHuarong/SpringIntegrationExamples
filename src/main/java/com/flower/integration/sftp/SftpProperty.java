package com.flower.integration.sftp;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("sftpProperty")
@ConfigurationProperties(prefix = "sftp")
public class SftpProperty {


    private String host;

    private Integer port;

    private String user;

    private String password;

    private Map<String,String> filePath;

    ////////////////////////////////////////////////////
    public String getSftpSendPath(){
        return filePath.get("send");
    }

    public String getSftpAchievePath(){
        return filePath.get("achieve");
    }

    public String getLocalTempDir(){
        return filePath.get("localPath");
    }

    ///////////////////////////////////////////////////
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Map<String, String> getFilePath() {
        return filePath;
    }

    public void setFilePath(Map<String, String> filePath) {
        this.filePath = filePath;
    }
}
