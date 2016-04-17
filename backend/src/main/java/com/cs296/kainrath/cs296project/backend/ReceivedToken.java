package com.cs296.kainrath.cs296project.backend;

/**
 * Created by kainrath on 4/13/16.
 */
public class ReceivedToken {
    private boolean received;

    public ReceivedToken(boolean received) {
        this.received = received;
    }

    public boolean getReceived() {
        return received;
    }
}
