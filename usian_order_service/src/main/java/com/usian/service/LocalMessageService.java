package com.usian.service;

import com.usian.pojo.LocalMessage;

import java.util.List;

public interface LocalMessageService {
    List<LocalMessage> selectlocalMessageByStatus(Integer status);
}
