package org.mrpdaemon.sec.encfs.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.mrpdaemon.sec.encfs.EncFSFileInfo;
import org.mrpdaemon.sec.encfs.EncFSFileProvider;

public class CommonsVFSFileProvider implements EncFSFileProvider {

	protected final FileSystemManager fileSystemManager;

	public CommonsVFSFileProvider(FileSystemManager fileSystemManager) {
		this.fileSystemManager = fileSystemManager;
	}

	public boolean move(String encOrigFileName, String encNewFileName) throws IOException {
		FileObject origFile = resolveFile(encOrigFileName);
		if (origFile.exists() == false)
			return false;

		FileObject newFile = resolveFile(encNewFileName);
		if (encNewFileName.lastIndexOf("/") > 0) {
			if (newFile.getParent().exists() == false) {
				return false;
			}
		}
		origFile.moveTo(newFile);
		return true;
	}

	private FileObject resolveFile(String encOrigFileName) throws FileSystemException {
		return fileSystemManager.resolveFile(fileSystemManager.getSchemes()[0] + ":" + encOrigFileName);
	}

	public boolean isDirectory(String encFileName) throws IOException {
		FileObject file = resolveFile(encFileName);
		return file.getType() == FileType.FOLDER;
	}

	public boolean delete(String encFileName) throws IOException {
		FileObject file = resolveFile(encFileName);
		return file.delete();
	}

	public boolean mkdir(String encDirName) throws IOException {
		FileObject file = resolveFile(encDirName);
		if (file.exists()) {
			return false;
		} else {
			if (encDirName.lastIndexOf("/") != 0) {
				if (file.getParent().exists() == false) {
					return false;
				}
			}
			file.createFolder();
			return true;
		}
	}

	public boolean mkdirs(String encDirName) throws IOException {
		String[] dirNameParts = encDirName.split("/");

		String tmpDirName = "";
		for (int i = 0; i < dirNameParts.length; i++) {
			if (tmpDirName.endsWith("/") == false) {
				tmpDirName += "/";
			}
			tmpDirName += dirNameParts[i];

			FileObject tmpDirFile = resolveFile(tmpDirName);
			boolean partResult = true;
			if (tmpDirFile.exists() == false) {
				partResult = mkdir(tmpDirName);
			} else if (tmpDirFile.getType() == FileType.FILE) {
				partResult = false;
			}

			if (partResult == false) {
				return false;
			}
		}

		return true;
	}

	public boolean copy(String encSrcFileName, String encTargetFileName) throws IOException {
		FileObject srcFile = resolveFile(encSrcFileName);
		FileObject targetFile = resolveFile(encTargetFileName);

		targetFile.copyFrom(srcFile, null);
		return true;
	}

	public List<EncFSFileInfo> listFiles(String encDirName) throws IOException {
		FileObject srcDir = resolveFile(encDirName);
		FileObject[] children = srcDir.getChildren();

		List<EncFSFileInfo> result = new ArrayList<EncFSFileInfo>(children.length);
		for (int i = 0; i < children.length; i++) {
			result.add(getFileInfo(children[i]));
		}

		return result;
	}

	public InputStream openInputStream(String encSrcFile) throws IOException {
		FileObject srcFile = resolveFile(encSrcFile);
		return srcFile.getContent().getInputStream();
	}

	public OutputStream openOutputStream(String encSrcFile) throws IOException {
		FileObject srcFile = resolveFile(encSrcFile);
		return srcFile.getContent().getOutputStream();
	}

	public EncFSFileInfo getFileInfo(String encSrcFile) throws IOException {
		FileObject srcFile = resolveFile(encSrcFile);
		return getFileInfo(srcFile);
	}

	public boolean exists(String encSrcFile) throws IOException {
		FileObject srcFile = resolveFile(encSrcFile);
		return srcFile.exists();
	}

	private EncFSFileInfo getFileInfo(FileObject fileObject) throws IOException {
		String name = fileObject.getName().getBaseName();
		String volumePath = fileObject.getName().getPath();
		volumePath = volumePath.substring(0, volumePath.length() - (name.length() + "/".length()));
		if (volumePath.equals("")) {
			volumePath = "/";
		}

		boolean isDirectory = fileObject.getType() == FileType.FOLDER;
		long modified = fileObject.getContent().getLastModifiedTime();
		long size = (isDirectory ? 0 : fileObject.getContent().getSize());
		boolean canRead = fileObject.isReadable();
		boolean canWrite = fileObject.isWriteable();
		boolean canExecute = false;

		EncFSFileInfo result = new EncFSFileInfo(name, volumePath, isDirectory, modified, size, canRead, canWrite,
				canExecute);

		return result;
	}

	public EncFSFileInfo createFile(String encTargetFile) throws IOException {
		if (exists(encTargetFile)) {
			throw new IOException("File already exists");
		}

		FileObject targetFile = resolveFile(encTargetFile);
		targetFile.createFile();

		return getFileInfo(targetFile);
	}

}
