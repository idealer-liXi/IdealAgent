package com.idealagent.infrastructure.repository;

import com.idealagent.domain.chat.model.entity.ChatMessage;
import com.idealagent.domain.chat.model.entity.ChatSession;
import com.idealagent.domain.chat.repository.IChatRepository;
import com.idealagent.infrastructure.persistent.dao.IAiMessageDao;
import com.idealagent.infrastructure.persistent.dao.IAiSessionDao;
import com.idealagent.infrastructure.persistent.po.AiMessage;
import com.idealagent.infrastructure.persistent.po.AiSession;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ChatRepository implements IChatRepository {
    private final IAiSessionDao sessionDao;
    private final IAiMessageDao messageDao;

    public ChatRepository(IAiSessionDao sessionDao, IAiMessageDao messageDao) {
        this.sessionDao = sessionDao;
        this.messageDao = messageDao;
    }

    @Override
    public Optional<ChatSession> findSession(String sessionId, Long userId) {
        return Optional.ofNullable(toSession(sessionDao.queryBySessionIdAndUserId(sessionId, userId)));
    }

    @Override
    public ChatSession saveSession(ChatSession session) {
        sessionDao.insert(toSessionPo(session));
        return session;
    }

    @Override
    public List<ChatSession> listSessions(Long userId) {
        return sessionDao.listByUserId(userId).stream().map(this::toSession).toList();
    }

    @Override
    public ChatMessage saveMessage(ChatMessage message) {
        messageDao.insert(toMessagePo(message));
        return message;
    }

    @Override
    public List<ChatMessage> listMessages(String sessionId, Long userId) {
        return messageDao.listBySessionId(sessionId).stream().map(this::toMessage).toList();
    }

    private AiSession toSessionPo(ChatSession session) {
        AiSession po = new AiSession();
        po.setSessionId(session.getSessionId());
        po.setSessionType(session.getType());
        po.setSessionTitle(session.getTitle());
        po.setUserId(session.getUserId());
        po.setTargetId(session.getTargetId());
        return po;
    }

    private ChatSession toSession(AiSession po) {
        if (po == null) {
            return null;
        }
        ChatSession session = new ChatSession();
        session.setSessionId(po.getSessionId());
        session.setType(po.getSessionType());
        session.setTitle(po.getSessionTitle());
        session.setUserId(po.getUserId());
        session.setTargetId(po.getTargetId());
        session.setCreateTime(po.getCreateTime());
        session.setUpdateTime(po.getUpdateTime());
        return session;
    }

    private AiMessage toMessagePo(ChatMessage message) {
        AiMessage po = new AiMessage();
        po.setMessageId(message.getMessageId());
        po.setSessionId(message.getSessionId());
        po.setMessageType(message.getType());
        po.setMessageRole(message.getRole());
        po.setMessageContent(message.getContent());
        return po;
    }

    private ChatMessage toMessage(AiMessage po) {
        ChatMessage message = new ChatMessage();
        message.setMessageId(po.getMessageId());
        message.setSessionId(po.getSessionId());
        message.setType(po.getMessageType());
        message.setRole(po.getMessageRole());
        message.setContent(po.getMessageContent());
        message.setCreateTime(po.getCreateTime());
        return message;
    }
}
