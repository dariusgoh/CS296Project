package com.cs296.kainrath.cs296project.backend;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kainrath on 4/20/16.
 */
// Class simple for sending List<ChatGroup> to the app from this endpoint
// Google endpoints does not allow returning List<> structure.
public class ChatGroupList {
    private List<ChatGroup> chatGroups;

    public ChatGroupList(List<ChatGroup> chatGroups) {
        this.chatGroups = chatGroups;
    }

    public ChatGroupList() {
        chatGroups = new ArrayList<ChatGroup>();
    }

    public boolean isEmpty() {
        return chatGroups == null || chatGroups.isEmpty();
    }

    public void addChatGroup(ChatGroup group) {
        chatGroups.add(group);
    }

    public void addChatGroups(List<ChatGroup> groups) {
        chatGroups.addAll(groups);
    }

    public void setChatGroups(List<ChatGroup> groups) {
        this.chatGroups = groups;
    }

    public List<ChatGroup> getChatGroups() {
        return chatGroups;
    }
}
