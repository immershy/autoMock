package zw.plugin.mock.generator.impl;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;

/**
 * Created by zhengwei on 2017/12/3.
 */
public class JavaModeFileGenerator extends BaseFileGenerator {

    public JavaModeFileGenerator(Project myProject, PsiDirectory myCurrentDir,String fileName) {
        super(myProject,myCurrentDir,fileName);
    }
}
