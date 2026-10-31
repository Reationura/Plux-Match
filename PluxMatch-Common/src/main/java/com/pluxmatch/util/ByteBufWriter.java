package com.pluxmatch.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * 轻量 Plugin Message 字节写入器，替代 {@code com.google.common.io.ByteStreams}，
 * 使 Common 模块不依赖 Guava（Velocity 未必提供 Guava）。
 * <p>
 * 采用 {@link DataOutputStream} 的 writeUTF/writeInt 编码，与 BungeeCord / Bukkit
 * 端 Guava {@code ByteArrayDataOutput}（其底层同为 DataOutput）完全兼容。
 *
 * @author Reationura
 */
public final class ByteBufWriter {

    private final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    private final DataOutputStream out = new DataOutputStream(bos);

    private ByteBufWriter() {
    }

    public static ByteBufWriter create() {
        return new ByteBufWriter();
    }

    public ByteBufWriter utf(String value) {
        try {
            out.writeUTF(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public ByteBufWriter b(int value) {
        try {
            out.write(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public ByteBufWriter i(int value) {
        try {
            out.writeInt(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public byte[] toByteArray() {
        try {
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bos.toByteArray();
    }
}
