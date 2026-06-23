# Project Overdrive

[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Platform](https://img.shields.io/badge/Platform-Linux%20%7C%20Windows-blue.svg)]()
[![Build](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()

**Project Overdrive** is a hardware-accelerated, terminal-and-graphical hybrid roguelike RPG framework written natively in Java. Set in the post-apocalyptic cybernetic era of 2142, Earth has fallen completely silent after a catastrophic mechanical plague. Built as the absolute final rogue unit engineered by Dr. Nex, your system's priority directive is to infiltrate procedurally expanding sectors, bypass security firewalls, harvest hardware upgrades, and access the central power infrastructure.

[![Watch the video](https://youtube.com)](https://youtu.be/QkTXXQsze-w)

---

## 🚀 Key Architectural Subsystems

### 1. Dual Execution Loop Layer
The platform seamlessly bridges two entirely separate game engines depending on user execution requirements:
- **Terminal System:** A streamlined, command-line utility buffer loop allowing high-level character stat tuning, dynamic configuration file parsing, and system database inspection.
- **Graphical GUI Frame Canvas:** An asynchronous visual subsystem built over custom AWT/Swing layouts tracking entity position vectors and rendering high-frequency grid updates.

### 2. Hardware-Accelerated Rendering Pipeline
To sustain fluid frame calculations and handle multiple multi-buffered particle streams effortlessly on any host machine, the engine targets JVM-level pipe flags directly upon initialization inside `game.java`:
```java
System.setProperty("sun.java2d.opengl", "true");
System.setProperty("sun.java2d.d3d", "true");      // Native Direct3D rendering for Windows hosts
System.setProperty("sun.java2d.pmoffscreen", "true");
```

This forces offscreen canvas buffer copies to render cleanly on dedicated GPU silicon rather than choking host CPU cycles.

### 3. Local Serialization Obfuscation Engine (`editor.java` & `encrypter.java`)

Local game profiles, character matrices, and game configurations are saved locally via CSV files (`playerData.csv` and `configData.csv`). To prevent raw data-tampering or value injecting, properties are translated dynamically using a multi-pass salt mechanism where properties are split into character arrays, masked with randomized modifiers, and verified via an inverse parser routine.

### 4. Deterministic Sector Map Generation (`map.java` & `mobs.java`)

Sectors, floor types (Normal, Fog, or Shifter), hostile spawn coordinates, upgrade containers, and area bounds are completely unique yet mathematically bound to a master seed. Using a deterministic pseudo-random sequence, map generation scales automatically as you breach deep corridors.

---

## 📁 Repository Layout

```text
Project-Overdrive/
│
├── game.java                  # Main application driver, hardware flags, & core loop
├── .gitignore                 # Clear mapping isolating local logs, saves, & compiled files
└── dependancy/                # Core modular sub-systems package
    ├── config.java            # Keybind hooks, area configurations, & CSV file handling
    ├── login.java             # Player profile identification and security barriers
    ├── editor.java            # Stateful storage serializer and order matrix map
    ├── encrypter.java         # Mathematical obfuscation translation layers
    ├── stats.java             # Player attribute allocation, level thresholds, and traits
    ├── item.java              # Procedural item drops, armor plating, and equipment buffers
    ├── chest.java             # Crate discovery systems adjusted via luck factors
    ├── map.java               # Seed-bound procedural maze generation arrays
    ├── mobs.java              # Unit state vectors and boss entity distributions
    ├── play.java              # Central collision detection and active module radar trackers
    ├── gui.java               # Graphic drawing canvas, frame interpolation, and key listeners
    ├── ProjectileSystem.java  # High-frequency projectile tracking and lifetime handlers
    ├── ParticleSystem.java    # Combat spark visual effect structures and fade physics
    └── Logger.java            # ANSI colorized diagnostic formatting stream
```

---

## 🎮 Interface & Keybinds

### 1. Main System Menu Options

Navigate core setup utilities via numerical indexing input commands inside the terminal interface:

* `1` — **Diagnostic Check:** View detailed status updates, current module loadouts, and base values.
* `2` — **Hardware Upgrades:** Distribute unassigned attribute points collected from field experience.
* `3` — **Launch Missions (GUI):** Boot up the accelerated graphic panel window canvas.
* `4` — **Launch Missions (CLI):** Run a lightweight text-only command-line map grid.
* `7` — **System Options:** Adjust hardware profile values or remap custom action listeners.
* `8` — **Admin Core:** Dynamic administrative interface used to inspect current user profiles.

### 2. Graphical Canvas Controls

* **Movement Vectors:** `W`, `A`, `S`, `D` keys
* **Mobility Dash Phase:** `Left Shift` (Uses system stamina pools)
* **Deflective Parry Frame:** `V`
* **Chassis Blink Displacement:** `G`
* **Module Auxiliary Skills:** Maps cleanly via active configs (Default: `R` / `F` based on sensors/manipulators equipped).

* **[NOTE] : ** You can change the keybinds from settings. 

---

## 🛠️ Installation & Compilation

### Requirements

* **Java Development Kit (JDK) 17** or higher must be configured in your system paths.

### 1. Project Compilation

Since the code relies heavily on custom interconnected sub-modules compiled into the `dependancy` package directory, you must run your compiler directly from the root path folder:

```bash
javac game.java dependancy/*.java
```

### 2. Execution Setup

Launch the primary game driver file layer using the JVM runtime environment:

```bash
java game
```
