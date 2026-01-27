package server.variplex.DonateCase.Tools;

import all.api.UTILS.Message;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import server.variplex.DonateCase.ThunderBolt;
import server.variplex.VariableX;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Animation {
    Plugin plugin = VariableX.getInstance();
    org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();

    public void startAnimation(Player player, int blockX, int blockY, int blockZ) {
        // ---------- читаем параметры из конфига (с дефолтами) ----------
        final int armorStandCount = ThunderBolt.getSettings().getConfig().getInt("armorStandCount", 15); // сколько стойк спавнить
        final int durationTicks = ThunderBolt.getSettings().getConfig().getInt("durationTicks", 35); // длительность первой фазы (тики)
        final double minRadius = ThunderBolt.getSettings().getConfig().getDouble("minRadius", 2.5); // минимальный радиус сферы
        final double maxRadius = ThunderBolt.getSettings().getConfig().getDouble("maxRadius", 3.0); // максимальный радиус сферы
        final double tetaAngleDeg = ThunderBolt.getSettings().getConfig().getDouble("tetaAngle", 315.0); // угол сектора в градусах
        final double minSeparation = ThunderBolt.getSettings().getConfig().getDouble("minSeparation", 0.6); // мин. дистанция между итоговыми позициями
        // параметры второй фазы (полётов по параболе для каждой стойки)
        final int perStandFlightTicks = ThunderBolt.getSettings().getConfig().getInt("perStandFlightTicks", 150); // длительность полёта одной стойки
        final int betweenFlightDelayTicks = ThunderBolt.getSettings().getConfig().getInt("betweenFlightDelayTicks", 0); // задержка между стартами (если нужна)
        // переключение: доля полёта, при которой запускается следующая стойка, и прирост после каждого переключения
        final double baseSwitchCoefficient = ThunderBolt.getSettings().getConfig().getDouble("switchCoefficient", 0.5); // базовый коэффициент переключения
        final double switchIncrement = ThunderBolt.getSettings().getConfig().getDouble("switchIncrement", 0.02); // прирост коэффициента при каждом переключении
        // лазер — шаг между точками (меньше = сплошнее, больше = реже частиц)
        final double laserStep = ThunderBolt.getSettings().getConfig().getDouble("laserStep", 0.2); // расстояние между точками лазера

        // имя частицы для лазера (например: ELECTRIC_SPARK). По умолчанию REDSTONE
        final String particleIdRaw = ThunderBolt.getSettings().getConfig().getString("particleID", "REDSTONE"); // id частицы из конфига

        // Параметры пульсации лазера (fadeIn, on, fadeOut, offMaxDelay)
        final int laserFadeIn = ThunderBolt.getSettings().getConfig().getInt("laser.fadeInTicks", 2); // тики плавного включения
        final int laserOn = ThunderBolt.getSettings().getConfig().getInt("laser.onTicks", 5); // тики полной активности лазера
        final int laserFadeOut = ThunderBolt.getSettings().getConfig().getInt("laser.fadeOutTicks", 2); // тики плавного затухания
        final int laserOffMaxDelay = ThunderBolt.getSettings().getConfig().getInt("laser.offMaxDelay", 5); // макс. случайная пауза между импульсами


        class SoundEffectOption { // вспомогательный класс для звуковых эффектов
            final String id;      // ключ в конфиге (например "removeItem")
            final Sound sound;    // enum Sound (Bukkit)
            final float volume;   // громкость
            final float pitch;    // высота
            SoundEffectOption(String id, Sound sound, float volume, float pitch) {
                this.id = id;
                this.sound = sound;
                this.volume = volume;
                this.pitch = pitch;
            }
        }
        // --- загрузка из конфига ---
        List<SoundEffectOption> soundOptions = new ArrayList<>();
        ConfigurationSection soundsSection = ThunderBolt.getSettings().getConfig().getConfigurationSection("sounds.effects");
        if (soundsSection != null) {
            for (String key : soundsSection.getKeys(false)) {
                List<?> raw = soundsSection.getList(key); // ожидается [SOUND_ENUM, volume, pitch]
                if (raw == null || raw.size() < 3) continue;

                String soundName = raw.get(0).toString();
                float volume;
                float pitch;
                try { volume = Float.parseFloat(raw.get(1).toString()); } catch (Exception e) { volume = 1.0f; }
                try { pitch = Float.parseFloat(raw.get(2).toString()); } catch (Exception e) { pitch = 1.0f; }

                Sound sound;
                try {
                    sound = Sound.valueOf(soundName); // преобразуем строку в enum
                } catch (Exception e) {
                    // если такого enum нет — пропускаем запись (или можно подставить дефолт)
                    continue;
                }

                soundOptions.add(new SoundEffectOption(key, sound, volume, pitch));
            }
        }


        // Чтение time.chances для случайного выбора времени
        class TimeOption { // вспомогательный класс для опций времени
            final String displayName; // текст для отображения
            final String command; // команда/идентификатор (пока не используется)
            final double chance; // шанс выпадения
            TimeOption(String displayName, String command, double chance) { this.displayName = displayName; this.command = command; this.chance = chance; } // конструктор
        }
        List<TimeOption> timeOptions = new ArrayList<>(); // список опций времени
        ConfigurationSection timeSection = ThunderBolt.getSettings().getConfig().getConfigurationSection("time.chances"); // секция конфига
        if (timeSection != null) { // если секция существует
            for (String key : timeSection.getKeys(false)) { // проходим по ключам
                List<?> raw = timeSection.getList(key); // читаем список из конфига
                if (raw == null || raw.size() < 3) continue; // проверка на корректность записи
                String name = raw.get(0).toString(); // текст отображения
                String cmd = raw.get(1).toString(); // команда/идентификатор
                double chance;
                try { chance = Double.parseDouble(raw.get(2).toString()); } catch (Exception e) { chance = 0.0; } // парсим шанс
                timeOptions.add(new TimeOption(name, cmd, chance)); // добавляем опцию
            }
        } else { // если секции нет — дефолты
            timeOptions.add(new TimeOption("&#FFA50030 дней", "30d", 0.91));
            timeOptions.add(new TimeOption("&#FFA50060 дней", "60d", 0.05));
            timeOptions.add(new TimeOption("&#FFA50090 дней", "90d", 0.03));
            timeOptions.add(new TimeOption("&#FFA500Навсегда", ";", 0.01));
        }

        // Попробуем получить Particle из имени из конфига, с безопасным fallback
        final Particle laserParticle; // переменная для частицы лазера
        Particle tmpParticle; // временная переменная
        try {
            tmpParticle = Particle.valueOf(particleIdRaw.trim().toUpperCase()); // пробуем получить enum по строке
        } catch (Exception ex) {
            tmpParticle = Particle.REDSTONE; // безопасный дефолт при ошибке
        }
        laserParticle = tmpParticle; // присваиваем окончательно

        // Читаем список возможных шлемов (displayName, MATERIAL, chance[, colorHex])
        class HeadOption { // вспомогательный класс для опций шлемов
            final String displayName; // отображаемое имя стойки
            final Material material; // материал предмета на голове
            double weight; // шанс/вес выпадения
            final Color color; // цвет для лазера (может быть null)
            HeadOption(String displayName, Material material, double weight, Color color) {
                this.displayName = displayName; this.material = material; this.weight = weight; this.color = color;
            } // конструктор
        }
        List<HeadOption> headOptions = new ArrayList<>(); // список опций шлемов
        ConfigurationSection headSection = ThunderBolt.getSettings().getConfig().getConfigurationSection("armorStandHeadList"); // секция шлемов
        if (headSection != null) { // если секция есть
            Set<String> keys = headSection.getKeys(false); // получаем все ключи
            for (String key : keys) { // проходим по каждому ключу
                List<?> raw = ThunderBolt.getSettings().getConfig().getList("armorStandHeadList." + key); // читаем список
                if (raw == null || raw.size() < 3) continue; // проверка на корректность
                String name = raw.get(0) == null ? "" : raw.get(0).toString(); // имя
                String matStr = raw.get(1) == null ? "STONE" : raw.get(1).toString(); // материал как строка
                double chance;
                try { chance = Double.parseDouble(raw.get(2).toString()); } catch (Exception e) { chance = 0.0; } // шанс
                Material mat = Material.matchMaterial(matStr); // получаем Material
                if (mat == null) mat = Material.STONE; // дефолтный материал

                // optional color: 4th element, formats accepted: "#RRGGBB" or "&#RRGGBB"
                Color color = null; // цвет по умолчанию null
                if (raw.size() >= 4 && raw.get(3) != null) { // если есть 4-й элемент
                    String colRaw = raw.get(3).toString().trim(); // читаем строку цвета
                    if (colRaw.startsWith("&")) colRaw = colRaw.substring(1); // убираем &
                    if (colRaw.startsWith("#")) colRaw = colRaw.substring(1); // убираем #
                    if (colRaw.length() == 6) { // если формат RRGGBB
                        try {
                            int rgb = Integer.parseInt(colRaw, 16); // парсим hex
                            int r = (rgb >> 16) & 0xFF; // красный
                            int g = (rgb >> 8) & 0xFF; // зелёный
                            int b = rgb & 0xFF; // синий
                            color = Color.fromRGB(r, g, b); // создаём Color
                        } catch (NumberFormatException ignored) {} // игнорируем ошибку парсинга
                    }
                }

                headOptions.add(new HeadOption(name, mat, Math.max(0.0, chance), color)); // добавляем опцию
            }
        }
        if (headOptions.isEmpty()) { // если не найдено опций — добавляем дефолт
            headOptions.add(new HeadOption("", Material.SHULKER_BOX, 1.0, null));
        }

        java.util.function.Function<String, String> colorize = s -> Message.translateColorCodes(s == null ? "" : s); // функция для перевода цветовых кодов

        // Подготовка мира и центра
        World world = Bukkit.getWorlds().get(0); // получаем первый мир (можно изменить)
        final Location center = new Location(world, blockX + 0.5, blockY + 0.5, blockZ + 0.5); // центр блока
        final Random rnd = new Random(); // генератор случайных чисел

        // Вспомогательный класс Target
        class Target { Vector offset; double radius; Target(Vector offset, double radius) { this.offset = offset; this.radius = radius; } } // хранит смещение и радиус

        // Генерация targets (как у вас)
        double halfAngleRad = Math.toRadians(tetaAngleDeg / 2.0); // половина угла в радианах
        if (halfAngleRad > Math.PI) halfAngleRad = Math.PI; // защищаем от >pi
        double cosThetaMax = Math.cos(halfAngleRad); // косинус верхней границы угла
        final double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0)); // золотой угол для распределения
        java.util.function.Supplier<Double> randRadius = () -> { double min3 = Math.pow(minRadius, 3); double max3 = Math.pow(maxRadius, 3); return Math.cbrt(min3 + rnd.nextDouble() * (max3 - min3)); }; // случайный радиус по объёму

        List<Target> targets = new ArrayList<>(armorStandCount); // список таргетов
        for (int i = 0; i < armorStandCount; i++) { // генерируем направления
            double t = (i + 0.5) / (double) armorStandCount; // параметр для равномерного распределения
            double z = 1.0 - t * (1.0 - cosThetaMax); // cos(theta)
            double phi = (i * goldenAngle) % (2.0 * Math.PI); // азимут
            double sinTheta = Math.sqrt(Math.max(0.0, 1.0 - z * z)); // sin(theta)
            double dirX = sinTheta * Math.cos(phi); // x компонента единичного вектора
            double dirY = z; // y компонента
            double dirZ = sinTheta * Math.sin(phi); // z компонента
            double r = randRadius.get(); // радиус
            Vector offset = new Vector(dirX * r, dirY * r, dirZ * r); // итоговое смещение
            targets.add(new Target(offset, r)); // добавляем таргет
        }

        // Коррекция по minSeparation (как у вас)
        for (int i = 0; i < targets.size(); i++) { // проходим по всем таргетам
            Target ti = targets.get(i); // текущий таргет
            int attempts = 0; // количество попыток коррекции
            boolean ok; // флаг успеха
            while (attempts < 20) { // ограничение по попыткам
                ok = true; // считаем, что всё ок
                for (int j = 0; j < targets.size(); j++) { // проверяем с остальными
                    if (i == j) continue; // пропускаем себя
                    double dist = targets.get(j).offset.clone().subtract(ti.offset).length(); // расстояние между таргетами
                    if (dist < minSeparation) { // если слишком близко
                        double newR = Math.min(maxRadius, ti.radius + (minSeparation - dist) * 0.6 + (rnd.nextDouble() * 0.2)); // предлагаем новый радиус
                        if (newR <= ti.radius + 1e-6) { // если нельзя увеличить радиус
                            Vector pert = ti.offset.clone(); // слегка сдвигаем направление
                            pert.setX(pert.getX() + (rnd.nextDouble() - 0.5) * 0.2);
                            pert.setZ(pert.getZ() + (rnd.nextDouble() - 0.5) * 0.2);
                            ti.offset = pert; // применяем сдвиг
                        } else {
                            Vector dir = ti.offset.clone().normalize(); // нормализуем направление
                            ti.radius = newR; // обновляем радиус
                            ti.offset = dir.multiply(newR); // умножаем направление на новый радиус
                        }
                        ok = false; // ещё нужны проверки
                        break; // выходим из внутреннего цикла
                    }
                }
                attempts++; // увеличиваем счётчик попыток
                if (ok) break; // если всё ок — выходим
            }
        }

        // Создание стоек (как у вас), но сохраняем также цвет для каждой стойки
        double totalWeight = 0.0; // суммарный вес для выбора шлемов
        for (HeadOption ho : headOptions) totalWeight += ho.weight; // суммируем веса
        if (totalWeight <= 0.0) { // если веса не заданы
            totalWeight = headOptions.size(); // делаем равные веса
            for (HeadOption ho : headOptions) ho.weight = 1.0; // задаём вес 1
        }

        List<ArmorStand> stands = new ArrayList<>(armorStandCount); // список созданных стойк
        List<Vector> finalOffsets = new ArrayList<>(armorStandCount); // список итоговых смещений
        List<Color> standColors = new ArrayList<>(armorStandCount); // цвета для лазеров

        for (int i = 0; i < armorStandCount; i++) { // создаём каждую стойку
            Target t = targets.get(i); // соответствующий таргет
            double pick = rnd.nextDouble() * totalWeight; // случайный выбор по весам
            double acc = 0.0; // аккумулятор веса
            HeadOption chosen = headOptions.get(0); // выбранная опция по умолчанию
            for (HeadOption ho : headOptions) { // выбираем по весам
                acc += ho.weight; // увеличиваем аккумулятор
                if (pick <= acc) { chosen = ho; break; } // если попали — выбираем
            }

            ItemStack helmet = new ItemStack(chosen.material); // создаём ItemStack для шлема
            ItemMeta meta = helmet.getItemMeta(); // получаем мета
            if (meta != null) { // если есть мета
                meta.setDisplayName(colorize.apply(chosen.displayName)); // задаём отображаемое имя
                helmet.setItemMeta(meta); // применяем мета
            }

            Location spawnLoc = center.clone(); // спавн точка — центр
            ArmorStand as = (ArmorStand) world.spawnEntity(spawnLoc, EntityType.ARMOR_STAND); // спавним стойку
            as.setGravity(false); // отключаем гравитацию
            as.setVisible(false); // делаем невидимой (только шлем виден)
            as.setInvulnerable(true); // делаем неуязвимой
            as.setMarker(false); // не делаем маркером
            as.setCustomName(colorize.apply(chosen.displayName)); // задаём кастомное имя
            as.setCustomNameVisible(true); // показываем имя
            as.setBasePlate(false); // убираем подставку
            as.setSmall(false); // размер по умолчанию
            as.setRemoveWhenFarAway(false); // не удалять при дальнем расстоянии
            try { as.setCanPickupItems(false); } catch (Throwable ignored) {} // запрет подбирать предметы (если есть метод)
            Objects.requireNonNull(as.getEquipment()).setHelmet(helmet); // ставим шлем на стойку
            stands.add(as); // сохраняем стойку
            finalOffsets.add(t.offset); // сохраняем её итоговое смещение
            standColors.add(chosen.color); // сохраняем цвет лазера для этой стойки
        }

        Plugin plugin = VariableX.getInstance(); // плагин для тасков

        // --- PREPARE PER-STAND PULSE STATE ---
        final int count = stands.size(); // количество фактически созданных стоек
        final int[] phaseTick = new int[count];       // current tick within cycle for each stand
        final int[] offDelay = new int[count];        // current randomized off delay after pulse
        final int basePulseLength = laserFadeIn + laserOn + laserFadeOut; // length of one pulse without off delay

        // initialize random offsets so pulses are not synchronized
        for (int i = 0; i < count; i++) { // для каждой стойки
            int randDelay = (laserOffMaxDelay > 0) ? rnd.nextInt(laserOffMaxDelay + 1) : 0; // случайный офф-дель
            offDelay[i] = randDelay; // сохраняем офф-дель
            // random start phase in [0 .. basePulseLength + offDelay]
            int cycleLen = basePulseLength + offDelay[i]; // длина цикла
            phaseTick[i] = cycleLen == 0 ? 0 : rnd.nextInt(cycleLen); // случайная фаза старта
        }

        // --- LASER TASK: рисует лазеры каждую итерацию для всех живых стоек, с пульсацией ---
        final AtomicBoolean laserRunning = new AtomicBoolean(true); // флаг работы лазера
        BukkitRunnable laserTask = new BukkitRunnable() { // таск для рисования лазеров
            @Override
            public void run() {
                if (!laserRunning.get()) { // если нужно остановиться
                    this.cancel(); // отменяем таск
                    return; // выходим
                }
                boolean anyAlive = false; // флаг, есть ли ещё стойки
                // стартовая точка лазера: уровень головы от центра блока (вы можете поменять высоту)
                Location laserStartBase = center.clone().add(0.0, 2.0, 0.0); // позиция начала лазера

                for (int i = 0; i < stands.size(); i++) { // проходим по всем стойкам
                    ArmorStand as = stands.get(i); // текущая стойка
                    if (as == null || as.isDead()) continue; // пропускаем если нет
                    anyAlive = true; // есть хотя бы одна живая стойка

                    // compute current intensity for this stand based on its phaseTick
                    // phases:
                    // 0..fadeIn-1  => fade in
                    // fadeIn..fadeIn+on-1 => full on
                    // fadeIn+on .. fadeIn+on+fadeOut-1 => fade out
                    // remaining => off
                    int fdIn = laserFadeIn; // fadeIn локально
                    int fdOn = laserOn; // on локально
                    int fdOut = laserFadeOut; // fadeOut локально
                    int curOff = offDelay[i]; // текущий офф-дель для стойки
                    int cycleLen = basePulseLength + curOff; // длина цикла для стойки
                    int pt = phaseTick[i]; // текущий тик фазы
                    double intensity = 0.0; // интенсивность (0..1)

                    if (pt < fdIn) { // фаза fade in
                        if (fdIn > 0) intensity = ((double)(pt + 1)) / (double) fdIn; else intensity = 1.0; // линейный рост
                    } else if (pt < fdIn + fdOn) { // полная фаза on
                        intensity = 1.0; // полная яркость
                    } else if (pt < fdIn + fdOn + fdOut) { // фаза fade out
                        int outTick = pt - (fdIn + fdOn); // тик внутри fade out
                        if (fdOut > 0) intensity = 1.0 - ((double)(outTick + 1) / (double) fdOut); else intensity = 0.0; // линейное убывание
                    } else {
                        intensity = 0.0; // офф
                    }

                    // advance phaseTick for next iteration; if we reach cycle end, reset and randomize new offDelay
                    phaseTick[i]++; // увеличиваем фазовый тик
                    if (phaseTick[i] >= cycleLen) { // если цикл завершился
                        int newOff = (laserOffMaxDelay > 0) ? rnd.nextInt(laserOffMaxDelay + 1) : 0; // новый офф
                        offDelay[i] = newOff; // сохраняем
                        phaseTick[i] = 0; // сбрасываем фазу
                        cycleLen = basePulseLength + newOff; // обновляем длину цикла
                    }

                    if (intensity <= 0.001) { // если интенсивность почти нулевая
                        // nothing to draw for this stand this tick
                        continue; // пропускаем рисование
                    }

                    // конец — центр головы стойки (приблизительно)
                    Location standLoc = as.getLocation(); // позиция стойки
                    Location laserEnd = standLoc.clone().add(0.0, 1.7, 0.0); // позиция конца лазера (голова)

                    Vector diff = laserEnd.toVector().subtract(laserStartBase.toVector()); // вектор от начала к концу
                    double length = diff.length(); // длина сегмента
                    if (length <= 0.0001) continue; // пропускаем если длина ноль
                    int steps = Math.max(1, (int) Math.ceil(length / laserStep)); // число шагов по лазеру
                    Vector stepVec = diff.clone().multiply(1.0 / (double) steps); // шаг вектора

                    Color color = standColors.get(i); // цвет для данной стойки
                    // scale dust size by intensity (min 0.1 to be visible)
                    Particle.DustOptions dust = (color == null) ? null : new Particle.DustOptions(color, (float)Math.max(0.1, Math.min(1.5, intensity))); // создаём DustOptions с размером по intensity

                    Location pos = laserStartBase.clone(); // начальная позиция для рисования
                    for (int s = 0; s <= steps; s++) { // идём по всем шагам
                        // Use intensity as probability to spawn each step (smooth fade in/out)
                        if (Math.random() <= intensity) { // условие вероятностного спавна для плавности
                            try {
                                if (dust != null) { // если есть DustOptions
                                    // spawn with dust data if supported
                                    world.spawnParticle(laserParticle, pos, 1, 0.0, 0.0, 0.0, 0.0, dust); // спавним частицу с данными
                                } else {
                                    // spawn without data
                                    world.spawnParticle(laserParticle, pos, 1, 0.0, 0.0, 0.0, 0.0); // спавним простую частицу
                                }
                            } catch (Throwable ignored) { // на случай ошибок у разных типов частиц
                                try { world.spawnParticle(laserParticle, pos, 1, 0.0, 0.0, 0.0, 0.0); } catch (Throwable ignored2) {} // финальный fallback
                            }
                        }
                        pos.add(stepVec); // двигаем позицию на шаг
                    }
                }

                if (!anyAlive) { // если ни одна стойка не жива
                    // все стойки удалены — остановим лазерную задачу
                    laserRunning.set(false); // отмечаем что лазер не нужен
                    this.cancel(); // отменяем таск
                }
            }
        };
        // запускаем лазерный таск сразу (каждый тик)
        laserTask.runTaskTimer(plugin, 0L, 1L); // запускаем таск каждые 1 тик

        String targetId = "start";
        soundOptions.stream()
                .filter(opt -> targetId.equals(opt.id))
                .findFirst()
                .ifPresent(opt -> player.playSound(center, opt.sound, opt.volume, opt.pitch));

        // --- Первая фаза: разлёт по ease-out (как у вас было), затем запускаем вторую фазу ---
        new BukkitRunnable() { // таск первой фазы
            int tick = 0; // счётчик тиков
            private double easeOutCubic(double t) { if (t <= 0) return 0; if (t >= 1) return 1; return 1 - Math.pow(1 - t, 3); } // функция ease-out cubic
            @Override
            public void run() {
                if (tick > durationTicks) { // если первая фаза закончилась
                    // Установка на финальные позиции
                    for (int i = 0; i < stands.size(); i++) { // выставляем стойки точно на финальные позиции
                        ArmorStand as = stands.get(i); // текущая стойка
                        if (as == null || as.isDead()) continue; // пропускаем если нет
                        Vector off = finalOffsets.get(i); // смещение
                        Location finalLoc = center.clone().add(off.getX(), off.getY(), off.getZ()); // финальная локейшн
                        as.teleport(finalLoc); // телепортируем стойку
                    }
                    this.cancel(); // отменяем таск первой фазы

                    // --- ВТОРАЯ ФАЗА: по очереди с перекрытиями, с глобальной защитой от дублей ---
                    List<Integer> order = new ArrayList<>(); // список индексов для случайного порядка
                    for (int i = 0; i < stands.size(); i++) order.add(i); // заполняем список
                    Collections.shuffle(order, rnd); // перемешиваем порядок

                    final AtomicInteger currentIndex = new AtomicInteger(0); // индекс следующей стойки для старта
                    final AtomicReference<Double> currentCoefficient = new AtomicReference<>(baseSwitchCoefficient); // текущий коэффициент переключения

                    final AtomicBoolean nextScheduled = new AtomicBoolean(false); // глобальный флаг планирования следующего старта

                    final Runnable[] startNextRef = new Runnable[1]; // ссылка на функцию старта следующей стойки

                    // Переменные для отслеживания последней стойки и отправки title
                    final AtomicReference<String> lastStandName = new AtomicReference<>(""); // имя последней удалённой стойки
                    final Set<Integer> completedStands = Collections.synchronizedSet(new HashSet<>()); // набор уже завершённых стоек

                    final java.util.function.Consumer<Integer> startFlight = (Integer idx) -> { // функция старта полёта одной стойки
                        if (idx == null) return; // защита от null
                        if (idx < 0 || idx >= order.size()) return; // защита от выхода за границы
                        int standIndex = order.get(idx); // индекс стойки в оригинальном списке
                        ArmorStand as = stands.get(standIndex); // получаем ArmorStand
                        if (as == null || as.isDead()) { // если стойка уже пропала
                            if (betweenFlightDelayTicks <= 0) { startNextRef[0].run(); } else { Bukkit.getScheduler().runTaskLater(plugin, startNextRef[0], betweenFlightDelayTicks); } // запускаем следующий
                            return; // выходим
                        }

                        Location startLoc = as.getLocation().clone(); // начальная позиция полёта (текущая позиция стойки)
                        Location endLoc = center.clone().add(0.0, 0.9, 0.0); // конечная точка полёта (над центром блока)

                        double horizontalDist = Math.hypot(startLoc.getX() - endLoc.getX(), startLoc.getZ() - endLoc.getZ()); // горизонтальная дистанция
                        double peak = Math.max(1.5, horizontalDist * 0.6 + 1.0); // пик параболы

                        final boolean[] nextStartedLocal = {false}; // локальный флаг, чтобы не запускать следующий несколько раз

                        new BukkitRunnable() { // внутренний таск для полёта стойки
                            int tTick = 0; // тик полёта

                            @Override
                            public void run() {
                                if (as == null || as.isDead()) { // если стойка пропала во время полёта
                                    this.cancel(); // отменяем таск
                                    if (nextScheduled.compareAndSet(false, true)) { // пытаемся занять право планирования следующего
                                        currentCoefficient.updateAndGet(v -> v + switchIncrement); // увеличиваем коэффициент
                                        if (betweenFlightDelayTicks <= 0) { startNextRef[0].run(); nextScheduled.set(false); } else { Bukkit.getScheduler().runTaskLater(plugin, () -> { startNextRef[0].run(); nextScheduled.set(false); }, betweenFlightDelayTicks); } // запускаем следующий с задержкой
                                    }
                                    return; // выходим
                                }

                                if (tTick > perStandFlightTicks) { // если полёт закончился
                                    // Запоминаем имя последней стойки
                                    lastStandName.set(as.getCustomName()); // сохраняем имя стойки
                                    completedStands.add(standIndex); // помечаем как завершённую

                                    // Если это последняя стойка - отправляем title
                                    if (completedStands.size() == stands.size()) { // если все стойки завершены
                                        double totalTimeWeight = 0.0; // сумма шансов
                                        for (TimeOption to : timeOptions) totalTimeWeight += to.chance; // считаем суммарный вес
                                        double pickTime = rnd.nextDouble() * totalTimeWeight; // случайное число в суммарном весе
                                        double accTime = 0.0; // аккумулятор для выбора
                                        TimeOption chosenTime = timeOptions.get(0); // по умолчанию первая
                                        for (TimeOption to : timeOptions) { accTime += to.chance; if (pickTime <= accTime) { chosenTime = to; break; } } // выбираем по шансам

                                        String titleText = lastStandName.get(); // основное название (название последней стойки)
                                        String subtitleText = chosenTime.displayName; // выбранный тайм-текст
                                        Message.sendTitle(player, colorize.apply(titleText), colorize.apply(subtitleText), 10, 70, 20); // отправляем title игроку
                                    }

                                    try { as.remove(); String targetId = "removeItem";
                                        soundOptions.stream()
                                                .filter(opt -> targetId.equals(opt.id))
                                                .findFirst()
                                                .ifPresent(opt -> player.playSound(center, opt.sound, opt.volume, opt.pitch)); } catch (Throwable ignored) {} // удаляем стойку
                                    this.cancel(); // отменяем таск полёта
                                    if (nextScheduled.compareAndSet(false, true)) { // пытаемся планировать следующий
                                        currentCoefficient.updateAndGet(v -> v + switchIncrement); // увеличиваем коэффициент
                                        if (betweenFlightDelayTicks <= 0) { startNextRef[0].run(); nextScheduled.set(false); } else { Bukkit.getScheduler().runTaskLater(plugin, () -> { startNextRef[0].run(); nextScheduled.set(false); }, betweenFlightDelayTicks); } // запускаем следующий с задержкой
                                    }
                                    return; // выходим
                                }

                                double coeff = currentCoefficient.get(); // текущий коэффициент переключения
                                int threshold = (int) Math.floor(perStandFlightTicks * coeff); // вычисляем порог для запуска следующего
                                if (!nextStartedLocal[0] && tTick >= threshold) { // если пора запустить следующий и локально ещё не стартовали
                                    if (nextScheduled.compareAndSet(false, true)) { // атомарно занимаем право планирования следующего
                                        nextStartedLocal[0] = true; // помечаем локально
                                        currentCoefficient.updateAndGet(v -> v + switchIncrement); // увеличиваем коэффициент
                                        if (betweenFlightDelayTicks <= 0) { startNextRef[0].run(); nextScheduled.set(false); } else { Bukkit.getScheduler().runTaskLater(plugin, () -> { startNextRef[0].run(); nextScheduled.set(false); }, betweenFlightDelayTicks); } // планируем следующий
                                    } else {
                                        nextStartedLocal[0] = true; // кто-то другой уже запланировал, помечаем чтобы не пытаться снова
                                    }
                                }

                                double s = (double) tTick / (double) perStandFlightTicks; // прогресс полёта 0..1
                                double x = startLoc.getX() * (1.0 - s) + endLoc.getX() * s; // лин. интерполяция X
                                double z = startLoc.getZ() * (1.0 - s) + endLoc.getZ() * s; // лин. интерполяция Z
                                double y = startLoc.getY() * (1.0 - s) + endLoc.getY() * s; // лин. интерполяция Y
                                double parabola = 4.0 * s * (1.0 - s) * peak; // параболическая добавка (пик в s=0.5)
                                Location pos = new Location(world, x, y + parabola, z); // итоговая позиция
                                try { as.teleport(pos); } catch (Throwable ignored) {} // телепортируем стойку
                                tTick++; // увеличиваем тик полёта
                            }
                        }.runTaskTimer(plugin, 0L, 1L); // запускаем таск полёта каждую итерацию
                    };

                    startNextRef[0] = () -> { // реализация запуска следующей стойки
                        int idx = currentIndex.getAndIncrement(); // получаем следующий индекс
                        if (idx >= order.size()) return; // если все обработаны — выходим
                        startFlight.accept(idx); // стартуем полёт для текущего индекса
                    };

                    startNextRef[0].run(); // запускаем первый полёт

                    return; // выходим из первой фазы
                }

                double progress = (double) tick / (double) durationTicks; // прогресс первой фазы 0..1
                double eased = easeOutCubic(progress); // сглаженный прогресс

                for (int i = 0; i < stands.size(); i++) { // перемещаем все стойки по easing
                    ArmorStand as = stands.get(i); // текущая стойка
                    if (as == null || as.isDead()) continue; // пропускаем если нет
                    Vector off = finalOffsets.get(i); // финальное смещение
                    Location pos = center.clone().add(off.getX() * eased, off.getY() * eased, off.getZ() * eased); // интерполированная позиция
                    as.teleport(pos); // телепортируем стойку
                }

                tick++; // увеличиваем тик первой фазы
            }

        }.runTaskTimer(plugin, 0L, 1L); // запускаем таск первой фазы каждую итерацию
    }
}
