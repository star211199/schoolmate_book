package com.schoolmate.ws;

import lombok.Data;

import java.io.Serializable;

/**
 * WebSocket 统一帧结构。
 *
 * <p>所有双向消息都套一层信封：{@code {"type":"xxx","data":{...},"ts":1234567890}}，
 * type 决定 data 的结构。这样调试时打开 Network → WS 就能直接看懂每条帧。
 *
 * @author Albot
 */
@Data
public class WsFrame implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 帧类型，见 WsFrameType */
    private String type;

    /** 载荷 */
    private Object data;

    /** 服务端时间戳（毫秒） */
    private Long ts;

    public WsFrame() {
    }

    public WsFrame(String type, Object data) {
        this.type = type;
        this.data = data;
        this.ts = System.currentTimeMillis();
    }

    public static WsFrame of(String type, Object data) {
        return new WsFrame(type, data);
    }

    public static WsFrame of(String type) {
        return new WsFrame(type, null);
    }
}
