package com.quertimizer.community.application.input;

import lombok.Getter;

@Getter
public class CommunityImageUploadInput {

    private final long size;
    private final byte[] content;

    public CommunityImageUploadInput(long size, byte[] content) {
        this.size = size;
        this.content = content == null ? new byte[0] : content.clone();
    }

    public boolean isEmpty() {
        return content.length == 0;
    }

    public byte[] getContent() {
        return content.clone();
    }
}
