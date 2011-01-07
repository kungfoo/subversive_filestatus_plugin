package ch.mollusca.subversive.filestatus;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.PackageFragment;

@SuppressWarnings("restriction")
public class IncrementalBuilder extends IncrementalProjectBuilder {

	public static final String ID = FileStatusPlugin.PLUGIN_ID + ".builder";

	private Map<String, Integer> stringLiteralsByResourceName = new TreeMap<String, Integer>();
	private IFile offendingFile;

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		stringLiteralsByResourceName = new TreeMap<String, Integer>();
		deleteOffendingFile();
		super.clean(monitor);
	}

	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		switch (kind) {
		case AUTO_BUILD:
		case INCREMENTAL_BUILD:
			performIncrementalBuild();
			break;
		case FULL_BUILD:
			performFullBuild();
			break;
		default:
			break;
		}
		return null;
	}

	private void performFullBuild() throws CoreException {
		IProject project = getProject();
		IJavaProject javaProject = getJavaProject(project);
		if (javaProject != null && project.getName().equals("subversive-bug")) {
			visitJavaProject(javaProject);
			recreateOffendingFile();
		}
	}

	private void visitJavaProject(IJavaProject project) throws JavaModelException {
		IPackageFragment[] packageFragments = project.getPackageFragments();
		for (IPackageFragment fragment : packageFragments) {
			// we do not care about package fragments in jars.
			if (fragment.getClass().equals(PackageFragment.class)) {
				IJavaElement[] javaElements = fragment.getChildren();
				visitAllJavaElements(fragment, javaElements);
			}
		}
	}

	private void visitAllJavaElements(IPackageFragment fragment, IJavaElement[] javaElements) throws JavaModelException {
		for (IJavaElement element : javaElements) {
			if (element.getElementType() == IJavaElement.COMPILATION_UNIT) {
				ICompilationUnit unit = (ICompilationUnit) element;
				visitCompilationUnit(unit);
			}
		}
	}

	private void visitCompilationUnit(ICompilationUnit unit) throws JavaModelException {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(unit);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
		CountsStringLiteralsVisitor visitor = new CountsStringLiteralsVisitor();
		astRoot.accept(visitor);
		stringLiteralsByResourceName.put(unit.getResource().getName(), visitor.getNumberOfStringLiterals());
	}

	private final class CountsStringLiteralsVisitor extends ASTVisitor {
		private int numberOfStringLiterals = 0;

		@Override
		public boolean visit(StringLiteral node) {
			numberOfStringLiterals++;
			return false;
		}

		public int getNumberOfStringLiterals() {
			return numberOfStringLiterals;
		}
	}

	private static IJavaProject getJavaProject(IProject project) throws JavaModelException {
		IJavaModel javamodel = createJavaModel();
		return javamodel.getJavaProject(project.getName());
	}

	public static IJavaModel createJavaModel() {
		return JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
	}

	private void performIncrementalBuild() throws CoreException {
		IResourceDelta delta = getDelta(getProject());
		delta.accept(new IResourceDeltaVisitor() {
			public boolean visit(IResourceDelta delta) throws CoreException {
				if (isJavaFile(delta)) {
					if (delta.getResource().exists()) {
						ICompilationUnit unit = getCompilationUnit((IFile) delta.getResource());
						visitCompilationUnit(unit);
					} else {
						stringLiteralsByResourceName.remove(delta.getResource().getName());
					}
					return false;
				} else {
					return true;
				}
			}
		});
		recreateOffendingFile();
	}

	private boolean isJavaFile(IResourceDelta delta) {
		return delta.getResource().getType() == IResource.FILE && delta.getResource().getName().endsWith(".java");
	}

	public static ICompilationUnit getCompilationUnit(IFile file) {
		try {
			return JavaModelManager.createCompilationUnitFrom(file,
					getJavaProject(file.getProject()));
		} catch (JavaModelException e) {
			throw new RuntimeException("Could not get compilation unit for " + file, e);
		}
	}

	private void recreateOffendingFile() throws CoreException {
		offendingFile = getProject().getFile("/offending-file");
		MapFileWriter mapFileWriter = new MapFileWriter(offendingFile);
		mapFileWriter.writeMapToFile(stringLiteralsByResourceName);
	}

	private void deleteOffendingFile() throws CoreException {
		offendingFile.delete(true, true, null);
	}

}
