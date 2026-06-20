package dependancy;

import java.util.ArrayList;
import java.util.List;

public class ProjectileSystem {
    public static class Projectile {
        public double x, y, vx, vy;
        public boolean isEnemy;
        public int life = 150;

        public Projectile(double x, double y, double vx, double vy, boolean isEnemy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.isEnemy = isEnemy;
        }

        public void update() {
            x += vx;
            y += vy;
            life--;
        }
    }

    public static List<Projectile> projectiles = new ArrayList<>();

    public static void fire(double x, double y, double vx, double vy, boolean isEnemy) {
        projectiles.add(new Projectile(x, y, vx, vy, isEnemy));
    }

    public static void update() {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update();
            if (p.life <= 0) projectiles.remove(i);
        }
    }
}
