package services;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RappelScheduler {

    private static ScheduledExecutorService scheduler;

    public static void demarrer() {
        if (scheduler != null && !scheduler.isShutdown()) return;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RappelScheduler");
            t.setDaemon(true);
            return t;
        });

        ServiceRappel serviceRappel = new ServiceRappel();

        // ✅ Délai initial 30s — laisse l'interface et la connexion principale
        //    se stabiliser avant le premier envoi
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("🔔 Vérification rappels patients...");
            serviceRappel.verifierEtEnvoyerRappels();
        }, 30, 24 * 60 * 60, TimeUnit.SECONDS);

        System.out.println("✅ RappelScheduler démarré — premier envoi dans 30s");
    }

    public static void arreter() {
        if (scheduler != null) {
            scheduler.shutdown();
            System.out.println("🛑 RappelScheduler arrêté");
        }
    }
}