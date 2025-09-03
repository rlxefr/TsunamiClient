package tsunami.features.modules.client;

import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import tsunami.TsunamiClient;
import tsunami.core.Managers;
import tsunami.features.modules.Module;
import tsunami.setting.Setting;
import tsunami.utility.Timer;
import tsunami.utility.discord.DiscordEventHandlers;
import tsunami.utility.discord.DiscordRPC;
import tsunami.utility.discord.DiscordRichPresence;

import java.io.*;
import java.util.Objects;
import java.util.Random;

import static tsunami.features.modules.client.ClientSettings.isRu;

public final class RPC extends Module {
    private static final DiscordRPC rpc = DiscordRPC.INSTANCE;
    public static Setting<Mode> mode = new Setting<>("Picture", Mode.MegaCute);
    public static Setting<Boolean> showIP = new Setting<>("ShowIP", true);
    public static Setting<sMode> smode = new Setting<>("StateMode", sMode.Stats);
    public static Setting<String> state = new Setting<>("State", "Beta? Recode? NextGen?");
    public static Setting<Boolean> nickname = new Setting<>("Nickname", true);
    public static DiscordRichPresence presence = new DiscordRichPresence();
    public static boolean started;

    private static String customImageUrl = "none";
    private static String customSmallImageUrl = "none";
    private final Timer timer_delay = new Timer();
    private static Thread thread;
    private String currentRandomMessage = "";
    private final Random random = new Random();

    private final String[] rpc_messages_en = {
            "Parkour", "Reporting cheaters", "Touching grass",
            "Asks how to bind", "Reporting bugs", "Watching Kilab"
    };

    private final String[] rpc_messages_ru = {
            "Паркурит", "Репортит читеров", "Трогает траву",
            "Спрашивает как забиндить", "Репортит баги", "Смотрит Флюгера"
    };

    public RPC() {
        super("DiscordRPC", Category.CLIENT);
    }

    public static void readFile() {
        try {
            File file = new File("Tsunami/misc/RPC.txt");
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line = reader.readLine();
                    if (line != null && line.contains("SEPARATOR")) {
                        String[] parts = line.split("SEPARATOR", 2);
                        if (parts.length >= 2) {
                            customImageUrl = parts[0].trim();
                            customSmallImageUrl = parts[1].trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading RPC file: " + e.getMessage());
            // Reset to defaults on error
            customImageUrl = "none";
            customSmallImageUrl = "none";
        }
    }

    public static void WriteFile(String url1, String url2) {
        File file = new File("Tsunami/misc/RPC.txt");
        try {
            // Ensure directory exists
            file.getParentFile().mkdirs();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(url1 + "SEPARATOR" + url2);
            }
        } catch (Exception e) {
            System.err.println("Error writing RPC file: " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        if (!started) {
            startRpc();
        }
    }

    @Override
    public void onDisable() {
        started = false;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            try {
                thread.join(1000); // Wait up to 1 second for thread to finish
            } catch (InterruptedException ignored) {
            }
        }

        try {
            rpc.Discord_Shutdown();
        } catch (Exception e) {
            System.err.println("Error shutting down Discord RPC: " + e.getMessage());
        }
    }

    @Override
    public void onUpdate() {
        if (!started) {
            startRpc();
        }
    }

    public void startRpc() {
        if (isDisabled() || started) return;

        try {
            started = true;
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            rpc.Discord_Initialize("1406960808449282248", handlers, true, "");

            presence.startTimestamp = (System.currentTimeMillis() / 1000L);
            presence.largeImageText = "v" + TsunamiClient.VERSION + " [" + TsunamiClient.GITHUB_HASH + "]";

            // Set initial random message
            updateRandomMessage();

            rpc.Discord_UpdatePresence(presence);

            thread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted() && started) {
                    try {
                        rpc.Discord_RunCallbacks();
                        updatePresence();
                        rpc.Discord_UpdatePresence(presence);

                        Thread.sleep(2000L);
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        System.err.println("Error in RPC thread: " + e.getMessage());
                        // Continue running despite errors
                    }
                }
            }, "TH-RPC-Handler");

            thread.setDaemon(true); // Make thread daemon so it doesn't prevent shutdown
            thread.start();

        } catch (Exception e) {
            System.err.println("Error starting Discord RPC: " + e.getMessage());
            started = false;
        }
    }

    private void updatePresence() {
        // Update details
        presence.details = getDetails();

        // Update state based on mode
        switch (smode.getValue()) {
            case Stats ->
                    presence.state = "Hacks: " + Managers.MODULE.getEnabledModules().size() + " / " + Managers.MODULE.modules.size();
            case Custom -> presence.state = state.getValue();
            case Version -> presence.state = "v" + TsunamiClient.VERSION + " for mc 1.21";
        }

        // Update nickname display
        if (nickname.getValue() && mc.getSession() != null) {
            presence.smallImageText = "Tsunami Enjoyer";
            presence.smallImageKey = "https://minotar.net/helm/" + mc.getSession().getUsername() + "/100.png";
        } else {
            presence.smallImageText = "";
            presence.smallImageKey = "";
        }

        // Set buttons
        presence.button_label_1 = "Download";
        presence.button_url_1 = "https://tsunamiclient.rf.gd/";

        // Update large image based on mode
        switch (mode.getValue()) {
            case Recode, MegaCute ->
                    presence.largeImageKey = "https://i.ibb.co/Y4J77YDj/639207e799a068a186599f00f33b424d.png";
            case Custom -> {
                readFile();
                if (!customImageUrl.equals("none")) {
                    presence.largeImageKey = customImageUrl;
                }
                if (!customSmallImageUrl.equals("none") && !nickname.getValue()) {
                    presence.smallImageKey = customSmallImageUrl;
                }
            }
        }
    }

    private void updateRandomMessage() {
        String[] messages = isRu() ? rpc_messages_ru : rpc_messages_en;
        int randomIndex = random.nextInt(messages.length);
        currentRandomMessage = messages[randomIndex];
    }

    private String getDetails() {
        if (mc.currentScreen instanceof MultiplayerScreen ||
                mc.currentScreen instanceof AddServerScreen ||
                mc.currentScreen instanceof TitleScreen) {

            // Update random message every 60 seconds
            if (timer_delay.passedMs(60 * 1000)) {
                updateRandomMessage();
                timer_delay.reset();
            }
            return currentRandomMessage;

        } else if (mc.getCurrentServerEntry() != null) {
            String serverInfo = mc.getCurrentServerEntry().address;
            if (isRu()) {
                return showIP.getValue() ? "Играет на " + serverInfo : "Играет на сервере";
            } else {
                return showIP.getValue() ? "Playing on " + serverInfo : "Playing on server";
            }
        } else if (mc.isInSingleplayer()) {
            return isRu() ? "Читерит в одиночке" : "SinglePlayer ";
        }

        return isRu() ? "В главном меню" : "In main menu";
    }

    public enum Mode {Custom, MegaCute, Recode}
    public enum sMode {Custom, Stats, Version}
}