package com.dorm.service.card.telephone;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dorm.entity.card.telephone.TelephoneCardPO;
import com.dorm.mapper.card.telephone.TelephoneCardMapper;
import org.springframework.stereotype.Service;

@Service
public class TelephoneCardServiceImpl extends ServiceImpl<TelephoneCardMapper, TelephoneCardPO> implements TelephoneCardService {
}
