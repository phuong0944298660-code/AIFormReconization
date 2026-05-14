package com.aiform.id995a.ocr;

import java.io.IOException;

@FunctionalInterface
public interface PaddleOcrGateway {
  String analyze(byte[] fileBytes, int fileType) throws IOException;
}
