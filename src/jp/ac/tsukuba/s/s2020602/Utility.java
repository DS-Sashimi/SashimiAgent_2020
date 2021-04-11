package jp.ac.tsukuba.s.s2020602;

import org.aiwolf.common.data.Agent;

import java.util.List;
import java.util.Map;

/** ときどき使うメソッド */
public class Utility {
    
    /** カウンタを初期化する（主に15人村のテーブルで使う） */
    public static void カウンタを初期化(Map<Agent, Integer> counter, List<Agent> agentList) {
        for (Agent agt : agentList) {
            counter.put(agt, 0);
        }
    }
    
    /** リストからランダムに選んで返す */
    public static <T> T randomSelect(List<T> list) {
        if (list.isEmpty()) {
            return null;
        } else {
            return list.get((int) (Math.random() * list.size()));
        }
    }
    
}
