package jp.ac.hal.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FilenameUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import jp.ac.hal.dao.FileStorageMapper;
import jp.ac.hal.ftp.FTPUtil;
import jp.ac.hal.model.FileStorage;
import jp.ac.hal.response.FileStorageList;
import jp.ac.hal.response.FileStorageResponse;

@Path("/download")
public class FileDownloadCtrl {

	private Map<String, String> propMap = new HashMap<>();
	@Context ServletContext servletContext;

	@POST
	@Path("/filelist")
	@Produces(MediaType.APPLICATION_JSON)
	public FileStorageList getFileList(@FormParam("registPerson") final String registPerson)  throws Exception {

		long registPersonLong = Long.parseLong(registPerson);

		List<FileStorage> fileList  = this.makeFileList(registPersonLong, "0");

		FileStorageList list = new FileStorageList();
		list.setFileStorageList(new ArrayList<FileStorageResponse>());


		for (FileStorage files : fileList) {
			list.getFileStorageList().add(new FileStorageResponse(
				files.getUploadId(),
				files.getRegistPerson(),
				files.getRegistDate(),
				files.getRegistLocation(),
				files.getVersion(),
				files.getStorageStartDate(),
				files.getStorageEndDate(),
				files.getRegistGroupName(),
				files.getRegistGroupPassword(),
				files.getJsessionId(),
				files.getInvalidFlag()
			));
		}

        return list;

	}

	@GET
	@Path("/downloadlink/{uploadId}")
	public Response downloadFile(@PathParam("uploadId") final String uploadId) {

		long uploadIdLong = Long.parseLong(uploadId);

		// UploadIdをキーに検索する
		FileStorage fileStorage = this.getFileInfo(uploadIdLong);

		// プロパティファイルから定数を取得する
		propMap = this.readProp();

    	// ファイル名を取得する
    	String filename = FilenameUtils.getName(fileStorage.getRegistLocation());

    	String dlFilePath = propMap.get("srcDirName")+ "/" +  uploadId;

    	File folder = new File(dlFilePath);
    	// フォルダの存在を確認する
    	if (folder.exists()) {
    		// ダウンロードファイルを一時保管場所から削除する
    		FileDownloadCtrl.delete(dlFilePath);
    	}

		// FTPでファイルを移動する
    	// ファイルを格納する
		this.exeFtp(filename, uploadId);

		// ダウンロード処理の実行
		StreamingOutput fileStream =  new StreamingOutput() {
			@Override
			public void write(java.io.OutputStream output) throws IOException, WebApplicationException {
				try {
					java.nio.file.Path path = Paths.get(dlFilePath+ "/" +  filename);
					byte[] data = Files.readAllBytes(path);
					output.write(data);
					output.flush();
				} catch (Exception e) {
					throw new WebApplicationException("ファイルが見つかりませんでした。");
				}
			}
		};

		return Response
	            .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM)
	            .header("content-disposition","attachment; filename = " + filename)
	            .build();
	}


	private List<FileStorage> makeFileList(long registPerson, String invalidFlag) {

		List<FileStorage> modelList = new ArrayList<FileStorage>();
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

				// ファイル格納情報一覧を登録者をキーに検索する
				modelList = mapper.selectByRegistPerson(registPerson, invalidFlag);
			} finally {
				session.close();
			}
		}

		return modelList;
	}

	private FileStorage getFileInfo(long uploadId) {

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

				// ファイル格納情報一覧を登録者をキーに検索する
				model = mapper.selectByPK(uploadId);
			} finally {
				session.close();
			}
		}

		return model;
	}

    private boolean exeFtp(String filename, String addFilepath) {

    	boolean ret = false;

    	// 共通プロパティファイルからFTP接続情報を取得する
    	String hostname = propMap.get("hostname");
    	String user = propMap.get("user");
    	String pass = propMap.get("pass");
    	String enc = propMap.get("enc");
    	String srcDirName = propMap.get("srcDirName");
//    	String destDirName =  propMap.get("destDirName");

    	File newdir = new File(srcDirName+ "/" +  addFilepath);
    	newdir.mkdir();

    	// FTPサーバに接続する
    	FTPUtil ftp = new FTPUtil();
    	ret = ftp.connect(hostname, user, pass, enc);

    	// ファイルを取得する
//    	ret = ftp.get(destDirName+ "/" +  addFilepath, filename, srcDirName + "/" +  addFilepath, filename);
    	ret = ftp.get(addFilepath, filename, srcDirName + "/" +  addFilepath, filename);

    	// FTPサーバから切断する
    	ret = ftp.close();

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


    private static void delete(String path) {
        File filePath = new File(path);
        String[] list = filePath.list();
        for(String file : list) {
            File f = new File(path + File.separator + file);
            if(f.isDirectory()) {
                delete(path + File.separator + file);
            }else {
                f.delete();
            }
        }
        filePath.delete();
    }

}
