package ru.clouddonate.cloudpaymentslegacy.shop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import lombok.Generated;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandException;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import ru.clouddonate.cloudpaymentslegacy.CloudPayments;
import ru.clouddonate.cloudpaymentslegacy.api.events.PurchaseApproveEvent;
import ru.clouddonate.cloudpaymentslegacy.config.Config;
import ru.clouddonate.cloudpaymentslegacy.http.GetResult;

public final class Shop {
   private static final String GET_URL = "https://api.cdonate.ru/api/v1/shops/{shop_id}/purchases/pending?server_id={server_id}";
   private static final String POST_URL = "https://api.cdonate.ru/api/v1/shops/{shop_id}/purchases/{purchase_id}/approve";
   private final String shopId;
   private final String shopKey;
   private final String serverId;
   private final long requestDelay;
   private final CloudPayments plugin;
   private BukkitRunnable runnable;

   public Shop(final String shopId, final String shopKey, final String serverId, long requestDelay, final CloudPayments plugin) {
      this.shopId = shopId;
      this.plugin = plugin;
      this.shopKey = shopKey;
      this.serverId = serverId;
      if (requestDelay < 20L) {
         this.requestDelay = 20L;
      } else {
         this.requestDelay = requestDelay;
      }

      this.runnable = new BukkitRunnable() {
         public void run() {
            try {
               String getUrl = "https://api.cdonate.ru/api/v1/shops/{shop_id}/purchases/pending?server_id={server_id}".replace("{shop_id}", shopId).replace("{server_id}", serverId);
               URL url = new URL(getUrl);
               HttpURLConnection getConnection = (HttpURLConnection)url.openConnection();
               getConnection.setRequestMethod("GET");
               getConnection.setRequestProperty("X-Shop-Key", shopKey);
               getConnection.setRequestProperty("Content-Type", "application/json");
               getConnection.setConnectTimeout(5000);
               getConnection.setReadTimeout(5000);
               int getResponseCode = getConnection.getResponseCode();
               if (getResponseCode != 200) {
                  if (Config.Settings.debug) {
                     plugin.getLogger().warning("Failed to fetch data. Response code: " + getResponseCode);
                  }
               } else {
                  StringBuilder response = new StringBuilder();
                  BufferedReader br = new BufferedReader(new InputStreamReader(getConnection.getInputStream(), StandardCharsets.UTF_8));
                  Throwable var7 = null;

                  try {
                     String line;
                     try {
                        while((line = br.readLine()) != null) {
                           response.append(line);
                        }
                     } catch (Throwable var22) {
                        var7 = var22;
                        throw var22;
                     }
                  } finally {
                     if (br != null) {
                        if (var7 != null) {
                           try {
                              br.close();
                           } catch (Throwable var21) {
                              var7.addSuppressed(var21);
                           }
                        } else {
                           br.close();
                        }
                     }

                  }

                  GetResult[] getResults = (GetResult[])((GetResult[])plugin.getConverterService().gson.fromJson(response.toString(), GetResult[].class));
                  if (getResults == null) {
                     if (Config.Settings.debug) {
                        plugin.getLogger().info("Not correct GET result format (null)");
                     }

                     return;
                  }

                  if (Config.Settings.debug) {
                     plugin.getLogger().info("GET return " + getResults.length + " results");
                  }

                  ArrayList<String> commands = new ArrayList();
                  GetResult[] var27 = getResults;
                  int var9 = getResults.length;
                  int var10 = 0;

                  while(true) {
                     if (var10 >= var9) {
                        if (!commands.isEmpty()) {
                           plugin.getServer().getScheduler().runTask(plugin, () -> {
                              ConsoleCommandSender sender = Bukkit.getConsoleSender();
                              Iterator var3 = commands.iterator();

                              while(var3.hasNext()) {
                                 String command = (String)var3.next();

                                 try {
                                    if (Config.Settings.debug) {
                                       plugin.getLogger().info("Executing command: " + command);
                                    }

                                    plugin.getServer().dispatchCommand(sender, command);
                                 } catch (CommandException var6) {
                                    plugin.getLogger().warning("Failed to execute command: " + command);
                                    var6.printStackTrace();
                                 }
                              }

                           });
                        }
                        break;
                     }

                     GetResult data = var27[var10];
                     String[] var12 = data.getCommands();
                     int postResponseCode = var12.length;

                     for(int var14 = 0; var14 < postResponseCode; ++var14) {
                        String command = var12[var14];
                        commands.add(command.replaceAll("\\{user}", data.getNickname()).replaceAll("\\{amount}", String.valueOf(data.getAmount())));
                     }

                     HttpURLConnection postConnection = this.getHttpURLConnection(data);
                     postResponseCode = postConnection.getResponseCode();
                     if (postResponseCode != 204 && Config.Settings.debug) {
                        plugin.getLogger().warning("Failed to approve purchase ID " + data.getId() + ". Response code: " + postResponseCode);
                     } else {
                        plugin.getMessengersManager().getConnectedMessengers().forEach((messenger) -> {
                           messenger.sendMessage("✅ Пришёл платёж: ID " + data.getId() + "\n\n❓ Информация:\n\ud83d\udc64 Никнейм: " + data.getNickname() + "\n\ud83e\udeaa Товар: " + data.getName() + " (кол-во: x" + data.getAmount() + ")\n\ud83d\udd25 Пришло с учётом комиссии сервиса: " + data.getPrice() + " рублей\n\n❤️ Благодарим за использование CloudDonate!");
                        });
                        plugin.getAnnouncementsManager().process(data);
                        plugin.getLocalStorage().addPayment(data);
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                           PurchaseApproveEvent event = new PurchaseApproveEvent(data);
                           plugin.getServer().getPluginManager().callEvent(event);
                        });
                     }

                     postConnection.disconnect();
                     ++var10;
                  }
               }

               getConnection.disconnect();
            } catch (IOException var24) {
               plugin.getLogger().severe("[CloudPayments] Error fetching shop data: " + var24.getMessage());
            }

         }

         private HttpURLConnection getHttpURLConnection(GetResult data) throws IOException {
            String postUrl = "https://api.cdonate.ru/api/v1/shops/{shop_id}/purchases/{purchase_id}/approve".replace("{shop_id}", shopId).replace("{purchase_id}", String.valueOf(data.getId()));
            HttpURLConnection postConnection = (HttpURLConnection)(new URL(postUrl)).openConnection();
            postConnection.setRequestMethod("POST");
            postConnection.setRequestProperty("X-Shop-Key", shopKey);
            postConnection.setRequestProperty("Content-Type", "application/json");
            postConnection.setDoOutput(true);
            postConnection.setConnectTimeout(5000);
            postConnection.setReadTimeout(5000);
            OutputStream os = postConnection.getOutputStream();
            Throwable var5 = null;

            try {
               byte[] input = "{}".getBytes(StandardCharsets.UTF_8);
               os.write(input, 0, input.length);
            } catch (Throwable var14) {
               var5 = var14;
               throw var14;
            } finally {
               if (os != null) {
                  if (var5 != null) {
                     try {
                        os.close();
                     } catch (Throwable var13) {
                        var5.addSuppressed(var13);
                     }
                  } else {
                     os.close();
                  }
               }

            }

            return postConnection;
         }
      };
      this.runnable.runTaskTimerAsynchronously(this.getPlugin(), this.getRequestDelay() * 20L, this.getRequestDelay() * 20L);
   }

   @Generated
   public String getShopId() {
      return this.shopId;
   }

   @Generated
   public String getShopKey() {
      return this.shopKey;
   }

   @Generated
   public String getServerId() {
      return this.serverId;
   }

   @Generated
   public long getRequestDelay() {
      return this.requestDelay;
   }

   @Generated
   public CloudPayments getPlugin() {
      return this.plugin;
   }

   @Generated
   public BukkitRunnable getRunnable() {
      return this.runnable;
   }

   @Generated
   public void setRunnable(BukkitRunnable runnable) {
      this.runnable = runnable;
   }
}
