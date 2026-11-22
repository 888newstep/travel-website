package travel.route.algorithm;

/**
 * GA 参数预设档位（2026-11-22 暴力基准实验结论）：
 * 混合局部搜索（NN种子+2-opt）下参数对质量不敏感，参数只用于调时间预算——
 * 因此用户偏好不进参数空间，按"用户愿意等多久"选档位即可。
 */
public enum GaPreset {
    /** 快速档：n<=10 的单日小规模，或接口 P95 压力大时 */
    FAST(60, 80, 0.05, 0.8, 3, 20),
    /** 标准档：默认，网格寻优最优参数（pop=150, mut=0.05） */
    STANDARD(150, 200, 0.05, 0.8, 7, 30),
    /** 高质量档：路线分享页/离线批算，允许更久换更优 */
    QUALITY(300, 400, 0.05, 0.8, 15, 40);

    final int population;
    final int generations;
    final double mutationRate;
    final double crossoverRate;
    final int elitismCount;
    final int earlyStopGenerations;

    GaPreset(int population, int generations, double mutationRate,
             double crossoverRate, int elitismCount, int earlyStopGenerations) {
        this.population = population;
        this.generations = generations;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.elitismCount = elitismCount;
        this.earlyStopGenerations = earlyStopGenerations;
    }

    /** 按名称解析，非法值回落 STANDARD（用户输入不信任） */
    public static GaPreset fromName(String name) {
        if (name == null || name.isBlank()) {
            return STANDARD;
        }
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return STANDARD;
        }
    }

    /** 按规模自动选档：小规模快档足够（暴力实验：n<=10 全局最优率两档相同） */
    public static GaPreset forScale(int attractionCount) {
        if (attractionCount <= 8) {
            return FAST;
        }
        if (attractionCount >= 60) {
            return QUALITY;
        }
        return STANDARD;
    }
}
