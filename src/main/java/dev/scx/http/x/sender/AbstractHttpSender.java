package dev.scx.http.x.sender;

import dev.scx.exception.ScxWrappedException;
import dev.scx.http.sender.IllegalSenderStateException;
import dev.scx.http.sender.ScxHttpReceiveException;
import dev.scx.http.sender.ScxHttpSendException;
import dev.scx.http.sender.ScxHttpSender;

import java.util.concurrent.locks.ReentrantLock;

import static dev.scx.http.x.sender.HttpSenderStatus.NOT_SENT;
import static dev.scx.http.x.sender.HttpSenderStatus.SENDING;

public abstract class AbstractHttpSender<T> implements ScxHttpSender<T> {

    protected final ReentrantLock sendLock; // 避免用户 多线程 send 搞乱状态
    protected HttpSenderStatus senderStatus;

    public AbstractHttpSender() {
        this.sendLock = new ReentrantLock();
        this.senderStatus = NOT_SENT;
    }

    @Override
    public final T send(BodyWriter bodyWriter) throws IllegalSenderStateException, ScxHttpSendException, ScxWrappedException, ScxHttpReceiveException {
        sendLock.lock();
        try {
            if (senderStatus != NOT_SENT) {
                throw new IllegalSenderStateException("Illegal Sender State : " + senderStatus);
            }
            // 设置发送状态为发送中.
            senderStatus = SENDING;
            // 后续 senderStatus 的 成功/失败 状态设置由 send0 保证.
            return send0(bodyWriter);
        } finally {
            sendLock.unlock();
        }
    }

    /// 设置发送器状态 (内部方法 不应由用户调用)
    public final void _setSenderStatus(HttpSenderStatus senderStatus) {
        this.senderStatus = senderStatus;
    }

    protected abstract T send0(BodyWriter bodyWriter) throws ScxHttpSendException, ScxWrappedException, ScxHttpReceiveException;

}
