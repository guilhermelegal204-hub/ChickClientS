package net.fabricmc.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChickClientPvpMod implements ClientModInitializer {

    // Tecla para abrir o menu do Client (Tecla H)
    private static KeyBinding menuKeyBinding;
    public static boolean toggleMenuOpen = false;

    // Configurações do Módulo: Teia-Automática
    public static boolean cobwebEnabled = true;
    public static boolean cobwebWeaponOnly = true;
    public static int cobwebDelayMs = 50; 
    private long lastCobwebTime = 0;

    // Configurações do Módulo: Perseguição de Ender Pearl
    public static boolean pearlTrackEnabled = true;
    public static int pearlTrackDelayMs = 100; 
    public static String pearlTrackMode = "Smooth"; // Opções: "Smooth" ou "Linear"
    private final Map<UUID, Vec3d> activePearls = new HashMap<>();
    private Vec3d targetPearlLanding = null;
    private long pearlTrackTriggerTime = 0;

    // Configurações do Módulo: S-Tap Automático
    public static boolean sTapEnabled = true;
    public static boolean sTapOnlyOnGround = true;
    public static int sTapDelayMs = 70; 
    private long sTapEndTime = 0;
    private boolean isSTapping = false;

    @Override
    public void onInitializeClient() {
        // Registra a tecla H para abrir o menu
        menuKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.chickclient.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.chickclient"
        ));

        // Evento principal executado a cada tick do jogo
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // 1. Monitora o clique da tecla H para abrir o menu
            if (menuKeyBinding.wasPressed()) {
                toggleMenuOpen = !toggleMenuOpen;
                client.player.sendMessage(net.minecraft.text.Text.of("§b[Chick Client] §fMenu aberto: " + toggleMenuOpen), true);
            }

            // 2. Execução da lógica do S-Tap Automático
            if (sTapEnabled && isSTapping) {
                if (System.currentTimeMillis() >= sTapEndTime) {
                    client.options.backKey.setPressed(false);
                    isSTapping = false;
                } else {
                    if (!sTapOnlyOnGround || client.player.isOnGround()) {
                        client.options.backKey.setPressed(true);
                        client.options.forwardKey.setPressed(false);
                    }
                }
            }

            // 3. Execução do rastreamento de Ender Pearl (Modo de Perseguição)
            if (pearlTrackEnabled) {
                // Rastreia pérolas no mundo
                for (Entity entity : client.world.getEntities()) {
                    if (entity instanceof EnderPearlEntity) {
                        activePearls.put(entity.getUuid(), entity.getPos());
                    }
                }

                // Verifica se alguma pérola sumiu (atingiu o chão)
                activePearls.entrySet().removeIf(entry -> {
                    Entity entity = client.world.getEntityLookup().get(entry.getKey());
                    if (entity == null || !entity.isAlive()) {
                        // Pérola pousou! Salva o local para onde o inimigo foi
                        targetPearlLanding = entry.getValue();
                        pearlTrackTriggerTime = System.currentTimeMillis() + pearlTrackDelayMs;
                        return true;
                    }
                    return false;
                });

                
