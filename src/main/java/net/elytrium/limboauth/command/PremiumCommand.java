/*
 * Copyright (C) 2021 - 2022 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboauth.command;

import com.j256.ormlite.dao.Dao;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import java.sql.SQLException;
import net.elytrium.java.commons.mc.serialization.Serializer;
import net.elytrium.limboauth.LimboAuth;
import net.elytrium.limboauth.Settings;
import net.elytrium.limboauth.handler.AuthSessionHandler;
import net.elytrium.limboauth.model.RegisteredPlayer;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.text.Component;

public class PremiumCommand implements SimpleCommand {

  private final LimboAuth plugin;
  private final Dao<RegisteredPlayer, String> playerDao;

  private final String confirmKeyword;
  private final Component notRegistered;
  private final Component alreadyPremium;
  private final Component successful;
  private final Component errorOccurred;
  private final Component notPremium;
  private final Component usage;
  private final Component notPlayer;

  public PremiumCommand(LimboAuth plugin, Dao<RegisteredPlayer, String> playerDao) {
    this.plugin = plugin;
    this.playerDao = playerDao;

    Serializer serializer = LimboAuth.getSerializer();
    this.confirmKeyword = Settings.IMP.MAIN.CONFIRM_KEYWORD;
    this.notRegistered = serializer.deserialize(Settings.IMP.MAIN.STRINGS.NOT_REGISTERED);
    this.alreadyPremium = serializer.deserialize(Settings.IMP.MAIN.STRINGS.ALREADY_PREMIUM);
    this.successful = serializer.deserialize(Settings.IMP.MAIN.STRINGS.PREMIUM_SUCCESSFUL);
    this.errorOccurred = serializer.deserialize(Settings.IMP.MAIN.STRINGS.ERROR_OCCURRED);
    this.notPremium = serializer.deserialize(Settings.IMP.MAIN.STRINGS.NOT_PREMIUM);
    this.usage = serializer.deserialize(Settings.IMP.MAIN.STRINGS.PREMIUM_USAGE);
    this.notPlayer = serializer.deserialize(Settings.IMP.MAIN.STRINGS.NOT_PLAYER);
  }

  @Override
  public void execute(SimpleCommand.Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();

    if (source instanceof Player) {

      String username = ((Player) source).getUsername();
      RegisteredPlayer player = AuthSessionHandler.fetchInfo(this.playerDao, username);
      if (player == null) {
        source.sendMessage(this.notRegistered, MessageType.SYSTEM);
      } else if (player.getHash().isEmpty()) {
        source.sendMessage(this.alreadyPremium, MessageType.SYSTEM);
      } else {
        if (this.plugin.isPremiumExternal(username)) {
          try {
            player.setHash("");
            this.playerDao.update(player);
            this.plugin.removePlayerFromCache(username);
            ((Player) source).disconnect(this.successful);
          } catch (SQLException e) {
            source.sendMessage(this.errorOccurred, MessageType.SYSTEM);
            e.printStackTrace();
          }
        } else {
          source.sendMessage(this.notPremium, MessageType.SYSTEM);
        }
      }

      return;

    } else {
      source.sendMessage(this.notPlayer, MessageType.SYSTEM);
    }
  }

  @Override
  public boolean hasPermission(SimpleCommand.Invocation invocation) {
    return invocation.source().getPermissionValue("limboauth.commands.premium") == Tristate.TRUE;
  }
}
