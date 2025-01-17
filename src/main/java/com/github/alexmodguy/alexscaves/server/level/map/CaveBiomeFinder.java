package com.github.alexmodguy.alexscaves.server.level.map;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class CaveBiomeFinder {

    private static final ExecutorService EXECUTORS = Executors.newCachedThreadPool();

    public CaveBiomeFinder() {
    }

    public void fillOutCaveMap(UUID uuid, ItemStack map, ServerLevel serverLevel, BlockPos center, Player player) {
        EXECUTORS.execute(new FilloutCaveBiomeMap(map, serverLevel, center, player, uuid));
    }

    /**
     * AC specific thread factory.
     */
    public static class MapThreadFactory implements ThreadFactory {
        /**
         * Ongoing thread IDs.
         */
        public static int id;

        @Override
        public Thread newThread(final Runnable runnable) {
            BlockableEventLoop<?> workqueue = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);
            ClassLoader classLoader;
            if (workqueue.isSameThread()) {
                classLoader = Thread.currentThread().getContextClassLoader();
            } else {
                classLoader = CompletableFuture.supplyAsync(() -> Thread.currentThread().getContextClassLoader(), workqueue).join();
            }
            final Thread thread = new Thread(runnable, "Alex's Caves Worker #" + (id++));
            thread.setDaemon(true);
            thread.setPriority(Thread.MAX_PRIORITY);
            if (thread.getContextClassLoader() != classLoader) {
                AlexsCaves.LOGGER.info("Corrected CCL of new Alex's Caves Thread, was: " + thread.getContextClassLoader().toString());
                thread.setContextClassLoader(classLoader);
            }
            thread.setUncaughtExceptionHandler((thread1, throwable) -> AlexsCaves.LOGGER.error("Alex's Caves Thread errored! ", throwable));
            return thread;
        }
    }
}
