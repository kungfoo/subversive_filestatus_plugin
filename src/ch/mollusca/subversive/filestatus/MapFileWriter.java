package ch.mollusca.subversive.filestatus;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class MapFileWriter {
	private final IFile file;

	public MapFileWriter(IFile file) {
		this.file = file;
	}

	public void writeMapToFile(Map<?, ?> map) throws CoreException {
		if (file != null) {

			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				OutputStreamWriter writer = new OutputStreamWriter(out);
				try {
					for (Object key : map.keySet()) {
						writer.append(key.toString())
								.append(": ")
								.append(map.get(key).toString())
								.append("\n");
					}
				} finally {
					writer.close();
				}
				writeFileIfDifferent(file, out.toByteArray());
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, FileStatusPlugin.PLUGIN_ID, "Could not write file: "
						+ file, e));
			}
		}
	}

	/**
	 * Writes the content to the file if there is a difference to the previous
	 * file content.
	 */
	public static void writeFileIfDifferent(IFile file, byte[] content)
			throws CoreException {
		boolean difference = false;
		if (!file.exists()) {
			difference = true;
		} else {
			File localFile = file.getLocation().toFile();
			int oldLength = (int) localFile.length();
			if (content.length != oldLength) {
				difference = true;
			} else {

				byte[] oldContent = new byte[oldLength];
				try {
					InputStream in = new BufferedInputStream(file.getContents());
					try {
						in.read(oldContent);
					} catch (IOException e) {
						difference = true; // try to write the file anyway
					} finally {
						try {
							in.close();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
					if (!difference) {
						difference = !Arrays.equals(oldContent, content);
					}
				} catch (Exception e) {
					difference = true;
				}
			}
		}
		if (difference) {
			writeFile(file, content);
		}
	}

	public static void writeFile(IFile file, byte[] content) throws CoreException {
		ByteArrayInputStream in = new ByteArrayInputStream(content);
		if (file.exists()) {
			file.setContents(in, true, true, null);
		} else {
			createFolders(file);
			file.create(in, true, null);
		}
	}

	/**
	 * Recursively creates the folders above a resource.
	 */
	public static void createFolders(IResource resource) throws CoreException {
		IResource parent = resource.getParent();
		if (parent == null || parent.exists())
			return;
		if (!parent.exists())
			createFolders(parent);
		((IFolder) parent).create(false, false, null);
	}
}
