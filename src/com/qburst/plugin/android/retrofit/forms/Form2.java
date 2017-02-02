package com.qburst.plugin.android.retrofit.forms;

import com.qburst.plugin.android.retrofit.EndPointDataModel;
import com.qburst.plugin.android.retrofit.RetrofitController;
import com.qburst.plugin.android.utils.log.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.Border;

/**
 * Created by sakkeer on 11/01/17.
 */
public class Form2 {
    private static final String TAG = "Form2";
    private int currentEndPoint;

    private JButton cancelButton;
    private JButton finishButton;
    private JPanel rootPanel;
    private JButton nextButton;
    private JButton previousButton;
    private JTextField endPointUrlTextField;
    private JComboBox methodChooserComboBox;
    private JTextArea requestModelTextArea;
    private JTextArea responseModelTextArea;
    private JButton formatResponseButton;
    private JButton formatRequestButton;
    private JTextField endPointNameTextField;

    private RetrofitController controller;

    private Form2() {
        cancelButton.addActionListener(e -> controller.hideForm());
        /*finishButton.addActionListener(e -> {
        });*/
        previousButton.addActionListener(e -> {
            storeData();
            if (currentEndPoint <= 1){
                controller.openForm1();
            }else{
                currentEndPoint--;
                setUpView();
            }
        });
        nextButton.addActionListener(e -> {
            storeData();
            if (currentEndPoint >= controller.getNoOfEndPoints()){
                controller.openForm3();
            }else{
                currentEndPoint++;
                setUpView();
            }
        });
        formatRequestButton.addActionListener(e -> {
            String json = requestModelTextArea.getText();
            requestModelTextArea.setText(formatJson(json));
        });
        formatResponseButton.addActionListener(e -> {
            String json = responseModelTextArea.getText();
            responseModelTextArea.setText(formatJson(json));
        });
    }

    private String formatJson(String json){
        json = json.trim();
        if (json.startsWith("{")) {
            JSONObject jsonObject = new JSONObject(json);
            return jsonObject.toString(4);
        } else if (json.startsWith("[")) {
            JSONArray jsonArray = new JSONArray(json);
            return jsonArray.toString(4);
        }else{
            return json;
        }
    }

    private void storeData() {
        EndPointDataModel endPointData = new EndPointDataModel();
        endPointData.setEndPointNo(currentEndPoint);
        // TODO: 23/01/17 Should add empty validation.
        endPointData.setEndPointName(endPointNameTextField.getText());
        endPointData.setEndPointUrl(endPointUrlTextField.getText());
        endPointData.setMethod(methodChooserComboBox.getSelectedItem().toString());
        // TODO: 23/01/17 Should add json validation.
        endPointData.setRequestModel(requestModelTextArea.getText());
        endPointData.setResponseModel(responseModelTextArea.getText());
        controller.setEndPointDataModel(endPointData);
    }

    private void setUpView() {
        controller.setTitle("End point "+currentEndPoint);
        EndPointDataModel endPointData = controller.getEndPointDataModel(currentEndPoint);
        endPointNameTextField.setText(endPointData.getEndPointName());
        endPointUrlTextField.setText(endPointData.getEndPointUrl());
        methodChooserComboBox.setSelectedItem(endPointData.getMethod());
        requestModelTextArea.setText(endPointData.getRequestModel());
        responseModelTextArea.setText(endPointData.getResponseModel());
    }

    public static Form2 main(String[] args, JFrame frame) {
        Log.d(TAG,"Creating new form 2....");
        Form2 form = new Form2();
        frame.setContentPane(form.rootPanel);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.getRootPane().setDefaultButton(form.finishButton);
        frame.setVisible(true);

        Border border = new JTextField().getBorder();
        //form.methodChooserComboBox.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        form.requestModelTextArea.setBorder(border);
        form.responseModelTextArea.setBorder(border);

        return form;
    }

    public void setData(RetrofitController controller){
        this.controller = controller;
    }

    public void setCurrentEndPoint(int currentEndPoint) {
        this.currentEndPoint = currentEndPoint;
        setUpView();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
