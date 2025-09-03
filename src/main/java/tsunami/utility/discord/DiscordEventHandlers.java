package tsunami.utility.discord;

import java.util.Arrays;
import java.util.List;
import tsunami.utility.discord.callbacks.JoinGameCallback;
import tsunami.utility.discord.callbacks.ErroredCallback;
import tsunami.utility.discord.callbacks.ReadyCallback;
import tsunami.utility.discord.callbacks.SpectateGameCallback;
import tsunami.utility.discord.callbacks.JoinRequestCallback;
import tsunami.utility.discord.callbacks.DisconnectedCallback;
import com.sun.jna.Structure;

public class DiscordEventHandlers extends Structure {
    public DisconnectedCallback disconnected;
    public JoinRequestCallback joinRequest;
    public SpectateGameCallback spectateGame;
    public ReadyCallback ready;
    public ErroredCallback errored;
    public JoinGameCallback joinGame;
    
    protected List<String> getFieldOrder() {
        return Arrays.asList("ready", "disconnected", "errored", "joinGame", "spectateGame", "joinRequest");
    }
}