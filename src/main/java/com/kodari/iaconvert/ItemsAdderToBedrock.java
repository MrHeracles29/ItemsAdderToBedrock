package com.kodari.iaconvert;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ItemsAdderToBedrock extends JavaPlugin implements CommandExecutor {

    private File outputFolder;

    @Override
    public void onEnable() {
        // Crear las carpetas del plugin si no existen
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        outputFolder = new File(getDataFolder(), "output");
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        // Registrar el comando /iaconvert de Kodari
        if (getCommand("iaconvert") != null) {
            getCommand("iaconvert").setExecutor(this);
        }

        getLogger().info("¡Plugin ItemsAdderToBedrock conectado con la logica correctamente!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("iaconvert")) {
            
            sender.sendMessage(ChatColor.YELLOW + "[IA-Converter] Iniciando la conversion a Bedrock en segundo plano...");
            
            // Tarea asincrona para que el servidor no sufra de lag
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    File itemsAdderPack = new File("plugins/ItemsAdder/data/resource_pack");
                    
                    if (!itemsAdderPack.exists()) {
                        sender.sendMessage(ChatColor.RED + "[Error] No se encontro la carpeta en: plugins/ItemsAdder/data/resource_pack");
                        return;
                    }

                    File tempDir = new File(getDataFolder(), "temp_bedrock_pack");
                    if (tempDir.exists()) deleteDirectory(tempDir);
                    tempDir.mkdirs();

                    File javaAssets = new File(itemsAdderPack, "assets");
                    if (javaAssets.exists()) {
                        File bedrockTextures = new File(tempDir, "textures/items");
                        bedrockTextures.mkdirs();
                        copyDirectory(javaAssets, bedrockTextures);
                    }

                    generateManifest(tempDir);

                    File zipOutput = new File(outputFolder, "IA_Bedrock_Pack.mcpack");
                    zipFolder(tempDir, zipOutput);

                    deleteDirectory(tempDir);

                    sender.sendMessage(ChatColor.GREEN + "[Exito] ¡Conversion completada!");
                    sender.sendMessage(ChatColor.GREEN + "[Exito] Archivo guardado en: plugins/ItemsAdderToBedrock/output/IA_Bedrock_Pack.mcpack");

                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "[Error] Ocurrio un fallo en la conversion. Revisa la consola.");
                    e.printStackTrace();
                }
            });
            return true;
        }
        return false;
    }

    private void generateManifest(File folder) throws IOException {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("format_version", 2);

        JsonObject header = new JsonObject();
        header.addProperty("description", "Convertido desde ItemsAdder Java");
        header.addProperty("name", "ItemsAdder Bedrock Pack");
        header.addProperty("uuid", UUID.randomUUID().toString());
        
        JsonArray version = new JsonArray();
        version.add(1); version.add(0); version.add(0);
        header.add("version", version);

        JsonArray minEngine = new JsonArray();
        minEngine.add(1); minEngine.add(21); minEngine.add(0);
        header.add("min_engine_version", minEngine);
        manifest.add("header", header);

        JsonArray modules = new JsonArray();
        JsonObject module = new JsonObject();
        module.addProperty("description", "Convertido desde ItemsAdder Java");
        module.addProperty("type", "resources");
        module.addProperty("uuid", UUID.randomUUID().toString());
        module.add("version", version);
        modules.add(module);
        manifest.add("modules", modules);

        File manifestFile = new File(folder, "manifest.json");
        try (FileWriter writer = new FileWriter(manifestFile)) {
            writer.write(manifest.toString());
        }
    }

    private void copyDirectory(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists()) destination.mkdirs();
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    copyDirectory(new File(source, file), new File(destination, file));
                }
            }
        } else {
            if (source.getName().endsWith(".png")) {
                Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void zipFolder(File srcFolder, File destZipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(destZipFile);
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {
            zipDir(srcFolder, srcFolder, zipOut);
        }
    }

    private void zipDir(File rootFolder, File srcFolder, ZipOutputStream zipOut) throws IOException {
        File[] files = srcFolder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                zipDir(rootFolder, file, zipOut);
            } else {
                String relPath = rootFolder.toURI().relativize(file.toURI()).getPath();
                zipOut.putNextEntry(new ZipEntry(relPath));
                Files.copy(file.toPath(), zipOut);
                zipOut.closeEntry();
            }
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDirectory(file);
            }
        }
        dir.delete();
    }
}
