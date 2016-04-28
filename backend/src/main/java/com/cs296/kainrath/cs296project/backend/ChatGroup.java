package com.cs296.kainrath.cs296project.backend;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kainrath on 4/19/16.
 */

// Latidude and Longitude represent the center of the Group
public class ChatGroup {
    private int chatId;
    private List<String> emails;
    private double latitude;
    private double longitude;
    private String interest;
    private int groupSize;

    public ChatGroup(String interest, int chatId, int groupSize, double latitude, double longitude) {
        this.interest = interest;
        this.chatId = chatId;
        this.groupSize = groupSize;
        this.longitude = longitude;
        this.latitude = latitude;
        emails = new ArrayList<>();
    }

    public boolean equals(ChatGroup other) {
        return this.chatId == other.chatId;
    }

    public String getInterest() {
        return interest;
    }

    public int getChatId() {
        return chatId;
    }

    public int getGroupSize() {
        return groupSize;
    }

    public void moveMember(double oldLat, double oldLong, double newLat, double newLong) {
        this.latitude = (this.latitude * groupSize - oldLat + newLat) / groupSize;
        this.longitude = (this.longitude * groupSize - oldLong + newLong) / groupSize;
    }

    // Adding a current member without increasing the group size
    // Used when constructing ChatGroup from database queries
    public void putCurrMember(String email) {
        this.emails.add(email);
    }

    // Adding a new member, will increase group size
    public void addUserToGroup(String email, double latitude, double longitude) {
        // Modifying ChatGroups location
        this.latitude = (this.latitude * groupSize + latitude) / (groupSize + 1);
        this.longitude = (this.longitude * groupSize + longitude) / (groupSize + 1);
        ++groupSize;
        emails.add(email);
    }

    public void removeUserFromGroup(double oldLat, double oldLong) {
        if (groupSize >= 1) {
            this.latitude = (this.latitude * groupSize - oldLat) / (groupSize - 1);
            this.longitude = (this.longitude * groupSize - oldLong) / (groupSize - 1);
            --groupSize;
        } else { // Group now empty
            this.latitude = -200;
            this.longitude = -200;
            groupSize = 0;
        }
    }

    public void removeUserFromGroup(String email, double oldLat, double oldLong) {
        emails.remove(email);
        removeUserFromGroup(oldLat, oldLong);
    }

    public void setGroupLatLong(double groupLat, double groupLong) {
        this.latitude = groupLat;
        this.longitude = groupLong;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public List<String> getEmails() {
        return emails;
    }
}
