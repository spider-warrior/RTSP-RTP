package com.jy.rtsp.component.decoder;

import com.jy.rtsp.cpmmon.enums.NalType;
import com.jy.rtsp.entity.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class ByteToRtpPackageDecoder extends MessageToMessageDecoder<byte[]>{

    private byte byte_00000001 = 1;
    private byte byte_00000011 = 0b00000011;
    private byte byte_00011111 = 0b00011111;
    private byte byte_01111111 = 0b01111111;

    @Override
    protected void decode(ChannelHandlerContext ctx, byte[] msg, List<Object> out) throws Exception {
        RtpPackage rtpPackage = new RtpPackage();
        IndexableBytes indexableBytes = new IndexableBytes(msg);

        // read rtp header
        RtpHeader rtpHeader = readRtpHeader(indexableBytes);
        rtpPackage.setRtpHeader(rtpHeader);
        // read extension
        if (rtpHeader.getExtension() == 1) {
            readExtension(indexableBytes);
        }
        // read nal indicator
        NalIndicator nalIndicator = readNalIndicator(indexableBytes);
        rtpPackage.setNalIndicator(nalIndicator);

        NalType nalType = NalType.getNalType(nalIndicator.getType());
        //分片
        if (NalType.FU_A == nalType || NalType.FU_B == nalType) {
            // read header
            NalHeader nalHeader = readNalHeader(indexableBytes);
            rtpPackage.setNalHeader(nalHeader);
        }
        //聚合
        else if (NalType.MTAP16 == nalType || NalType.MTAP24 == nalType) {

        }
        //单 nal 单元包
        else {

        }
        out.add(rtpPackage);
    }

    /**
     * 读取RTP header
     * */
    private RtpHeader readRtpHeader(IndexableBytes indexableBytes) {
        RtpHeader rtpHeader = new RtpHeader();
        //byte_1: version: 2 bit, padding: 1 bit, x: 1 bit, cc: 4  bit
        //byte_2: m: 1 bit, payload type: 7 bit
        //byte_3-byte_4: sequence no
        byte b1 = indexableBytes.readByte();
        byte b2 = indexableBytes.readByte();
        rtpHeader.setVersion((byte)((b1 & 0xff) >> 6));
        rtpHeader.setPadding((byte)((b1 & 0xff) >> 5 & byte_00000001));
        rtpHeader.setExtension((byte)((b1 & 0xff) >> 4 & byte_00000001));
        rtpHeader.setCsrcCount((byte)((b1 & 0xff) & 16));
        rtpHeader.setMark((byte)((b2&0xff) >> 7));
        rtpHeader.setPayloadType((byte)((b2&0xff) & byte_01111111));
        rtpHeader.setSequenceNo(indexableBytes.readShort());
        rtpHeader.setTimestamp(indexableBytes.readInt());
        rtpHeader.setSynchronizedCsrc(indexableBytes.readInt());
        int[] csrc = new int[rtpHeader.getCsrcCount()];
        for (int i=0; i<csrc.length; i++) {
            csrc[i] = indexableBytes.readInt();
        }
        rtpHeader.setSpecialCsrcs(csrc);
        return rtpHeader;
    }
    /**
     * 读取扩展
     * */
    private void readExtension(IndexableBytes indexableBytes) {

    }

    /**
     * 读取NAL indicator
     * */
    private NalIndicator readNalIndicator(IndexableBytes indexableBytes) {
        NalIndicator indicator = new NalIndicator();
        byte byte_1 = indexableBytes.readByte();
        // f: bit 1, nri: 2 bit, type: 5 bit
        indicator.setF((byte)((byte_1&0xff) >> 7));
        indicator.setNri((byte)(((byte_1&0xff) >> 5) & byte_00000011));
        indicator.setType((byte)((byte_1&0xff) & byte_00011111));
        return indicator;
    }

    private NalHeader readNalHeader(IndexableBytes indexableBytes) {
        NalHeader nalHeader = new NalHeader();
        byte byte_1 = indexableBytes.readByte();
        // S: 1 bit, E: 1 bit, R: 1 bit, type: 5 bit
        nalHeader.setStart((byte)((byte_1&0xff) >> 7));
        nalHeader.setEnd((byte)((byte_1&0xff) >> 6  & byte_00000001));
        nalHeader.setR((byte)((byte_1&0xff) >> 5  & byte_00000001));
        nalHeader.setType((byte)((byte_1&0xff) & byte_00011111));
        return nalHeader;
    }

}