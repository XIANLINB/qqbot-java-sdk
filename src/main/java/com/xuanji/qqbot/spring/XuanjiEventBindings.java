package com.xuanji.qqbot.spring;

import com.xuanji.qqbot.event.C2cMessageCreate;
import com.xuanji.qqbot.event.C2cMsgReceive;
import com.xuanji.qqbot.event.C2cMsgReject;
import com.xuanji.qqbot.event.Event;
import com.xuanji.qqbot.event.EventType;
import com.xuanji.qqbot.event.FriendAdd;
import com.xuanji.qqbot.event.FriendDel;
import com.xuanji.qqbot.event.GroupAddRobot;
import com.xuanji.qqbot.event.GroupAtMessageCreate;
import com.xuanji.qqbot.event.GroupDelRobot;
import com.xuanji.qqbot.event.GroupJoinRequest;
import com.xuanji.qqbot.event.GroupMemberAdd;
import com.xuanji.qqbot.event.GroupMemberRemove;
import com.xuanji.qqbot.event.GroupMessageCreate;
import com.xuanji.qqbot.event.GroupMsgReceive;
import com.xuanji.qqbot.event.GroupMsgReject;
import com.xuanji.qqbot.event.InteractionCreate;
import com.xuanji.qqbot.event.SubscribeMessageStatus;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 事件方法绑定：注解 → 事件类型 / 事件名。
 */
final class XuanjiEventBindings {
    record Binding(
            Class<? extends Annotation> ann,
            String eventName,
            Class<? extends Event> eventType,
            Function<Event, String> contentExtractor
    ) {
    }

    private static final List<Binding> BINDINGS = List.of(
            // 单聊
            new Binding(C2cMessageCreateEvent.class, EventType.C2C_MESSAGE_CREATE,
                    C2cMessageCreate.class, e -> ((C2cMessageCreate) e).content()),
            new Binding(FriendAddEvent.class, EventType.FRIEND_ADD, FriendAdd.class, e -> null),
            new Binding(FriendDelEvent.class, EventType.FRIEND_DEL, FriendDel.class, e -> null),
            new Binding(C2cMsgRejectEvent.class, EventType.C2C_MSG_REJECT, C2cMsgReject.class, e -> null),
            new Binding(C2cMsgReceiveEvent.class, EventType.C2C_MSG_RECEIVE, C2cMsgReceive.class, e -> null),
            // 群聊：@Command 用去掉 <@id>/表情后的正文匹配
            new Binding(GroupAtMessageCreateEvent.class, EventType.GROUP_AT_MESSAGE_CREATE,
                    GroupAtMessageCreate.class, e -> ((GroupAtMessageCreate) e).contentAsText()),
            new Binding(GroupMessageCreateEvent.class, EventType.GROUP_MESSAGE_CREATE,
                    GroupMessageCreate.class, e -> ((GroupMessageCreate) e).contentWithoutMentions()),
            new Binding(GroupAddRobotEvent.class, EventType.GROUP_ADD_ROBOT, GroupAddRobot.class, e -> null),
            new Binding(GroupDelRobotEvent.class, EventType.GROUP_DEL_ROBOT, GroupDelRobot.class, e -> null),
            new Binding(GroupMemberAddEvent.class, EventType.GROUP_MEMBER_ADD, GroupMemberAdd.class, e -> null),
            new Binding(GroupMemberRemoveEvent.class, EventType.GROUP_MEMBER_REMOVE, GroupMemberRemove.class, e -> null),
            new Binding(GroupMsgReceiveEvent.class, EventType.GROUP_MSG_RECEIVE, GroupMsgReceive.class, e -> null),
            new Binding(GroupMsgRejectEvent.class, EventType.GROUP_MSG_REJECT, GroupMsgReject.class, e -> null),
            new Binding(SubscribeMessageStatusEvent.class, EventType.SUBSCRIBE_MESSAGE_STATUS,
                    SubscribeMessageStatus.class, e -> null),
            new Binding(GroupJoinRequestEvent.class, EventType.GROUP_JOIN_REQUEST,
                    GroupJoinRequest.class, e -> null),
            // 互动
            new Binding(InteractionCreateEvent.class, EventType.INTERACTION_CREATE,
                    InteractionCreate.class, e -> null)
    );

    private XuanjiEventBindings() {
    }

    /**
     * 找出方法上的事件绑定（至多一个）。
     */
    static Binding find(Method method) {
        for (Binding b : BINDINGS) {
            if (AnnotatedElementUtils.hasAnnotation(method, b.ann())) {
                return b;
            }
        }
        return null;
    }

    /**
     * 取方法上全部 @Command。
     */
    static List<Command> commands(Method method) {
        List<Command> list = new ArrayList<>();
        Command[] all = method.getAnnotationsByType(Command.class);
        if (all != null) {
            for (Command c : all) {
                list.add(c);
            }
        }
        return list;
    }

    /**
     * 用 @Command 过滤消息内容。多个 Command 为或；无 Command 则始终通过。
     *
     * @return true 表示应触发
     */
    static boolean matchesCommands(List<Command> commands, String content) {
        if (commands.isEmpty()) {
            return true;
        }
        String raw = content == null ? "" : content;
        String trimmed = raw.trim();
        for (Command c : commands) {
            if (matchesOne(c, raw, trimmed)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesOne(Command c, String raw, String trimmed) {
        String pattern = c.value() == null ? "" : c.value();
        if (pattern.isEmpty()) {
            return false;
        }
        return switch (c.type()) {
            case PREFIX -> startsWith(trimmed, pattern, c.ignoreCase());
            case SUFFIX -> endsWith(trimmed, pattern, c.ignoreCase());
            case EXACT -> eq(trimmed, pattern, c.ignoreCase());
            case REGEX -> {
                int flags = c.ignoreCase()
                        ? java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE
                        : 0;
                yield java.util.regex.Pattern.compile(pattern, flags).matcher(raw).matches();
            }
        };
    }

    private static boolean startsWith(String s, String p, boolean ignoreCase) {
        if (ignoreCase) {
            return s.toLowerCase(java.util.Locale.ROOT).startsWith(p.toLowerCase(java.util.Locale.ROOT));
        }
        return s.startsWith(p);
    }

    private static boolean endsWith(String s, String p, boolean ignoreCase) {
        if (ignoreCase) {
            return s.toLowerCase(java.util.Locale.ROOT).endsWith(p.toLowerCase(java.util.Locale.ROOT));
        }
        return s.endsWith(p);
    }

    private static boolean eq(String s, String p, boolean ignoreCase) {
        if (ignoreCase) {
            return s.equalsIgnoreCase(p);
        }
        return s.equals(p);
    }
}
