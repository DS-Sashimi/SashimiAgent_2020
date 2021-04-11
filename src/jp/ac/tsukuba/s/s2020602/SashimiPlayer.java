package jp.ac.tsukuba.s.s2020602;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/** すべての もととなる Player（ゲームの人数と自分の役職によって各クラスを呼び出す） */
public class SashimiPlayer implements Player {
    
    /** プレイヤー本体 */
    private Player mePlayer;
    
    @Override
    public String getName() {
        return mePlayer.getName();
    }
    
    @Override
    public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
        // プレイヤーの人数と自分の役職によって，どのクラスのPlayerにするかを決めます。
        switch (gameSetting.getPlayerNum()) {
            case 5:
                switch (gameInfo.getRole()) {
                    case VILLAGER:
                        mePlayer = new SashimiVillager_5();
                        break;
                    case SEER:
                        mePlayer = new SashimiSeer_5();
                        break;
                    case WEREWOLF:
                        mePlayer = new SashimiWerewolf_5();
                        break;
                    case POSSESSED:
                        mePlayer = new SashimiPossessed_5();
                        break;
                    default:
                        // undefined
                        mePlayer = new SashimiBasePlayer_15();
                        break;
                }
                break;
            case 15:
            default:
                switch (gameInfo.getRole()) {
                    case VILLAGER:
                        mePlayer = new SashimiVillager_15();
                        break;
                    case SEER:
                        mePlayer = new SashimiSeer_15();
                        break;
                    case BODYGUARD:
                        mePlayer = new SashimiBodyguard();
                        break;
                    case MEDIUM:
                        mePlayer = new SashimiMedium();
                        break;
                    case WEREWOLF:
                        mePlayer = new SashimiWerewolf_15();
                        break;
                    case POSSESSED:
                        mePlayer = new SashimiPossessed_15();
                        break;
                    default:
                        // undefined
                        mePlayer = new SashimiBasePlayer_15();
                        break;
                }
                break;
        }
        
        mePlayer.initialize(gameInfo, gameSetting);
    }
    
    @Override
    public void update(GameInfo gameInfo) {
        mePlayer.update(gameInfo);
    }
    
    @Override
    public void dayStart() {
        mePlayer.dayStart();
    }
    
    @Override
    public void finish() {
        mePlayer.finish();
    }
    
    @Override
    public String talk() {
        return mePlayer.talk();
    }
    
    @Override
    public Agent vote() {
        return mePlayer.vote();
    }
    
    @Override
    public Agent divine() {
        return mePlayer.divine();
    }
    
    @Override
    public Agent guard() {
        return mePlayer.guard();
    }
    
    @Override
    public Agent attack() {
        return mePlayer.attack();
    }
    
    @Override
    public String whisper() {
        return mePlayer.whisper();
    }
    
}
