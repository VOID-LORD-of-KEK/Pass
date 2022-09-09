package com.example.pass.db;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class User {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "юзернейм")
    public String username;

    @ColumnInfo(name = "зафишрованные пароли")
    public String password;

}
