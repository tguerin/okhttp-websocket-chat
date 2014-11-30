package fr.tguerin.websocket.maze.event;

public class SendMessageEvent {

    public final String message;

    public SendMessageEvent(String message) {
        this.message = message;
    }
}
