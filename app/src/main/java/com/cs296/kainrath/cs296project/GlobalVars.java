package com.cs296.kainrath.cs296project;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.ListView;

import com.cs296.kainrath.cs296project.backend.locationApi.LocationApi;
import com.cs296.kainrath.cs296project.backend.locationApi.model.ChatGroup;
import com.cs296.kainrath.cs296project.backend.userApi.UserApi;
import com.cs296.kainrath.cs296project.backend.userApi.model.User;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by kainrath on 3/24/16.
 */
public class GlobalVars extends Application {
    private static String TAG = "GlobalVars";

    // For async tasks
    public static UserApi userApi = null;
    public static LocationApi locationApi = null;

    // For displaying chatgroups and messages
    public static Map<Integer, List<Pair<String, String>>> chatMessageMap;
    public static List<ChatGroup> chatGroups = new ArrayList<>();

    private static User user = null;
    private static boolean failed = false;

    private static double latit = -200;
    private static double longit = -200;

    private static final String email = "user_email";
    private static final String id = "user_id";
    private static final String interests = "interests";

    public static void setLatLong(double latitude, double longitude) {
        latit = latitude;
        longit = longitude;
    }

    public static boolean hasInterests() {
        if (user.getInterests() == null) {
            return false;
        } else {
            return !user.getInterests().isEmpty();
        }
    }

    public static double getLat() {
        return latit;
    }

    public static double getLong() {
        return longit;
    }

    public static void setUser(User usr) { user = usr; }

    public static User getUser() { return user; }

    public static boolean getFailed() { return failed; }

    public static void setFailed(boolean result) { failed = result; }

    public static List<Pair<String, String>> getChatMessageLog(int chatId) {
        if (chatMessageMap == null) {
            chatMessageMap = new TreeMap<>();
        }
        if (chatMessageMap.containsKey(chatId)) {
            return chatMessageMap.get(chatId);
        } else {
            List<Pair<String, String>> chatLog = new ArrayList<>();
            chatMessageMap.put(chatId, chatLog);
            return chatLog;
        }
    }

    public static void addMessage(int chatId, String email, String message) {
        Log.d(TAG, "Received new message from " + email + " for chat " + chatId + ", " + message);
        if (chatMessageMap == null) {
            Log.d(TAG, "chatMessageMap is null, creating map");
            chatMessageMap = new TreeMap<>();
        }
        List<Pair<String, String>> chatMessageList;
        if (!chatMessageMap.containsKey(chatId)) {
            Log.d(TAG, "Creating new list for chat group " + chatId);
            chatMessageList = new ArrayList<>();
            chatMessageMap.put(chatId, chatMessageList);
        } else {
            Log.d(TAG, "Retrieved chat log for chat group + " + chatId);
            chatMessageList = chatMessageMap.get(chatId);
        }
        Pair<String, String> pair;
        int chatMsgSize = chatMessageList.size();
        if (chatMsgSize > 0 && chatMessageList.get(chatMsgSize - 1).first.equals(email)) {
            pair = chatMessageList.get(chatMsgSize - 1);
            String new_msg = pair.second + "\n" + message;
            chatMessageList.remove(chatMsgSize - 1);
            pair = new Pair<>(email, new_msg);
            chatMessageList.add(pair);
        } else {
            pair = new Pair<>(email, message);
            chatMessageList.add(pair);
        }
    }

    public static void removeFromGroup(int chatId, String email) {
        for (ChatGroup g : chatGroups) {
            if (g.getChatId() == chatId) {
                List<String> emails = g.getEmails();
                emails.remove(email);
                g.setEmails(emails);
                g.setGroupSize(g.getGroupSize() - 1);
                Log.d(TAG, "removing " + email + " from chatgroup " + chatId + ", new size " + g.getGroupSize());
                return;
            }
        }
    }

    public static void addToGroup(int chatId, String email) {
        for (ChatGroup g : chatGroups) {
            if (g.getChatId() == chatId) {
                List<String> emails = g.getEmails();
                emails.add(email);
                g.setEmails(emails);
                g.setGroupSize(g.getGroupSize() + 1);
                Log.d(TAG, "adding " + email + " to chatgroup " + chatId + ", new size " + g.getGroupSize());
            }
        }
    }

    public static void addChatGroups(List<ChatGroup> groups) {
        Log.d(TAG, "Adding chat groups");
        chatGroups.clear();
        chatGroups.addAll(groups);
        // TODO: Clear message map of groups not in anymore
        Log.d(TAG, chatGroups.size() + " chat groups");
    }

    public static void emptyChatGroup() {
        if (chatMessageMap != null) {
            chatMessageMap.clear();
        }
        if (chatGroups != null) {
            chatGroups.clear();
        }
    }

    public static List<ChatGroup> getChatGroups() {
        return chatGroups;
    }

    public Bundle saveState(Bundle instance) {
        if (user != null) {
            instance.putString(id, user.getId());
            instance.putString(email, user.getEmail());
            instance.putStringArrayList(interests, (ArrayList<String>) user.getInterests());
        }
        return instance;
    }

    public Bundle restoreState(Bundle instance) {
        if (instance.getString(id) != null) {
            user = new User();
            user.setEmail(instance.getString(email));
            user.setId(instance.getString(id));
            user.setInterests(instance.getStringArrayList(interests));
        }
        return instance;
    }
}

