package jp.ac.hal.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import jp.ac.hal.common.UniqueId;
import jp.ac.hal.dao.FileStorageMapper;
import jp.ac.hal.ftp.FTPUtil;
import jp.ac.hal.model.FileStorage;
import jp.ac.hal.response.FileStorageResponse;

@Path("/upload")
public class FileUploadCtrl {

	private Map<String, String> propMap = new HashMap<>();
	@Context ServletContext servletContext;
    private FileStorage entity = new FileStorage();

	@POST
	@Path("/files")
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	@Produces({MediaType.APPLICATION_JSON})
	public FileStorageResponse uploadFile(
	        @FormDataParam("registPerson") String registPerson,
	        @FormDataParam("storageStartDate") String storageStartDate,
	        @FormDataParam("storageEndDate") String storageEndDate,
	        @FormDataParam("registGroupName") String registGroupName,
	        @FormDataParam("registGroupPassword") String registGroupPassword,
	        @FormDataParam("jsessionId") String jsessionId,
			@FormDataParam("file") InputStream fileInputStream,
	        @FormDataParam("file") FormDataContentDisposition fileMetaData
			) throws Exception {

		UniqueId now = new UniqueId();
		long uploadId = now.createId();

		// アップロードのファイル名を取得する
		String filename = fileMetaData.getFileName();

		// プロパティファイルから定数を取得する
		propMap = this.readProp();

		String UPLOAD_PATH = propMap.get("srcDirName") + "/" + filename;

	    try {
	        int read = 0;
	        byte[] bytes = new byte[1024];

	        OutputStream out = new FileOutputStream(new File(UPLOAD_PATH));
	        while ((read = fileInputStream.read(bytes)) != -1) {
	            out.write(bytes, 0, read);
	        }
	        out.flush();
	        out.close();
	    } catch (IOException e) {
	        throw new WebApplicationException("ファイルアップロードに失敗しました。");
	    }

		entity.setUploadId(uploadId);
		entity.setRegistPerson(Long.parseLong(registPerson));
		entity.setRegistDate(getTodyStr());
		entity.setVersion("1");
		entity.setStorageStartDate(storageStartDate);
		entity.setStorageEndDate(storageEndDate);
		entity.setRegistGroupName(registGroupName);
		entity.setRegistGroupPassword(registGroupPassword);
		entity.setJsessionId(jsessionId);
		entity.setInvalidFlag("0");

		FileStorageResponse fileStorage = new FileStorageResponse();
        fileStorage = this.exeFileUpload(filename, entity, UPLOAD_PATH);

        return fileStorage;
	}


    private FileStorageResponse exeFileUpload(String filename, FileStorage fileStorage, String UPLOAD_PATH) {

    	// ファイルを格納する
		this.exeFtp(filename, String.valueOf(fileStorage.getUploadId()));

		// 一時格納ファイルを削除する
	    File file = new File(UPLOAD_PATH);

		if (file.exists()){
			if (file.delete()){
				System.out.println(UPLOAD_PATH + "<--file delete success");
			}else{
				System.out.println(UPLOAD_PATH + "<--file delete fail");
			}
		}else{
			System.out.println(UPLOAD_PATH + "<--file not found");
		}

    	FileStorage model = new FileStorage();
        SqlSession session = null;

        try {
            SqlSessionFactory sqlSessionFactory
            = new SqlSessionFactoryBuilder().build(Resources.getResourceAsStream("resources/mybatis-config.xml"));
            session = sqlSessionFactory.openSession();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(session != null) {
            try {

            	FileStorageMapper mapper = session.getMapper(FileStorageMapper.class);
        		// ファイル格納情報を登録する
            	boolean ret = mapper.insertFileStorage(fileStorage);
            	System.out.println("insert ret--> " + ret);
            	session.commit();

            	// ファイル格納情報を取得する
            	model = mapper.selectByPK(fileStorage.getUploadId());

            } finally {
                session.close();
            }
        }

        FileStorageResponse ret = new FileStorageResponse();

        ret.setUploadId(model.getUploadId());
        ret.setRegistPerson(model.getRegistPerson());
        ret.setRegistDate(model.getRegistDate());
        ret.setRegistLocation(model.getRegistLocation());
        ret.setVersion(model.getVersion());
        ret.setStorageStartDate(model.getStorageStartDate());
        ret.setStorageEndDate(model.getStorageEndDate());
        ret.setRegistGroupName(model.getRegistGroupName());
        ret.setRegistGroupPassword(model.getRegistGroupPassword());
        ret.setJsessionId(model.getJsessionId());
        ret.setInvalidFlag(model.getInvalidFlag());

        return ret;

    }


    private boolean exeFtp(String filename, String addFilepath) {

    	boolean ret = false;

    	// 共通プロパティファイルからFTP接続情報を取得する
    	String hostname = propMap.get("hostname");
    	String user = propMap.get("user");
    	String pass = propMap.get("pass");
    	String enc = propMap.get("enc");
    	String srcDirName = propMap.get("srcDirName");
    	String destDirName =  propMap.get("destDirName");

    	// FTPサーバに接続する
    	FTPUtil ftp = new FTPUtil();
    	ret = ftp.connect(hostname, user, pass, enc);

    	// ディレクトリーを作成する
    	ftp.makeDir(addFilepath);
    	// ファイルを格納する
    	ret = ftp.put(srcDirName, filename, destDirName + "/" +  addFilepath, filename);

    	System.out.println("ftp.put--> " + ret);

    	entity.setRegistLocation(destDirName + "/" +  addFilepath + "/" + filename);

    	// FTPサーバから切断する
    	ret = ftp.close();

        return ret;

    }

    private String getTodyStr() {

    	String ret = new String();

		LocalDateTime d = LocalDateTime.now();
		DateTimeFormatter df1 =
				DateTimeFormatter.ofPattern("yyyyMMddHHmm");
		ret = df1.format(d);

    	return ret;
    }

    private Map<String, String> readProp() {

    	Map<String, String> map = new HashMap<>();

    	Properties properties = new Properties();
		try {
			String realPath = servletContext.getRealPath("/WEB-INF/common.properties");
			InputStream input = new FileInputStream(realPath);
			properties.load(input);
		} catch (IOException e) {
			// ファイル読み込みに失敗
			System.out.println(String.format("ファイルの読み込みに失敗しました"));
		}

		// Mapにキーと値を格納
        map.put("hostname", properties.getProperty("ftp.hostname"));
        map.put("user", properties.getProperty("ftp.user"));
        map.put("pass", properties.getProperty("ftp.password"));
        map.put("enc", properties.getProperty("ftp.enc"));
        map.put("srcDirName", properties.getProperty("ftp.srcdirname"));
        map.put("destDirName", properties.getProperty("ftp.destdirname"));

    	return map;
    }
}
