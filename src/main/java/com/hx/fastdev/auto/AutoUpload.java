package com.hx.fastdev.auto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hx.fastdev.generate.uppack.UppackUtils;
import com.hx.utils.linux.FileLinux;
import com.hx.utils.linux.LinuxI;
import com.hx.utils.linux.LinuxUtils;
import com.hx.utils.linux.model.LinuxUploadFileModel;
import com.hx.utils.properties.PropertiesUtils;

public class AutoUpload {
	private final static Logger log = LoggerFactory.getLogger(AutoUpload.class);
	
	public static void main(String[] args) {
		String[] proPaths = PropertiesUtils.getConfigClass("generate.uppack.pro.paths", String[].class);
		String homePath = PropertiesUtils.getConfigString("generate.uppack.home.path");
		String[] linuxPaths = PropertiesUtils.getConfigClass("generate.uppack.linux.paths", String[].class);
		
		if (proPaths.length != linuxPaths.length) {
			log.info("自动部署. 项目路径数和服务器路径数不一致终止");
			return;
		}
		for (int i = 0; i < proPaths.length; i++) {
			String savePath = UppackUtils.cliDataGenerateFiles(proPaths[i]);
			backUpload(new File(savePath + homePath), new FileLinux(linuxPaths[i]));
		}
		uploadJar();
		log.info("自动部署. 上传所有文件完成");
		LinuxUtils.getLinux().disconn();
	}
	/**
	 * 上传 jar 文件
	 * <pre>
	 * @author hx
	 * @version 创建时间：2020年9月1日  下午10:47:36
	 * </pre>
	 */
	public static void uploadJar() {
		JSONArray uploadJars = PropertiesUtils.getConfigClass("generate.uppack.upload.jar", JSONArray.class);
		if (null == uploadJars) {
			return;
		}
		List<LinuxUploadFileModel> linuxUploadFiles = new ArrayList<>();
		JSONObject uploadJar;
		String serverPath;
		LinuxUploadFileModel linuxUploadFile;
		String[] jarFiles;
		for (int i = 0; i < uploadJars.size(); i++) {
			uploadJar = uploadJars.getJSONObject(i);
			serverPath = uploadJar.getString("serverPath");
			jarFiles = uploadJar.getObject("jarFiles", String[].class);
			for (int j = 0; j < jarFiles.length; j++) {
				linuxUploadFile = LinuxUploadFileModel.newLinuxUploadFileModel(jarFiles[j], serverPath + FileLinux.separatorChar + new File(jarFiles[j]).getName());
				linuxUploadFiles.add(linuxUploadFile);
			}
		}
		LinuxI linux = LinuxUtils.getLinux();
		linux.uploadFile(linuxUploadFiles);
		log.info("自动部署. 上传 jar 完成");
	}
	/**
	 * 备份服务器目录并上传文件
	 * <pre>
	 * @author hx
	 * @version 创建时间：2020年8月31日  下午10:37:32
	 * @param localPath
	 *     本地文件夹路径    
	 * @param linuxPath
	 *     服务器项目路径
	 * </pre>
	 */
	public static void backUpload(File localPath, FileLinux linuxPath) {
		List<File> files = new ArrayList<>();
		getDirSizeAndFiles(files, localPath);
		if (files.isEmpty()) {
			log.info("自动部署. 不存在文件终止");
			return;
		}
		List<LinuxUploadFileModel> linuxUploadFileModels = new ArrayList<>();
		LinuxUploadFileModel linuxUploadFile;
		File file;
		for (int i = 0; i < files.size(); i++) {
			file = files.get(i);
			linuxUploadFile = LinuxUploadFileModel.newLinuxUploadFileModel(file.getPath(), linuxPath.getPath() + FileLinux.separatorChar + file.getPath().substring(localPath.getPath().length()));
			linuxUploadFileModels.add(linuxUploadFile);
		}
		LinuxI linux = LinuxUtils.getLinux();
		linux.backFile(linuxPath.getPath());
		log.info("自动部署. 上传文件: {}", linuxUploadFileModels);
		linux.uploadFile(linuxUploadFileModels);
		log.info("自动部署. 上传完成");
	}
	/**
	 * 获取文件夹下所有文件, 返回大小
	 * <pre>
	 * @author hx
	 * @version 创建时间：2020年8月31日  下午10:37:02
	 * @param listFiles
	 * @param file
	 * @return
	 * </pre>
	 */
	private static long getDirSizeAndFiles(List<File> listFiles, File file) {
		File[] files = file.listFiles();
		if (null == files || files.length == 0) {
			return 0;
		}
		long total = 0;
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				getDirSizeAndFiles(listFiles, files[i]);
			} else {
				listFiles.add(files[i]);
				total += files[i].length();
			}
		}
		return total;
	}
}
