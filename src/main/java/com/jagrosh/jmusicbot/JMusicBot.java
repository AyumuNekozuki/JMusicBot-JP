/*
 * Copyright 2018-2020 Cosgy Dev
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.jagrosh.jmusicbot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jmusicbot.commands.admin.*;
import com.jagrosh.jmusicbot.commands.dj.*;
import com.jagrosh.jmusicbot.commands.music.*;
import com.jagrosh.jmusicbot.commands.owner.*;
import com.jagrosh.jmusicbot.entities.Prompt;
import com.jagrosh.jmusicbot.gui.GUI;
import com.jagrosh.jmusicbot.settings.SettingsManager;
import com.jagrosh.jmusicbot.utils.OtherUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author John Grosh (jagrosh)
 */
public class JMusicBot {
    public final static String PLAY_EMOJI = "\u25B6"; // ▶
    public final static String PAUSE_EMOJI = "\u23F8"; // ⏸
    public final static String STOP_EMOJI = "\u23F9"; // ⏹
    public final static Permission[] RECOMMENDED_PERMS = {Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_MANAGE, Permission.MESSAGE_EXT_EMOJI,
            Permission.MANAGE_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.NICKNAME_CHANGE};
    public final static GatewayIntent[] INTENTS = {GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES};
    public static boolean CHECK_UPDATE = true;
    public static boolean COMMAND_AUDIT_ENABLED = false;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // startup log
        Logger log = getLogger("Startup");

        // create prompt to handle startup
        Prompt prompt = new Prompt("JMusicBot", "noguiモードに切り替えます。  -Dnogui=trueフラグを含めると、手動でnoguiモードで起動できます。",
                "true".equalsIgnoreCase(System.getProperty("nogui", "false")));

        // check deprecated nogui mode (new way of setting it is -Dnogui=true)
        for (String arg : args)
            if ("-nogui".equalsIgnoreCase(arg)) {
                prompt.alert(Prompt.Level.WARNING, "GUI", "-noguiフラグは廃止予定です。 "
                        + "jarの名前の前に-Dnogui = trueフラグを使用してください。 例：java -jar -Dnogui=true JMusicBot.jar");
            } else if ("-nocheckupdates".equalsIgnoreCase(arg)) {
                CHECK_UPDATE = false;
                log.info("アップデートチェックを無効にしました");
            } else if ("-auditcommands".equalsIgnoreCase(arg)) {
                COMMAND_AUDIT_ENABLED = true;
                log.info("実行されたコマンドの記録を有効にしました。");
            }

        // get and check latest version
        String version = OtherUtil.checkVersion(prompt);

        if (!System.getProperty("java.vm.name").contains("64"))
            prompt.alert(Prompt.Level.WARNING, "Java Version", "サポートされていないJavaバージョンを使用しています。64ビット版のJavaを使用してください。");

        // load config
        BotConfig config = new BotConfig(prompt);
        config.load();
        if (!config.isValid())
            return;

        // set up the listener
        EventWaiter waiter = new EventWaiter();
        SettingsManager settings = new SettingsManager();
        Bot bot = new Bot(waiter, config, settings);
        Bot.INSTANCE = bot;

        dev.cosgy.JMusicBot.commands.general.AboutCommand aboutCommand = new dev.cosgy.JMusicBot.commands.general.AboutCommand(Color.BLUE.brighter(),
                "[簡単にホストできる！](https://github.com/Cosgy-Dev/MusicBot-JP-java)JMusicBot JP(v" + version + ")",
                new String[] {"高品質の音楽再生", "FairQueue™テクノロジー", "自分で簡単にホスト"},
                RECOMMENDED_PERMS);
        aboutCommand.setIsAuthor(false);
        aboutCommand.setReplacementCharacter("\uD83C\uDFB6"); // 🎶

        // set up the command client
        CommandClientBuilder cb = new CommandClientBuilder()
                .setPrefix(config.getPrefix())
                .setAlternativePrefix(config.getAltPrefix())
                .setOwnerId(Long.toString(config.getOwnerId()))
                .setEmojis(config.getSuccess(), config.getWarning(), config.getError())
                .setHelpWord(config.getHelp())
                .setLinkedCacheSize(200)
                .setGuildSettingsManager(settings)
                .setListener(new dev.cosgy.JMusicBot.commands.listeners.CommandAudit());

        List<Command> commandList = new ArrayList<Command>() {{
            //その他
            add(aboutCommand);
            add(new dev.cosgy.JMusicBot.commands.general.InviteCommand());
            add(new dev.cosgy.JMusicBot.commands.general.PingCommand());
            add(new dev.cosgy.JMusicBot.commands.general.SettingsCmd(bot));
            if (config.getCosgyDevHost()) add(new dev.cosgy.JMusicBot.commands.general.InfoCommand(bot));
            // General
            add(new dev.cosgy.JMusicBot.commands.general.ServerInfo());
            add(new dev.cosgy.JMusicBot.commands.general.UserInfo());
            // Music
            add(new LyricsCmd(bot));
            add(new NowplayingCmd(bot));
            add(new PlayCmd(bot));
            add(new PlaylistsCmd(bot));
            add(new dev.cosgy.JMusicBot.commands.music.MylistCmd(bot));
            //add(new QueueCmd(bot));
            add(new dev.cosgy.JMusicBot.commands.music.QueueCmd(bot));
            add(new RemoveCmd(bot));
            add(new SearchCmd(bot));
            add(new SCSearchCmd(bot));
            add(new dev.cosgy.JMusicBot.commands.music.NicoSearchCmd(bot));
            add(new ShuffleCmd(bot));
            add(new SkipCmd(bot));
            add(new dev.cosgy.JMusicBot.commands.music.VolumeCmd(bot));
            // DJ
            add(new ForceRemoveCmd(bot));
            add(new ForceskipCmd(bot));
            add(new MoveTrackCmd(bot));
            add(new PauseCmd(bot));
            add(new PlaynextCmd(bot));
            //add(new RepeatCmd(bot));
            add(new dev.cosgy.JMusicBot.commands.dj.RepeatCmd(bot));
            add(new SkipToCmd(bot));
            add(new PlaylistCmd(bot));
            add(new StopCmd(bot));
            //add(new VolumeCmd(bot));
            // Admin
            add(new PrefixCmd(bot));
            add(new SetdjCmd(bot));
            add(new SettcCmd(bot));
            add(new SetvcCmd(bot));
            add(new AutoplaylistCmd(bot));
            // Owner
            add(new DebugCmd(bot));
            add(new SetavatarCmd(bot));
            add(new SetgameCmd(bot));
            add(new SetnameCmd(bot));
            add(new SetstatusCmd(bot));
            add(new dev.cosgy.JMusicBot.commands.owner.PublistCmd(bot));
            add(new ShutdownCmd(bot));
        }};

        cb.addCommands(commandList.toArray(new Command[0]));

        if (config.useEval())
            cb.addCommand(new EvalCmd(bot));
        boolean nogame = false;
        if (config.getStatus() != OnlineStatus.UNKNOWN)
            cb.setStatus(config.getStatus());
        if (config.getGame() == null)
            cb.useDefaultGame();
        else if (config.getGame().getName().toLowerCase().matches("(none|なし)")) {
            cb.setActivity(null);
            nogame = true;
        } else
            cb.setActivity(config.getGame());

        if (!prompt.isNoGUI()) {
            try {
                GUI gui = new GUI(bot);
                bot.setGUI(gui);
                gui.init();
            } catch (Exception e) {
                log.error("GUIを開くことができませんでした。次の要因が考えられます:\n"
                        + "サーバー上で実行している\n"
                        + "画面がない環境下で実行している\n"
                        + "このエラーを非表示にするには、 -Dnogui=true フラグを使用してGUIなしモードで実行してください。");
            }
        }

        log.info(config.getConfigLocation() + " から設定を読み込みました");

        // attempt to log in and start
        try {
            JDA jda = JDABuilder.create(config.getToken(), Arrays.asList(INTENTS))
                    .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                    .disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.EMOTE)
                    .setActivity(nogame ? null : Activity.playing("ロード中..."))
                    .setStatus(config.getStatus() == OnlineStatus.INVISIBLE || config.getStatus() == OnlineStatus.OFFLINE
                            ? OnlineStatus.INVISIBLE : OnlineStatus.DO_NOT_DISTURB)
                    .addEventListeners(cb.build(), waiter, new Listener(bot))
                    .setBulkDeleteSplittingEnabled(true)
                    .build();
            bot.setJDA(jda);
        } catch (LoginException ex) {
            prompt.alert(Prompt.Level.ERROR, "JMusicBot", ex + "\n" +
                    "正しい設定ファイルを編集していることを確認してください。Botトークンでのログインに失敗しました。" +
                    "正しいBotトークンを入力してください。(CLIENT SECRET ではありません!)\n" +
                    "設定ファイルの場所: " + config.getConfigLocation());
            System.exit(1);
        } catch (IllegalArgumentException ex) {
            prompt.alert(Prompt.Level.ERROR, "JMusicBot", "設定の一部が無効です:" + ex + "\n" +
                    "設定ファイルの場所: " + config.getConfigLocation());
            System.exit(1);
        }
    }
}
