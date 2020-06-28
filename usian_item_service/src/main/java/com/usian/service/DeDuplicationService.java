package com.usian.service;

import com.usian.pojo.DeDuplication;

public interface DeDuplicationService {
    DeDuplication selectDeDuplicationByTxNo(String txNo);

    void insertDeDuplication(String txNo);
}
