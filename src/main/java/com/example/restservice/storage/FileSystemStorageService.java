package com.example.restservice.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream; 
import java.nio.file.Path;
import java.nio.file.Paths; 

import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.stereotype.Service; 
import org.springframework.web.multipart.MultipartFile;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

@Service
public class FileSystemStorageService implements StorageService {

	private Path rootLocation;

	private String remoteHost = "banjo.rit.edu";
	private String username = "nam6711";
	private String password = "Hithere@28"; 

	@Autowired
	public FileSystemStorageService() {
		StorageProperties properties = new StorageProperties();
		this.rootLocation = Paths.get(properties.getLocation());
	}
	 
    private ChannelSftp setupJsch() throws JSchException {
        JSch jsch = new JSch();
        jsch.setKnownHosts("src/main/resources/known_hosts");
        Session jschSession = jsch.getSession(username, remoteHost);
        jschSession.setPassword(password);
        jschSession.connect(); 
        return (ChannelSftp) jschSession.openChannel("sftp");
    } 

	@Override
    public void store(MultipartFile file, String folder) throws JSchException, SftpException, IOException {
        ChannelSftp channelSftp = setupJsch();
        channelSftp.connect();
      
        String remoteDir = "www/froggie-adventures/site/media" + folder + "/"; 
		System.out.println("\n\n\n" + remoteDir);
		InputStream inputStream = file.getInputStream();

		channelSftp.put(inputStream, remoteDir + file.getOriginalFilename());      

        channelSftp.exit();
    }   
 
    public void store(File file, String folder) throws JSchException, SftpException, IOException {
        ChannelSftp channelSftp = setupJsch();
        channelSftp.connect();
      
        String remoteDir = "www/froggie-adventures/site/media" + folder + "/"; 
		System.out.println("\n\n\n" + remoteDir); 

		channelSftp.put(file.getAbsolutePath(), remoteDir + file.getName());      

        channelSftp.exit();
    }   
}
