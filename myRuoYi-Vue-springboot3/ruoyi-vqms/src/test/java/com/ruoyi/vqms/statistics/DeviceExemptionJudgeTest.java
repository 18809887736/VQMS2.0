package com.ruoyi.vqms.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ruoyi.vqms.statistics.DeviceExemptionJudge.DeviceSample;
import com.ruoyi.vqms.statistics.DeviceExemptionJudge.Direction;
import com.ruoyi.vqms.statistics.DeviceExemptionJudge.PqPoint;
import com.ruoyi.vqms.statistics.DeviceExemptionJudge.Verdict;

class DeviceExemptionJudgeTest {

    private static final BigDecimal TOL = new BigDecimal("2000");

    private static final List<PqPoint> GEN_CURVE = List.of(
            new PqPoint(new BigDecimal("0"), new BigDecimal("250000"), new BigDecimal("-150000")),
            new PqPoint(new BigDecimal("150000"), new BigDecimal("225000"), new BigDecimal("-120000")),
            new PqPoint(new BigDecimal("300000"), new BigDecimal("200000"), new BigDecimal("-100000")));

    private static DeviceSample gen(long id, String code, BigDecimal p, BigDecimal q) {
        return new DeviceSample(id, code, 1, true, p, q,
                null, new BigDecimal("200000"), new BigDecimal("-100000"), GEN_CURVE);
    }

    @Test
    void directionResolvedByBandPosition() {
        assertEquals(Direction.INJECT, DeviceExemptionJudge.resolveDirection(
                bd("230"), bd("232"), bd("224"), bd("225")));
        assertEquals(Direction.ABSORB, DeviceExemptionJudge.resolveDirection(
                bd("230"), bd("232"), bd("233"), bd("234")));
        assertNull(DeviceExemptionJudge.resolveDirection(
                bd("230"), bd("232"), bd("230.5"), bd("231.5")));
        assertNull(DeviceExemptionJudge.resolveDirection(bd("230"), bd("232"), null, bd("231")));
    }

    @Test
    void allGeneratorsAtCurveUpLimitLowVoltageExempted() {
        // P=300MW → 曲线端点 qUp=200000；两机 Q 都顶满 → 免考
        List<DeviceSample> devices = List.of(
                gen(1, "GEN_01", bd("300000"), bd("199000")),
                gen(2, "GEN_02", bd("300000"), bd("200000")));
        Verdict v = DeviceExemptionJudge.judge(devices, Direction.INJECT, TOL);
        assertTrue(v.exempted());
        assertTrue(v.blockers().isEmpty());
    }

    @Test
    void curveInterpolatedAtMidP() {
        // P=75MW → qUp 插值 = 250000 + (225000−250000)×0.5 = 237500；Q=236000 在容差内顶满
        List<DeviceSample> devices = List.of(gen(1, "GEN_01", bd("75000"), bd("236000")));
        assertTrue(DeviceExemptionJudge.judge(devices, Direction.INJECT, TOL).exempted());
        // 留余力：Q=230000 距 237500 差 7500 > 2000 → 不免
        assertFalse(DeviceExemptionJudge.judge(
                List.of(gen(1, "GEN_01", bd("75000"), bd("230000"))), Direction.INJECT, TOL).exempted());
    }

    @Test
    void absorbDirectionAtQDownLimitExempted() {
        // 偏高：P=150MW → qDown 插值 = −135000；Q=−134000 顶满
        List<DeviceSample> devices = List.of(gen(1, "GEN_01", bd("150000"), bd("-134000")));
        assertTrue(DeviceExemptionJudge.judge(devices, Direction.ABSORB, TOL).exempted());
    }

    @Test
    void wrongDirectionNotExempted() {
        // 偏低需要发出，机组却在吸收 → 不免考
        Verdict v = DeviceExemptionJudge.judge(
                List.of(gen(1, "GEN_01", bd("300000"), bd("-98000"))), Direction.INJECT, TOL);
        assertFalse(v.exempted());
        assertTrue(v.blockers().get(0).contains("NOT_AT_Q_UP"));
    }

    @Test
    void oneDeviceSlackBlocksAll() {
        // 一台顶满一台留余力 → 整体不免考（全部设备尽力才免）
        List<DeviceSample> devices = List.of(
                gen(1, "GEN_01", bd("300000"), bd("200000")),
                gen(2, "GEN_02", bd("300000"), bd("180000")));
        Verdict v = DeviceExemptionJudge.judge(devices, Direction.INJECT, TOL);
        assertFalse(v.exempted());
        assertEquals(1, v.blockers().size());
        assertTrue(v.blockers().get(0).startsWith("GEN_02"));
    }

    @Test
    void missingTelemetryBlocks() {
        Verdict v = DeviceExemptionJudge.judge(
                List.of(gen(1, "GEN_01", bd("300000"), null)), Direction.INJECT, TOL);
        assertFalse(v.exempted());
        assertTrue(v.blockers().get(0).contains("Q_MISSING"));
    }

    @Test
    void missingPBlocksGeneratorCurve() {
        // 发电机无 P → 曲线不可插值 → LIMIT_UNRESOLVED
        Verdict v = DeviceExemptionJudge.judge(
                List.of(gen(1, "GEN_01", null, bd("200000"))), Direction.INJECT, TOL);
        assertFalse(v.exempted());
        assertTrue(v.blockers().get(0).contains("LIMIT_UNRESOLVED"));
    }

    @Test
    void inverterCapacityCircle() {
        // S=350000, P=300000 → Q=±√(350000²−300000²)=±180277；发出侧顶满
        DeviceSample inv = new DeviceSample(3, "INV_01", 2, true,
                bd("300000"), bd("180000"), bd("350000"), null, null, List.of());
        assertTrue(DeviceExemptionJudge.judge(List.of(inv), Direction.INJECT, TOL).exempted());
        // 吸收侧：Q=−179000 ≥ −(180277+2000) 顶满
        DeviceSample invAbs = new DeviceSample(3, "INV_01", 2, true,
                bd("300000"), bd("-179000"), bd("350000"), null, null, List.of());
        assertTrue(DeviceExemptionJudge.judge(List.of(invAbs), Direction.ABSORB, TOL).exempted());
        // 无 S → 不可解析
        DeviceSample noS = new DeviceSample(3, "INV_01", 2, true,
                bd("300000"), bd("180000"), null, null, null, List.of());
        assertFalse(DeviceExemptionJudge.judge(List.of(noS), Direction.INJECT, TOL).exempted());
    }

    @Test
    void unidirectionalDevices() {
        // 电容器组：发出侧顶额定=免；吸收侧能力为 0，Q≈0 即尽力
        DeviceSample cap = new DeviceSample(4, "CAP_01", 4, true,
                null, bd("40000"), null, bd("40000"), null, List.of());
        assertTrue(DeviceExemptionJudge.judge(List.of(cap), Direction.INJECT, TOL).exempted());
        DeviceSample capOff = new DeviceSample(4, "CAP_01", 4, true,
                null, bd("0"), null, bd("40000"), null, List.of());
        assertTrue(DeviceExemptionJudge.judge(List.of(capOff), Direction.ABSORB, TOL).exempted());
        // 电抗器：吸收侧顶额定；发出侧能力 0，Q≈0 尽力
        DeviceSample reac = new DeviceSample(5, "REAC_01", 5, true,
                null, bd("-30000"), null, null, bd("-30000"), List.of());
        assertTrue(DeviceExemptionJudge.judge(List.of(reac), Direction.ABSORB, TOL).exempted());
        DeviceSample reacOff = new DeviceSample(5, "REAC_01", 5, true,
                null, bd("0"), null, null, bd("-30000"), List.of());
        assertTrue(DeviceExemptionJudge.judge(List.of(reacOff), Direction.INJECT, TOL).exempted());
    }

    @Test
    void svcUsesStaticRating() {
        DeviceSample svc = new DeviceSample(6, "SVC_01", 3, true,
                null, bd("-99000"), null, bd("100000"), bd("-100000"), List.of());
        assertTrue(DeviceExemptionJudge.judge(List.of(svc), Direction.ABSORB, TOL).exempted());
    }

    @Test
    void nonLoopDevicesIgnoredAndEmptyLoopNotExempt() {
        // 非闭环设备不考察：全部不在闭环 → 不免考
        DeviceSample off = new DeviceSample(1, "GEN_01", 1, false,
                bd("300000"), bd("200000"), null, bd("200000"), bd("-100000"), GEN_CURVE);
        Verdict v = DeviceExemptionJudge.judge(List.of(off), Direction.INJECT, TOL);
        assertFalse(v.exempted());
        assertTrue(v.blockers().contains("NO_AVC_LOOP_DEVICE"));
    }

    @Test
    void nullDirectionNotExempted() {
        assertFalse(DeviceExemptionJudge.judge(
                List.of(gen(1, "GEN_01", bd("300000"), bd("200000"))), null, TOL).exempted());
    }

    @Test
    void generatorWithoutCurveFallsBackToStaticRating() {
        DeviceSample staticGen = new DeviceSample(1, "GEN_01", 1, true,
                bd("300000"), bd("199000"), null, bd("200000"), bd("-100000"), List.of());
        assertTrue(DeviceExemptionJudge.judge(List.of(staticGen), Direction.INJECT, TOL).exempted());
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
