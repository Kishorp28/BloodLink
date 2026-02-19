package com.example.blood;

public class User {

    public String name, mobile, email, password, city, address, bloodgroup;

    public User() {
    }

    public User(String name, String mobile, String email, String password,
                String city, String address, String bloodgroup) {

        this.name = name;
        this.mobile = mobile;
        this.email = email;
        this.password = password;
        this.city = city;
        this.address = address;
        this.bloodgroup = bloodgroup;
    }
}
