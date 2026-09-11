package com.xuanji.qqbot.ws;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewaySupervisorTest {
    @Test
    void backoffGrowsAndCaps() {
        long a1 = GatewaySupervisor.backoffMs(1);
        long a3 = GatewaySupervisor.backoffMs(3);
        long a20 = GatewaySupervisor.backoffMs(20);
        assertTrue(a1 >= 1000 && a1 < 1400);
        assertTrue(a3 > a1);
        assertTrue(a20 <= 60_000 + 300);
    }

    @Test
    void decideFollowsOfficialErrorTable() {
        // 无 close code（op7/op9/心跳/网络）→ 保持会话 Resume
        assertEquals(GatewaySupervisor.Action.RESUME, GatewaySupervisor.decide(-1));
        // 官方明确可 Resume
        assertEquals(GatewaySupervisor.Action.RESUME, GatewaySupervisor.decide(4009));
        assertEquals(GatewaySupervisor.Action.RESUME, GatewaySupervisor.decide(4008));
        // 无效 session/seq → 丢弃会话重新 Identify
        assertEquals(GatewaySupervisor.Action.IDENTIFY, GatewaySupervisor.decide(4006));
        assertEquals(GatewaySupervisor.Action.IDENTIFY, GatewaySupervisor.decide(4007));
        assertEquals(GatewaySupervisor.Action.IDENTIFY, GatewaySupervisor.decide(4900));
        assertEquals(GatewaySupervisor.Action.IDENTIFY, GatewaySupervisor.decide(4913));
        // 下架/封禁/配置类错误 → 停止重连
        assertEquals(GatewaySupervisor.Action.STOP, GatewaySupervisor.decide(4914));
        assertEquals(GatewaySupervisor.Action.STOP, GatewaySupervisor.decide(4915));
        assertEquals(GatewaySupervisor.Action.STOP, GatewaySupervisor.decide(4013));
        assertEquals(GatewaySupervisor.Action.STOP, GatewaySupervisor.decide(4014));
    }
}
