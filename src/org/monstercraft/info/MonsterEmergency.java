package org.monstercraft.info;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.monstercraft.info.plugin.Emailer;
import org.monstercraft.info.plugin.api.Carrier;
import org.monstercraft.info.plugin.utils.CarrierUtils;
import org.monstercraft.info.plugin.utils.JarUtils;
import org.monstercraft.info.plugin.utils.Metrics;

/**
 * This class represents the main plugin. All actions related to the plugin are forwarded by this class
 * 
 * @author Fletch_to_99 <fletchto99@hotmail.com>
 * 
 */
public class MonsterEmergency extends JavaPlugin implements Listener {

    private static Emailer email;
    protected static String TO;
    protected static Carrier CARRIER;
    protected static String NUMBER;
    protected static String STMP_PORT;
    protected static String STMP_HOST;
    protected static String STMP_USERNAME;
    protected static String STMP_PASSWORD;

    @Override
    public void onEnable() {
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
            getLogger()
                    .warning(
                            ChatColor.DARK_RED
                                    + "The default config has been generated. Please modify the config.");
            getLogger().warning("Shutting down plugin...");
            Bukkit.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        MonsterEmergency.STMP_HOST = getConfig().getString("SMTP.HOST");
        MonsterEmergency.STMP_PORT = getConfig().getString("SMTP.PORT");
        MonsterEmergency.STMP_USERNAME = getConfig().getString("SMTP.USERNAME");
        MonsterEmergency.STMP_PASSWORD = getConfig().getString("SMTP.PASSWORD");
        MonsterEmergency.TO = getConfig().getString(
                "CONTACT.EMAIL.EMAIL_ADDRESS");
        MonsterEmergency.NUMBER = getConfig().getString("CONTACT.PHONE.NUMBER");
        MonsterEmergency.CARRIER = CarrierUtils.findCarrier(getConfig()
                .getString("CONTACT.PHONE.CARRIER"));
        try {
            final File[] libs = new File[] {
                    new File(getDataFolder(), "activation.jar"),
                    new File(getDataFolder(), "mail.jar") };
            for (final File lib : libs) {
                if (!lib.exists()) {
                    JarUtils.extractFromJar(lib.getName(),
                            lib.getAbsolutePath());
                }
            }
            for (final File lib : libs) {
                if (!lib.exists()) {
                    getLogger().warning(
                            "There was an error loading MonsterEmergency! Could not find lib: "
                                    + lib.getName());
                    Bukkit.getServer().getPluginManager().disablePlugin(this);
                    return;
                }
                addURL(JarUtils.getJarUrl(lib));
            }
            getServer().getPluginManager().registerEvents(this, this);
            try {
                new Metrics(this).start();
            } catch (final IOException e) {
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        MonsterEmergency.email = new Emailer();
    }

    protected static Emailer getEmailer() {
        return MonsterEmergency.email;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command,
            final String label, final String[] args) {
        if (label.equalsIgnoreCase("emergency")
                && sender.hasPermission("monsteremergency.send")) {
            final StringBuilder sb = new StringBuilder();
            for (final String arg : args) {
                sb.append(arg + " ");
            }
            final String message = sb.toString().trim() + "\r\n\r\nThanks,\r\n"
                    + sender.getName();
            if (message == null || message.length() <= 10) {
                sender.sendMessage(ChatColor.RED
                        + "You must leave a valid message of 10 characters or more!");
                return true;
            }
            sender.sendMessage(ChatColor.BLUE
                    + "Sending message... This may take a few seconds.");
            Bukkit.getScheduler().scheduleAsyncDelayedTask(this,
                    new Runnable() {

                        @Override
                        public void run() {
                            boolean mail, txt, either = false;
                            if (MonsterEmergency.CARRIER != null
                                    && !MonsterEmergency.NUMBER
                                            .equalsIgnoreCase("null")) {
                                txt = EmergencyAPI.sendTextMessage(message);
                                sender.sendMessage(txt ? ChatColor.GREEN
                                        + "Text message sent!"
                                        : ChatColor.RED
                                                + "Error sending txt! Tell the owner to verify their STMP information on MonsterEmergency!");
                                either = true;
                            }
                            if (!MonsterEmergency.TO.equalsIgnoreCase("null")) {
                                mail = EmergencyAPI
                                        .sendMail(
                                                "There is a minecraft problem - MonsterEmergency",
                                                message);
                                sender.sendMessage(mail ? ChatColor.GREEN
                                        + "Email sent!"
                                        : ChatColor.RED
                                                + "Error sending email! Tell the owner to verify their STMP information on MonsterEmergency!");
                                either = true;
                            }
                            if (!either) {
                                sender.sendMessage(ChatColor.RED
                                        + "The owner has misconfigured MonsterEmergency. Please notify them.");
                            }
                        }
                    });
        } else if (label.equalsIgnoreCase("emergency")) {
            sender.sendMessage(ChatColor.RED
                    + "You don't have permission to execute this command!");
        }
        return true;
    }

    private void addURL(final URL url) throws IOException {
        final URLClassLoader sysloader = (URLClassLoader) ClassLoader
                .getSystemClassLoader();
        final Class<URLClassLoader> sysclass = URLClassLoader.class;
        try {
            final Method method = sysclass.getDeclaredMethod("addURL",
                    new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] { url });
        } catch (final Throwable t) {
            t.printStackTrace();
            throw new IOException("Error adding " + url
                    + " to system classloader");
        }
    }

}
