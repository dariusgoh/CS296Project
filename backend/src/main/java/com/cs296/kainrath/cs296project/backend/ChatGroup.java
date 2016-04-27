package com.cs296.kainrath.cs296project.backend;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kainrath on 4/19/16.
 */

// Latidude and Longitude represent the center of the Group
public class ChatGroup {
    private int chatId;
    private List<String> user_ids;
    private double latitude;
    private double longitude;
    private String interest;
    private int groupSize;
    private List<String> messages;

    public ChatGroup(String interest, int chatId, int groupSize, double latitude, double longitude) {
        this.interest = interest;
        this.chatId = chatId;
        this.groupSize = groupSize;
        this.longitude = longitude;
        this.latitude = latitude;
        user_ids = new ArrayList<>();
    }

    /*
    public ChatGroup(String interest, String user_id, String user_token, double latitude, double longitude) {
        this.interest = interest;
        this.latitude = latitude;
        this.longitude = longitude;
        user_ids = new ArrayList<String>();
        user_tokens = new ArrayList<String>();
        user_ids.add(user_id);
        user_tokens.add(user_token);
    }

    public ChatGroup(String interest, List<String> user_ids, List<String> user_tokens, double latitude, double longitude) {
        this.interest = interest;
        this.user_tokens = user_tokens;
        this.user_ids = user_ids;
        this.latitude = latitude;
        this.longitude = longitude;
    }*/

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

    // USE FOR ADDING A CURRENT MEMBER, WILL NOT INCREASE GROUPSIZE
    public void putCurrMember(String user_id) {
        this.user_ids.add(user_id);
    }

    // USE FOR ADDING A NEW MEMBER, WILL INCREMENT GROUPSIZE
    public void addUserToGroup(String user_id, double latitude, double longitude) {
        this.latitude = (this.latitude * groupSize + latitude) / (groupSize + 1);
        this.longitude = (this.longitude * groupSize + longitude) / (groupSize + 1);
        ++groupSize;
        user_ids.add(user_id);
    }

    public void removeUserFromGroup(String user_id, double oldLat, double oldLong) {
        if (groupSize >= 1) {
            this.latitude = (this.latitude * groupSize - oldLat) / (groupSize - 1);
            this.longitude = (this.longitude * groupSize - oldLong) / (groupSize - 1);
            --groupSize;
            user_ids.remove(user_id);
        } else { // Empty Group
            this.latitude = 0;
            this.longitude = 0;
            groupSize = 0;
            user_ids.clear();
        }
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

    public List<String> getUserIds() {
        return user_ids;
    }

    public List<String> getAllMessages() {
        return messages;
    }

    public String getLastMessage() {
        if (messages != null && !messages.isEmpty()) {
            return messages.get(messages.size() - 1);
        }
        return null;
    }

    public void addMessage(String message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
    }
}
