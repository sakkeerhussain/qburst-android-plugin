package com.qburst.plugin.android.retrofit.forms;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;


import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.qburst.plugin.android.retrofit.RetrofitController;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sakkeer on 11/01/17.
 */
public class Form1 {
    private JButton cancelButton;
    private JButton finishButton;
    private JPanel rootPanel;
    private JTextField baseUrlTextField;
    private JTextField noOfEndPointsTextField;
    private JButton nextButton;
    private JComboBox modulesList;
    private JLabel errorLabel;
    private JComboBox sourceFolderList;

    private RetrofitController controller;
    private Project project;
    private List<Module> modules;
    private List<SourceFolder> sourceFolders;
    private DocumentListener documentListener;
    private Boolean flag = false;

    private Form1() {
        intializeArrayList();
        addActionListeners();
        addDocumenListener();

    }

    private void addDocumenListener() {
        createDocumentListener();
        baseUrlTextField.getDocument().addDocumentListener(documentListener);
        noOfEndPointsTextField.getDocument().addDocumentListener(documentListener);
    }


        private void createDocumentListener() {
            documentListener = new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    if(flag)
                        validData();

                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    if(flag)
                        validData();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    if(flag)
                        validData();
                }
            };
        }


    private void intializeArrayList() {
        modules = new ArrayList<>();
        sourceFolders = new ArrayList<>();
    }

    private void addActionListeners() {
        cancelButtonActionListener();
        nextButtonActionListener();
        modulesListActionListener();
    }
    private boolean validData()
    {
        String baseUrl = baseUrlTextField.getText();
        try {
            URL url = new URL(baseUrl);
            URLConnection conn = url.openConnection();
            conn.connect();
        } catch (MalformedURLException exp) {
            errorLabel.setText("Invalid base URL provided.");
            return false;
        } catch (IOException e1) {}

        String noOfEndPointsString = noOfEndPointsTextField.getText();
        int noOfEndPoints = 0;
        try {
            noOfEndPoints = Integer.parseInt(noOfEndPointsString);
        }catch (Exception exception){
            errorLabel.setText("Invalid number provided for no. of end points.");
            return false;
        }
        errorLabel.setText("");
        return true;

    }


    private void cancelButtonActionListener() {
        cancelButton.addActionListener(e -> controller.hideForm());
    }
    private void nextButtonActionListener() {
        nextButton.addActionListener(e -> {
            flag = true;
            if(!validData())
                return;

            controller.setBaseUrl(baseUrlTextField.getText());
            controller.setNoOfEndPoints(Integer.parseInt(noOfEndPointsTextField.getText()));
            controller.setModuleSelected(modules.get(modulesList.getSelectedIndex()));
            controller.setSourceFolderSelected(sourceFolders.get(sourceFolderList.getSelectedIndex()));
            controller.openForm2(true);
        });
    }
    private void modulesListActionListener() {
        modulesList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSourceFolderList();
            }
        });
    }



    private void updateSourceFolderList() {
        controller.setModuleSelected(modules.get(modulesList.getSelectedIndex()));
        ModuleRootManager root = ModuleRootManager.getInstance(controller.getModuleSelected());
        VirtualFile contentRoot = root.getContentRoots()[0];
        sourceFolders = controller.getSourceRoots(controller.getModuleSelected());
        sourceFolderList.removeAllItems();
        for(SourceFolder sourceFolder: sourceFolders) {

            String contentRootStr = contentRoot.getUrl();
            String sourceRootStr = sourceFolder.getUrl();
            if (sourceRootStr.startsWith(contentRootStr)) {
                sourceFolderList.addItem(sourceRootStr.replaceFirst(contentRootStr, ""));
            }else{
                sourceFolderList.addItem(sourceRootStr);
            }
        }
        this.controller.setSourceFolderSelected(sourceFolders.get(0));

    }

    public static Form1 main(String[] args, JFrame frame) {
        Form1 form = new Form1();
        frame.setContentPane(form.rootPanel);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.getRootPane().setDefaultButton(form.finishButton);
        frame.setVisible(true);
        return form;
    }

    public void setData(RetrofitController controller, Project project, String baseUrl, int noOfEndPoints, Module moduleSelected){
        this.controller = controller;
        this.project = project;
        for (Module module:ModuleManager.getInstance(project).getModules()){
            if (controller.getSourceRoots(module).size() >= 1){
                modules.add(module);
            }
        }

        for (Module module : modules) {
            modulesList.addItem(module.getName());
        }
        this.controller.setModuleSelected(modules.get(0));
        //SOURCE FOLDER LISTING

        updateSourceFolderList();

        this.controller.setTitle("base config");

        if (baseUrl == null || baseUrl.equals("")){
            baseUrlTextField.setText("http://");
        }else{
            baseUrlTextField.setText(baseUrl);
        }
        if (noOfEndPoints == 0){
            noOfEndPointsTextField.setText("");
        }else{
            noOfEndPointsTextField.setText(String.valueOf(noOfEndPoints));
        }
        if (moduleSelected != null){
            modulesList.setSelectedItem(moduleSelected);
        }

    }
}
