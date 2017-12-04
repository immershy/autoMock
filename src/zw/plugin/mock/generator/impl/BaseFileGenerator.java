package zw.plugin.mock.generator.impl;

import static zw.plugin.mock.EventLogger.log;

import com.intellij.ide.fileTemplates.JavaTemplateUtil;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import zw.plugin.mock.generator.Generator;

/**
 * Created by zhengwei on 2017/12/3.
 */
public class BaseFileGenerator implements Generator {

    protected final Project myProject;
    protected PsiElementFactory myFactory;
    protected PsiDirectory myCurrentDir;
    protected String fileName;
    protected PsiShortNamesCache myShortNamesCache;
    protected GlobalSearchScope myProjectScope;
    protected JavaDirectoryService myDirectoryService;

    public BaseFileGenerator(Project project, PsiDirectory myCurrentDir, String fileName) {
        this.myProject = project;
        this.myFactory = JavaPsiFacade.getElementFactory(project);
        this.myCurrentDir = myCurrentDir;
        this.fileName = fileName;
        myShortNamesCache = PsiShortNamesCache.getInstance(project);
        myProjectScope = GlobalSearchScope.projectScope(project);
        myDirectoryService = JavaDirectoryService.getInstance();
    }

    @Override
    public void generate() {
        generateFile(myCurrentDir, fileName, JavaTemplateUtil.INTERNAL_CLASS_TEMPLATE_NAME, (javaFile, psiClass) -> {
            String clzName = fileName.substring(0, fileName.lastIndexOf("Test"));

            log("开始为类：" + clzName + " 添加Mockito测试用例");
            // 添加被测试类
            String mockFieldName =
                String.valueOf(clzName.charAt(0)).toLowerCase() + clzName.substring(1, clzName.length());
            if (psiClass.findFieldByName(mockFieldName, false) == null) {
                PsiType psiType = PsiType.getTypeByName(clzName, myProject, myProjectScope);
                PsiField psiField = myFactory.createField(mockFieldName, psiType);
                psiField.getModifierList().addAnnotation("InjectMocks");
                psiClass.add(psiField);
            }

            PsiClass origianlClz = myShortNamesCache.getClassesByName(clzName, myProjectScope)[0];
            // 添加mock属性
            for (PsiField field : origianlClz.getFields()) {
                log("新增Mock属性：" + field.getName());
                if (psiClass.findFieldByName(field.getName(), false) == null) {
                    if (!field.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                        PsiField psiField1 = myFactory.createField(field.getName(), field.getType());
                        psiField1.getModifierList().addAnnotation("Mock");
                        psiClass.add(psiField1);
                    }
                }
            }

            // 添加被测试方法
            for (PsiMethod method : origianlClz.getMethods()) {
                if (!method.getModifierList().hasModifierProperty(PsiModifier.PUBLIC)) {
                    continue;
                }
                String testMethod = method.getName() + "Test";
                log("开始新增方法：" + testMethod);
                PsiMethod method1 = myFactory.createMethod(testMethod, PsiType.VOID);
                if (psiClass.findMethodBySignature(method1, false) == null) {
                    method1.getModifierList().addAnnotation("Test");
                    String paramStr = "";
                    for (PsiParameter psiParameter : method.getParameterList().getParameters()) {
                        PsiClass paramClz = ((PsiClassType) psiParameter.getType()).resolve();
                        PsiImportStatement importStatement = myFactory
                            .createImportStatement(paramClz);
                        javaFile.getImportList().add(importStatement);
                        PsiExpression init = myFactory
                            .createExpressionFromText("new " + paramClz.getName() + "()", psiParameter);
                        PsiDeclarationStatement psiDeclarationStatement = myFactory
                            .createVariableDeclarationStatement(psiParameter.getName(), psiParameter.getType(),
                                init);
                        method1.getBody().add(psiDeclarationStatement);
                        paramStr =
                            paramStr.length() == 0 ? psiParameter.getName() : paramStr + "," + psiParameter.getName();
                    }

                    PsiCodeBlock block = myFactory
                        .createCodeBlockFromText(
                            "{\n" + mockFieldName + "." + method.getName() + "(" + paramStr + ");" + "\n}", null);
                    block.getLBrace().delete();
                    block.getRBrace().delete();
                    method1.getBody().add(block);
                    psiClass.add(method1);
                }
            }

            FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
            OpenFileDescriptor fileDescriptor = new OpenFileDescriptor(myProject, javaFile.getVirtualFile());
            fileEditorManager.openTextEditor(fileDescriptor, true);
            log("BaseFileGenerator: " + clzName + " 文件创建完成");
        });
    }

    protected void generateFile(final PsiDirectory directory, final String fileName, final String type,
        final onFileGeneratedListener listener) {
        WriteCommandAction.runWriteCommandAction(myProject, () -> {
            String fixedFileName = fileName;
            PsiClass[] psiClasses = myShortNamesCache.getClassesByName(fixedFileName, myProjectScope);//NotNull
            PsiClass psiClass;
            PsiJavaFile javaFile;
            if (psiClasses.length != 0) {//if the class already exist.
                psiClass = psiClasses[0];
                javaFile = (PsiJavaFile) psiClass.getContainingFile();
                listener.onJavaFileGenerated(javaFile, psiClass);
                return;
            }
            psiClass = myDirectoryService.createClass(directory, fixedFileName, type);
            PsiClass baseClz = myShortNamesCache.getClassesByName("MockitoTestBase", myProjectScope)[0];
            psiClass.getExtendsList().add(myFactory.createClassReferenceElement(baseClz));
            javaFile = (PsiJavaFile) psiClass.getContainingFile();
            PsiPackage psiPackage = myDirectoryService.getPackage(directory);

            javaFile.setPackageName(psiPackage.getQualifiedName());
            javaFile.getImportList().add(myFactory.createImportStatementOnDemand("org.mockito"));
            javaFile.getImportList().add(myFactory.createImportStatementOnDemand("org.junit"));

            log("BaseFileGenerator: " + fixedFileName + " 文件创建完成");
            listener.onJavaFileGenerated(javaFile, psiClass);
        });
    }

    @FunctionalInterface
    protected interface onFileGeneratedListener {

        void onJavaFileGenerated(PsiJavaFile javaFile, PsiClass psiClass);
    }

}
