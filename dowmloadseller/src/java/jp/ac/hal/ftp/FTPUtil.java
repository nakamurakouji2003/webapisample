package jp.ac.hal.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class FTPUtil {

	/** FTPクライアント */
	private FTPClient client = new FTPClient();

	/**
	 * @param client FTPクライアント
	 */
	public void setClient(FTPClient client) {
		this.client = client;
	}

	/**
	 * FTPサーバに接続する
	 * @param hostname ホストネーム
	 * @param user FTPサーバユーザー名
	 * @param pass FTPサーバパスワード
	 * @param enc  FTPサーバエンコード用文字コード
	 * @return true:成功
	 */
	public boolean connect(String hostname, String user, String pass, String enc) {
		try {
			// encoding設定
			// connectメソッドの前に呼び出さなければならない
			client.setControlEncoding(enc);
			// FTPサーバーへ接続
			client.connect(hostname);
			// 正常に接続できたかを判定する
			if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
				throw new IOException("Ftp not Connected ReplyCode=" + client.getReplyCode());
			}
			// ログイン処理
			if (!client.login(user, pass)) {
				throw new IOException("login failed");
			}
			// PASVモードに設定
			client.enterLocalPassiveMode();
			// バイナリ方式に変更
			client.setFileType(FTP.BINARY_FILE_TYPE);
			return true;
		} catch (IOException e) {
		return false;
		}
	}

	/**
	 * FTPサーバからログアウトする
	 * @return true:成功
	 */
	public boolean close() {
		try {
			if (client != null && client.isConnected()) {
				// FTPサーバーからログアウト
				client.logout();
				// FTPサーバーから切断
				client.disconnect();
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * FTPサーバからファイルを取得する
	 * @param srcDirName FTPのディレクトリ名
	 * @param srcFileName FTPのファイル名
	 * @param destDirName 保存先ディレクトリ名
	 * @param destFileName 保存先ファイル名
	 * @return 成否
	 */
	public boolean get(String srcDirName, String srcFileName, String destDirName, String destFileName) {
		boolean result;

		// ファイルを指定
		File dest = new File(destDirName + "/" + destFileName);

		try (FileOutputStream out = new FileOutputStream(dest)) {
			// ファイルを取得
			String srcFullPath = srcDirName + "/" + srcFileName;
			// バッファサイズを1MBに
			client.setBufferSize(1024 * 1024);
			result = client.retrieveFile(srcFullPath, out);
			if (!result) {
				return false;
			}

			// タイムスタンプを維持
			String mTime = client.getModificationTime(srcFullPath);

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			Date modificationDate = dateFormat.parse(mTime.substring(mTime.indexOf(" ") + 1));

			long epocMTime = modificationDate.getTime();
			dest.setLastModified(epocMTime);
		} catch (Exception e) {
			return false;
		}

		return result;
	}

	/**
	 * FTPサーバにファイルを保存する
	 * @param srcDirName ディレクトリ名
	 * @param srcFileName ファイル名
	 * @param destDirName FTPの保存先ディレクトリ名
	 * @param destFileName FTPの保存先ファイル名
	 * @return 成否
	 */
	public boolean put(String srcDirName, String srcFileName, String destDirName, String destFileName) {
		boolean result;
		// ファイルを指定
		File source = new File(srcDirName + "/" + srcFileName);

		try (FileInputStream in = new FileInputStream(source)) {

			// ファイルを送信
//			result = client.storeFile(destDirName + "/" + destFileName, in);
			result = client.storeFile(destFileName, in);
			in.close();
		} catch (IOException e) {
			return false;
		}

		return result;
	}

	/**
	 * ファイルの存在確認
	 * @param srcDirName ディレクトリ名
	 * @param srcFileName ファイル名
	 * @return true: 存在する
	 */
	public boolean exists(String srcDirName, String srcFileName) {
		// ファイルを指定
		try {
			FTPFile[] files = client.listFiles(srcDirName + "/" + srcFileName);
			return files.length != 0;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * FTPサーバのファイルサイズを取得する
	 * 返り値は「byte」単位
	 * @param srcDirName ディレクトリ名
	 * @param srcFileName ファイル名
	 * @return fileSize ファイルサイズ
	 */
	public long getFileSize(String srcDirName, String srcFileName) {
		try {
			String fullPath = srcDirName + "/" + srcFileName;
			// ファイルを指定
			FTPFile[] files = client.listFiles(fullPath);
			if (files.length == 0) {
			throw new IOException("no such file : " + fullPath);
			}
			return files[0].getSize();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * ディレクトリーの作成
	 * @param filepath ディレクトリ名
	 * @return true: 成功
	 */
	public boolean makeDir(String filepath) {

		boolean ret = false;

		// ディレクトリがなければ作る
		try {
			ret = client.makeDirectory(filepath);
			if ( ret ) {
				ret = client.changeWorkingDirectory(filepath);
			} else {
				// ディレクトリが作れない場合、移動してみる
				ret = client.changeWorkingDirectory(filepath);
				if ( !ret ) {
					System.out.println("Server Directory Failed: " + filepath);
					return ret;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return ret;

	}
}