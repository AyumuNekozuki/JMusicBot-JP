/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.commands.DJCommand;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class PauseCmd extends DJCommand 
{
    public PauseCmd(Bot bot)
    {
        super(bot);
        this.name = "pause";
        this.help = "現在の曲を一時停止します";
        this.bePlaying = true;
    }

    @Override
    public void doCommand(CommandEvent event) 
    {
        AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
        if(handler.getPlayer().isPaused())
        {
            event.replyWarning("曲はすでに一時停止しています。 `"+event.getClient().getPrefix()+" play` を使用して一時停止を解除する事ができます。");
            return;
        }
        handler.getPlayer().setPaused(true);
        event.replySuccess("**"+handler.getPlayer().getPlayingTrack().getInfo().title+"**を一時停止にしました。 `"+event.getClient().getPrefix()+" play` を使用すると一時停止を解除できます。");

        if(bot.getConfig().getChangeNickName()) {
            Member botMember = event.getGuild().getSelfMember();
            // botのニックネーム変更

            // botにニックネームがつけられていないとき
            if(botMember.getNickname() == null || botMember.getNickname().isEmpty()) {
                // ニックネームの変更権限があるかどうか
                if(!botMember.hasPermission(Permission.NICKNAME_CHANGE)) return;
                // ニックネームを変更
                event.getGuild().getController().setNickname(botMember, "⏸ " + botMember.getUser().getName()).complete();

                // botにニックネームがつけられているとき
            } else {
                // ニックネームの変更権限があるかどうか
                if(!botMember.hasPermission(Permission.NICKNAME_CHANGE)) return;
                // ニックネームを変更
                event.getGuild().getController().setNickname(botMember, "⏸ " + botMember.getNickname().replaceAll("^[⏯⏹] ", "")).complete();
            }
        }
    }
}
