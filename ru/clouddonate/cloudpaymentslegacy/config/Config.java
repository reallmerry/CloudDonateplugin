package ru.clouddonate.cloudpaymentslegacy.config;

import java.util.List;
import lombok.Generated;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import ru.clouddonate.cloudpaymentslegacy.CloudPayments;

public final class Config {
   public static String format(String string) {
      return ChatColor.translateAlternateColorCodes('&', string);
   }

   public static void load(CloudPayments cloudPayments) {
      FileConfiguration config = cloudPayments.getConfig();
      Config.Messages.noPermission = format(cloudPayments.getConfig().getString("messages.noPermission"));
      Config.Messages.reload = format(cloudPayments.getConfig().getString("messages.reload"));
      Config.Messages.debugDisabled = format(cloudPayments.getConfig().getString("messages.debug-disabled"));
      Config.Messages.debugEnabled = format(cloudPayments.getConfig().getString("messages.debug-enabled"));
      Config.Settings.debug = config.getBoolean("settings.debug-mode");
      Config.Settings.checkUpdates = config.getBoolean("settings.check-updates");
      Config.Settings.requestDelay = config.getLong("settings.request-delay");
      Config.LocalStorage.Payments.enabled = config.getBoolean("local-storage.payments.enabled");
      Config.LocalStorage.Payments.format = config.getString("local-storage.payments.format");
      Config.LocalStorage.Statistic.enabled = config.getBoolean("local-storage.statistic.enabled");
      Config.Settings.Shop.shopId = config.getString("settings.shop.shop-id");
      Config.Settings.Shop.shopKey = config.getString("settings.shop.shop-key");
      Config.Settings.Shop.serverId = config.getString("settings.shop.server-id");
      Config.Messengers.Telegram.enabled = config.getBoolean("messengers.telegram.enabled");
      Config.Messengers.Telegram.apiToken = config.getString("messengers.telegram.api-token");
      Config.Messengers.Telegram.ids = config.getStringList("messengers.telegram.ids");
   }

   @Generated
   private Config() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static class Messages {
      public static String noPermission;
      public static String reload;
      public static String debugDisabled;
      public static String debugEnabled;
   }

   public static class LocalStorage {
      public static class Payments {
         public static boolean enabled;
         public static String format;
      }

      public static class Statistic {
         public static boolean enabled;
      }
   }

   public static class Settings {
      public static boolean debug;
      public static boolean checkUpdates;
      public static long requestDelay;

      public static class Shop {
         public static String shopId;
         public static String shopKey;
         public static String serverId;
      }
   }

   public static class Messengers {
      public static class Telegram {
         public static boolean enabled;
         public static List<String> ids;
         public static String apiToken;
      }
   }
}
