package ch.mollusca.subversive.filestatus;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class FileStatusPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ch.mollusca.subversive.filestatus"; //$NON-NLS-1$

	// The shared instance
	private static FileStatusPlugin plugin;

	/**
	 * The constructor
	 */
	public FileStatusPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for (IProject project : root.getProjects()) {
			if (project.getName().equals("subversive-bug")) {
				addBuilder(project);
			}
		}
	}

	private void addBuilder(IProject project) throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();
		boolean found = false;

		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(IncrementalBuilder.ID)) {
				found = true;
				break;
			}
		}
		if (!found) {
			// add builder to project
			ICommand command = desc.newCommand();
			command.setBuilderName(IncrementalBuilder.ID);
			ICommand[] newCommands = new ICommand[commands.length + 1];

			// Add it before other builders.
			System.arraycopy(commands, 0, newCommands, 1, commands.length);
			newCommands[0] = command;
			desc.setBuildSpec(newCommands);
			project.setDescription(desc, null);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static FileStatusPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
