package zw.plugin.mock.generator.impl;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;
import zw.plugin.mock.generator.Generator;

/**
 * Created by zhengwei on 2017/12/2.
 */
public class BaseDirGenerator implements Generator {

    protected final Project myProject;
    protected final String myPackageName;
    protected PsiDirectory srcTestJavaDir;
    protected PsiDirectory myCurrentDir;
    protected String className;


    public BaseDirGenerator(AnActionEvent actionEvent) {
        myProject = actionEvent.getData(PlatformDataKeys.EDITOR).getProject();
        PsiJavaFile javaFile = (PsiJavaFile) actionEvent.getData(CommonDataKeys.PSI_FILE);
        myPackageName = javaFile.getPackageName();
        locateRootDir(javaFile.getContainingFile().getParent());
        String fileName = javaFile.getName();
        className = fileName.substring(0, fileName.indexOf(".java"))+"Test";
    }

    @Override
    public void generate() {
        generateDirsBasedOnSuffix();
        new JavaModeFileGenerator(myProject, myCurrentDir, className).generate();
    }

    private void locateRootDir(PsiDirectory currentDir) {
        String currentDirName = currentDir.getName();
        if (currentDirName.equals("src")) {
            PsiDirectory srcDir = currentDir;
            PsiDirectory testDir = moveDirPointer(srcDir, "test");
            srcTestJavaDir = moveDirPointer(testDir, "java");
            myCurrentDir = srcTestJavaDir;
        } else {
            PsiDirectory parent = currentDir.getParent();
            if (parent != null) {
                locateRootDir(parent);
            }
        }
    }

    protected void generateDirsBasedOnSuffix() {
        String[] subPackages = myPackageName.split("\\.");
        for (String subPackage : subPackages) {
            myCurrentDir = moveDirPointer(myCurrentDir, subPackage);
        }
    }


    @NotNull
    protected PsiDirectory moveDirPointer(@NotNull final PsiDirectory currentDir, @NotNull final String subPackage) {
        final PsiDirectory[] subDirectory = {currentDir.findSubdirectory(subPackage)};
        if (subDirectory[0] == null) {
            WriteCommandAction.runWriteCommandAction(currentDir.getProject(), () -> {
                subDirectory[0] = currentDir.createSubdirectory(subPackage);
            });
        }
        return subDirectory[0];
    }

}
