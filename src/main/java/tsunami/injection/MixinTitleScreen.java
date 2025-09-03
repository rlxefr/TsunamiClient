package tsunami.injection;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tsunami.gui.misc.DialogScreen;
import tsunami.gui.mainmenu.MainMenuScreen;
import tsunami.utility.render.TextureStorage;
import tsunami.core.manager.client.ModuleManager;
import tsunami.features.modules.client.ClientSettings;

import static tsunami.features.modules.Module.mc;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {

    private static final String VERSION_URL = "https://tsunamiclient.rf.gd/version.txt";
    private static final String CURRENT_VERSION = "1.1";

    protected MixinTitleScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    public void postInitHook(CallbackInfo ci) {
        // Custom main menu
        if (ClientSettings.customMainMenu.getValue() && !MainMenuScreen.getInstance().confirm
                && ModuleManager.clickGui.getBind().getKey() != -1) {
            mc.setScreen(MainMenuScreen.getInstance());
        }

        // Language selection
        if (ModuleManager.clickGui.getBind().getKey() == -1) {
            DialogScreen dialogScreen1 = new DialogScreen(
                    TextureStorage.questionPic,
                    "Hello!",
                    "What's your language?",
                    "Русский",
                    "English",
                    () -> {
                        ClientSettings.language.setValue(ClientSettings.Language.RU);
                        mc.setScreen(MainMenuScreen.getInstance());
                    },
                    () -> {
                        ClientSettings.language.setValue(ClientSettings.Language.ENG);
                        mc.setScreen(MainMenuScreen.getInstance());
                    }
            );
            mc.setScreen(dialogScreen1);
        }

        // Async version check
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
        }
    }
}
