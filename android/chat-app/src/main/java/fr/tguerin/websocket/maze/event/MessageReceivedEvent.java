package fr.tguerin.websocket.maze.event;

public class MessageReceivedEvent {

    public final String message;

    public MessageReceivedEvent(String message) {
        this.message = message;
    }
}
