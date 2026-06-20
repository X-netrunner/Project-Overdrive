package dependancy;

import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

public class ParticleSystem {
    public static class Particle {
        public double x, y, vx, vy;
        public int life;
        public Color col;
        public double size;

        public Particle(double x, double y, Color col) {
            this.x = x;
            this.y = y;
            this.vx = (Math.random() - 0.5) * 0.2;
            this.vy = (Math.random() - 0.5) * 0.2;
            this.life = 20 + (int)(Math.random() * 20);
            this.col = col;
            this.size = Math.random() * 4 + 2;
        }

        public void update() {
            x += vx;
            y += vy;
            life--;
        }
    }

    public static List<Particle> particles = new ArrayList<>();

    public static void spawn(double x, double y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    public static void update() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.life <= 0) particles.remove(i);
        }
    }
}
