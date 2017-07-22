package com.example.paperpass;

/**
 * Created by dheeraj on 22/07/17.
 */

public class QP_Post {

    private String Image_Id;
    private String Image_Url;
    private String Course_Code;
    private String User_Id;

    public QP_Post() {

    }

    public QP_Post(String Image_Url, String Course_Code, String User_Id) {
        this.Image_Url = Image_Url;
        this.Course_Code = Course_Code;
        this.User_Id = User_Id;
    }

    public String getImage_Id() {
        return Image_Id;
    }

    public void setImage_Id(String image_Id) {
        Image_Id = image_Id;
    }

    public String getUser_Id() {
        return User_Id;
    }

    public void setUser_Id(String user_Id) {
        User_Id = user_Id;
    }

    public String getImage_Url() {
        return Image_Url;
    }

    public void setImage_Url(String image_Url) {
        Image_Url = image_Url;
    }

    public String getCourse_Code() {
        return Course_Code;
    }

    public void setCourse_Code(String course_Code) {
        Course_Code = course_Code;
    }

}
