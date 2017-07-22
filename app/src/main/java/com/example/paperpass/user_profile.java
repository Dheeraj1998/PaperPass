package com.example.paperpass;

import java.util.ArrayList;

/**
 * Created by dheeraj on 22/07/17.
 */

public class user_profile {
    private String Name;
    private ArrayList<String> Image_Url;

    public user_profile() {
      /* Blank default constructor essential for Firebase */
    }

    //Getters and setters
    public String getName() {
        return Name;
    }

    public void setName(String Name) {
        this.Name = Name;
    }

}
