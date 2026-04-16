package com.clone.getchu.domain.trade.enums;

import com.clone.getchu.global.exception.BusinessException;
import com.clone.getchu.global.exception.ErrorCode;

import static com.clone.getchu.global.exception.ErrorCode.INVALID_STATUS_TRANSITION;
import com.clone.getchu.global.exception.ForbiddenException;
import com.clone.getchu.domain.trade.enums.TradeRole;

/**
 * 거래 진행 상태 Enum
 *
 * - next()   : 다음 단계 상태를 반환
 * - cancel() : 취소 후 되돌아갈 상태를 반환
 *
 * 전이 흐름:
 *   SALE → (next) → RESERVED → (next) → TRADING → (next) → SOLD
 *                  ↑ (cancel) ↓          ↑ (cancel) ↓
 *                    SALE                   SALE
 */
public enum TradeStatus {

    //판매중: 예약 요청을 받아 RESERVED로 전이 가능
    SALE {
        @Override
        public TradeStatus next(TradeRole role) {
            return RESERVED;
        }

        @Override
        public TradeStatus cancel(TradeRole role) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    },

    //예약중: 거래 시작(TRADING) 또는 예약 취소(SALE) 가능
    RESERVED {
        @Override
        public TradeStatus next(TradeRole role) {
            if (role != TradeRole.SELLER) {
                throw new ForbiddenException(ErrorCode.TRADE_FORBIDDEN);
            }
            return TRADING;
        }

        @Override
        public TradeStatus cancel(TradeRole role) {
            return SALE;
        }
    },

    //거래중: 거래 완료(SOLD) 또는 취소(SALE) 가능
    TRADING {
        @Override
        public TradeStatus next(TradeRole role) {
            if (role != TradeRole.BUYER) {
                throw new ForbiddenException(ErrorCode.TRADE_FORBIDDEN);
            }
            return SOLD;
        }

        @Override
        public TradeStatus cancel(TradeRole role) {
            return SALE; //판매자와 구매자 모두 거래중 취소 가능
        }
    },

    //거래완료: 리뷰 작성 가능, 취소(SALE) 불가
    SOLD {
        @Override
        public TradeStatus next(TradeRole role) {
            return REVIEWED;
        }

        @Override
        public TradeStatus cancel(TradeRole role) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    },

    //리뷰 작성: 다음단계와 취소(SALE) 불가
    REVIEWED {
        @Override
        public TradeStatus next(TradeRole role) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        @Override
        public TradeStatus cancel(TradeRole role) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    };

    //다음 단계 상태를 반환
    public abstract TradeStatus next(TradeRole role);

    //취소 시 되돌아갈 상태를 반환합니다.
    public abstract TradeStatus cancel(TradeRole role);
}
