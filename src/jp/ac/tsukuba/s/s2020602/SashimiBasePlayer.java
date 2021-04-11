package jp.ac.tsukuba.s.s2020602;

import org.aiwolf.client.lib.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

/** すべてのPlayerクラスで共通している部分 */
public abstract class SashimiBasePlayer implements Player {
    
    /** デバッグ中かどうか：trueにすると，動作確認のためのprint文が実行されます。 */
    boolean debug = false;
    
    /** 最新のゲーム情報 */
    GameInfo currentGameInfo;
    
    /** プレイヤーの人数 */
    int N;
    
    /** このエージェント（このプログラム内では「主人公」と呼ぶことがあります。） */
    Agent me;
    
    /** 自分（主人公）の（本当の）役職 */
    Role myRole;
    
    /** 日付 */
    int day;
    
    /** talk()のターン */
    int talkTurn;
    
    /** GameInfo.talkList の読み込みのヘッド */
    int talkListHead;
    
    /** 発言用待ち行列。dayStart()でclearする。 */
    Deque<Content> talkQueue = new LinkedList<>();
    
    /** 今（主にupdate()で）読み取っている発言（Talk）の発言者 */
    Agent talker; // 取り扱い注意（？）
    
    /** 自分以外の生存エージェント（initialize()で初期化。executedAgentsとkilledAgentsの更新時に更新。） */
    List<Agent> aliveOthers;
    
    /** 追放されたエージェント（initialize()で初期化。dayStart()とupdate()で更新。） */
    List<Agent> executedAgents = new ArrayList<>();
    
    /** （人狼によって）殺されたエージェント（initialize()で初期化。dayStart()で更新。） */
    List<Agent> killedAgents = new ArrayList<>();
    
    /** （主人公を含む）全員のカミングアウト状況 */
    Map<Agent, Role> comingoutMap = new HashMap<>();
    
    /** BigDecimal の除算をする際の MathContext */
    MathContext mc = new MathContext(5, RoundingMode.HALF_UP);
    
    @Override
    public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
        currentGameInfo = gameInfo;
        day = -1;
        me = currentGameInfo.getAgent();
        N = gameSetting.getPlayerNum();
        myRole = currentGameInfo.getRole();
        
        aliveOthers = new ArrayList<>(currentGameInfo.getAliveAgentList());
        aliveOthers.remove(me);
        
        executedAgents.clear();
        killedAgents.clear();
        
        comingoutMap.clear();
    }
    
    @Override
    public void dayStart() {
        talkTurn = -1;
        talkListHead = 0;
        talkQueue.clear();
        
        // 前日に追放されたエージェントを登録
        addExecutedAgent(currentGameInfo.getExecutedAgent());
        
        // 昨夜死亡した（襲撃された）エージェントを登録
        if (!currentGameInfo.getLastDeadAgentList().isEmpty()) {
            addKilledAgent(currentGameInfo.getLastDeadAgentList().get(0));
        }
    }
    
    @Override
    public void update(GameInfo gameInfo) {
        currentGameInfo = gameInfo;
        
        // 一日の最初の呼び出しは dayStart() の前なので，何もしない
        if (currentGameInfo.getDay() == day + 1) {
            day = currentGameInfo.getDay();
            return;
        }
        
        // 以下，一日の2回目以降の呼び出しにおける処理
        
        // 夜限定：追放されたエージェントを登録
        addExecutedAgent(currentGameInfo.getLatestExecutedAgent());
        
        // 発言を読み取る
        for (int i = talkListHead; i < currentGameInfo.getTalkList().size(); i++) {
            Talk talk = currentGameInfo.getTalkList().get(i);
            talker = talk.getAgent();
            
            if (talker == me) {
                continue; // 自分の発言はスルーする
            }
            
            Content content = new Content(talk.getText());
            
            // subject が UNSPEC の場合は，発話者に入れ替える
            if (content.getSubject() == Content.UNSPEC) {
                content = replaceSubject(content, talker);
            }
            
            parseSentence(content);
        }
        
        talkListHead = currentGameInfo.getTalkList().size();
    }
    
    @Override
    public void finish() {
    }
    
    static Content replaceSubject(Content content, Agent newSubject) {
        if (content.getTopic() == Topic.SKIP || content.getTopic() == Topic.OVER) {
            return content;
        }
        if (newSubject == Content.UNSPEC) {
            return new Content(Content.stripSubject(content.getText()));
        } else {
            return new Content(newSubject + " " + Content.stripSubject(content.getText()));
        }
    }
    
    /** contentを発言キューに格納します。 */
    void enqueueTalk(Content content) {
        if (content.getSubject() == Content.UNSPEC) {
            talkQueue.offer(replaceSubject(content, me));
        } else {
            talkQueue.offer(content);
        }
    }
    
    /** 発言キューからcontentを1つ取り出して，返します。 */
    String dequeueTalk() {
        if (talkQueue.isEmpty()) {
            return Talk.SKIP;
        }
        Content content = talkQueue.poll();
        if (content.getSubject() == me) {
            return Content.stripSubject(content.getText());
        }
        return content.getText();
    }
    
    /** 再帰的に文を解析する */
    abstract void parseSentence(Content content);
    
    /** 演算子文を解析する */
    void parseOperator(Content content) {
        switch (content.getOperator()) {
            case BECAUSE:
                parseSentence(content.getContentList().get(1));
                break;
            case DAY:
                parseSentence(content.getContentList().get(0));
                break;
            case AND:
            case OR:
            case XOR:
                for (Content c : content.getContentList()) {
                    parseSentence(c);
                }
                break;
            case REQUEST:
                // 未対応です。
                break;
            case INQUIRE:
                // 未対応です。
                break;
            default:
                break;
        }
    }
    
    abstract void addExecutedAgent(Agent executedAgent);
    
    abstract void addKilledAgent(Agent killedAgent);
    
    /** debugがtrueのときに限り，動作確認用の文字列を出力します。カッコ内に呼び出し元のメソッドと行番号もプリントします。 */
    void log(String str) {
        if (debug) {
            System.out.print("log: " + str);
            printPlace();
        }
    }
    
    /** どこで そのメソッドを呼んでいるかを印刷 */
    public static void printPlace() {
        StackTraceElement[] st = (new Throwable()).getStackTrace();
        
        // log()メソッドの1つ前の呼び出し元（st[2]）のメソッド名と行番号
        String methodName = st[2].getMethodName();
        int line = st[2].getLineNumber();
        
        System.out.println("（" + methodName + "：" + line + "）");
    }
    
    /** エージェントが生きているかどうかを返す */
    boolean isAlive(Agent agent) {
        return currentGameInfo.getStatusMap().get(agent) == Status.ALIVE;
    }
    
    /** エージェントが殺されたかどうかを返す */
    boolean isKilled(Agent agent) {
        return killedAgents.contains(agent);
    }
    
    /** エージェントがカミングアウトしたかどうかを返す */
    boolean isCo(Agent agent) {
        return comingoutMap.containsKey(agent);
    }
    
    /** 役職がカミングアウトされたかどうかを返す */
    boolean isCo(Role role) {
        return comingoutMap.containsValue(role);
    }
    
    // 発話生成を簡略化するためのwrapper
    
    static Content agreeContent(Agent subject, TalkType talkType, int talkDay, int talkID) {
        return new Content(new AgreeContentBuilder(subject, talkType, talkDay, talkID));
    }
    
    static Content disagreeContent(Agent subject, TalkType talkType, int talkDay, int talkID) {
        return new Content(new DisagreeContentBuilder(subject, talkType, talkDay, talkID));
    }
    
    static Content voteContent(Agent subject, Agent target) {
        return new Content(new VoteContentBuilder(subject, target));
    }
    
    static Content votedContent(Agent subject, Agent target) {
        return new Content(new VotedContentBuilder(subject, target));
    }
    
    static Content attackContent(Agent subject, Agent target) {
        return new Content(new AttackContentBuilder(subject, target));
    }
    
    static Content attackedContent(Agent subject, Agent target) {
        return new Content(new AttackedContentBuilder(subject, target));
    }
    
    static Content guardContent(Agent subject, Agent target) {
        return new Content(new GuardCandidateContentBuilder(subject, target));
    }
    
    static Content guardedContent(Agent subject, Agent target) {
        return new Content(new GuardedAgentContentBuilder(subject, target));
    }
    
    static Content estimateContent(Agent subject, Agent target, Role role) {
        return new Content(new EstimateContentBuilder(subject, target, role));
    }
    
    static Content coContent(Agent subject, Agent target, Role role) {
        return new Content(new ComingoutContentBuilder(subject, target, role));
    }
    
    static Content requestContent(Agent subject, Agent target, Content content) {
        return new Content(new RequestContentBuilder(subject, target, content));
    }
    
    static Content inquiryContent(Agent subject, Agent target, Content content) {
        return new Content(new InquiryContentBuilder(subject, target, content));
    }
    
    static Content divinationContent(Agent subject, Agent target) {
        return new Content(new DivinationContentBuilder(subject, target));
    }
    
    static Content divinedContent(Agent subject, Agent target, Species result) {
        return new Content(new DivinedResultContentBuilder(subject, target, result));
    }
    
    static Content identContent(Agent subject, Agent target, Species result) {
        return new Content(new IdentContentBuilder(subject, target, result));
    }
    
    static Content andContent(Agent subject, Content... contents) {
        return new Content(new AndContentBuilder(subject, contents));
    }
    
    static Content orContent(Agent subject, Content... contents) {
        return new Content(new OrContentBuilder(subject, contents));
    }
    
    static Content xorContent(Agent subject, Content content1, Content content2) {
        return new Content(new XorContentBuilder(subject, content1, content2));
    }
    
    static Content notContent(Agent subject, Content content) {
        return new Content(new NotContentBuilder(subject, content));
    }
    
    static Content dayContent(Agent subject, int day, Content content) {
        return new Content(new DayContentBuilder(subject, day, content));
    }
    
    static Content becauseContent(Agent subject, Content reason, Content action) {
        return new Content(new BecauseContentBuilder(subject, reason, action));
    }
    
}
