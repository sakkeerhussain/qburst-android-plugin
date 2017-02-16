package com.qburst.plugin.android.retrofit;

import android.app.assist.AssistStructure;
import com.android.tools.idea.gradle.parser.BuildFileKey;
import com.android.tools.idea.gradle.parser.Dependency;
import com.android.tools.idea.gradle.parser.GradleBuildFile;
import com.android.tools.idea.gradle.parser.GradleSettingsFile;
import com.android.tools.idea.gradle.project.GradleProjectImporter;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.roots.impl.SourceFolderImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.SmartList;
import com.qburst.plugin.android.retrofit.forms.Form1;
import com.qburst.plugin.android.retrofit.forms.Form2;
import com.qburst.plugin.android.retrofit.forms.Form3;
import com.qburst.plugin.android.utils.classutils.ClassManager;
import com.qburst.plugin.android.utils.classutils.ClassModel;
import com.qburst.plugin.android.utils.classutils.FieldModel;
import com.qburst.plugin.android.utils.http.HTTPUtils;
import com.qburst.plugin.android.utils.http.UrlParamModel;
import com.qburst.plugin.android.utils.log.Log;
import com.qburst.plugin.android.utils.notification.NotificationManager;
import com.qburst.plugin.android.utils.string.StringUtils;
import com.qburst.plugin.android.utils.string.UrlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.*;

/**
 * Created by sakkeer on 11/01/17.
 */
public class RetrofitController {
    private static final String TAG = "RetrofitController";

    private Project project;
    private Module moduleSelected;
    private String baseUrl;
    private String packageName;
    private int noOfEndPoints;
    private List<EndPointDataModel> endPointDataModelList;

    public RetrofitController() {
        endPointDataModelList = new ArrayList<>();
        packageName = Constants.PACKAGE_NAME_RETROFIT;
    }

    private JFrame frame;

    private SourceFolder sourceFolderSelected;

    public void integrateRetrofitAction(AnActionEvent event) {
        this.project = event.getData(PlatformDataKeys.PROJECT);
        this.frame = new JFrame("Retrofit");
        openForm1();
    }

    public void openForm1(){
        Log.d(TAG, "openForm1() called");
        String[] flags = new String[0];
        Form1 form1 = Form1.main(flags, frame);
        form1.setData(this, project, baseUrl, packageName, noOfEndPoints, moduleSelected);
    }

    public void openForm2(boolean fromStart){
        Log.d(TAG, "openForm2() called");
        String[] flags = new String[0];
        Form2 form = Form2.main(flags, frame);
        form.setData(this);
        if (fromStart) {
            form.setCurrentEndPoint(1);
        }else{
            form.setCurrentEndPoint(this.noOfEndPoints);
        }
    }

    public void openForm3(){
        Log.d(TAG, "openForm3() called");
        String[] flags = new String[0];
        Form3 form3 = Form3.main(flags, frame);
        form3.setData(this);
    }

    public void hideForm(){
        Log.d(TAG, "hideForm() called");
        frame.setVisible(false);
    }

    public void setTitle(String title){
        frame.setTitle("Retrofit - "+title);
    }

    public void setModuleSelected(Module moduleSelected) {
        this.moduleSelected = moduleSelected;
    }
    public void setSourceFolderSelected(SourceFolder sourceFolderSelected){
        this.sourceFolderSelected = sourceFolderSelected;
    }

    public Module getModuleSelected()
    {
        return moduleSelected;
    }

    public void setEndPointDataModel(EndPointDataModel endPointDataModel) {
        boolean alreadyExisting = false;
        for (EndPointDataModel endPointDataModelIter : endPointDataModelList) {
            if (endPointDataModelIter.getEndPointNo() == endPointDataModel.getEndPointNo()) {
                alreadyExisting = true;
                this.endPointDataModelList.set(this.endPointDataModelList.indexOf(endPointDataModelIter), endPointDataModel);
            }
        }
        if (!alreadyExisting) {
            this.endPointDataModelList.add(endPointDataModel);
        }
    }

    public EndPointDataModel getEndPointDataModel(int position) {
        for (EndPointDataModel endPointDataModel : endPointDataModelList) {
            if (endPointDataModel.getEndPointNo() == position) {
                return endPointDataModel;
            }
        }
        return new EndPointDataModel();
    }

    public int getNoOfEndPoints() {
        return noOfEndPoints;
    }

    public void integrateRetrofit() {
        hideForm();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Integrating Retrofit") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setFraction(0);
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    String errorMessage = null;
                    if (!addDependencies(indicator)) {
                        errorMessage = "Dependency injection failed!";
                    } else if (!createPackage(indicator)) {
                        errorMessage = "Creating package failed!";
                    } else if (!createClasses(indicator)) {
                        errorMessage = "Creating Class failed!";
                    }

                    if (errorMessage == null) {
                        NotificationManager.get().integrationCompletedNotification(project);
                        ApplicationManager.getApplication().invokeLater(() -> GradleProjectImporter.getInstance().requestProjectSync(project, null));
                    } else {
                        NotificationManager.get().integrationFailedNotification(project, errorMessage);
                    }
                });
            }
        });
    }

    private boolean addDependencies(ProgressIndicator indicator) {
        indicator.setText("Adding dependencies");
        if (moduleSelected == null) { return false; }
        String moduleGradlePath = GradleSettingsFile.getModuleGradlePath(moduleSelected);
        if (moduleGradlePath == null) { return false; }
        GradleSettingsFile mySettingsFile = GradleSettingsFile.get(project);
        final GradleBuildFile buildFile;
        if (mySettingsFile == null) { return false; }
        buildFile = mySettingsFile.getModuleBuildFile(moduleGradlePath);
        if (buildFile == null) { return false; }
        List<Dependency> value = (List<Dependency>)buildFile.getValue(BuildFileKey.DEPENDENCIES);
        final List<Dependency> dependencies = value != null ? value : new ArrayList<Dependency>();

        boolean added = false;
        //new MavenArtifact
        Dependency retrofit = new Dependency(Dependency.Scope.COMPILE,
                Dependency.Type.EXTERNAL,
                Constants.DEPENDENCY_RETROFIT);
        if (!dependencies.contains(retrofit)) {
            dependencies.add((retrofit));
            added = true;
        }
        Dependency retrofitGson = new Dependency(Dependency.Scope.COMPILE,
                Dependency.Type.EXTERNAL,
                Constants.DEPENDENCY_RETROFIT_GSON);
        if (!dependencies.contains(retrofitGson)) {
            dependencies.add((retrofitGson));
            added = true;
        }
        Dependency retrofitLogging = new Dependency(Dependency.Scope.COMPILE,
                Dependency.Type.EXTERNAL,
                Constants.DEPENDENCY_RETROFIT_LOGGING);
        if (!dependencies.contains(retrofitLogging)) {
            dependencies.add((retrofitLogging));
            added = true;
        }
        if (added) {
            buildFile.setValue(BuildFileKey.DEPENDENCIES, dependencies);
        }
        return true;
    }

    private boolean createPackage(ProgressIndicator indicator){
        indicator.setText("Creating directories");
        indicator.setFraction(0.2);
        DirectoryManager directoryManager = DirectoryManager.get();

        String[] dirNames = packageName.split("\\.");
        VirtualFile dir = sourceFolderSelected.getFile();
        for (String dirName : dirNames) {
            dir = directoryManager.createDirectory(dir, dirName);
            if (dir == null){ return false; }
        }
        VirtualFile modelDir = directoryManager.createDirectory(dir, "model");
        if (modelDir == null){ return false; }
        if (directoryManager.createDirectory(modelDir, "request") == null){ return false; }
        if (directoryManager.createDirectory(modelDir, "response") == null){ return false; }

        return true;
    }

    private boolean createClasses(ProgressIndicator indicator) {

        PsiPackage pkgRequest = JavaPsiFacade.getInstance(project).findPackage(packageName  + Constants.PACKAGE_NAME_RETROFIT_REQUEST);
        PsiPackage pkgResponse = JavaPsiFacade.getInstance(project).findPackage(packageName  + Constants.PACKAGE_NAME_RETROFIT_RESPONSE);
        if (pkgRequest == null || pkgResponse == null){
            //NotificationManager.get().integrationFailedNotification(project, errorMessage);
            return false;
        }

        indicator.setText("Creating request model classes");
        indicator.setFraction(0.4);
        if (!createRequestModelClasses(pkgRequest.getDirectories()[0], indicator)){
            return false;
        }
        indicator.setText("Creating response model classes");
        indicator.setFraction(0.6);
        if (!createResponseModelClasses(pkgResponse.getDirectories()[0], indicator)){
            return false;
        }


        //get retrofit directory
        PsiPackage pkg = JavaPsiFacade.getInstance(project).findPackage(Constants.PACKAGE_NAME_RETROFIT);
        if (pkg == null){
            return false;
        }
        PsiDirectory psiDirectory = pkg.getDirectories()[0];
        if (!createServiceClass(psiDirectory, indicator)){
            return false;
        }
        if (!createManagerClass(psiDirectory, indicator)){
            return false;
        }
        return true;
    }

    private boolean createRequestModelClasses(PsiDirectory psiDirectoryRequest, ProgressIndicator indicator) {
        for (EndPointDataModel endPointDataModel : endPointDataModelList) {
            if (new HTTPUtils().isPayloadNotSupportingMethod(endPointDataModel.getMethod())) {
                return true;
            }
            ClassModel classModel = new JsonManager().getRequestClassModel(endPointDataModel,
                    project, psiDirectoryRequest);
            indicator.setText("Creating "+classModel.getName());
            classModel.setPackageName(packageName  + Constants.PACKAGE_NAME_RETROFIT_REQUEST);
            if (!ClassManager.get().createClass(classModel)){
                return false;
            }
        }
        return true;
    }

    private boolean createResponseModelClasses(PsiDirectory psiDirectoryResponse, ProgressIndicator indicator) {
        // TODO: 10/02/17 Create base class for response model accoding to comon fields of all APIs.
        for (EndPointDataModel endPointDataModel : endPointDataModelList) {
            ClassModel classModel = new JsonManager().getResponseClassModel(endPointDataModel,
                    project, psiDirectoryResponse);
            classModel.setPackageName(Constants.PACKAGE_NAME_RETROFIT_RESPONSE);
            indicator.setText("Creating "+classModel.getName());
            if (!ClassManager.get().createClass(classModel)){
                return false;
            }
        }
        return true;
    }

    private boolean createServiceClass(PsiDirectory psiDirectory, ProgressIndicator indicator) {

        //Creating service class
        indicator.setText("Creating service class");
        indicator.setFraction(0.8);
        ClassModel classModel = new ClassModel(project, psiDirectory, Constants.className.SERVICE, ClassModel.Type.INTERFACE);
        classModel.setPackageName(Constants.PACKAGE_NAME_RETROFIT);
        for (int i = 0; i < noOfEndPoints; i++) {
            EndPointDataModel endPointData = endPointDataModelList.get(i);

            String url = endPointData.getEndPointUrl();
            String prettyUrl = new UrlStringUtil().getPrettyUrl(url);

            String annotationString = String.format(Constants.ServiceInterface.ANNOTATION_FORMAT,
                    endPointData.getMethod(), prettyUrl);

            String requestParamsString = "";
            String requestClassName = endPointData.getSimpleRequestModelClassName();
            if (requestClassName != null && !requestClassName.equals("")) {
                String requestClassObjName = new StringUtils().lowersFirstLetter(requestClassName);
                String reqBodyStr = String.format(Constants.ServiceInterface.REQUEST_PARAM_BODY,
                        requestClassName,
                        requestClassObjName);
                requestParamsString = requestParamsString.concat(reqBodyStr);
            }

            List<UrlParamModel> pathParams = new UrlStringUtil().getListOfPathParams(url);
            for (UrlParamModel pathParam:pathParams){
                String reqQueryStr = String.format(Constants.ServiceInterface.REQUEST_PARAM_PATH,
                        pathParam.getKey(), new UrlStringUtil().getParamType(pathParam.getValue()),
                        new StringUtils().lowersFirstLetter(pathParam.getKey()));
                requestParamsString = requestParamsString.concat(reqQueryStr);
            }

            List<UrlParamModel> queryParams = new UrlStringUtil().getListOfQueryParams(url);
            for (UrlParamModel queryParam:queryParams){
                String reqQueryStr = String.format(Constants.ServiceInterface.REQUEST_PARAM_QUERY,
                        queryParam.getKey(), new UrlStringUtil().getParamType(queryParam.getValue()),
                        new StringUtils().lowersFirstLetter(queryParam.getKey()));
                requestParamsString = requestParamsString.concat(reqQueryStr);
            }
            if (requestParamsString.length() > 1
                    && requestParamsString.charAt(requestParamsString.length() - 1) == ' '
                    && requestParamsString.charAt(requestParamsString.length() - 2) == ',') {
                requestParamsString = requestParamsString.substring(0, requestParamsString.length()-2);
            }

            String methodString = String.format(Constants.ServiceInterface.METHOD,
                    annotationString,
                    packageName + endPointData.getResponseModelClassName(),
                    endPointData.getEndPointName(),
                    requestParamsString);

            classModel.addMethod(methodString);
        }

        return ClassManager.get().createClass(classModel);
    }

    private boolean createManagerClass(PsiDirectory psiDirectory, ProgressIndicator indicator) {
        indicator.setText("Creating manager class");
        indicator.setFraction(1.0);
        ClassModel classModel = new ClassModel(project, psiDirectory, Constants.className.MANAGER, ClassModel.Type.CLASS);
        classModel.setPackageName(Constants.PACKAGE_NAME_RETROFIT);
        FieldModel staticField = new FieldModel(classModel, "private", true, true,
                "String", "BASE_URL");
        staticField.setValue(new StringUtils().getValueAsString(baseUrl));
        classModel.addField(staticField);
        classModel.addMethod(Constants.GET_INSTANCE_METHOD);
        return ClassManager.get().createClass(classModel);
    }

    public List<SourceFolder> getSourceRoots(Module moduleSelected) {
        List<SourceFolder> result = new SmartList<>();
        ModuleRootManager root = ModuleRootManager.getInstance(moduleSelected);
        for (ContentEntry contentEntry : root.getContentEntries()) {
            Set<? extends JpsModuleSourceRootType<?>> rootType = Collections.singleton(JavaSourceRootType.SOURCE);
            final List<SourceFolder> sourceFolders = contentEntry.getSourceFolders(rootType);
            for (SourceFolder sourceFolder : sourceFolders) {
                if (sourceFolder == null) continue;
                if (sourceFolder.getFile() == null) continue;
                if (isForGeneratedSources((SourceFolderImpl) sourceFolder)) continue;
                result.add(sourceFolder);
            }
        }
        return result;
    }

    private boolean isForGeneratedSources(SourceFolderImpl sourceFolder) {
        JavaSourceRootProperties properties = sourceFolder.getJpsElement().getProperties(JavaModuleSourceRootTypes.SOURCES);
        return properties != null && properties.isForGeneratedSources();
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setNoOfEndPoints(int noOfEndPoints) {
        this.noOfEndPoints = noOfEndPoints;
    }

}
