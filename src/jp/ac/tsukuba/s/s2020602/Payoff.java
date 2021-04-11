package jp.ac.tsukuba.s.s2020602;

import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Team;

import java.math.BigDecimal;

/** 利得のクラス。各役職のプレイヤーが追放されたときの各陣営の利得を定義しています。 */
public class Payoff {
    
    /** 負けの利得（-2点） */
    public static final BigDecimal MAKE = BigDecimal.valueOf(-2);
    
    /** 引き分けの利得（0点） */
    public static final BigDecimal HIKIWAKE = BigDecimal.ZERO;
    
    /** 勝ちの利得（2点） */
    public static final BigDecimal KACHI = BigDecimal.valueOf(2);
    
    
    
    /** 人狼を追放したときの村人陣営の利得 */
    public static final BigDecimal werewolfExecute = KACHI;
    /** 人狼が追放されたときの人狼陣営の利得 */
    public static final BigDecimal werewolfExecuted = MAKE;
    
    /** 占い師が追放されたときの村人陣営の利得 */
    public static final BigDecimal seerExecuted = BigDecimal.valueOf(-1.2);
    /** 占い師を追放したときの人狼陣営の利得 */
    public static final BigDecimal seerExecute = seerExecuted.negate();
    
    /** 村人が追放されたときの村人陣営の利得 */
    public static final BigDecimal villagerExecuted = BigDecimal.valueOf(-0.3);
    /** 村人を追放したときの人狼陣営の利得 */
    public static final BigDecimal villagerExecute = villagerExecuted.negate();
    
    /** 狂人を追放したときの村人陣営の利得 */
    public static final BigDecimal possessedExecute = BigDecimal.valueOf(0.8);
    /** 狂人が追放されたときの人狼陣営の利得 */
    public static final BigDecimal possessedExecuted = possessedExecute.negate();
    
    /** 狩人が追放されたときの村人陣営の利得 */
    public static final BigDecimal bodyguardExecuted = BigDecimal.valueOf(-0.9);
    /** 狩人を追放したときの人狼陣営の利得 */
    public static final BigDecimal bodyguardExecute = villagerExecuted.negate();
    
    /** 霊媒師が追放されたときの村人陣営の利得 */
    public static final BigDecimal mediumExecuted = BigDecimal.valueOf(-0.6);
    /** 霊媒師を追放したときの人狼陣営の利得 */
    public static final BigDecimal mediumExecute = villagerExecuted.negate();
    
    
    public static BigDecimal payoff_5(Role 追放される人の役職, Team 主人公の陣営) {
        return payoff_15(追放される人の役職, 主人公の陣営);
    }
    
    /** 追放される人の役職と，自分（主人公）の陣営から，自分の利得を返します。 */
    public static BigDecimal payoff_15(Role 追放される人の役職, Team 主人公の陣営) {
        switch (主人公の陣営) {
            case VILLAGER:
                switch (追放される人の役職) {
                    case BODYGUARD:
                        return bodyguardExecuted;
                    case MEDIUM:
                        return mediumExecuted;
                    case POSSESSED:
                        return possessedExecute;
                    case SEER:
                        return seerExecuted;
                    case VILLAGER:
                        return villagerExecuted;
                    case WEREWOLF:
                        return werewolfExecute;
                    default:
                        // undefined
                        return BigDecimal.ZERO;
                }
            case WEREWOLF:
                switch (追放される人の役職) {
                    case BODYGUARD:
                        return bodyguardExecute;
                    case MEDIUM:
                        return mediumExecute;
                    case POSSESSED:
                        return possessedExecuted;
                    case SEER:
                        return seerExecute;
                    case VILLAGER:
                        return villagerExecute;
                    case WEREWOLF:
                        return werewolfExecuted;
                    default:
                        // undefined
                        return BigDecimal.ZERO;
                }
            default:
                // undefined
                return BigDecimal.ZERO;
        }
    }
}
