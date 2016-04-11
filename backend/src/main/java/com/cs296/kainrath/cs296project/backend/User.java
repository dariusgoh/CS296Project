package com.cs296.kainrath.cs296project.backend;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by kainrath on 3/25/16.
 */

@Entity
public class User {
    @Id
    String user_id;
    @Index
    String user_email;
    
    Set<String> interests;

    public User() {
        this(null, null);
    }

    public User(String id, String email) {
        interests = new TreeSet<String>();
        user_id = id;
        user_email = email;
    }

    public String getId() { return this.user_id; }

    public void setId(String id) {
        this.user_id = id;
    }

    public String getEmail() {
        return this.user_email;
    }

    public void setEmail(String email) {
        this.user_email = email;
    }

    public Set<String> getInterests() {
        return this.interests;
    }

    public void addInterests(List<String> interests) {
        this.interests.addAll(interests);
    }

    public void addInterests(String interest) { this.interests.add(interest); }

    public void setInterests(Set<String> interests) { this.interests = interests; }
}
